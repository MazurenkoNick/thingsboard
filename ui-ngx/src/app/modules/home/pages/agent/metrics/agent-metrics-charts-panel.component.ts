///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import * as echarts from 'echarts/core';
import { AttributeService } from '@core/http/attribute.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityId } from '@shared/models/id/entity-id';
import {
  ECharts,
  EChartsOption,
  echartsModule
} from '@home/components/widget/lib/chart/echarts-widget.models';
import { AgentMetricsSubscription } from '@home/pages/agent/util/agent-metrics-subscription';
import {
  BYTES_EMPTY_AXIS_MAX,
  CPU_EMPTY_AXIS_MAX,
  emptyAxisMax,
  formatBytes,
  METRIC_KEY_CPU_PERCENT,
  METRIC_KEY_MEMORY_BYTES,
  MetricsSnapshot
} from '@home/pages/agent/util/agent-metrics';
import { AggregationType } from '@shared/models/time/time.models';
import { TsValue } from '@shared/models/query/query.models';

const WINDOW_MS = 10 * 60 * 1000;
const HISTORY_LIMIT = 600;

interface Point {
  ts: number;
  value: number;
}

@Component({
  selector: 'tb-agent-metrics-charts-panel',
  templateUrl: './agent-metrics-charts-panel.component.html',
  styleUrls: ['./agent-metrics-charts-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AgentMetricsChartsPanelComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() entityId: EntityId | null = null;
  @Input() agentId: EntityId | null = null;
  @Input() title = '';
  @Input() closeable = false;

  @Output() close = new EventEmitter<void>();

  @ViewChild('cpuChart', { static: false }) cpuChartRef!: ElementRef<HTMLDivElement>;
  @ViewChild('memChart', { static: false }) memChartRef!: ElementRef<HTMLDivElement>;
  @ViewChild('panelRoot', { static: false }) panelRootRef!: ElementRef<HTMLDivElement>;

  private destroy$ = new Subject<void>();
  private subscription: AgentMetricsSubscription;
  private cpuChart: ECharts | null = null;
  private memChart: ECharts | null = null;
  private cpuPoints: Point[] = [];
  private memPoints: Point[] = [];
  private historyLoaded = false;
  private viewReady = false;
  private resizeObserver: ResizeObserver | null = null;

  constructor(private attributeService: AttributeService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone) {
    echartsModule.init();
    this.subscription = new AgentMetricsSubscription(telemetryWsService, zone);
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.initCharts();
    this.installResizeObserver();
    this.startStreaming();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.viewReady) {
      return;
    }
    if (changes['entityId'] || changes['agentId']) {
      this.resetState();
      this.startStreaming();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.subscription.tearDown();
    this.resizeObserver?.disconnect();
    this.cpuChart?.dispose();
    this.memChart?.dispose();
  }

  onCloseClick(): void {
    this.close.emit();
  }

  private initCharts(): void {
    this.zone.runOutsideAngular(() => {
      this.cpuChart = echarts.init(this.cpuChartRef.nativeElement, null, { renderer: 'canvas' });
      this.memChart = echarts.init(this.memChartRef.nativeElement, null, { renderer: 'canvas' });
      this.cpuChart.setOption(this.baseOption(
        'CPU usage',
        'CPU',
        '#1976d2',
        (v) => `${(v as number).toFixed(2)}%`,
        (v: number) => `${v}%`,
        CPU_EMPTY_AXIS_MAX
      ));
      this.memChart.setOption(this.baseOption(
        'Memory usage',
        'Memory',
        '#7b1fa2',
        (v) => formatBytes(v as number),
        (v: number) => formatBytes(v, 0),
        BYTES_EMPTY_AXIS_MAX
      ));
    });
  }

  private installResizeObserver(): void {
    if (typeof ResizeObserver === 'undefined' || !this.panelRootRef) {
      return;
    }
    this.resizeObserver = new ResizeObserver(() => {
      this.cpuChart?.resize();
      this.memChart?.resize();
    });
    this.resizeObserver.observe(this.panelRootRef.nativeElement);
  }

  private resetState(): void {
    this.subscription.tearDown();
    this.cpuPoints = [];
    this.memPoints = [];
    this.historyLoaded = false;
    this.applyToCharts();
  }

  private startStreaming(): void {
    if (!this.entityId) {
      return;
    }
    this.loadHistory();
    this.subscription.snapshot$.pipe(takeUntil(this.destroy$)).subscribe(snap => this.onSnapshot(snap));
    this.subscription.subscribe(this.entityId, this.agentId);
  }

  private loadHistory(): void {
    const entity = this.entityId;
    if (!entity) {
      return;
    }
    const endTs = Date.now();
    const startTs = endTs - WINDOW_MS;
    this.attributeService.getEntityTimeseries(
      entity, [METRIC_KEY_CPU_PERCENT, METRIC_KEY_MEMORY_BYTES],
      startTs, endTs, HISTORY_LIMIT, AggregationType.NONE, undefined
    ).pipe(takeUntil(this.destroy$)).subscribe(data => {
      this.cpuPoints = parsePoints(data[METRIC_KEY_CPU_PERCENT]);
      this.memPoints = parsePoints(data[METRIC_KEY_MEMORY_BYTES]);
      this.historyLoaded = true;
      this.applyToCharts();
    });
  }

  private onSnapshot(snap: MetricsSnapshot): void {
    if (!this.historyLoaded) {
      return;
    }
    const now = Date.now();
    if (snap.cpuPercent != null) {
      this.cpuPoints.push({ ts: now, value: snap.cpuPercent });
    }
    if (snap.memoryBytes != null) {
      this.memPoints.push({ ts: now, value: snap.memoryBytes });
    }
    this.trimPoints(now);
    this.applyToCharts();
  }

  private trimPoints(now: number): void {
    const cutoff = now - WINDOW_MS;
    this.cpuPoints = this.cpuPoints.filter(p => p.ts >= cutoff);
    this.memPoints = this.memPoints.filter(p => p.ts >= cutoff);
  }

  private applyToCharts(): void {
    if (!this.cpuChart || !this.memChart) {
      return;
    }
    const cpuSeries = this.cpuPoints.map(p => [p.ts, p.value]);
    const memSeries = this.memPoints.map(p => [p.ts, p.value]);
    const now = Date.now();
    const windowStart = now - WINDOW_MS;
    this.zone.runOutsideAngular(() => {
      this.cpuChart!.setOption({
        xAxis: { min: windowStart, max: now },
        series: [{ name: 'CPU', data: cpuSeries }]
      });
      this.memChart!.setOption({
        xAxis: { min: windowStart, max: now },
        series: [{ name: 'Memory', data: memSeries }]
      });
    });
  }

  private baseOption(title: string,
                     seriesName: string,
                     color: string,
                     tooltipFormatter: (v: number) => string,
                     yAxisFormatter: (v: number) => string,
                     emptyMax: number): EChartsOption {
    const rgba = (alpha: number) => {
      const r = parseInt(color.slice(1, 3), 16);
      const g = parseInt(color.slice(3, 5), 16);
      const b = parseInt(color.slice(5, 7), 16);
      return `rgba(${r},${g},${b},${alpha})`;
    };
    return {
      grid: { left: 64, right: 16, top: 44, bottom: 32 },
      title: {
        text: title,
        textStyle: { fontSize: 15, fontWeight: 700, color: 'rgba(0,0,0,0.86)' },
        left: 12, top: 10
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const p = Array.isArray(params) ? params[0] : params;
          if (!p?.value) { return ''; }
          const [ts, v] = p.value as [number, number];
          const time = new Date(ts).toLocaleTimeString();
          return `<div style="font-size:11px;color:rgba(0,0,0,0.6)">${seriesName} · ${time}</div>` +
                 `<div style="font-weight:600;font-size:13px">${tooltipFormatter(v)}</div>`;
        }
      },
      xAxis: {
        type: 'time',
        axisLine: { lineStyle: { color: 'rgba(0,0,0,0.16)' } },
        axisLabel: { color: 'rgba(0,0,0,0.6)', fontSize: 11 },
        splitLine: { show: false }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: emptyAxisMax(emptyMax),
        name: seriesName,
        nameLocation: 'middle',
        nameGap: 48,
        nameTextStyle: { color: 'rgba(0,0,0,0.6)', fontSize: 11, fontWeight: 600 },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          color: 'rgba(0,0,0,0.6)',
          fontSize: 11,
          formatter: yAxisFormatter
        },
        splitLine: { lineStyle: { color: 'rgba(0,0,0,0.06)' } }
      },
      dataZoom: [{ type: 'inside', yAxisIndex: 0, filterMode: 'none', zoomOnMouseWheel: true, moveOnMouseMove: true, throttle: 30 }],
      animation: false,
      series: [{
        type: 'line',
        name: seriesName,
        showSymbol: false,
        smooth: 0.2,
        lineStyle: { width: 2, color },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: rgba(0.25) },
              { offset: 1, color: rgba(0) }
            ]
          }
        },
        data: []
      }]
    } as EChartsOption;
  }
}

function parsePoints(entries: Array<TsValue> | undefined): Point[] {
  if (!entries) { return []; }
  const out: Point[] = [];
  for (const entry of entries) {
    const v = typeof entry.value === 'number' ? entry.value as number : parseFloat(entry.value);
    if (Number.isFinite(v)) {
      out.push({ ts: entry.ts, value: v });
    }
  }
  return out.sort((a, b) => a.ts - b.ts);
}

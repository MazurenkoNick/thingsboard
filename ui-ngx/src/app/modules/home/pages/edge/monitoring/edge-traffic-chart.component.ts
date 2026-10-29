// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  ViewChild
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as echarts from 'echarts/core';
import { ECharts, EChartsOption, echartsModule } from '@home/components/widget/lib/chart/echarts-widget.models';
import { EdgeStatsMetric, formatCount } from '@home/pages/edge/monitoring/edge-monitoring.models';

/** One direction's aggregated stats: a value per bucket (null for a lag bucket with no report). */
export interface EdgeTrafficChartData {
  start: number;
  end: number;
  interval: number;
  series: {[metric in EdgeStatsMetric]?: Array<number | null>};
}

interface SeriesInfo {
  metric: EdgeStatsMetric;
  label: string;
  color: string;
  swatch: string;
}

const seriesInfos: SeriesInfo[] = [
  {metric: EdgeStatsMetric.ADDED, label: 'edge.monitoring.series-added', color: '#305680', swatch: 'background: #305680'},
  {metric: EdgeStatsMetric.PUSHED, label: 'edge.monitoring.series-pushed', color: '#6fb1ec', swatch: 'background: #6fb1ec'},
  {metric: EdgeStatsMetric.LAG, label: 'edge.monitoring.series-lag', color: '#7b3fa0',
    swatch: 'height: 0; border-top: 2px dashed #7b3fa0'},
  {metric: EdgeStatsMetric.TMP_FAILED, label: 'edge.monitoring.series-tmp-failed', color: '#e8891c', swatch: 'background: #e8891c'},
  {metric: EdgeStatsMetric.PERMANENTLY_FAILED, label: 'edge.monitoring.series-permanently-failed', color: '#b3261e',
    swatch: 'background: #b3261e'}
];

const AXIS_LABEL = {color: '#5f6b7a', fontSize: 10};

@Component({
  selector: 'tb-edge-traffic-chart',
  templateUrl: './edge-traffic-chart.component.html',
  styleUrls: ['./edge-traffic-chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class EdgeTrafficChartComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() title: string;
  @Input() hint: string;
  @Input() data: EdgeTrafficChartData;

  @ViewChild('chart', {static: true}) chartRef: ElementRef<HTMLElement>;

  readonly seriesInfos = seriesInfos;
  hidden = new Set<EdgeStatsMetric>();
  totals: {[metric: string]: string} = {};

  private chart: ECharts;
  private resizeObserver: ResizeObserver;

  constructor(private zone: NgZone,
              private translate: TranslateService) {
    echartsModule.init();
  }

  ngAfterViewInit() {
    this.zone.runOutsideAngular(() => {
      this.chart = echarts.init(this.chartRef.nativeElement, null, {renderer: 'canvas'});
      this.resizeObserver = new ResizeObserver(() => this.chart?.resize());
      this.resizeObserver.observe(this.chartRef.nativeElement);
    });
    this.render();
  }

  ngOnChanges() {
    this.updateTotals();
    this.render();
  }

  ngOnDestroy() {
    this.resizeObserver?.disconnect();
    this.chart?.dispose();
  }

  toggle(metric: EdgeStatsMetric) {
    if (this.hidden.has(metric)) {
      this.hidden.delete(metric);
    } else {
      this.hidden.add(metric);
    }
    this.render();
  }

  private updateTotals() {
    this.totals = {};
    if (!this.data) {
      return;
    }
    for (const info of seriesInfos) {
      const values = this.data.series[info.metric] ?? [];
      if (info.metric === EdgeStatsMetric.LAG) {
        const last = [...values].reverse().find(v => v !== null && v !== undefined);
        this.totals[info.metric] = last !== undefined ? formatCount(last) : '—';
      } else {
        this.totals[info.metric] = formatCount(values.reduce((acc, v) => acc + (v ?? 0), 0));
      }
    }
  }

  private render() {
    if (!this.chart || !this.data) {
      return;
    }
    const {start, end, interval} = this.data;
    const points = (metric: EdgeStatsMetric) =>
      (this.data.series[metric] ?? []).map((v, i) => [Math.min(start + i * interval + interval / 2, end), v]);
    const visible = (metric: EdgeStatsMetric) => !this.hidden.has(metric);
    const name = (metric: EdgeStatsMetric) => this.translate.instant(seriesInfos.find(s => s.metric === metric).label);
    const line = (metric: EdgeStatsMetric, color: string, yAxisIndex: number, dashed = false) => ({
      type: 'line', name: name(metric), data: visible(metric) ? points(metric) : [], xAxisIndex: 0, yAxisIndex,
      showSymbol: false, connectNulls: true, color,
      lineStyle: {width: 2, color, type: dashed ? [5, 3] : 'solid'}
    });
    const failureTotals = (this.data.series[EdgeStatsMetric.TMP_FAILED] ?? [])
      .map((v, i) => (v ?? 0) + (this.data.series[EdgeStatsMetric.PERMANENTLY_FAILED]?.[i] ?? 0));
    const maxFailures = Math.max(0, ...failureTotals);
    // Nothing is drawn for a range without failures, so the bars never clutter a healthy chart.
    const bar = (metric: EdgeStatsMetric, color: string) => ({
      type: 'bar', name: name(metric), data: visible(metric) && maxFailures > 0 ? points(metric) : [], xAxisIndex: 0, yAxisIndex: 2,
      stack: 'failures', color, barMaxWidth: 8, z: 1
    });
    const option: EChartsOption = {
      animation: false,
      grid: {left: 48, right: 48, top: 12, bottom: 28},
      tooltip: {trigger: 'axis', valueFormatter: v => v === null || v === undefined ? '—' : formatCount(v as number)},
      xAxis: {
        type: 'time', min: start, max: end, splitLine: {show: false},
        axisLine: {lineStyle: {color: '#d5dbe3'}}, axisTick: {show: false},
        axisLabel: {...AXIS_LABEL, hideOverlap: true}
      },
      yAxis: [
        {type: 'value', min: 0, minInterval: 1, splitNumber: 2, axisLabel: {...AXIS_LABEL, formatter: v => formatCount(v)},
          splitLine: {lineStyle: {color: '#edf0f4'}}},
        {type: 'value', min: 0, minInterval: 1, splitNumber: 2, position: 'right',
          axisLabel: {...AXIS_LABEL, color: '#7b3fa0', formatter: v => formatCount(v)}, splitLine: {show: false}},
        // Own hidden scale, so a handful of failures stays visible next to thousands of messages:
        // the tallest failure bar reaches a quarter of the chart height; exact counts are in the tooltip.
        {type: 'value', show: false, min: 0, max: Math.max(1, maxFailures) * 4}
      ],
      series: [
        line(EdgeStatsMetric.ADDED, '#305680', 0),
        line(EdgeStatsMetric.PUSHED, '#6fb1ec', 0),
        line(EdgeStatsMetric.LAG, '#7b3fa0', 1, true),
        bar(EdgeStatsMetric.TMP_FAILED, '#e8891c'),
        bar(EdgeStatsMetric.PERMANENTLY_FAILED, '#b3261e')
      ] as any
    };
    this.zone.runOutsideAngular(() => this.chart.setOption(option, {notMerge: true}));
  }
}

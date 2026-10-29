// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { catchError, of } from 'rxjs';
import { AgentService } from '@core/http/agent.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AttributeScope, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { PageLink } from '@shared/models/page/page-link';
import { AgentAppUnit, AgentAppUnitType } from '@shared/models/agent.models';
import { AgentId } from '@shared/models/id/agent-id';
import { LogViewerComponent } from '@home/pages/agent/log-viewer/log-viewer.component';
import { AGENTS_URL, EdgeManagedBy } from '@home/pages/edge/monitoring/edge-monitoring.models';

enum LogLevelFilter {
  ALL = 'all',
  WARN = 'warn',
  ERROR = 'error'
}

const LEVEL_PATTERN = /\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL)\b/;
const MAX_BUFFERED_LINES = 2000;
const MAX_UNITS = 50;
// The edge's own container, as opposed to its database or queue containers in the same application.
const EDGE_UNIT_PATTERN = /edge/i;

interface BufferedLine {
  text: string;
  level: string;
}

@Component({
  selector: 'tb-edge-logs-panel',
  templateUrl: './edge-logs-panel.component.html',
  styleUrls: ['./edge-logs-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class EdgeLogsPanelComponent implements OnChanges, OnDestroy {

  // undefined while the agent application lookup is running, null when the edge isn't managed by an agent.
  @Input() managedBy: EdgeManagedBy | null;

  @Output() installWithAgent = new EventEmitter<void>();

  @ViewChild(LogViewerComponent) logViewer: LogViewerComponent;

  readonly levelFilters = [
    {value: LogLevelFilter.ALL, label: 'edge.monitoring.log-level-all'},
    {value: LogLevelFilter.WARN, label: 'edge.monitoring.log-level-warn'},
    {value: LogLevelFilter.ERROR, label: 'edge.monitoring.log-level-error'}
  ];
  readonly agentsUrl = AGENTS_URL;

  level = LogLevelFilter.ALL;
  units: AgentAppUnit[] = [];
  unit: AgentAppUnit;
  unitsLoaded = false;
  agentOnline = true;

  private buffer: BufferedLine[] = [];
  private lastLevel = 'INFO';
  private lastSeenSeq = 0;
  private logSub: TelemetrySubscriber;
  private agentActiveSub: TelemetrySubscriber;

  constructor(private agentService: AgentService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private cd: ChangeDetectorRef) {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.managedBy && changes.managedBy.currentValue?.appId !== changes.managedBy.previousValue?.appId) {
      this.tearDown();
      this.units = [];
      this.unit = null;
      this.unitsLoaded = false;
      if (this.managedBy) {
        this.loadUnits(this.managedBy.appId);
        this.subscribeAgentActive(this.managedBy.agentId);
      }
    }
  }

  ngOnDestroy() {
    this.tearDown();
  }

  get logViewerUrl(): string {
    return `${AGENTS_URL}/${this.managedBy.agentId}/applications/${this.managedBy.appId}/units/${this.unit.id.id}/logs`;
  }

  selectUnit(unit: AgentAppUnit) {
    if (unit?.id.id !== this.unit?.id.id) {
      this.tearDownLogs();
      this.unit = unit;
      this.buffer = [];
      this.lastSeenSeq = 0;
      this.logViewer?.clear();
      this.subscribeLogs();
    }
  }

  setLevel(level: LogLevelFilter) {
    if (this.level !== level) {
      this.level = level;
      this.logViewer?.clear();
      this.logViewer?.appendLines(this.buffer.filter(line => this.matchesLevel(line.level)).map(line => line.text));
    }
  }

  private loadUnits(appId: string) {
    this.agentService.getAgentAppUnits(appId, new PageLink(MAX_UNITS), AgentAppUnitType.CONTAINER,
      {ignoreErrors: true, ignoreLoading: true}).pipe(
      catchError(() => of(null))
    ).subscribe(page => {
      this.units = (page?.data ?? []).filter(unit => unit.type === AgentAppUnitType.CONTAINER);
      this.unit = this.units.find(unit => EDGE_UNIT_PATTERN.test(unit.identifier)) ?? this.units[0] ?? null;
      this.unitsLoaded = true;
      this.cd.markForCheck();
      // The log viewer renders after this change detection pass.
      setTimeout(() => this.subscribeLogs());
    });
  }

  private subscribeLogs() {
    if (!this.unit || !this.agentOnline || this.logSub) {
      return;
    }
    this.logSub = TelemetrySubscriber.createLogsSubscription(this.telemetryWsService, this.unit.id, this.zone, this.lastSeenSeq);
    this.logSub.logs$.subscribe(update => {
      if (!update) {
        return;
      }
      this.zone.run(() => {
        if (update.droppedLines > 0) {
          this.logViewer?.recordDropped(update.droppedLines);
        }
        if (update.evictedChunks > 0) {
          this.logViewer?.appendGap(update.evictedChunks);
        }
        if (update.lines?.length) {
          const lines = update.lines.map(text => this.toBufferedLine(text));
          this.buffer.push(...lines);
          if (this.buffer.length > MAX_BUFFERED_LINES) {
            this.buffer.splice(0, this.buffer.length - MAX_BUFFERED_LINES);
          }
          const visible = lines.filter(line => this.matchesLevel(line.level)).map(line => line.text);
          if (visible.length) {
            this.logViewer?.appendLines(visible);
          }
        }
        if (update.latestSeq > this.lastSeenSeq) {
          this.lastSeenSeq = update.latestSeq;
        }
      });
    });
    this.logSub.subscribe();
  }

  // Like the agent log page: the stream stops while the agent is offline and resumes from the last seen line.
  private subscribeAgentActive(agentId: string) {
    if (!agentId) {
      return;
    }
    this.agentActiveSub = TelemetrySubscriber.createEntityAttributesSubscription(
      this.telemetryWsService, new AgentId(agentId), AttributeScope.SERVER_SCOPE, this.zone, ['active']);
    this.agentActiveSub.data$.subscribe(update => {
      const entries = update?.data?.['active'];
      if (!entries?.length) {
        return;
      }
      const value = entries[0][1];
      const active = value === true || value === 'true';
      this.zone.run(() => {
        this.agentOnline = active;
        this.cd.markForCheck();
        if (active) {
          this.subscribeLogs();
        } else {
          this.tearDownLogs();
        }
      });
    });
    this.agentActiveSub.subscribe();
  }

  // Lines without a level (e.g. stack trace lines) belong to the previous log record.
  private toBufferedLine(text: string): BufferedLine {
    const match = LEVEL_PATTERN.exec(text);
    if (match) {
      this.lastLevel = match[1] === 'WARNING' ? 'WARN' : match[1];
    }
    return {text, level: this.lastLevel};
  }

  private matchesLevel(level: string): boolean {
    switch (this.level) {
      case LogLevelFilter.WARN:
        return level === 'WARN' || level === 'ERROR' || level === 'FATAL';
      case LogLevelFilter.ERROR:
        return level === 'ERROR' || level === 'FATAL';
      default:
        return true;
    }
  }

  private tearDownLogs() {
    if (this.logSub) {
      this.logSub.unsubscribe();
      this.logSub.complete();
      this.logSub = null;
    }
  }

  private tearDown() {
    this.tearDownLogs();
    if (this.agentActiveSub) {
      this.agentActiveSub.unsubscribe();
      this.agentActiveSub.complete();
      this.agentActiveSub = null;
    }
    this.buffer = [];
    this.lastSeenSeq = 0;
  }
}

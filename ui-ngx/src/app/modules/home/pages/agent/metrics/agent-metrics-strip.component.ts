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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import {
  diskHostPercent,
  formatBytes,
  formatCpu,
  formatDiskWithMax,
  formatMemoryWithMax,
  formatUpdatedAt,
  memoryHostPercent,
  MetricsSnapshot
} from '@home/pages/agent/util/agent-metrics';

@Component({
  selector: 'tb-agent-metrics-strip',
  templateUrl: './agent-metrics-strip.component.html',
  styleUrls: ['./agent-metrics-strip.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AgentMetricsStripComponent {

  @Input() snapshot: MetricsSnapshot | null = null;
  @Input() showChartsButton = false;
  @Input() chartsActive = false;
  @Input() compact = false;
  @Input() aggregated = true;

  @Output() toggleCharts = new EventEmitter<void>();

  get cpuLabel(): string {
    return formatCpu(this.snapshot?.cpuPercent ?? null, this.snapshot?.onlineCpus ?? null);
  }

  get cpuTooltip(): string | null {
    const snap = this.snapshot;
    if (!snap || snap.cpuPercent == null) {
      return null;
    }
    const head = snap.onlineCpus == null
      ? `cpuPercent ${snap.cpuPercent.toFixed(2)}%`
      : `cpuPercent ${snap.cpuPercent.toFixed(2)}% of ${snap.onlineCpus} cores (cap ${(snap.onlineCpus * 100).toFixed(0)}%)`;
    return joinLines(head, formatUpdatedAt(snap.cpuPercentTs));
  }

  get memLabel(): string {
    return formatMemoryWithMax(this.snapshot?.memoryBytes ?? null, this.snapshot?.hostMemoryBytes ?? null);
  }

  get memTooltip(): string | null {
    const snap = this.snapshot;
    if (!snap || snap.memoryBytes == null) {
      return null;
    }
    const pct = memoryHostPercent(snap.memoryBytes, snap.hostMemoryBytes);
    const head = pct == null
      ? formatBytes(snap.memoryBytes)
      : `${formatBytes(snap.memoryBytes)} of ${formatBytes(snap.hostMemoryBytes!)} (${pct.toFixed(1)}%)`;
    return joinLines(head, formatUpdatedAt(snap.memoryBytesTs));
  }

  get hasDisk(): boolean {
    return this.snapshot?.diskBytes != null;
  }

  get diskLabel(): string {
    return formatDiskWithMax(this.snapshot?.diskBytes ?? null, this.snapshot?.hostDiskTotal ?? null);
  }

  get diskTooltip(): string | null {
    const snap = this.snapshot;
    if (!snap || snap.diskBytes == null) {
      return null;
    }
    const pct = diskHostPercent(snap.diskBytes, snap.hostDiskTotal);
    const head = pct == null
      ? formatBytes(snap.diskBytes)
      : `${formatBytes(snap.diskBytes)} of ${formatBytes(snap.hostDiskTotal!)} (${pct.toFixed(1)}%)`;
    return joinLines(head, formatUpdatedAt(snap.diskBytesTs));
  }

  get hasVolume(): boolean {
    return this.snapshot?.volumeBytes != null;
  }

  get volumeLabel(): string {
    return formatBytes(this.snapshot?.volumeBytes ?? null);
  }

  get volumeTooltip(): string | null {
    const snap = this.snapshot;
    if (!snap || snap.volumeBytes == null) {
      return null;
    }
    return joinLines(formatBytes(snap.volumeBytes), formatUpdatedAt(snap.volumeBytesTs));
  }

  onToggleCharts(event: Event): void {
    event.stopPropagation();
    this.toggleCharts.emit();
  }
}

function joinLines(...parts: (string | null | undefined)[]): string {
  return parts.filter((p): p is string => !!p).join(' · ');
}

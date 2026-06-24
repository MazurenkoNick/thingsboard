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
  ChangeDetectorRef,
  Component,
  Input,
  NgZone,
  OnDestroy,
  Pipe,
  PipeTransform,
  ViewChild
} from '@angular/core';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Subject } from 'rxjs';
import { auditTime, takeUntil } from 'rxjs/operators';

export interface LogEntry {
  kind: 'line' | 'gap';
  text: string;
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, c => {
    switch (c) {
      case '&': return '&amp;';
      case '<': return '&lt;';
      case '>': return '&gt;';
      case '"': return '&quot;';
      default:  return '&#39;';
    }
  });
}

function escapeRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

@Pipe({ name: 'tbLogHighlight', pure: true, standalone: false })
export class LogHighlightPipe implements PipeTransform {
  constructor(private readonly sanitizer: DomSanitizer) {}
  transform(text: string, needle: string | null | undefined): SafeHtml {
    const escaped = escapeHtml(text ?? '');
    if (!needle) {
      return this.sanitizer.bypassSecurityTrustHtml(escaped);
    }
    const re = new RegExp(escapeRegex(needle), 'gi');
    const out = escaped.replace(re, m => `<mark class="tb-log-mark">${m}</mark>`);
    return this.sanitizer.bypassSecurityTrustHtml(out);
  }
}

@Component({
  selector: 'tb-log-viewer',
  templateUrl: './log-viewer.component.html',
  styleUrls: ['./log-viewer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class LogViewerComponent implements AfterViewInit, OnDestroy {

  static readonly MAX_LINES = 2000;
  private static readonly STICK_THRESHOLD_PX = 24;

  @ViewChild(CdkVirtualScrollViewport, { static: true })
  viewport: CdkVirtualScrollViewport;

  @Input() filenamePrefix?: string;

  lines: LogEntry[] = [];

  paused = false;
  pendingCount = 0;
  droppedTotal = 0;

  followTail = true;
  search = '';

  private pendingBuffer: LogEntry[] = [];
  private readonly destroy$ = new Subject<void>();
  private scrollAfterRender = false;

  constructor(private readonly cdr: ChangeDetectorRef,
              private readonly zone: NgZone) {}

  ngAfterViewInit(): void {
    this.viewport.elementScrolled()
      .pipe(auditTime(16), takeUntil(this.destroy$))
      .subscribe(() => this.zone.run(() => this.recomputeFollow()));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  appendLine(text: string): void {
    if (this.paused) {
      this.pendingBuffer.push({ kind: 'line', text });
      this.pendingCount = this.pendingBuffer.length;
      this.cdr.markForCheck();
      return;
    }
    this.lines = [...this.lines, { kind: 'line', text }];
    this.trim();
    this.scheduleStickyScroll();
    this.cdr.markForCheck();
  }

  appendLines(texts: string[]): void {
    if (!texts || texts.length === 0) {
      return;
    }
    if (this.paused) {
      for (const text of texts) {
        this.pendingBuffer.push({ kind: 'line', text });
      }
      this.pendingCount = this.pendingBuffer.length;
      this.cdr.markForCheck();
      return;
    }
    const newEntries: LogEntry[] = new Array(texts.length);
    for (let i = 0; i < texts.length; i++) {
      newEntries[i] = { kind: 'line', text: texts[i] };
    }
    this.lines = this.lines.length === 0 ? newEntries : this.lines.concat(newEntries);
    this.trim();
    this.scheduleStickyScroll();
    this.cdr.markForCheck();
  }

  recordDropped(count: number): void {
    if (!count || count <= 0) {
      return;
    }
    this.droppedTotal += count;
    this.cdr.markForCheck();
  }

  appendGap(evictedChunks: number): void {
    const label = `${evictedChunks} log ${evictedChunks === 1 ? 'chunk' : 'chunks'} dropped (buffer limit reached)`;
    const entry: LogEntry = { kind: 'gap', text: label };
    if (this.paused) {
      this.pendingBuffer.push(entry);
      this.pendingCount = this.pendingBuffer.length;
      this.cdr.markForCheck();
      return;
    }
    this.lines = [...this.lines, entry];
    this.trim();
    this.scheduleStickyScroll();
    this.cdr.markForCheck();
  }

  clear(): void {
    this.lines = [];
    this.pendingBuffer = [];
    this.pendingCount = 0;
    this.droppedTotal = 0;
    this.cdr.markForCheck();
  }

  togglePause(): void {
    if (this.paused) {
      if (this.pendingBuffer.length) {
        this.lines = [...this.lines, ...this.pendingBuffer];
        this.trim();
        this.pendingBuffer = [];
        this.pendingCount = 0;
        this.scheduleStickyScroll();
      }
      this.paused = false;
    } else {
      this.paused = true;
    }
    this.cdr.markForCheck();
  }

  toggleFollow(): void {
    this.followTail = !this.followTail;
    if (this.followTail) {
      this.jumpToLatest();
    }
    this.cdr.markForCheck();
  }

  jumpToLatest(): void {
    if (!this.viewport || !this.lines.length) { return; }
    this.viewport.scrollToIndex(this.lines.length - 1, 'auto');
    this.followTail = true;
    this.cdr.markForCheck();
  }

  onSearchInput(event: Event): void {
    this.search = (event.target as HTMLInputElement).value ?? '';
    this.cdr.markForCheck();
  }

  clearSearch(): void {
    this.search = '';
    this.cdr.markForCheck();
  }

  async copyAll(): Promise<void> {
    if (!this.lines.length || !navigator.clipboard) { return; }
    const payload = this.lines.map(l => l.text).join('\n');
    try {
      await navigator.clipboard.writeText(payload);
    } catch {
      // noop
    }
  }

  download(): void {
    if (!this.lines.length) { return; }
    const payload = this.lines.map(l => l.text).join('\n') + '\n';
    const blob = new Blob([payload], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const ts = new Date().toISOString().replace(/[:.]/g, '-').replace(/-\d{3}Z$/, 'Z');
    const safePrefix = (this.filenamePrefix || 'logs').replace(/[^a-zA-Z0-9._-]+/g, '_');
    a.download = `${safePrefix}-${ts}.log`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  trackByIndex = (i: number, _entry: LogEntry): number => i;

  private trim(): void {
    if (this.lines.length > LogViewerComponent.MAX_LINES) {
      this.lines = this.lines.slice(this.lines.length - LogViewerComponent.MAX_LINES);
    }
  }

  private scheduleStickyScroll(): void {
    if (!this.followTail || this.scrollAfterRender) { return; }
    this.scrollAfterRender = true;
    this.zone.runOutsideAngular(() => {
      requestAnimationFrame(() => {
        this.scrollAfterRender = false;
        if (this.followTail && this.viewport && this.lines.length) {
          this.viewport.scrollToIndex(this.lines.length - 1, 'auto');
        }
      });
    });
  }

  private recomputeFollow(): void {
    if (!this.viewport) { return; }
    const distanceFromBottom = this.viewport.measureScrollOffset('bottom');
    const atBottom = distanceFromBottom <= LogViewerComponent.STICK_THRESHOLD_PX;
    if (atBottom !== this.followTail) {
      this.followTail = atBottom;
      this.cdr.markForCheck();
    }
  }
}

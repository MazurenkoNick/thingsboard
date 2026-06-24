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
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MatCheckboxDefaultOptions, MAT_CHECKBOX_DEFAULT_OPTIONS } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { Sort, SortDirection } from '@angular/material/sort';
import { DatePipe } from '@angular/common';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import {
  AgentAppEventActionType,
  agentAppEventActionTypeTranslationMap,
  AgentAppProfile,
  AgentAppProfileRelationInfo,
  AgentApplicationType,
  AgentBulkAction,
  AgentBulkActionStatus,
  agentBulkActionStatusTranslationMap,
  AgentProfileInfo,
  AgentProvisionType
} from '@shared/models/agent.models';
import {
  AgentProfileAssignProfileDialogComponent,
  AgentProfileAssignProfileDialogData
} from '@home/pages/agent/dialog/agent-profile-assign-profile-dialog.component';
import {
  AgentProfileBulkActionDialogComponent,
  AgentProfileBulkActionDialogData
} from '@home/pages/agent/dialog/agent-profile-bulk-action-dialog.component';

interface ProfileRow {
  profile: AgentAppProfileRelationInfo;
  expanded: boolean;
  loading: boolean;
  loaded: boolean;
  actions: AgentBulkAction[];
  actionsPageIndex: number;
  actionsPageSize: number;
  actionsTotal: number;
}

const DEFAULT_ACTIONS_PAGE_SIZE = 5;
const ACTIONS_PAGE_SIZE_OPTIONS = [5, 10, 25];

interface ActionMeta {
  icon: string;
  label: string;
}

interface StatusMeta {
  color: string;
  icon: string;
  label: string;
}

const ACTION_ICONS: Record<string, string> = {
  RESTART:  'restart_alt',
  UPDATE:   'sync_alt',
  UPGRADE:  'arrow_upward',
  DELETE:   'delete',
  ROLLBACK: 'undo'
};

const STATUS_CONFIG: Record<string, { color: string; icon: string }> = {
  QUEUED:       { color: '#616161', icon: 'schedule' },
  IN_PROGRESS:  { color: '#1565c0', icon: 'autorenew' },
  STARTED:      { color: '#2e7d32', icon: 'check_circle' },
  START_FAILED: { color: '#c62828', icon: 'error' }
};

const TYPE_BADGE: Record<string, { bg: string; color: string }> = {
  EDGE:    { bg: '#e8eaf6', color: '#283593' },
  GATEWAY: { bg: '#e0f2f1', color: '#00695c' },
  GENERIC: { bg: '#f3e5f5', color: '#6a1b9a' }
};

@Component({
  selector: 'tb-agent-profile-merged-profiles',
  templateUrl: './agent-profile-merged-profiles.component.html',
  styleUrls: ['./agent-profile-merged-profiles.component.scss'],
  providers: [{ provide: MAT_CHECKBOX_DEFAULT_OPTIONS, useValue: { clickAction: 'noop' } as MatCheckboxDefaultOptions }],
  standalone: false
})
export class AgentProfileMergedProfilesComponent implements OnInit, OnChanges, OnDestroy {

  @Input() agentProfile: AgentProfileInfo;
  @Input() active: boolean;

  rows: ProfileRow[] = [];
  loading = false;
  searchText = '';
  textSearchMode = false;
  sortActive = 'name';
  sortDirection: SortDirection = 'asc';
  selectedAction: AgentBulkAction | null = null;
  selectedActionProfile: AgentAppProfileRelationInfo | null = null;

  @ViewChild('searchInput') searchInput?: ElementRef<HTMLInputElement>;

  readonly profileColumns = ['expand', 'name', 'type', 'template', 'assignedApplications', 'relateOnAutoDiscovery', 'actions'];
  readonly bulkColumns = ['createdTime', 'actionType', 'status', 'counts', 'errorMsg', 'open'];

  get autoProvisionEnabled(): boolean {
    return !!this.agentProfile?.provisionType && this.agentProfile.provisionType !== AgentProvisionType.DISABLED;
  }

  private readonly destroy$ = new Subject<void>();

  constructor(private agentService: AgentService,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.agentProfile && !changes.agentProfile.firstChange) {
      this.resetState();
      this.load();
    }
    if (changes.active && this.active && !changes.active.firstChange) {
      this.refreshExpanded();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── profile-row rendering ──────────────────────────────────────────────────

  trackProfile = (_: number, row: ProfileRow) => row.profile.id.id;

  trackAction = (_: number, action: AgentBulkAction) => action.id.id;

  get filteredRows(): ProfileRow[] {
    const q = this.searchText.trim().toLowerCase();
    let rows = this.rows;
    if (q) {
      rows = rows.filter(r =>
        (r.profile.name || '').toLowerCase().includes(q) ||
        (r.profile.appType || '').toLowerCase().includes(q) ||
        this.templateLabel(r).toLowerCase().includes(q));
    }
    if (this.sortActive && this.sortDirection) {
      const dir = this.sortDirection === 'desc' ? -1 : 1;
      rows = [...rows].sort((a, b) =>
        this.sortValue(a, this.sortActive).localeCompare(this.sortValue(b, this.sortActive)) * dir);
    }
    return rows;
  }

  templateLabel(row: ProfileRow): string {
    return row.profile.templateCurrentVersion || '';
  }

  assignedAppsCount(row: ProfileRow): number {
    return row.profile.assignedApplicationsCount ?? 0;
  }

  onSortChange(s: Sort): void {
    this.sortActive = s.active;
    this.sortDirection = s.direction || 'asc';
  }

  enterSearchMode(): void {
    this.textSearchMode = true;
    setTimeout(() => this.searchInput?.nativeElement.focus(), 0);
  }

  exitSearchMode(): void {
    this.textSearchMode = false;
    this.searchText = '';
  }

  private sortValue(r: ProfileRow, col: string): string {
    switch (col) {
      case 'name':                 return (r.profile.name || '').toLowerCase();
      case 'type':                 return (r.profile.appType || '').toLowerCase();
      case 'template':             return this.templateLabel(r).toLowerCase();
      case 'assignedApplications': return String(this.assignedAppsCount(r)).padStart(12, '0');
      default:                     return '';
    }
  }

  typeBadgeStyle(appType: string): { [k: string]: string } {
    const c = TYPE_BADGE[appType] || { bg: '#eeeeee', color: '#616161' };
    return { background: c.bg, color: c.color };
  }

  toggleRow(row: ProfileRow, $event?: Event): void {
    if ($event) { $event.stopPropagation(); }
    row.expanded = !row.expanded;
    if (row.expanded && !row.loaded && !row.loading) {
      this.loadActions(row);
    }
  }

  openProfile(row: ProfileRow, $event?: Event): void {
    if ($event) { $event.stopPropagation(); }
    this.router.navigateByUrl(`/edgeManagement/profiles/application/${row.profile.id.id}`);
  }

  upgradeEnabled(row: ProfileRow): boolean {
    return row.profile.appType !== AgentApplicationType.GENERIC;
  }

  // ── per-row actions ────────────────────────────────────────────────────────

  bulkRestart($event: Event, row: ProfileRow): void {
    this.openBulk($event, row, AgentAppEventActionType.RESTART);
  }
  bulkUpdate($event: Event, row: ProfileRow): void {
    this.openBulk($event, row, AgentAppEventActionType.UPDATE);
  }
  bulkUpgrade($event: Event, row: ProfileRow): void {
    this.openBulk($event, row, AgentAppEventActionType.UPGRADE);
  }
  bulkDelete($event: Event, row: ProfileRow): void {
    this.openBulk($event, row, AgentAppEventActionType.DELETE);
  }

  unassign($event: Event, row: ProfileRow): void {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.confirm(
      this.translate.instant('agent.unassign-profile'),
      this.translate.instant('agent.unassign-profile-confirm', { profile: row.profile.name }),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).pipe(takeUntil(this.destroy$)).subscribe(confirm => {
      if (confirm) {
        this.agentService.unassignAppProfileFromAgentProfile(this.agentProfile.id.id, row.profile.id.id)
          .pipe(takeUntil(this.destroy$)).subscribe(() => this.load());
      }
    });
  }

  relatesOnAutoDiscovery(row: ProfileRow): boolean {
    return !!row.profile.additionalInfo?.['relates-on-auto-discovery'];
  }

  autoDiscoveryEnabled(row: ProfileRow): boolean {
    return row.profile.appType !== AgentApplicationType.GENERIC;
  }

  toggleAutoDiscovery($event: Event, row: ProfileRow): void {
    if ($event) {
      $event.preventDefault();
      $event.stopPropagation();
    }
    const relate = !this.relatesOnAutoDiscovery(row);
    this.dialogService.confirm(
      this.translate.instant('agent.relate-on-auto-discovery-confirm-title'),
      this.translate.instant(relate ? 'agent.relate-on-auto-discovery-enable-confirm'
        : 'agent.relate-on-auto-discovery-disable-confirm', { profile: row.profile.name }),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).pipe(takeUntil(this.destroy$)).subscribe(confirm => {
      if (confirm) {
        this.agentService.setAppProfileRelatesOnAutoDiscovery(this.agentProfile.id.id, row.profile.id.id, relate)
          .pipe(takeUntil(this.destroy$)).subscribe({ next: () => this.load(), error: () => this.load() });
      }
    });
  }

  refresh(): void {
    this.load();
  }

  openAssignDialog(): void {
    this.agentService.getAgentProfileAppProfileInfos(this.agentProfile.id.id)
      .pipe(takeUntil(this.destroy$)).subscribe((profiles) => {
      const selectedIds = (profiles || []).map(p => p.id.id);
      this.dialog.open<AgentProfileAssignProfileDialogComponent, AgentProfileAssignProfileDialogData, string[]>(
        AgentProfileAssignProfileDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog'],
          data: { selectedIds }
        }
      ).afterClosed().pipe(takeUntil(this.destroy$)).subscribe(nextIds => {
        if (nextIds !== null && nextIds !== undefined) {
          this.agentService.assignAppProfilesToAgentProfile(this.agentProfile.id.id, nextIds)
            .pipe(takeUntil(this.destroy$)).subscribe(() => this.load());
        }
      });
    });
  }

  // ── bulk-action sub-row rendering ──────────────────────────────────────────

  actionMeta(actionType: AgentAppEventActionType): ActionMeta {
    const key = agentAppEventActionTypeTranslationMap.get(actionType);
    return {
      icon: ACTION_ICONS[actionType] || 'bolt',
      label: key ? this.translate.instant(key) : String(actionType)
    };
  }

  statusMeta(status: AgentBulkActionStatus): StatusMeta {
    const cfg = STATUS_CONFIG[status] || { color: '#616161', icon: 'help_outline' };
    const key = agentBulkActionStatusTranslationMap.get(status);
    return {
      color: cfg.color,
      icon: cfg.icon,
      label: key ? this.translate.instant(key) : String(status)
    };
  }

  formatDate(ts: number): string {
    return this.datePipe.transform(ts, 'yyyy-MM-dd HH:mm:ss') || '';
  }

  selectAction(row: ProfileRow, action: AgentBulkAction): void {
    this.selectedAction = action;
    this.selectedActionProfile = row.profile;
  }

  closePanel(): void {
    this.selectedAction = null;
    this.selectedActionProfile = null;
  }

  openFullDetails(): void {
    if (this.selectedAction) {
      this.router.navigateByUrl(`/edgeManagement/profiles/agent/bulk/${this.selectedAction.id.id}`);
    }
  }

  // ── data loading ───────────────────────────────────────────────────────────

  private load(): void {
    if (!this.agentProfile) { return; }
    this.loading = true;
    this.agentService.getAgentProfileAppProfileInfos(this.agentProfile.id.id, { ignoreErrors: true } as any).pipe(
      takeUntil(this.destroy$)
    ).subscribe(profiles => {
      const prevExpanded = new Set(this.rows.filter(r => r.expanded).map(r => r.profile.id.id));
      const prevState = new Map(this.rows.map(r => [r.profile.id.id, r] as const));
      this.rows = (profiles || [])
        .map(p => {
          const prev = prevState.get(p.id.id);
          return {
            profile: p,
            expanded: prevExpanded.has(p.id.id),
            loading: false,
            loaded: false,
            actions: [],
            actionsPageIndex: prev?.actionsPageIndex ?? 0,
            actionsPageSize: prev?.actionsPageSize ?? DEFAULT_ACTIONS_PAGE_SIZE,
            actionsTotal: prev?.actionsTotal ?? 0
          } as ProfileRow;
        });
      this.loading = false;
      this.rows.filter(r => r.expanded).forEach(r => this.loadActions(r));
      this.cd.markForCheck();
    });
  }

  private loadActions(row: ProfileRow): void {
    if (!this.agentProfile) { return; }
    row.loading = true;
    const pageLink = new PageLink(row.actionsPageSize, row.actionsPageIndex, null,
      { property: 'createdTime', direction: Direction.DESC });
    this.agentService.getAgentProfileAppProfileBulkActions(
      this.agentProfile.id.id, row.profile.id.id, pageLink
    ).pipe(takeUntil(this.destroy$)).subscribe(page => {
      row.actions = page.data;
      row.actionsTotal = page.totalElements ?? page.data.length;
      const maxIndex = Math.max(0, Math.ceil(row.actionsTotal / row.actionsPageSize) - 1);
      if (row.actionsPageIndex > maxIndex) {
        row.actionsPageIndex = maxIndex;
        // Re-fetch on the clamped page.
        this.loadActions(row);
        return;
      }
      row.loading = false;
      row.loaded = true;
      this.cd.markForCheck();
    });
  }

  onActionsPageChange(row: ProfileRow, e: { pageIndex: number; pageSize: number }): void {
    row.actionsPageIndex = e.pageIndex;
    row.actionsPageSize = e.pageSize;
    this.loadActions(row);
  }

  readonly actionsPageSizeOptions = ACTIONS_PAGE_SIZE_OPTIONS;

  private refreshExpanded(): void {
    this.rows.filter(r => r.expanded).forEach(r => this.loadActions(r));
  }

  private openBulk($event: Event, row: ProfileRow, actionType: AgentAppEventActionType): void {
    if ($event) { $event.stopPropagation(); }
    this.dialog.open<AgentProfileBulkActionDialogComponent, AgentProfileBulkActionDialogData>(
      AgentProfileBulkActionDialogComponent, {
        disableClose: false,
        panelClass: ['tb-dialog'],
        data: {
          agentProfile: this.agentProfile,
          profile: row.profile,
          actionType
        }
      }
    ).afterClosed().pipe(takeUntil(this.destroy$)).subscribe(bulkAction => {
      if (bulkAction) {
        if (!row.expanded) {
          row.expanded = true;
        }
        this.loadActions(row);
      }
    });
  }

  private resetState(): void {
    this.rows = [];
    this.selectedAction = null;
    this.selectedActionProfile = null;
  }
}

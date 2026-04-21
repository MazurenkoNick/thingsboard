///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Direction } from '@shared/models/page/sort-order';
import { AgentService } from '@core/http/agent.service';
import {
  AgentAppEventActionType,
  agentAppEventActionTypeTranslationMap,
  AgentBulkAction,
  AgentBulkActionStatus,
  agentBulkActionStatusTranslationMap,
  AgentGroupInfo
} from '@shared/models/agent.models';

const statusConfig: Record<string, { color: string; icon: string }> = {
  QUEUED:       { color: '#616161', icon: 'schedule' },
  IN_PROGRESS:  { color: '#1565c0', icon: 'autorenew' },
  STARTED:      { color: '#2e7d32', icon: 'check_circle' },
  START_FAILED: { color: '#c62828', icon: 'error' }
};

const actionIcons: Record<string, string> = {
  RESTART:  'restart_alt',
  UPDATE:   'sync_alt',
  UPGRADE:  'arrow_upward',
  DELETE:   'delete',
  ROLLBACK: 'undo'
};

function statusBadge(status: AgentBulkActionStatus, label: string): string {
  const c = statusConfig[status] || { color: '#616161', icon: 'help_outline' };
  return `<span style="display:inline-flex;align-items:center;gap:4px;font-size:13px;font-weight:500;color:${c.color};">`
    + `<span class="material-icons" style="font-size:18px;">${c.icon}</span>${label}</span>`;
}

function actionBadge(action: string, label: string): string {
  const icon = actionIcons[action] || 'bolt';
  return `<span style="display:inline-flex;align-items:center;gap:4px;font-size:13px;color:rgba(0,0,0,0.76);">`
    + `<span class="material-icons" style="font-size:18px;">${icon}</span>${label}</span>`;
}

function countsCell(action: AgentBulkAction): string {
  const total = action.total || 0;
  const submitted = action.submitted || 0;
  const skipped = total - submitted;
  const parts: string[] = [];
  parts.push(`<span style="color:#2e7d32;font-weight:500;">${submitted}</span>/${total}`);
  if (skipped > 0) {
    parts.push(`<span style="color:#ef6c00;font-size:11px;margin-left:6px;">${skipped} skipped</span>`);
  }
  return parts.join('');
}

export class AgentGroupBulkActionsTableConfig extends EntityTableConfig<AgentBulkAction> {

  constructor(private readonly group: AgentGroupInfo,
              private readonly agentService: AgentService,
              private readonly translate: TranslateService,
              private readonly datePipe: DatePipe,
              private readonly router: Router) {
    super();

    this.tableTitle = this.translate.instant('agent.bulk-actions');
    this.detailsPanelEnabled = false;
    this.selectionEnabled = false;
    this.searchEnabled = false;
    this.addEnabled = false;
    this.entitiesDeleteEnabled = false;
    this.pageMode = false;
    this.defaultSortOrder = { property: 'createdTime', direction: Direction.DESC };

    this.entityTranslations = { noEntities: 'agent.no-bulk-actions' } as any;
    this.entityResources = {} as any;

    this.handleRowClick = ($event: Event, action: AgentBulkAction) => {
      if ($event) { $event.stopPropagation(); }
      this.router.navigateByUrl(`/edgeManagement/agentGroups/bulk/${action.id.id}`);
      return true;
    };

    this.columns.push(
      new DateEntityTableColumn<AgentBulkAction>('createdTime', 'common.created-time', this.datePipe, '160px'),
      new EntityTableColumn<AgentBulkAction>('actionType', 'agent.bulk-action-type', '140px',
        (a) => actionBadge(a.actionType, this.actionLabel(a.actionType)), () => ({}), false),
      new EntityTableColumn<AgentBulkAction>('status', 'agent.bulk-action-status', '160px',
        (a) => statusBadge(a.status, this.statusLabel(a.status)), () => ({}), false),
      new EntityTableColumn<AgentBulkAction>('counts', 'agent.bulk-action-counts', '160px',
        (a) => countsCell(a), () => ({}), false),
      new EntityTableColumn<AgentBulkAction>('errorMsg', 'agent.bulk-action-error', '30%',
        (a) => a.errorMsg || '', () => ({}), false)
    );

    this.entitiesFetchFunction = (pageLink) =>
      this.agentService.getGroupBulkActions(this.group.id.id, pageLink);
  }

  private actionLabel(action: AgentAppEventActionType): string {
    const key = agentAppEventActionTypeTranslationMap.get(action);
    return key ? this.translate.instant(key) : action;
  }

  private statusLabel(status: AgentBulkActionStatus): string {
    const key = agentBulkActionStatusTranslationMap.get(status);
    return key ? this.translate.instant(key) : status;
  }
}

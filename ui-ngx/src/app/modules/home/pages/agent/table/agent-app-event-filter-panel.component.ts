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

import { Component, Inject, InjectionToken } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import {
  AgentAppEventActionType,
  AgentAppEventStatus
} from '@shared/models/agent.models';

export interface AgentAppEventFilterValue {
  actionType: AgentAppEventActionType | null;
  status: AgentAppEventStatus | null;
}

export interface AgentAppEventFilterPanelData {
  value: AgentAppEventFilterValue;
}

export const AGENT_APP_EVENT_FILTER_PANEL_DATA =
  new InjectionToken<AgentAppEventFilterPanelData>('AgentAppEventFilterPanelData');

@Component({
  selector: 'tb-agent-app-event-filter-panel',
  templateUrl: './agent-app-event-filter-panel.component.html',
  styleUrls: ['./agent-app-filter-panel.component.scss'],
  standalone: false
})
export class AgentAppEventFilterPanelComponent {

  readonly actionOptions = Object.values(AgentAppEventActionType)
    .filter(a => a !== AgentAppEventActionType.ROLLBACK);
  readonly statusOptions = Object.values(AgentAppEventStatus);

  draft: AgentAppEventFilterValue;
  result: AgentAppEventFilterValue | null = null;

  constructor(@Inject(AGENT_APP_EVENT_FILTER_PANEL_DATA) public data: AgentAppEventFilterPanelData,
              private overlayRef: OverlayRef) {
    this.draft = { ...data.value };
  }

  reset(): void {
    this.draft = { actionType: null, status: null };
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  apply(): void {
    this.result = { ...this.draft };
    this.overlayRef.dispose();
  }
}

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

import { Component, Inject, InjectionToken } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import { AgentAppUnitType } from '@shared/models/agent.models';

export interface AgentAppUnitFilterValue {
  type: AgentAppUnitType | null;
}

export interface AgentAppUnitFilterPanelData {
  value: AgentAppUnitFilterValue;
}

export const AGENT_APP_UNIT_FILTER_PANEL_DATA =
  new InjectionToken<AgentAppUnitFilterPanelData>('AgentAppUnitFilterPanelData');

@Component({
  selector: 'tb-agent-app-unit-filter-panel',
  templateUrl: './agent-app-unit-filter-panel.component.html',
  styleUrls: ['./agent-app-filter-panel.component.scss'],
  standalone: false
})
export class AgentAppUnitFilterPanelComponent {

  readonly typeOptions = Object.values(AgentAppUnitType);

  draft: AgentAppUnitFilterValue;
  result: AgentAppUnitFilterValue | null = null;

  constructor(@Inject(AGENT_APP_UNIT_FILTER_PANEL_DATA) public data: AgentAppUnitFilterPanelData,
              private overlayRef: OverlayRef) {
    this.draft = { ...data.value };
  }

  reset(): void {
    this.draft = { type: null };
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  apply(): void {
    this.result = { ...this.draft };
    this.overlayRef.dispose();
  }
}

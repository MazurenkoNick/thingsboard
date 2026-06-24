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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AgentApplication, AgentInfo } from '@shared/models/agent.models';

@Component({
  selector: 'tb-agent-deploy-status',
  templateUrl: './agent-deploy-status.component.html',
  styleUrls: ['./agent-deploy-status.component.scss'],
  standalone: false
})
export class AgentDeployStatusComponent {

  @Input() agent: AgentInfo;
  @Input() application: AgentApplication | null = null;

  @Output() goToAgent = new EventEmitter<void>();
  @Output() goToAgentApplication = new EventEmitter<void>();
  @Output() goToAgentEvents = new EventEmitter<void>();

  showInstallCommand = false;

  constructor(private translate: TranslateService) {}

  get online(): boolean {
    return !!this.agent?.active;
  }

  get managingDuration(): string {
    if (!this.application?.createdTime) {
      return '';
    }
    const ms = Date.now() - this.application.createdTime;
    const days = Math.floor(ms / 86_400_000);
    if (days >= 1) {
      return this.translate.instant('agent.deploy-status-duration-days', { count: days });
    }
    const hours = Math.floor(ms / 3_600_000);
    if (hours >= 1) {
      return this.translate.instant('agent.deploy-status-duration-hours', { count: hours });
    }
    const minutes = Math.max(1, Math.floor(ms / 60_000));
    return this.translate.instant('agent.deploy-status-duration-minutes', { count: minutes });
  }

}

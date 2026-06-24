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

import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppEvent,
  AgentApplicationType,
  AgentInfo
} from '@shared/models/agent.models';

export interface AgentAppInstallWizardData {
  agentId?: string;
  agent?: AgentInfo;
  mode?: 'install' | 'update' | 'upgrade';
  application?: AgentApplication;
  lockedType?: AgentApplicationType;
  lockedRelatedEntity?: EntityId;
  navigateToAgentOnFinish?: boolean;
  selectAgent?: boolean;
  showBack?: boolean;
}

// Returned via afterClosed() when the user leaves the wizard through the
// toolbar back arrow, so the caller can re-open the screen it came from.
export const AGENT_INSTALL_WIZARD_BACK = { back: true };

export interface AgentAppUpgradeResult {
  profileOnly: true;
}

// Emitted by a flow component when its work is done. The dispatcher maps this
// to the dialog/embedded completion (close + optional navigate / progress).
export interface AgentAppWizardFinish {
  event: AgentAppEvent | null;
  application?: AgentApplication | null;
  // Open the event-progress dialog after closing (install non-selectAgent,
  // update, upgrade submit success).
  withProgress?: boolean;
  // Deploy-status step: just close with the event; navigation already done by
  // the flow's goToAgent/goToAgentApplication/goToAgentEvents.
  closeOnly?: boolean;
}

export interface AgentTypeCard {
  type: AgentApplicationType;
  icon: string;
  labelKey: string;
  descKey: string;
}

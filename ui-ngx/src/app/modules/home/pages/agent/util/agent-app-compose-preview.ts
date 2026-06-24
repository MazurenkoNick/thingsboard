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
  AgentApplication,
  AgentApplicationOrigin,
  AgentApplicationType,
  AgentAppTemplate
} from '@shared/models/agent.models';
import { AgentId } from '@shared/models/id/agent-id';
import { parseComposeYaml } from '@home/pages/agent/util/agent-compose-yaml';

// Minimal INSTALLED draft posted to the merge-preview endpoint for a fresh
// install before the user has typed any compose.
export function buildInstallMergeDraft(agentId: string,
                                       selectedType: AgentApplicationType,
                                       appName: string,
                                       template: AgentAppTemplate): AgentApplication {
  return {
    name: appName,
    appType: selectedType,
    agentId: new AgentId(agentId) as any,
    templateId: template.id,
    origin: AgentApplicationOrigin.INSTALLED
  } as any;
}

// Update mode re-uses the existing application, only re-pointing the template.
export function buildUpdateMergeDraft(existingApplication: AgentApplication,
                                      template: AgentAppTemplate): AgentApplication {
  return { ...existingApplication, templateId: template.id } as any;
}

// Draft posted when re-merging after a related entity is picked (install mode):
// carries the user's current compose so host/credential overrides survive.
export function buildRelatedMergeDraft(agentId: string,
                                       selectedType: AgentApplicationType,
                                       appName: string,
                                       template: AgentAppTemplate,
                                       composeYaml: string,
                                       fallbackCompose: any): AgentApplication {
  return {
    name: appName,
    appType: selectedType,
    agentId: new AgentId(agentId) as any,
    templateId: template.id,
    origin: AgentApplicationOrigin.INSTALLED,
    config: { type: 'DOCKER_COMPOSE', compose: parseComposeYaml(composeYaml, fallbackCompose) } as any
  } as any;
}

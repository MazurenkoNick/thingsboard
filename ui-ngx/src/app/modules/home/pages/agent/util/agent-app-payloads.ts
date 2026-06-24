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
  AgentAppProfile,
  AgentAppTemplate
} from '@shared/models/agent.models';
import { applyCredentialValuesToCompose } from '@home/pages/agent/util/agent-credentials';
import { parseComposeYaml } from '@home/pages/agent/util/agent-compose-yaml';

export interface UpgradePayloadOpts {
  existingApplication: AgentApplication;
  template: AgentAppTemplate;
  // Profile-bound upgrade: compose comes from the existing app and only the
  // edited credentials are written back. Otherwise the user-edited YAML is used.
  profileBound: boolean;
  selectedType: AgentApplicationType | null;
  credentialValues: Record<string, string>;
  composeYaml: string;
  mergedApp: AgentApplication | null;
}

export function buildUpgradeApplication(opts: UpgradePayloadOpts): any {
  let outboundCompose: any;
  if (opts.profileBound) {
    outboundCompose = (opts.existingApplication.config as any)?.compose;
    if (outboundCompose) {
      applyCredentialValuesToCompose(outboundCompose, opts.selectedType, opts.credentialValues);
    }
  } else {
    outboundCompose = parseComposeYaml(opts.composeYaml, (opts.mergedApp?.config as any)?.compose);
  }
  return {
    ...opts.existingApplication,
    templateId: opts.template.id,
    config: {
      ...((opts.existingApplication.config as any) || { type: 'DOCKER_COMPOSE' }),
      compose: outboundCompose
    }
  };
}

export interface UpdatePayloadOpts {
  existingApplication: AgentApplication;
  appName: string;
  composeYaml: string;
  mergedApp: AgentApplication | null;
  composeType?: string;
}

export function buildUpdateApplication(opts: UpdatePayloadOpts): any {
  return {
    ...opts.existingApplication,
    name: opts.appName.trim(),
    config: {
      ...((opts.existingApplication.config as any) || { type: 'DOCKER_COMPOSE' }),
      compose: parseComposeYaml(opts.composeYaml, (opts.mergedApp?.config as any)?.compose),
      composeType: opts.composeType
    }
  };
}

export interface InstallPayloadOpts {
  selectedType: AgentApplicationType | null;
  agentId: string;
  appName: string;
  composeYaml: string;
  mergedApp: AgentApplication | null;
  template: AgentAppTemplate | null;
  useProfile: boolean;
  selectedProfile: AgentAppProfile | null;
  composeType?: string;
}

export function buildInstallApplication(opts: InstallPayloadOpts): any {
  const compose = () => parseComposeYaml(opts.composeYaml, (opts.mergedApp?.config as any)?.compose);
  let application: any;
  if (opts.selectedType === AgentApplicationType.GENERIC) {
    application = {
      name: opts.appName.trim(),
      appType: AgentApplicationType.GENERIC,
      agentId: { id: opts.agentId, entityType: 'AGENT' },
      templateId: opts.template?.id,
      config: { type: 'DOCKER_COMPOSE', compose: compose(), composeType: opts.composeType },
      origin: AgentApplicationOrigin.INSTALLED
    };
  } else {
    const base: any = opts.mergedApp || {
      agentId: { id: opts.agentId, entityType: 'AGENT' },
      appType: opts.selectedType,
      templateId: opts.template?.id,
      origin: AgentApplicationOrigin.INSTALLED
    };
    application = {
      ...base,
      name: opts.appName.trim(),
      config: {
        ...((opts.mergedApp && opts.mergedApp.config) || { type: 'DOCKER_COMPOSE' }),
        compose: compose(),
        composeType: opts.composeType
      }
    };
  }
  // Attach profile reference if using a profile-based install.
  if (opts.useProfile && opts.selectedProfile) {
    application.applicationProfileId = opts.selectedProfile.id;
    application.templateId = opts.selectedProfile.templateId;
  }
  return application;
}

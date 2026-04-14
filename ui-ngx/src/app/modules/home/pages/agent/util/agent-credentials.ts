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

import { AgentApplicationType } from '@shared/models/agent.models';

export type CredFieldType = 'text' | 'password' | 'select';

export interface CredField {
  key: string;
  labelKey: string;
  type: CredFieldType;
  options?: { value: string; labelKey: string }[];
  // When present, the field is only rendered if the current value of the
  // gating key is included. Used for GATEWAY where TB_GW_* fields are driven
  // by TB_GW_SECURITY_TYPE.
  showWhen?: { key: string; values: string[] };
}

// Mirrors AgentApplicationType.credentialEnvKeys on the backend. Keep in sync.
export const CREDENTIAL_SCHEMAS: Partial<Record<AgentApplicationType, CredField[]>> = {
  [AgentApplicationType.EDGE]: [
    { key: 'CLOUD_ROUTING_KEY', labelKey: 'agent.app-install-cred-routing-key', type: 'text' },
    { key: 'CLOUD_ROUTING_SECRET', labelKey: 'agent.app-install-cred-routing-secret', type: 'text' }
  ],
  [AgentApplicationType.GATEWAY]: [
    { key: 'TB_GW_SECURITY_TYPE', labelKey: 'agent.app-install-cred-security-type', type: 'select',
      options: [
        { value: 'accessToken', labelKey: 'agent.app-install-cred-security-access-token' },
        { value: 'usernamePassword', labelKey: 'agent.app-install-cred-security-username-password' }
      ] },
    { key: 'TB_GW_ACCESS_TOKEN', labelKey: 'agent.app-install-cred-access-token', type: 'password',
      showWhen: { key: 'TB_GW_SECURITY_TYPE', values: ['accessToken'] } },
    { key: 'TB_GW_CLIENT_ID', labelKey: 'agent.app-install-cred-client-id', type: 'text',
      showWhen: { key: 'TB_GW_SECURITY_TYPE', values: ['usernamePassword'] } },
    { key: 'TB_GW_USERNAME', labelKey: 'agent.app-install-cred-username', type: 'text',
      showWhen: { key: 'TB_GW_SECURITY_TYPE', values: ['usernamePassword'] } },
    { key: 'TB_GW_PASSWORD', labelKey: 'agent.app-install-cred-password', type: 'password',
      showWhen: { key: 'TB_GW_SECURITY_TYPE', values: ['usernamePassword'] } }
  ]
};

export const MAIN_IMAGE_PATTERNS: Partial<Record<AgentApplicationType, RegExp>> = {
  [AgentApplicationType.EDGE]: /^thingsboard\/tb-edge:.+$/,
  [AgentApplicationType.GATEWAY]: /^thingsboard\/tb-gateway:.+$/
};

export function credentialSchemaFor(type: AgentApplicationType | null | undefined): CredField[] {
  return type ? (CREDENTIAL_SCHEMAS[type] || []) : [];
}

export function visibleCredentialFields(type: AgentApplicationType | null | undefined,
                                        values: Record<string, string>): CredField[] {
  return credentialSchemaFor(type).filter(f =>
    !f.showWhen || f.showWhen.values.includes(values[f.showWhen.key])
  );
}

export function findMainServiceEnv(compose: any, type: AgentApplicationType | null | undefined): any | null {
  if (!type) { return null; }
  const pattern = MAIN_IMAGE_PATTERNS[type];
  if (!pattern || !compose || !compose.services || typeof compose.services !== 'object') {
    return null;
  }
  for (const service of Object.values<any>(compose.services)) {
    if (service && typeof service.image === 'string' && pattern.test(service.image)
        && service.environment && typeof service.environment === 'object') {
      return service.environment;
    }
  }
  return null;
}

export function extractCredentialValues(compose: any, type: AgentApplicationType | null | undefined): Record<string, string> {
  const values: Record<string, string> = {};
  const schema = credentialSchemaFor(type);
  if (!schema.length) { return values; }
  const env = findMainServiceEnv(compose, type) || {};
  for (const field of schema) {
    values[field.key] = env[field.key] != null ? String(env[field.key]) : '';
  }
  return values;
}

// Mutates `compose` in place: writes credential values into the main service's
// env. Only writes a key if it already exists or the user supplied a non-empty
// value, mirroring backend `setEnvVariables` semantics.
export function applyCredentialValuesToCompose(compose: any,
                                               type: AgentApplicationType | null | undefined,
                                               values: Record<string, string>): void {
  const env = findMainServiceEnv(compose, type);
  if (!env) { return; }
  for (const field of credentialSchemaFor(type)) {
    const v = values[field.key];
    if (field.key in env || (v != null && v !== '')) {
      env[field.key] = v ?? '';
    }
  }
}

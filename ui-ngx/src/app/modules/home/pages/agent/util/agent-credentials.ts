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

// Returns the env node of the main service. Compose env may be either:
//   - map form: { KEY: "VAL", ... }    (returned as a plain object)
//   - list form: [ "KEY=VAL", ... ]   (returned as an array)
// Both are valid Docker Compose syntax; agents emit the list form when they
// reconstruct compose from `docker inspect`. Callers must branch on Array.
export function findMainServiceEnv(compose: any, type: AgentApplicationType | null | undefined): any | null {
  if (!type) { return null; }
  const pattern = MAIN_IMAGE_PATTERNS[type];
  if (!pattern || !compose || !compose.services || typeof compose.services !== 'object') {
    return null;
  }
  for (const service of Object.values<any>(compose.services)) {
    if (service && typeof service.image === 'string' && pattern.test(service.image)
        && service.environment != null
        && (Array.isArray(service.environment) || typeof service.environment === 'object')) {
      return service.environment;
    }
  }
  return null;
}

function envHasKey(env: any, key: string): boolean {
  if (!env) { return false; }
  if (Array.isArray(env)) {
    const prefix = `${key}=`;
    return env.some((e: any) => typeof e === 'string' && (e === key || e.startsWith(prefix)));
  }
  return key in env;
}

function envGet(env: any, key: string): string | null {
  if (!env) { return null; }
  if (Array.isArray(env)) {
    const prefix = `${key}=`;
    for (const e of env) {
      if (typeof e !== 'string') { continue; }
      if (e === key) { return ''; }
      if (e.startsWith(prefix)) { return e.substring(prefix.length); }
    }
    return null;
  }
  return env[key] != null ? String(env[key]) : null;
}

function envSet(env: any, key: string, value: string): void {
  if (!env) { return; }
  if (Array.isArray(env)) {
    const prefix = `${key}=`;
    const idx = env.findIndex((e: any) => typeof e === 'string' && (e === key || e.startsWith(prefix)));
    if (idx >= 0) {
      env[idx] = `${key}=${value ?? ''}`;
    } else {
      env.push(`${key}=${value ?? ''}`);
    }
    return;
  }
  env[key] = value ?? '';
}

export function extractCredentialValues(compose: any, type: AgentApplicationType | null | undefined): Record<string, string> {
  const values: Record<string, string> = {};
  const schema = credentialSchemaFor(type);
  if (!schema.length) { return values; }
  const env = findMainServiceEnv(compose, type);
  for (const field of schema) {
    const v = envGet(env, field.key);
    values[field.key] = v != null ? v : '';
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
    if (envHasKey(env, field.key) || (v != null && v !== '')) {
      envSet(env, field.key, v ?? '');
    }
  }
}

// Normalises a raw value that the user typed or pasted into a credential
// input. Handles the common case where the user pastes a full YAML list
// entry (e.g. `      - CLOUD_ROUTING_KEY=abc` or the concatenation of two
// entries). Without this, multi-line pastes corrupt the surrounding
// compose YAML when the value is serialised back out.
export function normalizeCredValue(key: string, raw: string): string {
  if (raw == null) { return ''; }
  // Collapse newlines so the value remains single-line — prevents breaking
  // the compose YAML dump downstream.
  let v = String(raw).replace(/\r?\n/g, ' ').trim();
  // Strip leading YAML list marker ("- ") if the user pasted a compose env entry.
  v = v.replace(/^-\s*/, '');
  // Strip "KEY=" prefix (case-insensitive match on the env var name).
  const prefix = `${key}=`;
  if (v.toUpperCase().startsWith(prefix.toUpperCase())) {
    v = v.substring(prefix.length);
  }
  // If the paste contained two env-var entries concatenated (user copied
  // multiple lines), keep only the value of the first one — the next one
  // begins at " - OTHER_KEY=" or just " OTHER_KEY=".
  const tail = v.match(/^(.*?)\s+-?\s*[A-Z][A-Z0-9_]{3,}\s*=/);
  if (tail) {
    v = tail[1];
  }
  return v.trim();
}

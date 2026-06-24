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

import * as YAML from 'yaml';
import { AgentApplication, AgentAppStepType, AgentAppTemplate } from '@shared/models/agent.models';

export function scalarYaml(value: any): string {
  if (typeof value === 'string') {
    const needsQuote = /^(true|false|null|yes|no|on|off|\d|-)/i.test(value)
      || value.includes(':') || value.includes('#')
      || value.includes('\n') || value.includes('\r') || value.includes('\t')
      || value.includes('"') || value.includes('\\');
    if (needsQuote) {
      const escaped = value
        .replace(/\\/g, '\\\\')
        .replace(/"/g, '\\"')
        .replace(/\n/g, '\\n')
        .replace(/\r/g, '\\r')
        .replace(/\t/g, '\\t');
      return `"${escaped}"`;
    }
    return value;
  }
  return String(value);
}

export function dumpYaml(value: any, indent = 0): string {
  const pad = '  '.repeat(indent);
  if (value === null || value === undefined) { return `${pad}null\n`; }
  if (Array.isArray(value)) {
    if (value.length === 0) { return `${pad}[]\n`; }
    let out = '';
    for (const item of value) {
      if (item !== null && typeof item === 'object') {
        const lines = dumpYaml(item, indent + 1).split('\n');
        let firstReplaced = false;
        for (const line of lines) {
          if (!line.trim()) { continue; }
          if (!firstReplaced) {
            out += `${pad}- ${line.trimStart()}\n`;
            firstReplaced = true;
          } else {
            out += `${line}\n`;
          }
        }
      } else {
        out += `${pad}- ${scalarYaml(item)}\n`;
      }
    }
    return out;
  }
  if (typeof value === 'object') {
    const keys = Object.keys(value);
    if (keys.length === 0) { return `${pad}{}\n`; }
    let out = '';
    for (const key of keys) {
      const v = value[key];
      if (v === null || v === undefined) {
        out += `${pad}${key}:\n`;
      } else if (typeof v === 'object') {
        if (Array.isArray(v) && v.length === 0) {
          out += `${pad}${key}: []\n`;
        } else if (!Array.isArray(v) && Object.keys(v).length === 0) {
          out += `${pad}${key}:\n`;
        } else {
          out += `${pad}${key}:\n`;
          out += dumpYaml(v, indent + 1);
        }
      } else {
        out += `${pad}${key}: ${scalarYaml(v)}\n`;
      }
    }
    return out;
  }
  return `${pad}${scalarYaml(value)}\n`;
}

export function dumpCompose(app: AgentApplication): string {
  const compose: any = app?.config && (app.config as any).compose;
  if (!compose) {
    return '';
  }
  return dumpYaml(compose, 0).trimEnd() + '\n';
}

const COMPOSE_TYPE_LABEL_KEYS: Record<string, string> = {
  kafka: 'agent.compose-type-kafka',
  hybrid: 'agent.compose-type-hybrid',
  in_memory: 'agent.compose-type-in-memory'
};

// Returns the locale key for a known compose type, or null when the raw key
// should be shown as-is.
export function composeTypeLabelKey(composeType: string): string | null {
  return COMPOSE_TYPE_LABEL_KEYS[(composeType || '').toLowerCase()] || null;
}

function findComposeTemplates(template: AgentAppTemplate): Record<string, any> | null {
  for (const step of (template.startSteps || [])) {
    const anyStep = step as any;
    if (step.type === AgentAppStepType.COMPOSE_TEMPLATE && anyStep.composeTemplates) {
      return anyStep.composeTemplates;
    }
  }
  return null;
}

export function composeTemplateKeys(template: AgentAppTemplate | null | undefined): string[] {
  if (!template) {
    return [];
  }
  const templates = findComposeTemplates(template);
  return templates ? Object.keys(templates) : [];
}

export function dumpRawTemplateCompose(template: AgentAppTemplate, composeType?: string): string {
  const templates = findComposeTemplates(template);
  if (templates) {
    const keys = Object.keys(templates);
    if (keys.length) {
      const key = (composeType && keys.includes(composeType)) ? composeType : keys[0];
      return dumpYaml(templates[key], 0).trimEnd() + '\n';
    }
  }
  const compose: any = (template.config as any)?.compose;
  return compose ? (dumpYaml(compose, 0).trimEnd() + '\n') : '';
}

export function pickComposeType(template: AgentAppTemplate): string {
  const keys = composeTemplateKeys(template);
  return keys.length ? keys[0] : 'default';
}

export function parseComposeYaml(yaml: string, fallbackCompose?: any): any {
  if (yaml && yaml.trim().length > 0) {
    try {
      return YAML.parse(yaml);
    } catch (e) { /* fall through */ }
  }
  if (fallbackCompose) {
    return fallbackCompose;
  }
  return { services: {} };
}

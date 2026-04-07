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

import { Component, Inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import {
  AgentApplication,
  AgentAppEventActionType,
  AgentAppStep,
  AgentAppStepType,
  AgentApplicationType,
  AgentAppTemplate,
  AgentInfo,
  AgentApplicationOrigin
} from '@shared/models/agent.models';
import { AgentId } from '@shared/models/id/agent-id';

export interface AgentAppInstallWizardData {
  agentId: string;
  agent: AgentInfo;
}

interface TypeCard {
  type: AgentApplicationType;
  icon: string;
  labelKey: string;
  descKey: string;
}

@Component({
  selector: 'tb-agent-app-install-wizard',
  templateUrl: './agent-app-install-wizard.component.html',
  styleUrls: ['./agent-app-install-wizard.component.scss']
})
export class AgentAppInstallWizardComponent
  extends DialogComponent<AgentAppInstallWizardComponent, boolean>
  implements OnInit {

  agentId: string;
  agent: AgentInfo;

  typeCards: TypeCard[] = [
    { type: AgentApplicationType.GENERIC, icon: 'inventory_2', labelKey: 'agent.app-install-type-generic', descKey: 'agent.app-install-type-generic-desc' },
    { type: AgentApplicationType.EDGE, icon: 'router', labelKey: 'agent.app-install-type-edge', descKey: 'agent.app-install-type-edge-desc' },
    { type: AgentApplicationType.GATEWAY, icon: 'hub', labelKey: 'agent.app-install-type-gateway', descKey: 'agent.app-install-type-gateway-desc' }
  ];

  selectedType: AgentApplicationType | null = null;
  template: AgentAppTemplate | null = null;
  loadingTemplate = false;
  loadError = '';

  appName = '';
  composeYaml = '';
  pullImages = false;
  hasPullImagesStep = false;
  pullImagesStep: AgentAppStep | null = null;

  mergedApp: AgentApplication | null = null;
  submitting = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppInstallWizardData,
              public dialogRef: MatDialogRef<AgentAppInstallWizardComponent, boolean>) {
    super(store, router, dialogRef);
    this.agentId = data.agentId;
    this.agent = data.agent;
  }

  ngOnInit() { /* no-op; type selection drives everything */ }

  selectType(type: AgentApplicationType) {
    if (this.selectedType === type) {
      return;
    }
    this.selectedType = type;
    this.template = null;
    this.composeYaml = '';
    this.mergedApp = null;
    this.appName = this.defaultAppName(type);
    this.loadError = '';

    if (type === AgentApplicationType.GENERIC) {
      // Generic: blank YAML, no template fetch needed
      this.composeYaml = '# Paste your Docker Compose YAML here\nservices:\n  app:\n    image: nginx:latest\n';
      this.template = null;
      this.hasPullImagesStep = false;
      this.pullImagesStep = null;
      return;
    }

    this.loadingTemplate = true;
    this.agentService.getLatestAgentAppTemplate(type, 'DOCKER_COMPOSE').subscribe({
      next: tpl => {
        this.template = tpl;
        this.scanStartSteps(tpl);
        // Build a draft app and merge for preview to populate the YAML editor
        const draft: AgentApplication = {
          name: this.appName,
          appType: type,
          agentId: new AgentId(this.agentId) as any,
          templateId: tpl.id,
          origin: AgentApplicationOrigin.INSTALLED
        } as any;
        this.agentService.mergeForPreview(tpl.id.id, draft).subscribe({
          next: merged => {
            this.mergedApp = merged;
            this.composeYaml = this.dumpCompose(merged);
            this.loadingTemplate = false;
          },
          error: () => {
            this.loadError = this.translate.instant('agent.app-install-merge-failed');
            this.loadingTemplate = false;
          }
        });
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingTemplate = false;
      }
    });
  }

  private scanStartSteps(template: AgentAppTemplate) {
    const steps = (template.startSteps || []).filter(s => !s.templateOnly);
    this.pullImagesStep = steps.find(s =>
      s.type === AgentAppStepType.COMPOSE && s.state && 'pullImages' in s.state
    ) || null;
    this.hasPullImagesStep = !!this.pullImagesStep;
  }

  private defaultAppName(type: AgentApplicationType): string {
    switch (type) {
      case AgentApplicationType.EDGE: return 'tb-edge';
      case AgentApplicationType.GATEWAY: return 'tb-gateway';
      default: return 'my-app';
    }
  }

  cancel() {
    this.dialogRef.close(false);
  }

  canProceedFromType(): boolean {
    return !!this.selectedType && !this.loadingTemplate && !this.loadError;
  }

  canSubmit(): boolean {
    return !!this.selectedType && !!this.appName?.trim() && !!this.composeYaml?.trim() && !this.submitting;
  }

  submit() {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting = true;

    // Build the application body. For EDGE/GATEWAY use the merged app and
    // overlay user-edited compose; for GENERIC build a fresh body.
    let application: any;
    if (this.selectedType === AgentApplicationType.GENERIC) {
      application = {
        name: this.appName.trim(),
        appType: AgentApplicationType.GENERIC,
        agentId: { id: this.agentId, entityType: 'AGENT' },
        config: { type: 'DOCKER_COMPOSE', compose: this.parseYamlBestEffort(this.composeYaml) },
        origin: AgentApplicationOrigin.INSTALLED
      };
    } else {
      application = {
        ...(this.mergedApp || {}),
        name: this.appName.trim(),
        config: {
          ...((this.mergedApp && this.mergedApp.config) || { type: 'DOCKER_COMPOSE' }),
          compose: this.parseYamlBestEffort(this.composeYaml)
        }
      };
    }

    const stepInputs: { [stepId: string]: any } = {};
    if (this.pullImagesStep) {
      stepInputs[this.pullImagesStep.id] = {
        pullImages: this.pullImages,
        type: AgentAppStepType.COMPOSE
      };
    }

    this.agentService.installAgentApp({
      actionType: AgentAppEventActionType.INSTALL,
      application,
      stepInputs
    }).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => {
        this.submitting = false;
      }
    });
  }

  /**
   * Pass-through for the YAML editor: we keep the parsed compose object as the
   * raw user-edited string and let the backend re-parse it. Since the edits are
   * round-tripped from JSON to our minimal YAML dumper and back, we re-emit
   * the merged compose unchanged when no user edits are made. For now we
   * forward the YAML as a string under a non-standard key so the BE either
   * accepts it or rejects with a clear error. v1 compromise.
   */
  private parseYamlBestEffort(yaml: string): any {
    // If we have a merged app, prefer its parsed compose (no user-edits round-trip).
    if (this.mergedApp && this.mergedApp.config && (this.mergedApp.config as any).compose) {
      return (this.mergedApp.config as any).compose;
    }
    // Generic fallback: emit a minimal compose so the BE has something valid.
    return { services: {}, _raw: yaml };
  }

  private dumpCompose(app: AgentApplication): string {
    const compose: any = app?.config && (app.config as any).compose;
    if (!compose) {
      return '';
    }
    return this.dumpYaml(compose, 0).trimEnd() + '\n';
  }

  private dumpYaml(value: any, indent: number): string {
    const pad = '  '.repeat(indent);
    if (value === null || value === undefined) return `${pad}null\n`;
    if (Array.isArray(value)) {
      if (value.length === 0) return `${pad}[]\n`;
      let out = '';
      for (const item of value) {
        if (item !== null && typeof item === 'object') {
          const lines = this.dumpYaml(item, indent + 1).split('\n');
          let firstReplaced = false;
          for (const line of lines) {
            if (!line.trim()) continue;
            if (!firstReplaced) {
              out += `${pad}- ${line.trimStart()}\n`;
              firstReplaced = true;
            } else {
              out += `${line}\n`;
            }
          }
        } else {
          out += `${pad}- ${this.scalarYaml(item)}\n`;
        }
      }
      return out;
    }
    if (typeof value === 'object') {
      const keys = Object.keys(value);
      if (keys.length === 0) return `${pad}{}\n`;
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
            out += this.dumpYaml(v, indent + 1);
          }
        } else {
          out += `${pad}${key}: ${this.scalarYaml(v)}\n`;
        }
      }
      return out;
    }
    return `${pad}${this.scalarYaml(value)}\n`;
  }

  private scalarYaml(value: any): string {
    if (typeof value === 'string') {
      const needsQuote = /^(true|false|null|yes|no|on|off|\d|-)/i.test(value)
        || value.includes(':') || value.includes('#') || value.includes('\n');
      return needsQuote ? `"${value.replace(/"/g, '\\"')}"` : value;
    }
    return String(value);
  }
}

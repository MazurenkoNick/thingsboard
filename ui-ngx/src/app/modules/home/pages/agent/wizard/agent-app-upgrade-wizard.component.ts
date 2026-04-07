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
  AgentAppTemplate
} from '@shared/models/agent.models';

export interface AgentAppUpgradeWizardData {
  application: AgentApplication;
}

interface VolumeChoice {
  key: string;
  selected: boolean;
}

@Component({
  selector: 'tb-agent-app-upgrade-wizard',
  templateUrl: './agent-app-upgrade-wizard.component.html',
  styleUrls: ['./agent-app-upgrade-wizard.component.scss']
})
export class AgentAppUpgradeWizardComponent
  extends DialogComponent<AgentAppUpgradeWizardComponent, boolean>
  implements OnInit {

  application: AgentApplication;
  newTemplate: AgentAppTemplate | null = null;
  mergedApp: AgentApplication | null = null;

  fromVersion: string | null = null;
  toVersion: string | null = null;

  upgradeSteps: AgentAppStep[] = [];
  backupVolumeStep: AgentAppStep | null = null;
  volumes: VolumeChoice[] = [];

  templateYaml = '';
  currentYaml = '';

  loading = true;
  loadError = '';
  submitting = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppUpgradeWizardData,
              public dialogRef: MatDialogRef<AgentAppUpgradeWizardComponent, boolean>) {
    super(store, router, dialogRef);
    this.application = data.application;
    this.fromVersion = (this.application as any).currentVersion || null;
  }

  ngOnInit() {
    this.currentYaml = this.dumpCompose(this.application);
    this.resolveNewTemplate();
  }

  private resolveNewTemplate() {
    // 1. If desiredTemplateId is already set, fetch it directly.
    const desiredId = (this.application as any).desiredTemplateId?.id;
    if (desiredId) {
      this.fetchTemplateById(desiredId);
      return;
    }
    // 2. Otherwise fetch the current template, then resolve nextVersion.
    if (!this.application.templateId?.id) {
      this.failLoad('agent.app-upgrade-no-template');
      return;
    }
    this.agentService.getAgentAppTemplateById(this.application.templateId.id).subscribe({
      next: current => {
        if (!current.nextVersion) {
          this.failLoad('agent.app-upgrade-no-next-version');
          return;
        }
        // Use latest as the canonical fetch — backend returns the head template
        // for (appType, configType), which equals the next version we expect.
        const configType = current.config?.type || 'DOCKER_COMPOSE';
        this.agentService.getLatestAgentAppTemplate(current.appType, configType).subscribe({
          next: latest => this.useNewTemplate(latest),
          error: () => this.failLoad('agent.app-upgrade-load-failed')
        });
      },
      error: () => this.failLoad('agent.app-upgrade-load-failed')
    });
  }

  private fetchTemplateById(templateId: string) {
    this.agentService.getAgentAppTemplateById(templateId).subscribe({
      next: t => this.useNewTemplate(t),
      error: () => this.failLoad('agent.app-upgrade-load-failed')
    });
  }

  private useNewTemplate(template: AgentAppTemplate) {
    this.newTemplate = template;
    this.toVersion = template.currentVersion || null;
    this.upgradeSteps = (template.upgradeSteps || []).filter(s => !s.templateOnly);
    this.backupVolumeStep = this.upgradeSteps.find(s => s.type === AgentAppStepType.BACKUP_VOLUME) || null;
    // Call mergeForPreview to get the merged compose for the diff and to power
    // the volume keys list (the merged compose may add or rename volumes).
    this.agentService.mergeForPreview(template.id.id, this.application).subscribe({
      next: merged => {
        this.mergedApp = merged;
        this.templateYaml = this.dumpCompose(merged);
        this.volumes = this.parseVolumeKeys(merged).map(key => ({ key, selected: true }));
        this.loading = false;
      },
      error: () => this.failLoad('agent.app-upgrade-merge-failed')
    });
  }

  private failLoad(messageKey: string) {
    this.loadError = this.translate.instant(messageKey);
    this.loading = false;
  }

  private parseVolumeKeys(app: AgentApplication): string[] {
    const compose: any = app?.config && (app.config as any).compose;
    if (!compose || !compose.volumes || typeof compose.volumes !== 'object') {
      return [];
    }
    return Object.keys(compose.volumes);
  }

  /**
   * Minimal YAML dumper for docker-compose objects. Handles the shapes the
   * agent backend actually emits: nested objects, string/number/boolean
   * scalars, arrays of scalars, and arrays of objects. No anchors, no flow
   * style. This is intentionally limited — we just need readable side-by-side
   * output for the v1 diff view.
   */
  private dumpCompose(app: AgentApplication): string {
    const compose: any = app?.config && (app.config as any).compose;
    if (!compose) {
      return '';
    }
    return this.dumpYaml(compose, 0).trimEnd() + '\n';
  }

  private dumpYaml(value: any, indent: number): string {
    const pad = '  '.repeat(indent);
    if (value === null || value === undefined) {
      return `${pad}null\n`;
    }
    if (Array.isArray(value)) {
      if (value.length === 0) {
        return `${pad}[]\n`;
      }
      let out = '';
      for (const item of value) {
        if (item !== null && typeof item === 'object') {
          const lines = this.dumpYaml(item, indent + 1).split('\n');
          // Replace the first non-empty line's leading padding with "- "
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
      if (keys.length === 0) {
        return `${pad}{}\n`;
      }
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
      // Quote strings that look like numbers, booleans, or contain colons.
      const needsQuote = /^(true|false|null|yes|no|on|off|\d|-)/i.test(value)
        || value.includes(':')
        || value.includes('#')
        || value.includes('\n');
      return needsQuote ? `"${value.replace(/"/g, '\\"')}"` : value;
    }
    if (typeof value === 'boolean' || typeof value === 'number') {
      return String(value);
    }
    return String(value);
  }

  toggleVolume(v: VolumeChoice) {
    v.selected = !v.selected;
  }

  get selectedVolumeCount(): number {
    return this.volumes.filter(v => v.selected).length;
  }

  cancel() {
    this.dialogRef.close(false);
  }

  submit() {
    if (this.submitting || !this.mergedApp || !this.newTemplate) {
      return;
    }
    this.submitting = true;
    const stepInputs: { [stepId: string]: any } = {};
    if (this.backupVolumeStep) {
      stepInputs[this.backupVolumeStep.id] = {
        backupVolumes: this.volumes.filter(v => v.selected).map(v => v.key),
        type: AgentAppStepType.BACKUP_VOLUME
      };
    }
    this.agentService.createAgentAppEvent(this.mergedApp.id.id, {
      actionType: AgentAppEventActionType.UPGRADE,
      application: this.mergedApp,
      stepInputs
    }).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => {
        this.submitting = false;
      }
    });
  }

  get appType(): AgentApplicationType | undefined {
    return this.application?.appType;
  }
}

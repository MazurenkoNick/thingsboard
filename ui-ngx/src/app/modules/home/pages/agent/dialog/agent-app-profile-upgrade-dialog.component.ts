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

import { Component, ElementRef, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import {
  AgentAppProfile,
  AgentAppStepType,
  AgentAppTemplate,
} from '@shared/models/agent.models';
import * as YAML from 'yaml';
import { getAceDiff } from '@shared/models/ace/ace.models';
import { Ace } from 'ace-builds';

export interface AgentAppProfileUpgradeDialogData {
  profile: AgentAppProfile;
}

@Component({
  selector: 'tb-agent-app-profile-upgrade-dialog',
  templateUrl: './agent-app-profile-upgrade-dialog.component.html',
  styleUrls: ['../wizard/agent-app-install-wizard.component.scss']
})
export class AgentAppProfileUpgradeDialogComponent
  extends DialogComponent<AgentAppProfileUpgradeDialogComponent, boolean>
  implements OnInit, OnDestroy {

  @ViewChild('diffViewer', { static: false })
  diffViewerElmRef: ElementRef<HTMLElement>;

  profile: AgentAppProfile;
  fromVersion: string | null = null;
  toVersion: string | null = null;
  template: AgentAppTemplate | null = null;

  proposedYaml = '';
  currentYaml = '';
  composeYaml = '';
  private differ: any = null;

  loadingTemplate = true;
  loadError = '';
  submitting = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppProfileUpgradeDialogData,
              public dialogRef: MatDialogRef<AgentAppProfileUpgradeDialogComponent, boolean>) {
    super(store, router, dialogRef);
    this.profile = data.profile;
  }

  ngOnInit() {
    this.resolveUpgradeTemplate();
  }

  ngOnDestroy() {
    if (this.differ) {
      try { this.differ.destroy(); } catch (_) {}
      this.differ = null;
    }
  }

  cancel() {
    this.dialogRef.close(false);
  }

  canSubmit(): boolean {
    return !!this.template && !this.submitting && !this.loadError;
  }

  submit() {
    if (!this.canSubmit()) { return; }
    this.submitting = true;

    const updated: any = {
      ...this.profile,
      templateId: this.template!.id,
      config: {
        ...((this.profile.config as any) || { type: 'DOCKER_COMPOSE' }),
        compose: this.parseYamlBestEffort(this.composeYaml)
      }
    };

    this.agentService.saveAgentAppProfile(updated).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => { this.submitting = false; }
    });
  }

  private resolveUpgradeTemplate() {
    if (!this.profile.templateId?.id) {
      this.failLoad('agent.app-upgrade-no-template');
      return;
    }
    this.agentService.getAgentAppTemplateById(this.profile.templateId.id).subscribe({
      next: current => {
        this.fromVersion = current.currentVersion || null;
        if (!current.nextVersion) {
          this.failLoad('agent.app-upgrade-no-next-version');
          return;
        }
        const configType = current.config?.type || 'DOCKER_COMPOSE';
        this.agentService.getAgentAppTemplateByVersion(
          current.appType, configType, current.nextVersion
        ).subscribe({
          next: next => this.applyUpgradeTemplate(next),
          error: () => this.failLoad('agent.app-upgrade-load-failed')
        });
      },
      error: () => this.failLoad('agent.app-upgrade-load-failed')
    });
  }

  private applyUpgradeTemplate(tpl: AgentAppTemplate) {
    this.template = tpl;
    this.toVersion = tpl.currentVersion || null;
    this.proposedYaml = this.dumpRawTemplateCompose(tpl);
    this.currentYaml = this.dumpCompose(this.profile);
    this.composeYaml = this.currentYaml;
    this.loadingTemplate = false;
    setTimeout(() => this.initDiff(), 0);
  }

  private failLoad(messageKey: string) {
    this.loadError = this.translate.instant(messageKey);
    this.loadingTemplate = false;
  }

  private initDiff() {
    if (!this.diffViewerElmRef?.nativeElement) {
      setTimeout(() => this.initDiff(), 50);
      return;
    }
    getAceDiff().subscribe((AceDiffCtor) => {
      this.differ = new AceDiffCtor({
        element: this.diffViewerElmRef.nativeElement,
        mode: 'ace/mode/text',
        left: { copyLinkEnabled: true, editable: false, content: this.proposedYaml },
        right: { copyLinkEnabled: false, editable: true, content: this.currentYaml }
      });
      const leftEditor: Ace.Editor = this.differ.getEditors().left;
      const rightEditor: Ace.Editor = this.differ.getEditors().right;
      leftEditor.setShowFoldWidgets(false);
      rightEditor.setShowFoldWidgets(false);
      leftEditor.getSession().setMode('ace/mode/yaml');
      rightEditor.getSession().setMode('ace/mode/yaml');
      (leftEditor as any).setOption('scrollPastEnd', false);
      (rightEditor as any).setOption('scrollPastEnd', false);
      leftEditor.renderer.setScrollMargin(0, 0, 0, 0);
      rightEditor.renderer.setScrollMargin(0, 0, 0, 0);
      this.forceEditorFontSize(leftEditor, 12);
      this.forceEditorFontSize(rightEditor, 12);
      rightEditor.getSession().on('change', () => {
        this.composeYaml = rightEditor.getValue();
        if (this.differ) { this.differ.diff(); }
      });
      setTimeout(() => {
        leftEditor.resize(true);
        rightEditor.resize(true);
        if (this.differ) { this.differ.diff(); }
      }, 50);
    });
  }

  private forceEditorFontSize(editor: Ace.Editor, px: number) {
    const container = (editor as any).container as HTMLElement | undefined;
    if (container?.style) {
      container.style.setProperty('font-size', `${px}px`, 'important');
    }
    editor.setFontSize(px);
    const renderer: any = editor.renderer;
    if (typeof renderer.updateFontSize === 'function') { renderer.updateFontSize(); }
    if (typeof renderer.onResize === 'function') { renderer.onResize(true); }
  }

  private dumpRawTemplateCompose(template: AgentAppTemplate): string {
    const steps = (template.startSteps || []);
    for (const step of steps) {
      const anyStep = step as any;
      if (step.type === AgentAppStepType.COMPOSE_TEMPLATE && anyStep.composeTemplates) {
        const keys = Object.keys(anyStep.composeTemplates);
        if (keys.length) {
          return this.dumpYaml(anyStep.composeTemplates[keys[0]], 0).trimEnd() + '\n';
        }
      }
    }
    const compose: any = (template.config as any)?.compose;
    return compose ? (this.dumpYaml(compose, 0).trimEnd() + '\n') : '';
  }

  private dumpCompose(entity: any): string {
    const compose: any = entity?.config && (entity.config as any).compose;
    if (!compose) { return ''; }
    return this.dumpYaml(compose, 0).trimEnd() + '\n';
  }

  private parseYamlBestEffort(yaml: string): any {
    if (yaml?.trim()) {
      try { return YAML.parse(yaml); } catch (_) {}
    }
    return (this.profile?.config as any)?.compose || { services: {} };
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
            if (!firstReplaced) { out += `${pad}- ${line.trimStart()}\n`; firstReplaced = true; }
            else { out += `${line}\n`; }
          }
        } else { out += `${pad}- ${this.scalarYaml(item)}\n`; }
      }
      return out;
    }
    if (typeof value === 'object') {
      const keys = Object.keys(value);
      if (keys.length === 0) return `${pad}{}\n`;
      let out = '';
      for (const key of keys) {
        const v = value[key];
        if (v === null || v === undefined) { out += `${pad}${key}:\n`; }
        else if (typeof v === 'object') {
          if (Array.isArray(v) && v.length === 0) { out += `${pad}${key}: []\n`; }
          else if (!Array.isArray(v) && Object.keys(v).length === 0) { out += `${pad}${key}:\n`; }
          else { out += `${pad}${key}:\n`; out += this.dumpYaml(v, indent + 1); }
        } else { out += `${pad}${key}: ${this.scalarYaml(v)}\n`; }
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

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
  AgentApplicationType,
  AgentAppTemplate,
} from '@shared/models/agent.models';
import * as YAML from 'yaml';
import { getAceDiff } from '@shared/models/ace/ace.models';
import { Ace } from 'ace-builds';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';

export interface AgentAppProfileWizardData {
  profile?: AgentAppProfile;
}

interface TypeCard {
  type: AgentApplicationType;
  icon: string;
  labelKey: string;
  descKey: string;
}

@Component({
  selector: 'tb-agent-app-profile-wizard',
  templateUrl: './agent-app-profile-wizard.component.html',
  styleUrls: ['./agent-app-install-wizard.component.scss']
})
export class AgentAppProfileWizardComponent
  extends DialogComponent<AgentAppProfileWizardComponent, AgentAppProfile>
  implements OnInit, OnDestroy {

  @ViewChild('diffViewer', { static: false })
  diffViewerElmRef: ElementRef<HTMLElement>;
  private differ: any = null;
  private pendingDiffInit = false;

  // Left (read-only) = raw template compose; right (editable) = same content,
  // user-editable. This is the value persisted on submit.
  proposedYaml = '';
  currentYaml = '';

  typeCards: TypeCard[] = [
    { type: AgentApplicationType.GENERIC, icon: 'inventory_2', labelKey: 'agent.app-install-type-generic', descKey: 'agent.app-install-type-generic-desc' },
    { type: AgentApplicationType.EDGE, icon: 'router', labelKey: 'agent.app-install-type-edge', descKey: 'agent.app-install-type-edge-desc' },
    { type: AgentApplicationType.GATEWAY, icon: 'hub', labelKey: 'agent.app-install-type-gateway', descKey: 'agent.app-install-type-gateway-desc' }
  ];

  selectedType: AgentApplicationType | null = null;
  template: AgentAppTemplate | null = null;
  composeType: string | null = null;
  loadingTemplate = false;
  loadError = '';

  // Template version selection
  availableTemplates: AgentAppTemplate[] = [];
  selectedTemplateId: string | null = null;
  loadingTemplates = false;
  private templatesByTypeCache = new Map<AgentApplicationType, AgentAppTemplate[]>();

  profileName = '';
  profileDescription = '';
  composeYaml = '';

  submitting = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppProfileWizardData,
              public dialogRef: MatDialogRef<AgentAppProfileWizardComponent, AgentAppProfile>) {
    super(store, router, dialogRef);
  }

  ngOnInit() {
    // Prefetch templates for all types so type selection is instant.
    this.typeCards.forEach(card => {
      this.agentService.getAgentAppTemplatesByAppType(card.type).subscribe({
        next: templates => this.templatesByTypeCache.set(card.type, templates),
        error: () => {}
      });
    });
  }

  get titleKey(): string {
    return 'agent.app-profile-wizard-title';
  }

  selectType(type: AgentApplicationType) {
    if (this.selectedType === type) {
      return;
    }
    this.selectedType = type;
    this.template = null;
    this.composeType = null;
    this.composeYaml = '';
    this.selectedTemplateId = null;
    this.profileName = this.defaultProfileName(type);
    this.loadError = '';
    this.loadTemplatesForType(type);
  }

  private loadTemplatesForType(type: AgentApplicationType) {
    const cached = this.templatesByTypeCache.get(type);
    if (cached) {
      this.availableTemplates = [...cached]
        .sort((a, b) => (b.currentVersion || '').localeCompare(a.currentVersion || ''));
      return;
    }
    this.loadingTemplates = true;
    this.agentService.getAgentAppTemplatesByAppType(type).subscribe({
      next: templates => {
        this.templatesByTypeCache.set(type, templates);
        this.availableTemplates = [...templates]
          .sort((a, b) => (b.currentVersion || '').localeCompare(a.currentVersion || ''));
        this.loadingTemplates = false;
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingTemplates = false;
      }
    });
  }

  onTemplateSelected(templateId: string) {
    this.selectedTemplateId = templateId;
    const tpl = this.availableTemplates.find(t => t.id.id === templateId);
    if (tpl) {
      this.applyTemplate(tpl);
    }
  }

  private applyTemplate(tpl: AgentAppTemplate) {
    this.loadingTemplate = false;
    this.template = tpl;
    this.composeType = this.pickComposeType(tpl);
    // Side-by-side preview seeded from the raw template compose. No
    // mergeForPreview call — the server will apply profile semantics on save.
    this.proposedYaml = this.dumpRawTemplateCompose(tpl);
    this.currentYaml = this.proposedYaml;
    this.composeYaml = this.currentYaml;
    this.scheduleDiffInit();
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

  cancel() {
    this.dialogRef.close(undefined);
  }

  canProceedFromType(): boolean {
    return !!this.selectedType && !!this.selectedTemplateId && !!this.template
      && !this.loadingTemplate && !this.loadingTemplates && !this.loadError;
  }

  canSubmit(): boolean {
    return !!this.selectedType && !!this.profileName?.trim() && !!this.composeYaml?.trim() && !this.submitting;
  }

  submit() {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting = true;

    const profile: any = {
      name: this.profileName.trim(),
      appType: this.selectedType,
      templateId: this.template?.id,
      config: {
        type: 'DOCKER_COMPOSE',
        compose: this.parseYamlBestEffort(this.composeYaml)
      },
      description: this.profileDescription?.trim() || undefined,
    };

    this.agentService.saveAgentAppProfile(profile).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: () => {
        this.submitting = false;
      }
    });
  }

  ngOnDestroy(): void {
    if (this.differ) {
      try { this.differ.destroy(); } catch (_) {}
      this.differ = null;
    }
  }

  // --- Diff viewer ---

  private scheduleDiffInit() {
    if (this.pendingDiffInit) { return; }
    this.pendingDiffInit = true;
    setTimeout(() => this.initDiff(), 0);
  }

  private initDiff() {
    this.pendingDiffInit = false;
    if (!this.diffViewerElmRef || !this.diffViewerElmRef.nativeElement) {
      setTimeout(() => this.initDiff(), 50);
      return;
    }
    if (this.differ) {
      try { this.differ.destroy(); } catch (_) {}
      this.differ = null;
    }
    getAceDiff().subscribe((AceDiffCtor) => {
      this.differ = new AceDiffCtor({
        element: this.diffViewerElmRef.nativeElement,
        mode: 'ace/mode/text',
        left:  { copyLinkEnabled: true,  editable: false, content: this.proposedYaml },
        right: { copyLinkEnabled: false, editable: true,  content: this.currentYaml }
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
      confineWheelToAceEditor((leftEditor as any).container, leftEditor,
        () => leftEditor.isFocused());
      confineWheelToAceEditor((rightEditor as any).container, rightEditor,
        () => rightEditor.isFocused());
      confineWheelToAceEditor(this.diffViewerElmRef?.nativeElement, rightEditor,
        () => leftEditor.isFocused() || rightEditor.isFocused());
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

  // --- YAML utilities ---

  private pickComposeType(template: AgentAppTemplate): string {
    const steps = (template.startSteps || []);
    for (const step of steps) {
      const anyStep = step as any;
      if (step.type === AgentAppStepType.COMPOSE_TEMPLATE && anyStep.composeTemplates) {
        const keys = Object.keys(anyStep.composeTemplates);
        if (keys.length) { return keys[0]; }
      }
    }
    return 'default';
  }

  private defaultProfileName(type: AgentApplicationType): string {
    switch (type) {
      case AgentApplicationType.EDGE: return 'Edge Profile';
      case AgentApplicationType.GATEWAY: return 'Gateway Profile';
      default: return 'App Profile';
    }
  }

  private parseYamlBestEffort(yaml: string): any {
    if (yaml?.trim()) {
      try { return YAML.parse(yaml); } catch (_) {}
    }
    return { services: {} };
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

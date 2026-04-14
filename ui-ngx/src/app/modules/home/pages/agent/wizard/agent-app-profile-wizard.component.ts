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
import { getAce } from '@shared/models/ace/ace.models';
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

  private installEditor: Ace.Editor | null = null;
  private installEditorSettingValue = false;
  @ViewChild('installYamlEditor', { static: false })
  set installYamlEditorRef(ref: ElementRef<HTMLElement> | undefined) {
    if (ref && !this.installEditor) {
      this.initInstallEditor(ref.nativeElement);
    }
  }

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

  mergedProfile: AgentAppProfile | null = null;
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
    this.mergedProfile = null;
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
    this.runMergeForPreview(tpl);
  }

  private runMergeForPreview(tpl: AgentAppTemplate) {
    const type = this.selectedType!;
    const draft: any = {
      name: this.profileName,
      appType: type,
      templateId: tpl.id,
    };
    this.agentService.mergeProfileForPreview(tpl.id.id, draft, this.composeType || undefined).subscribe({
      next: merged => {
        this.mergedProfile = merged;
        this.composeYaml = this.dumpCompose(merged);
        this.syncInstallEditor();
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-merge-failed');
      }
    });
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
    if (this.installEditor) {
      try { this.installEditor.destroy(); } catch (_) {}
      this.installEditor = null;
    }
  }

  // --- Ace editor ---

  private initInstallEditor(host: HTMLElement) {
    getAce().subscribe((ace) => {
      const editor: Ace.Editor = ace.edit(host);
      editor.setTheme('ace/theme/textmate');
      editor.session.setMode('ace/mode/yaml');
      editor.session.setUseWrapMode(false);
      editor.setShowPrintMargin(false);
      (editor as any).setOption('scrollPastEnd', false);
      editor.renderer.setScrollMargin(0, 0, 0, 0);
      this.forceEditorFontSize(editor, 12);
      editor.setOption('tabSize', 2);
      editor.setOption('useSoftTabs', true);
      editor.setOption('showLineNumbers', true);
      editor.setOption('highlightActiveLine', false);
      editor.setValue(this.composeYaml || '', -1);
      editor.getSession().on('change', () => {
        this.installEditorSettingValue = true;
        this.composeYaml = editor.getValue();
        this.installEditorSettingValue = false;
      });
      this.installEditor = editor;
      this.confineWheelToEditor((editor as any).container, editor);
      setTimeout(() => editor.resize(true), 0);
    });
  }

  private syncInstallEditor() {
    if (this.installEditor && !this.installEditorSettingValue) {
      const current = this.installEditor.getValue();
      if (current !== (this.composeYaml || '')) {
        this.installEditor.setValue(this.composeYaml || '', -1);
      }
    }
  }

  private confineWheelToEditor(host: HTMLElement | null | undefined, editor: Ace.Editor | null) {
    confineWheelToAceEditor(host, editor);
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

  private dumpCompose(entity: any): string {
    const compose: any = entity?.config && (entity.config as any).compose;
    if (!compose) { return ''; }
    return this.dumpYaml(compose, 0).trimEnd() + '\n';
  }

  private parseYamlBestEffort(yaml: string): any {
    if (yaml?.trim()) {
      try { return YAML.parse(yaml); } catch (_) {}
    }
    if (this.mergedProfile?.config && (this.mergedProfile.config as any).compose) {
      return (this.mergedProfile.config as any).compose;
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

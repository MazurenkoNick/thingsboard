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
  AgentApplication,
  AgentAppEventActionType,
  AgentAppProfile,
  AgentAppStep,
  AgentAppStepType,
  AgentApplicationType,
  AgentAppTemplate,
  AgentInfo,
  AgentApplicationOrigin
} from '@shared/models/agent.models';
import { AgentId } from '@shared/models/id/agent-id';
import * as YAML from 'yaml';
import { getAce, getAceDiff } from '@shared/models/ace/ace.models';
import { Ace } from 'ace-builds';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';
import {
  applyCredentialValuesToCompose,
  CredField,
  credentialSchemaFor,
  extractCredentialValues,
  visibleCredentialFields as visibleCredFieldsFor
} from '@home/pages/agent/util/agent-credentials';

export interface AgentAppInstallWizardData {
  agentId: string;
  agent: AgentInfo;
  mode?: 'install' | 'update' | 'upgrade';
  application?: AgentApplication;
}

interface VolumeChoice {
  key: string;
  selected: boolean;
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
  implements OnInit, OnDestroy {

  @ViewChild('diffViewer', { static: false })
  diffViewerElmRef: ElementRef<HTMLElement>;

  // Install-mode single yaml editor. Element only appears when step 2 renders,
  // so we use a setter to init the editor lazily on first attach.
  private installEditor: Ace.Editor | null = null;
  private installEditorSettingValue = false;
  @ViewChild('installYamlEditor', { static: false })
  set installYamlEditorRef(ref: ElementRef<HTMLElement> | undefined) {
    if (ref && !this.installEditor && this.mode === 'install') {
      this.initInstallEditor(ref.nativeElement);
    }
  }

  agentId: string;
  agent: AgentInfo;
  mode: 'install' | 'update' | 'upgrade' = 'install';
  existingApplication: AgentApplication | null = null;

  // Upgrade-mode state. Populated when mode === 'upgrade' in ngOnInit.
  fromVersion: string | null = null;
  toVersion: string | null = null;
  upgradeSteps: AgentAppStep[] = [];
  backupVolumeStep: AgentAppStep | null = null;
  backupVolumes: VolumeChoice[] = [];

  // Update mode: side-by-side diff state.
  // Left (read-only): merged template preview (what the template would add).
  // Right (editable): currently persisted compose — this is what gets saved.
  proposedYaml = '';
  currentYaml = '';
  private differ: any = null;
  private pendingDiffInit = false;

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

  appName = '';
  composeYaml = '';
  pullImages = false;
  hasPullImagesStep = false;
  pullImagesStep: AgentAppStep | null = null;

  mergedApp: AgentApplication | null = null;
  submitting = false;

  // Template cache so re-selecting a type in the install wizard is instant.
  // Install mode pre-warms all three types on open; selectType reads from here
  // and skips the HTTP round-trip (and the "Loading template…" flash).
  private templateCache = new Map<AgentApplicationType, AgentAppTemplate>();

  // Profile-based install state
  useProfile = false;
  availableProfiles: AgentAppProfile[] = [];
  selectedProfile: AgentAppProfile | null = null;
  loadingProfiles = false;

  // Profile-based install: credential env values extracted from the compose so
  // the user can edit them even though the compose editor is read-only. Keys
  // match AgentApplicationType.credentialEnvKeys on the backend.
  credentialValues: Record<string, string> = {};

  // Update mode for profile-managed apps: when checked, the backend won't
  // re-resolve compose from the (possibly upgraded) profile — only the
  // credentials carried by the request are applied.
  skipProfileRefetch = false;

  get isProfileManagedUpdate(): boolean {
    return this.mode === 'update' && !!this.existingApplication?.applicationProfileId;
  }

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppInstallWizardData,
              public dialogRef: MatDialogRef<AgentAppInstallWizardComponent, boolean>) {
    super(store, router, dialogRef);
    this.agentId = data.agentId;
    this.agent = data.agent;
    this.mode = data.mode || 'install';
    this.existingApplication = data.application || null;
  }

  ngOnInit() {
    if (this.mode === 'upgrade' && this.existingApplication) {
      this.appName = this.existingApplication.name;
      this.selectedType = this.existingApplication.appType;
      this.fromVersion = (this.existingApplication as any).currentVersion || null;
      this.loadingTemplate = true;
      this.resolveUpgradeTemplate();
    } else if (this.mode === 'update' && this.existingApplication) {
      // Pre-seed from existing app and load template + merge for preview.
      this.appName = this.existingApplication.name;
      this.selectType(this.existingApplication.appType);
    } else if (this.mode === 'install') {
      // Prefetch all three templates in the background so clicking a type card
      // doesn't trigger a visible "Loading template…" flash on the first click.
      this.typeCards.forEach(card => {
        this.agentService.getLatestAgentAppTemplate(card.type, 'DOCKER_COMPOSE').subscribe({
          next: tpl => this.templateCache.set(card.type, tpl),
          error: () => { /* swallow — selectType will retry on demand */ }
        });
      });
    }
  }

  get titleKey(): string {
    switch (this.mode) {
      case 'upgrade': return 'agent.app-upgrade-title';
      case 'update': return 'agent.app-update-wizard-title';
      default: return 'agent.app-install-title';
    }
  }

  get ctaKey(): string {
    switch (this.mode) {
      case 'upgrade': return 'agent.app-upgrade-cta';
      case 'update': return 'agent.app-update-cta';
      default: return 'agent.app-install-cta';
    }
  }

  get subtitleParams(): any {
    if (this.mode === 'upgrade') {
      return {
        name: this.existingApplication?.name,
        from: this.fromVersion || '—',
        to: this.toVersion || '—'
      };
    }
    return this.mode === 'update'
      ? { name: this.existingApplication?.name }
      : { name: this.agent?.name };
  }

  get subtitleKey(): string {
    switch (this.mode) {
      case 'upgrade': return 'agent.app-upgrade-heading';
      case 'update': return 'agent.app-update-on-app';
      default: return 'agent.app-install-on-agent';
    }
  }

  selectType(type: AgentApplicationType) {
    if (this.selectedType === type) {
      return;
    }
    this.selectedType = type;
    this.template = null;
    this.composeType = null;
    this.composeYaml = '';
    this.mergedApp = null;
    this.selectedProfile = null;
    this.availableProfiles = [];
    if (this.mode !== 'update') {
      this.appName = this.defaultAppName(type);
    }
    this.loadError = '';

    if (this.useProfile) {
      this.loadProfilesForType(type);
    } else {
      this.loadTemplateForType(type);
    }
  }

  toggleUseProfile(value: boolean) {
    this.useProfile = value;
    this.selectedProfile = null;
    this.template = null;
    this.composeYaml = '';
    this.mergedApp = null;
    this.loadError = '';
    this.credentialValues = {};
    this.syncInstallEditor();
    this.applyInstallEditorReadOnly();
    if (this.selectedType) {
      if (value) {
        this.loadProfilesForType(this.selectedType);
      } else {
        this.availableProfiles = [];
        this.loadTemplateForType(this.selectedType);
      }
    }
  }

  selectProfile(profile: AgentAppProfile) {
    if (!profile) { return; }
    this.selectedProfile = profile;
    this.appName = profile.name;
    // Load the template from the profile to get step inputs
    if (profile.templateId?.id) {
      this.loadingTemplate = true;
      this.agentService.getAgentAppTemplateById(profile.templateId.id).subscribe({
        next: tpl => {
          this.template = tpl;
          this.scanStartSteps(tpl);
          this.composeType = this.pickComposeType(tpl);
          this.composeYaml = this.dumpCompose(profile as any);
          this.initCredentialValues();
          this.syncInstallEditor();
          this.applyInstallEditorReadOnly();
          this.loadingTemplate = false;
        },
        error: () => {
          this.loadError = this.translate.instant('agent.app-install-template-failed');
          this.loadingTemplate = false;
        }
      });
    }
  }

  get showCredentialForm(): boolean {
    if (!this.selectedType || credentialSchemaFor(this.selectedType).length === 0) {
      return false;
    }
    if (this.mode === 'install') {
      return this.useProfile && !!this.selectedProfile;
    }
    if (this.mode === 'update') {
      return !!this.existingApplication?.applicationProfileId;
    }
    return false;
  }

  get visibleCredentialFields(): CredField[] {
    return visibleCredFieldsFor(this.selectedType, this.credentialValues);
  }

  onCredentialChange(field: CredField, value: string) {
    this.credentialValues[field.key] = value ?? '';
    this.writeCredentialsToCompose();
  }

  private initCredentialValues() {
    const parsed = this.parseYamlBestEffort(this.composeYaml);
    this.credentialValues = extractCredentialValues(parsed, this.selectedType);
  }

  private writeCredentialsToCompose() {
    if (!this.selectedType) { return; }
    const parsed = this.parseYamlBestEffort(this.composeYaml);
    applyCredentialValuesToCompose(parsed, this.selectedType, this.credentialValues);
    this.composeYaml = this.dumpYaml(parsed, 0).trimEnd() + '\n';
    this.syncInstallEditor();
  }

  findProfileById(id: string): AgentAppProfile | undefined {
    return this.availableProfiles.find(p => p.id.id === id);
  }

  private loadProfilesForType(type: AgentApplicationType) {
    this.loadingProfiles = true;
    this.agentService.getAgentAppProfilesByAppType(type).subscribe({
      next: profiles => {
        this.availableProfiles = profiles;
        this.loadingProfiles = false;
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingProfiles = false;
      }
    });
  }

  private loadTemplateForType(type: AgentApplicationType) {
    const cached = this.templateCache.get(type);
    if (cached) {
      this.applyTemplate(cached);
      return;
    }
    this.loadingTemplate = true;
    this.agentService.getLatestAgentAppTemplate(type, 'DOCKER_COMPOSE').subscribe({
      next: tpl => {
        this.templateCache.set(type, tpl);
        this.applyTemplate(tpl);
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingTemplate = false;
      }
    });
  }

  private applyTemplate(tpl: AgentAppTemplate) {
    this.loadingTemplate = false;
    this.template = tpl;
    this.scanStartSteps(tpl);
    this.composeType = this.pickComposeType(tpl);
    // Profile-managed update: skip diff/preview merge — the user can only
    // edit credentials, so we hydrate the form from the existing app and
    // expose a skip-refetch checkbox below the cred inputs.
    if (this.mode === 'update' && this.existingApplication?.applicationProfileId) {
      this.composeYaml = this.dumpCompose(this.existingApplication);
      this.initCredentialValues();
      return;
    }
    this.runMergeForPreview(tpl);
  }

  private runMergeForPreview(tpl: AgentAppTemplate) {
    const type = this.selectedType!;
    const draft: AgentApplication = this.mode === 'update' && this.existingApplication
      ? ({ ...this.existingApplication, templateId: tpl.id } as any)
      : ({
          name: this.appName,
          appType: type,
          agentId: new AgentId(this.agentId) as any,
          templateId: tpl.id,
          origin: AgentApplicationOrigin.INSTALLED
        } as any);
    this.agentService.mergeForPreview(tpl.id.id, draft, this.composeType || undefined).subscribe({
      next: merged => {
        this.mergedApp = merged;
        if (this.mode === 'update' && this.existingApplication) {
          this.proposedYaml = this.dumpCompose(merged);
          this.currentYaml = this.dumpCompose(this.existingApplication);
          this.composeYaml = this.currentYaml;
          this.scheduleDiffInit();
        } else {
          this.composeYaml = this.dumpCompose(merged);
          this.syncInstallEditor();
        }
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-merge-failed');
      }
    });
  }

  // -------- Upgrade mode (mode === 'upgrade') --------

  private resolveUpgradeTemplate() {
    const app = this.existingApplication!;
    const desiredId = (app as any).desiredTemplateId?.id;
    if (desiredId) {
      this.agentService.getAgentAppTemplateById(desiredId).subscribe({
        next: t => this.applyUpgradeTemplate(t),
        error: () => this.failUpgradeLoad('agent.app-upgrade-load-failed')
      });
      return;
    }
    if (!app.templateId?.id) {
      this.failUpgradeLoad('agent.app-upgrade-no-template');
      return;
    }
    this.agentService.getAgentAppTemplateById(app.templateId.id).subscribe({
      next: current => {
        if (!current.nextVersion) {
          this.failUpgradeLoad('agent.app-upgrade-no-next-version');
          return;
        }
        // The detail endpoint doesn't populate currentVersion on the
        // application, so fromVersion is usually null coming in from
        // ngOnInit. Resolve it from the linked template so the "from → to"
        // row doesn't show a dash.
        if (!this.fromVersion && current.currentVersion) {
          this.fromVersion = current.currentVersion;
        }
        // Fetch the template whose currentVersion matches the current
        // template's nextVersion pointer — single-hop upgrade. Do NOT fall
        // back to "latest" because that would skip any intermediate versions
        // (and their upgradeSteps / migrations) on multi-hop chains.
        const configType = current.config?.type || 'DOCKER_COMPOSE';
        this.agentService.getAgentAppTemplateByVersion(
          current.appType, configType, current.nextVersion
        ).subscribe({
          next: next => this.applyUpgradeTemplate(next),
          error: () => this.failUpgradeLoad('agent.app-upgrade-load-failed')
        });
      },
      error: () => this.failUpgradeLoad('agent.app-upgrade-load-failed')
    });
  }

  private applyUpgradeTemplate(template: AgentAppTemplate) {
    this.template = template;
    this.toVersion = template.currentVersion || null;
    this.upgradeSteps = (template.upgradeSteps || []).filter(s => !s.templateOnly);

    // Scan upgradeSteps for input-required steps (backup volumes, pull images).
    this.backupVolumeStep = this.upgradeSteps.find(s => s.type === AgentAppStepType.BACKUP_VOLUME) || null;
    this.pullImagesStep = this.upgradeSteps.find(s =>
      (s.type === AgentAppStepType.COMPOSE_MIGRATION || s.type === AgentAppStepType.COMPOSE)
      && (s.state as any) && 'pullImages' in (s.state as any)
    ) || null;
    this.hasPullImagesStep = !!this.pullImagesStep;
    if (this.pullImagesStep) {
      this.pullImages = !!(this.pullImagesStep.state as any)?.pullImages;
    }

    // Volumes are backed up from the CURRENT app — the data we need to
    // preserve through the upgrade lives in the existing volumes.
    this.backupVolumes = this.parseVolumeKeys(this.existingApplication!)
      .map(key => ({ key, selected: true }));

    // Side-by-side diff: left = raw new template compose (no mergeForPreview),
    // right = current persisted compose. Both panes are read-only in upgrade
    // mode — this is preview-and-confirm, not edit.
    this.proposedYaml = this.dumpRawTemplateCompose(template);
    this.currentYaml = this.dumpCompose(this.existingApplication!);
    this.composeYaml = this.currentYaml;
    this.loadingTemplate = false;
    this.scheduleDiffInit();
  }

  private failUpgradeLoad(messageKey: string) {
    this.loadError = this.translate.instant(messageKey);
    this.loadingTemplate = false;
  }

  private dumpRawTemplateCompose(template: AgentAppTemplate): string {
    const steps = (template.startSteps || []);
    for (const step of steps) {
      const anyStep = step as any;
      if (step.type === AgentAppStepType.COMPOSE_TEMPLATE && anyStep.composeTemplates) {
        const keys = Object.keys(anyStep.composeTemplates);
        if (keys.length) {
          const compose = anyStep.composeTemplates[keys[0]];
          return this.dumpYaml(compose, 0).trimEnd() + '\n';
        }
      }
    }
    const compose: any = (template.config as any)?.compose;
    return compose ? (this.dumpYaml(compose, 0).trimEnd() + '\n') : '';
  }

  private parseVolumeKeys(app: AgentApplication): string[] {
    const compose: any = app?.config && (app.config as any).compose;
    if (!compose || !compose.volumes || typeof compose.volumes !== 'object') {
      return [];
    }
    return Object.keys(compose.volumes);
  }

  toggleBackupVolume(v: VolumeChoice) {
    v.selected = !v.selected;
  }

  get selectedBackupVolumeCount(): number {
    return this.backupVolumes.filter(v => v.selected).length;
  }

  /**
   * Find the first compose type key from the template's COMPOSE_TEMPLATE step
   * (ComposeTypeChoiceStep). For GENERIC and GATEWAY templates this is usually
   * a single entry; for EDGE it's typically `in_memory`/`kafka`/`hybrid` and
   * we pick the first one as a v1 default. Falls back to the literal string
   * "default" so the request still carries a value (the BE requires the param).
   */
  private pickComposeType(template: AgentAppTemplate): string {
    const steps = (template.startSteps || []);
    for (const step of steps) {
      const anyStep = step as any;
      if (step.type === AgentAppStepType.COMPOSE_TEMPLATE && anyStep.composeTemplates) {
        const keys = Object.keys(anyStep.composeTemplates);
        if (keys.length) {
          return keys[0];
        }
      }
    }
    return 'default';
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

  ngOnDestroy(): void {
    if (this.differ) {
      try { this.differ.destroy(); } catch (_) { /* no-op */ }
      this.differ = null;
    }
    if (this.installEditor) {
      try { this.installEditor.destroy(); } catch (_) { /* no-op */ }
      this.installEditor = null;
    }
  }

  private confineWheelToEditor(host: HTMLElement | null | undefined, editor: Ace.Editor | null) {
    if (!host || !editor) { return; }
    host.addEventListener('wheel', (ev: WheelEvent) => {
      ev.preventDefault();
      ev.stopPropagation();
      const session = editor.getSession();
      session.setScrollTop(session.getScrollTop() + ev.deltaY);
      if (ev.deltaX) {
        session.setScrollLeft(session.getScrollLeft() + ev.deltaX);
      }
    }, { passive: false });
  }

  private forceEditorFontSize(editor: Ace.Editor, px: number) {
    const container = (editor as any).container as HTMLElement | undefined;
    if (container?.style) {
      container.style.setProperty('font-size', `${px}px`, 'important');
    }
    editor.setFontSize(px);
    const renderer: any = editor.renderer;
    if (typeof renderer.updateFontSize === 'function') {
      renderer.updateFontSize();
    }
    if (typeof renderer.onResize === 'function') {
      renderer.onResize(true);
    }
  }

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
      this.applyInstallEditorReadOnly();
      this.confineWheelToEditor((editor as any).container, editor);
      // If composeYaml updates later (async mergeForPreview), push into editor.
      setTimeout(() => editor.resize(true), 0);
    });
  }

  private applyInstallEditorReadOnly() {
    if (!this.installEditor) { return; }
    const readOnly = this.useProfile && !!this.selectedProfile;
    this.installEditor.setReadOnly(readOnly);
    const cursorLayer = (this.installEditor.renderer as any).$cursorLayer;
    if (cursorLayer?.element?.style) {
      cursorLayer.element.style.display = readOnly ? 'none' : '';
    }
  }

  // Called from selectType / mergeForPreview when composeYaml changes programmatically.
  private syncInstallEditor() {
    if (this.installEditor && !this.installEditorSettingValue) {
      const current = this.installEditor.getValue();
      if (current !== (this.composeYaml || '')) {
        this.installEditor.setValue(this.composeYaml || '', -1);
      }
    }
  }

  private scheduleDiffInit() {
    if (this.pendingDiffInit) {
      return;
    }
    this.pendingDiffInit = true;
    // Defer until the step body is attached to the DOM.
    setTimeout(() => this.initDiff(), 0);
  }

  private initDiff() {
    this.pendingDiffInit = false;
    if (!this.diffViewerElmRef || !this.diffViewerElmRef.nativeElement) {
      // Element not yet in DOM (stepper step not rendered). Retry.
      setTimeout(() => this.initDiff(), 50);
      return;
    }
    if (this.differ) {
      try { this.differ.destroy(); } catch (_) { /* no-op */ }
      this.differ = null;
    }
    getAceDiff().subscribe((AceDiffCtor) => {
      this.differ = new AceDiffCtor({
        element: this.diffViewerElmRef.nativeElement,
        mode: 'ace/mode/text',
        left: {
          // Copy arrows enabled in both update and upgrade — users need to
          // cherry-pick template changes into their compose on the right.
          copyLinkEnabled: true,
          editable: false,
          content: this.proposedYaml
        },
        right: {
          copyLinkEnabled: false,
          editable: true,
          content: this.currentYaml
        }
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
      // Only confine the wheel when the target editor is focused, so the
      // wizard body can scroll past the diff viewer while neither pane is
      // active — matching the detail-page compose editor behavior.
      confineWheelToAceEditor((leftEditor as any).container, leftEditor,
        () => leftEditor.isFocused());
      confineWheelToAceEditor((rightEditor as any).container, rightEditor,
        () => rightEditor.isFocused());
      // Wheel on the diff-viewer host itself (e.g. center gutter with copy
      // arrows) is routed through the right/editable editor, but only while
      // one of the panes is focused.
      confineWheelToAceEditor(this.diffViewerElmRef?.nativeElement, rightEditor,
        () => leftEditor.isFocused() || rightEditor.isFocused());
      // Keep composeYaml in sync with the editable right side so
      // canSubmit/submit read fresh content.
      rightEditor.getSession().on('change', () => {
        this.composeYaml = rightEditor.getValue();
        if (this.differ) { this.differ.diff(); }
      });
      // Force a layout pass and re-run the diff so the copy arrows line up with
      // the actual rendered rows. Single pass — re-calling forceEditorFontSize
      // inside realign causes ace-diff to mis-position arrows when its row
      // metrics shift mid-render.
      setTimeout(() => {
        leftEditor.resize(true);
        rightEditor.resize(true);
        if (this.differ) { this.differ.diff(); }
      }, 50);
    });
  }

  cancel() {
    this.dialogRef.close(false);
  }

  canProceedFromType(): boolean {
    return !!this.selectedType && !this.loadingTemplate && !this.loadError;
  }

  canSubmit(): boolean {
    if (this.mode === 'upgrade') {
      return !!this.template && !!this.existingApplication && !this.submitting && !this.loadError;
    }
    if (this.useProfile) {
      return !!this.selectedType && !!this.selectedProfile && !!this.appName?.trim() && !!this.composeYaml?.trim() && !this.submitting;
    }
    return !!this.selectedType && !!this.appName?.trim() && !!this.composeYaml?.trim() && !this.submitting;
  }

  submit() {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting = true;

    // Upgrade path: ship the existing app with the new templateId, plus the
    // upgrade step inputs (backup volumes + pullImages). The BE's
    // UpgradeActionHandler re-resolves config from profile for profile-bound
    // apps and overlays the incoming config otherwise.
    if (this.mode === 'upgrade' && this.existingApplication && this.template) {
      // Ship existing app + new templateId. If the user merged chunks from
      // the new template (left) into the right pane, composeYaml now carries
      // their edited compose — overlay it onto existingApplication.config so
      // the UpgradeActionHandler picks up the customized version.
      const outbound: any = {
        ...this.existingApplication,
        templateId: this.template.id,
        config: {
          ...((this.existingApplication.config as any) || { type: 'DOCKER_COMPOSE' }),
          compose: this.parseYamlBestEffort(this.composeYaml)
        }
      };
      const stepInputs: { [stepId: string]: any } = {};
      if (this.backupVolumeStep) {
        stepInputs[this.backupVolumeStep.id] = {
          backupVolumes: this.backupVolumes.filter(v => v.selected).map(v => v.key),
          type: AgentAppStepType.BACKUP_VOLUME
        };
      }
      if (this.pullImagesStep) {
        stepInputs[this.pullImagesStep.id] = {
          pullImages: this.pullImages,
          type: this.pullImagesStep.type
        };
      }
      this.agentService.createAgentAppEvent(this.existingApplication.id.id, {
        actionType: AgentAppEventActionType.UPGRADE,
        application: outbound,
        stepInputs
      }).subscribe({
        next: () => this.dialogRef.close(true),
        error: () => {
          this.submitting = false;
        }
      });
      return;
    }

    // Build the application body. For EDGE/GATEWAY use the merged app and
    // overlay user-edited compose; for GENERIC build a fresh body.
    // In update mode start from the existing application so we preserve id/agentId/etc.
    let application: any;
    if (this.mode === 'update' && this.existingApplication) {
      application = {
        ...this.existingApplication,
        name: this.appName.trim(),
        config: {
          ...((this.existingApplication.config as any) || { type: 'DOCKER_COMPOSE' }),
          compose: this.parseYamlBestEffort(this.composeYaml)
        }
      };
    } else if (this.selectedType === AgentApplicationType.GENERIC) {
      application = {
        name: this.appName.trim(),
        appType: AgentApplicationType.GENERIC,
        agentId: { id: this.agentId, entityType: 'AGENT' },
        config: { type: 'DOCKER_COMPOSE', compose: this.parseYamlBestEffort(this.composeYaml) },
        origin: AgentApplicationOrigin.INSTALLED
      };
    } else {
      // Profile-based installs skip mergeForPreview, so mergedApp is null here
      // and we must populate the mandatory fields (agentId, appType, origin)
      // ourselves — otherwise the server rejects with "Agent application should
      // be assigned to agent!".
      const base: any = this.mergedApp || {
        agentId: { id: this.agentId, entityType: 'AGENT' },
        appType: this.selectedType,
        origin: AgentApplicationOrigin.INSTALLED
      };
      application = {
        ...base,
        name: this.appName.trim(),
        config: {
          ...((this.mergedApp && this.mergedApp.config) || { type: 'DOCKER_COMPOSE' }),
          compose: this.parseYamlBestEffort(this.composeYaml)
        }
      };
    }

    // Attach profile reference if using a profile-based install
    if (this.useProfile && this.selectedProfile) {
      application.applicationProfileId = this.selectedProfile.id;
      application.templateId = this.selectedProfile.templateId;
    }

    const stepInputs: { [stepId: string]: any } = {};
    if (this.pullImagesStep) {
      stepInputs[this.pullImagesStep.id] = {
        pullImages: this.pullImages,
        type: AgentAppStepType.COMPOSE
      };
    }

    if (this.mode === 'update' && this.existingApplication) {
      this.agentService.createAgentAppEvent(this.existingApplication.id.id, {
        actionType: AgentAppEventActionType.UPDATE,
        application,
        stepInputs,
        ...(this.isProfileManagedUpdate ? { skipProfileRefetch: this.skipProfileRefetch } : {})
      }).subscribe({
        next: () => this.dialogRef.close(true),
        error: () => {
          this.submitting = false;
        }
      });
    } else {
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
  }

  private parseYamlBestEffort(yaml: string): any {
    if (yaml && yaml.trim().length > 0) {
      try {
        return YAML.parse(yaml);
      } catch (e) {
        // Fall through to merged-app fallback below.
      }
    }
    if (this.mergedApp && this.mergedApp.config && (this.mergedApp.config as any).compose) {
      return (this.mergedApp.config as any).compose;
    }
    return { services: {} };
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

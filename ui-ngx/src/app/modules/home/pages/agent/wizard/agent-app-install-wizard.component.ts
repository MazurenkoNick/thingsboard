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
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import {
  AgentApplication,
  AgentAppEvent,
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
  normalizeCredValue,
  visibleCredentialFields as visibleCredFieldsFor
} from '@home/pages/agent/util/agent-credentials';
import {
  buildBackupVolumeInput,
  buildComposeDownInput,
  buildPullImagesInput,
  classifyStepsForAction,
  ClassifiedStep,
  extractComposeVolumeKeys,
  readInitialPullImages,
  StepInputKind
} from '@home/pages/agent/util/agent-app-steps';
import { openAgentAppEventProgress } from '@home/pages/agent/util/agent-app-event-progress';

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

// One rendered user-input block per classified step. Per-kind local state
// lives on optional fields; only the field matching `kind` is populated.
export interface StepBinding {
  kind: StepInputKind;
  step: AgentAppStep;
  backupVolumes?: VolumeChoice[];
  pullImages?: boolean;
  removeVolumes?: boolean;
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
  extends DialogComponent<AgentAppInstallWizardComponent, AgentAppEvent | null>
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

  // One binding per user-input step surfaced by the template for the active
  // mode's action. Multiple steps of the same kind each render their own
  // block. Ordering follows the BE step-list.
  bindings: StepBinding[] = [];

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

  // Profile-bound upgrades share the semantics of profile-managed updates:
  // compose comes from the profile, the user only edits credentials. Steps
  // (backup volumes, pull images) still apply.
  get isProfileBoundUpgrade(): boolean {
    return this.mode === 'upgrade' && !!this.existingApplication?.applicationProfileId;
  }

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              private dialog: MatDialog,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppInstallWizardData,
              public dialogRef: MatDialogRef<AgentAppInstallWizardComponent, AgentAppEvent | null>) {
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
    // The two branches live in different *ngIf subtrees — the opposite
    // branch's editor/diff is about to be removed from the DOM, so tear it
    // down now. The ViewChild setter will re-init against the freshly
    // attached element.
    this.destroyInstallEditor();
    this.destroyDiffViewer();
    if (this.selectedType) {
      if (value) {
        this.loadProfilesForType(this.selectedType);
      } else {
        this.availableProfiles = [];
        this.loadTemplateForType(this.selectedType);
      }
    }
  }

  private destroyInstallEditor() {
    if (this.installEditor) {
      try { this.installEditor.destroy(); } catch (_) { /* no-op */ }
      this.installEditor = null;
    }
  }

  private destroyDiffViewer() {
    if (this.differ) {
      try { this.differ.destroy(); } catch (_) { /* no-op */ }
      this.differ = null;
    }
    this.pendingDiffInit = false;
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
          this.initBindings(tpl);
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
    this.credentialValues[field.key] = normalizeCredValue(field.key, value);
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
    this.initBindings(tpl);
    this.composeType = this.pickComposeType(tpl);
    // Profile-managed update: skip diff/preview merge — the user can only
    // edit credentials, so we hydrate the form from the existing app and
    // expose a skip-refetch checkbox below the cred inputs.
    if (this.mode === 'update' && this.existingApplication?.applicationProfileId) {
      this.composeYaml = this.dumpCompose(this.existingApplication);
      this.initCredentialValues();
      return;
    }
    // Install with custom config: show side-by-side — left is the raw
    // template compose (read-only), right is the same compose, editable.
    // Skip mergeForPreview; the backend merges/overlays during the actual
    // install when it runs the start steps.
    if (this.mode === 'install' && !this.useProfile) {
      this.proposedYaml = this.dumpRawTemplateCompose(tpl);
      this.currentYaml = this.proposedYaml;
      this.composeYaml = this.currentYaml;
      this.scheduleDiffInit();
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
    this.initBindings(template);

    // Side-by-side diff: left = raw new template compose (no mergeForPreview),
    // right = current persisted compose. Both panes are read-only in upgrade
    // mode — this is preview-and-confirm, not edit. Profile-bound upgrades
    // skip the diff entirely (compose is authoritative from the profile) and
    // expose the credentials form instead — seed it from the app's compose.
    this.proposedYaml = this.dumpRawTemplateCompose(template);
    this.currentYaml = this.dumpCompose(this.existingApplication!);
    this.composeYaml = this.currentYaml;
    if (this.isProfileBoundUpgrade) {
      const compose: any = (this.existingApplication?.config as any)?.compose;
      this.credentialValues = extractCredentialValues(compose, this.selectedType);
    }
    this.loadingTemplate = false;
    if (!this.isProfileBoundUpgrade) {
      this.scheduleDiffInit();
    }
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

  toggleBackupVolume(v: VolumeChoice) {
    v.selected = !v.selected;
  }

  trackBinding(_idx: number, b: StepBinding): string {
    return b.step.id;
  }

  get hasBackupVolumeInput(): boolean {
    return this.bindings.some(b => b.kind === 'backupVolume');
  }

  // Summed across all backup-volume bindings. The current template shape
  // produces at most one such binding, but the BE could emit several and
  // the summary row should reflect all selections, not the first one.
  get selectedBackupVolumeCount(): number {
    let count = 0;
    for (const b of this.bindings) {
      if (b.kind === 'backupVolume' && b.backupVolumes) {
        count += b.backupVolumes.filter(v => v.selected).length;
      }
    }
    return count;
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

  private modeAction(): AgentAppEventActionType {
    switch (this.mode) {
      case 'update':  return AgentAppEventActionType.UPDATE;
      case 'upgrade': return AgentAppEventActionType.UPGRADE;
      default:        return AgentAppEventActionType.INSTALL;
    }
  }

  private initBindings(template: AgentAppTemplate) {
    this.bindings = classifyStepsForAction(template, this.modeAction())
      .map(cs => this.createBinding(cs));
  }

  private createBinding({ kind, step }: ClassifiedStep): StepBinding {
    switch (kind) {
      case 'backupVolume':
        return { kind, step, backupVolumes: this.seedBackupVolumes() };
      case 'pullImages':
        return { kind, step, pullImages: readInitialPullImages(step) };
      case 'composeDown':
        return { kind, step, removeVolumes: false };
    }
  }

  // In upgrade mode the volumes we prompt to back up are the CURRENT app's —
  // those hold the data we need to preserve. Pre-selected so the default is
  // "don't lose anything." Non-upgrade modes don't currently surface a
  // backup-volume input; seed empty defensively.
  private seedBackupVolumes(): VolumeChoice[] {
    if (this.mode === 'upgrade' && this.existingApplication) {
      return extractComposeVolumeKeys(this.existingApplication).map(key => ({ key, selected: true }));
    }
    return [];
  }

  private buildStepInputs(): { [stepId: string]: any } {
    const out: { [stepId: string]: any } = {};
    for (const b of this.bindings) {
      out[b.step.id] = this.buildStepPayload(b);
    }
    return out;
  }

  private buildStepPayload(b: StepBinding): any {
    switch (b.kind) {
      case 'backupVolume':
        return buildBackupVolumeInput(
          b.step,
          (b.backupVolumes || []).filter(v => v.selected).map(v => v.key)
        );
      case 'pullImages':
        return buildPullImagesInput(b.step, !!b.pullImages);
      case 'composeDown':
        return buildComposeDownInput(b.step, !!b.removeVolumes);
    }
  }

  private defaultAppName(type: AgentApplicationType): string {
    switch (type) {
      case AgentApplicationType.EDGE: return 'tb-edge';
      case AgentApplicationType.GATEWAY: return 'tb-gateway';
      default: return 'my-app';
    }
  }

  ngOnDestroy(): void {
    this.destroyDiffViewer();
    this.destroyInstallEditor();
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
    this.dialogRef.close(null);
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
      // Ship existing app + new templateId. For standalone upgrades the user
      // may have merged chunks from the new template (left) into the right
      // pane, so composeYaml carries their edited compose — overlay it.
      // For profile-bound upgrades the compose comes from the profile and we
      // only patch credentials into the existing compose; UpgradeActionHandler
      // re-resolves from the profile regardless.
      let outboundCompose: any;
      if (this.isProfileBoundUpgrade) {
        outboundCompose = (this.existingApplication.config as any)?.compose;
        if (outboundCompose) {
          applyCredentialValuesToCompose(outboundCompose, this.selectedType, this.credentialValues);
        }
      } else {
        outboundCompose = this.parseYamlBestEffort(this.composeYaml);
      }
      const outbound: any = {
        ...this.existingApplication,
        templateId: this.template.id,
        config: {
          ...((this.existingApplication.config as any) || { type: 'DOCKER_COMPOSE' }),
          compose: outboundCompose
        }
      };
      const stepInputs = this.buildStepInputs();
      this.agentService.createAgentAppEvent(this.existingApplication.id.id, {
        actionType: AgentAppEventActionType.UPGRADE,
        application: outbound,
        stepInputs
      }).subscribe({
        next: event => this.finishWithProgress(this.existingApplication!, event),
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
      // Custom-config installs skip mergeForPreview (we render the diff from
      // the raw template instead), so mergedApp is null and we must populate
      // the mandatory fields (agentId, appType, templateId, origin) ourselves
      // — otherwise the server rejects with "Agent application should be
      // assigned to agent!".
      const base: any = this.mergedApp || {
        agentId: { id: this.agentId, entityType: 'AGENT' },
        appType: this.selectedType,
        templateId: this.template?.id,
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

    const stepInputs = this.buildStepInputs();

    if (this.mode === 'update' && this.existingApplication) {
      this.agentService.createAgentAppEvent(this.existingApplication.id.id, {
        actionType: AgentAppEventActionType.UPDATE,
        application,
        stepInputs,
        ...(this.isProfileManagedUpdate ? { skipProfileRefetch: this.skipProfileRefetch } : {})
      }).subscribe({
        next: event => this.finishWithProgress(this.existingApplication!, event),
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
        next: resp => this.finishWithProgress(resp?.application, resp?.event ?? null),
        error: () => {
          this.submitting = false;
        }
      });
    }
  }

  // Close the wizard first, then open the progress dialog over the underlying
  // page. Opening both simultaneously stacks dialogs and makes "close" feel
  // ambiguous — the wizard fades out before the progress dialog appears.
  private finishWithProgress(application: AgentApplication | null | undefined, event: AgentAppEvent | null) {
    this.dialogRef.close(event);
    if (application && event) {
      openAgentAppEventProgress(this.dialog, application, event).subscribe();
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
        || value.includes(':') || value.includes('#')
        || value.includes('\n') || value.includes('\r') || value.includes('\t')
        || value.includes('"') || value.includes('\\');
      if (needsQuote) {
        // Double-quoted YAML scalars: escape backslash first, then other specials.
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
}

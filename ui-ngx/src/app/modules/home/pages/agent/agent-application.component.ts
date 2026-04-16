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

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Inject, OnDestroy, ViewChild } from '@angular/core';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import {
  AgentAppProfile,
  AgentApplicationInfo,
  AgentApplicationType,
  agentApplicationTypeTranslationMap,
  AgentAppEventActionType
} from '@shared/models/agent.models';
import * as YAML from 'yaml';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AgentService } from '@core/http/agent.service';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { EntityService } from '@core/http/entity.service';
import { baseDetailsPageByEntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentAppDeleteDialogComponent,
  AgentAppDeleteDialogData
} from '@home/pages/agent/dialog/agent-app-delete-dialog.component';
import {
  AgentAppInstallWizardComponent,
  AgentAppInstallWizardData
} from '@home/pages/agent/wizard/agent-app-install-wizard.component';
import { mergeMap } from 'rxjs/operators';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';
import {
  applyCredentialValuesToCompose,
  CredField,
  credentialSchemaFor,
  extractCredentialValues,
  normalizeCredValue,
  visibleCredentialFields as visibleCredFieldsFor
} from '@home/pages/agent/util/agent-credentials';
import { of } from 'rxjs';

@Component({
  selector: 'tb-agent-application',
  templateUrl: './agent-application.component.html',
  styleUrls: ['./agent-application.component.scss']
})
export class AgentApplicationComponent extends EntityComponent<AgentApplicationInfo>
  implements AfterViewInit, OnDestroy {

  entityType = EntityType;
  private fullscreenHost: HTMLElement | null = null;

  agentApplicationTypes = Object.values(AgentApplicationType);
  agentApplicationTypeTranslationMap = agentApplicationTypeTranslationMap;

  templateVersion = '';

  relatedEntity: { name: string; url: string; typeLabel: string } | null = null;

  availableProfiles: AgentAppProfile[] = [];
  loadingProfiles = false;
  // Cached compose YAML of the previously non-profile state so the user can
  // restore it by clearing the profile selector. Captured on first switch to
  // a profile and cleared once they detach it.
  private detachedComposeYaml: string | null = null;

  // Profile-managed apps: only these env keys are editable on the detail page.
  // The compose editor stays read-only and the form below patches the creds
  // into the compose on save.
  credentialValues: Record<string, string> = {};

  private composeEditor: Ace.Editor | null = null;
  private composeEditorSettingValue = false;
  private pendingComposeValue: string | null = null;
  @ViewChild('composeAceEditor', { static: false })
  set composeAceRef(ref: ElementRef<HTMLElement> | undefined) {
    if (ref && !this.composeEditor) {
      this.initComposeEditor(ref.nativeElement);
    }
  }

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: AgentApplicationInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AgentApplicationInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              private agentService: AgentService,
              private entityService: EntityService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private router: Router,
              private hostElementRef: ElementRef<HTMLElement>) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngAfterViewInit(): void {
    // The shared entity-details-page wraps us in a .settings-card capped at
    // 60–80% width. Mark that card so our scoped override can widen it.
    // Defer to a microtask so the lookup runs after the tab body finishes
    // attaching the host element to the DOM.
    Promise.resolve().then(() => {
      let card: HTMLElement | null =
        this.hostElementRef.nativeElement.closest('.settings-card') as HTMLElement | null;
      if (!card) {
        card = document.querySelector('.settings-card');
      }
      if (card) {
        this.fullscreenHost = card;
        this.fullscreenHost.classList.add('tb-agent-app-fullscreen');
      }
    });
  }

  ngOnDestroy(): void {
    if (this.fullscreenHost) {
      this.fullscreenHost.classList.remove('tb-agent-app-fullscreen');
      this.fullscreenHost = null;
    }
    if (this.composeEditor) {
      try { this.composeEditor.destroy(); } catch (_) { /* no-op */ }
      this.composeEditor = null;
    }
  }

  updateFormState() {
    super.updateFormState();
    this.applyComposeEditorReadOnly();
  }

  private initComposeEditor(host: HTMLElement) {
    getAce().subscribe((ace) => {
      const editor: Ace.Editor = ace.edit(host);
      editor.setTheme('ace/theme/textmate');
      editor.session.setMode('ace/mode/yaml');
      editor.session.setUseWrapMode(false);
      editor.setShowPrintMargin(false);
      (editor as any).setOption('scrollPastEnd', false);
      editor.renderer.setScrollMargin(0, 0, 0, 0);
      editor.setFontSize(12);
      // Override the global .ace_editor { font-size: 16px !important } from
      // styles.scss by setting font-size inline with !important.
      const container = (editor as any).container as HTMLElement | undefined;
      container?.style?.setProperty('font-size', '12px', 'important');
      editor.setOption('tabSize', 2);
      editor.setOption('useSoftTabs', true);
      editor.setOption('showLineNumbers', true);
      editor.setOption('highlightActiveLine', false);
      const initial = this.pendingComposeValue
        ?? (this.entityForm?.get('composeYaml')?.value as string)
        ?? '';
      editor.setValue(initial, -1);
      this.pendingComposeValue = null;
      editor.getSession().on('change', () => {
        if (this.composeEditorSettingValue) {
          return;
        }
        const ctrl = this.entityForm?.get('composeYaml');
        if (ctrl && ctrl.value !== editor.getValue()) {
          ctrl.setValue(editor.getValue());
          ctrl.markAsDirty();
        }
      });
      this.composeEditor = editor;
      // Confine wheel input to this editor. stopPropagation alone won't
      // work — ace uses virtual scrolling and doesn't preventDefault on
      // deltas it can't consume, so those spill into the browser's default
      // scroll chain and scroll the surrounding details page. Instead we
      // preventDefault on the container and manually forward the delta
      // into ace's scrollTop/scrollLeft so the editor still scrolls while
      // it has room to move.
      confineWheelToAceEditor(container, editor, () => !!this.isEdit && editor.isFocused());
      this.applyComposeEditorReadOnly();
      setTimeout(() => editor.resize(true), 0);
    });
  }

  private pushComposeToEditor(yaml: string) {
    if (!this.composeEditor) {
      this.pendingComposeValue = yaml;
      return;
    }
    if (this.composeEditor.getValue() === yaml) {
      return;
    }
    this.composeEditorSettingValue = true;
    this.composeEditor.setValue(yaml || '', -1);
    this.composeEditorSettingValue = false;
  }

  private applyComposeEditorReadOnly() {
    if (!this.composeEditor) {
      return;
    }
    // Profile-managed apps are always read-only — credential edits go through
    // the credentials form below and are merged into the compose on save.
    const readOnly = !this.isEdit || this.isProfileManaged;
    this.composeEditor.setReadOnly(readOnly);
    const cursorLayer = (this.composeEditor.renderer as any).$cursorLayer;
    if (cursorLayer?.element?.style) {
      cursorLayer.element.style.display = readOnly ? 'none' : '';
    }
  }

  get isProfileManaged(): boolean {
    return !!this.selectedProfileId;
  }

  get selectedProfileId(): string | null {
    // Once the form exists it is the source of truth — `??` would have fallen
    // back to the persisted entity when the user explicitly cleared the
    // selector (value === null), making the credentials form linger.
    const ctrl = this.entityForm?.get('applicationProfileId');
    if (ctrl) {
      return ctrl.value ?? null;
    }
    return this.entity?.applicationProfileId?.id ?? null;
  }

  onProfileChange(profileId: string | null) {
    // Called from mat-select selectionChange AND from the clear button. The
    // select path has already written via formControlName; the clear button
    // hasn't, so we setValue unconditionally — both paths converge here.
    const ctrl = this.entityForm.get('applicationProfileId');
    if (ctrl.value !== (profileId ?? null)) {
      ctrl.setValue(profileId ?? null);
    }
    ctrl.markAsDirty();

    if (profileId) {
      if (this.detachedComposeYaml == null) {
        this.detachedComposeYaml = this.entityForm.get('composeYaml')?.value ?? this.dumpCompose(this.entity);
      }
      const profile = this.availableProfiles.find(p => p.id.id === profileId);
      if (profile) {
        this.applyProfileConfig(profile);
      } else if (this.entity?.appType) {
        this.agentService.getAgentAppProfilesByAppType(this.entity.appType).subscribe(list => {
          this.availableProfiles = list;
          const match = list.find(p => p.id.id === profileId);
          if (match) { this.applyProfileConfig(match); }
        });
      }
    } else {
      // Detached — restore the previous compose so the editor becomes editable
      // again. Credentials form hides via isProfileManaged → false.
      const restored = this.detachedComposeYaml ?? this.dumpCompose(this.entity);
      this.entityForm.get('composeYaml').setValue(restored);
      this.entityForm.get('composeYaml').markAsDirty();
      this.pushComposeToEditor(restored);
      this.credentialValues = {};
      this.detachedComposeYaml = null;
    }
    this.applyComposeEditorReadOnly();
  }

  private applyProfileConfig(profile: AgentAppProfile) {
    const profileCompose: any = (profile.config as any)?.compose;
    if (!profileCompose) { return; }
    const yaml = this.dumpYaml(profileCompose, 0).trimEnd() + '\n';
    this.entityForm.get('composeYaml').setValue(yaml);
    this.entityForm.get('composeYaml').markAsDirty();
    this.pushComposeToEditor(yaml);
    this.credentialValues = extractCredentialValues(profileCompose, this.entity?.appType);
  }

  private loadProfilesForType(appType: AgentApplicationType | undefined) {
    if (!appType) { return; }
    this.loadingProfiles = true;
    this.agentService.getAgentAppProfilesByAppType(appType).subscribe({
      next: profiles => {
        this.availableProfiles = profiles;
        this.loadingProfiles = false;
      },
      error: () => { this.loadingProfiles = false; }
    });
  }

  get showCredentialForm(): boolean {
    return this.isProfileManaged && credentialSchemaFor(this.entity?.appType).length > 0;
  }

  get visibleCredentialFields(): CredField[] {
    return visibleCredFieldsFor(this.entity?.appType, this.credentialValues);
  }

  onCredentialChange(field: CredField, value: string) {
    this.credentialValues[field.key] = normalizeCredValue(field.key, value);
    // Reflect the change in the compose YAML form control + the editor preview
    // so what the user sees matches what gets sent on save.
    const yamlText: string = this.entityForm?.get('composeYaml')?.value ?? '';
    let parsed: any;
    try {
      parsed = yamlText ? YAML.parse(yamlText) : null;
    } catch (_) {
      parsed = null;
    }
    if (!parsed) {
      parsed = (this.entity?.config as any)?.compose;
    }
    if (!parsed) { return; }
    applyCredentialValuesToCompose(parsed, this.entity?.appType, this.credentialValues);
    const newYaml = this.dumpYaml(parsed, 0).trimEnd() + '\n';
    this.entityForm.get('composeYaml').setValue(newYaml);
    this.entityForm.get('composeYaml').markAsDirty();
    this.pushComposeToEditor(newYaml);
  }

  private initCredentialValues(entity: AgentApplicationInfo) {
    if (!entity?.applicationProfileId) {
      this.credentialValues = {};
      return;
    }
    const compose: any = entity?.config && (entity.config as any).compose;
    this.credentialValues = extractCredentialValues(compose, entity.appType);
  }

  hideDelete() {
    return true;
  }

  /**
   * Upgrade is allowed when the app's linked template has a newer version.
   * For profile-bound apps we additionally require that the profile has
   * already moved to a different template (usually newer) — that's the
   * scenario where triggering an app-level upgrade realigns the app with
   * the profile's current template, i.e. profile.template.currentVersion
   * matches app.template.nextVersion. When the profile still points at
   * the app's current template, the user should upgrade the profile first
   * (or use the bulk action from the agent group).
   */
  canUpgrade(): boolean {
    if (this.entity?.appType === AgentApplicationType.GENERIC) { return false; }
    if (!this.entity?.nextVersion) { return false; }
    const profileId = this.entity.applicationProfileId?.id;
    if (!profileId) { return true; }
    const profile = this.availableProfiles.find(p => p.id.id === profileId);
    if (!profile?.templateId?.id) { return false; }
    return profile.templateId.id !== this.entity.templateId?.id;
  }

  buildForm(entity: AgentApplicationInfo): UntypedFormGroup {
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      applicationProfileId: [entity?.applicationProfileId?.id ?? null],
      composeYaml: [this.dumpCompose(entity)]
    });
  }

  updateForm(entity: AgentApplicationInfo) {
    const yaml = this.dumpCompose(entity);
    this.entityForm.patchValue({
      name: entity.name,
      applicationProfileId: entity?.applicationProfileId?.id ?? null,
      composeYaml: yaml
    });
    this.pushComposeToEditor(yaml);
    this.initCredentialValues(entity);
    this.detachedComposeYaml = entity?.applicationProfileId ? null : yaml;
    this.loadProfilesForType(entity?.appType);
    this.applyComposeEditorReadOnly();
    this.templateVersion = entity?.currentVersion || '';
    this.resolveRelatedEntity(entity?.relatedEntityId);
    // The detail GET returns AgentApplication (no currentVersion / nextVersion
    // — those are only joined on the list endpoint). Resolve them from the
    // linked template so templateVersion renders the actual version AND the
    // upgrade button / hint can read entity.nextVersion.
    if (entity?.templateId?.id && (!entity.currentVersion || !entity.nextVersion)) {
      this.agentService.getAgentAppTemplateById(entity.templateId.id).subscribe(tpl => {
        if (tpl?.currentVersion) {
          this.templateVersion = tpl.currentVersion;
          if (!entity.currentVersion) {
            entity.currentVersion = tpl.currentVersion;
          }
        }
        if (tpl?.nextVersion && !entity.nextVersion) {
          entity.nextVersion = tpl.nextVersion;
          this.cd.markForCheck();
        }
      });
    }
  }

  private resolveRelatedEntity(relatedEntityId: EntityId | undefined | null) {
    this.relatedEntity = null;
    if (!relatedEntityId?.id || !relatedEntityId?.entityType) {
      return;
    }
    const basePath = baseDetailsPageByEntityType.get(relatedEntityId.entityType as EntityType);
    if (!basePath) {
      return;
    }
    const url = `${basePath}/${relatedEntityId.id}`;
    const typeLabel = this.translate.instant(`entity.type-${relatedEntityId.entityType.toLowerCase()}`);
    // Show the id as a fallback so the field isn't blank while the name loads
    // (or if the lookup fails — e.g. the related entity was deleted).
    this.relatedEntity = { name: relatedEntityId.id, url, typeLabel };
    this.entityService.getEntity(
      relatedEntityId.entityType as EntityType,
      relatedEntityId.id,
      { ignoreLoading: true, ignoreErrors: true } as any
    ).subscribe({
      next: (e: any) => {
        if (this.relatedEntity) {
          this.relatedEntity = { ...this.relatedEntity, name: e?.name || relatedEntityId.id };
          this.cd.markForCheck();
        }
      }
    });
  }

  get selectedProfileName(): string {
    const id = this.selectedProfileId;
    if (!id) { return ''; }
    const match = this.availableProfiles.find(p => p.id.id === id);
    // Fall back to the profileName the info endpoint joined onto the entity —
    // covers the window before `availableProfiles` resolves on first render.
    return match?.name || (this.entity as any)?.profileName || '';
  }

  openRelatedEntity($event: Event) {
    if ($event) { $event.stopPropagation(); $event.preventDefault(); }
    if (this.relatedEntity?.url) {
      this.router.navigateByUrl(this.relatedEntity.url);
    }
  }

  prepareFormValue(formValue: any): any {
    const prepared = super.prepareFormValue(formValue);
    let compose = (this.entity?.config as any)?.compose;
    const yamlText: string = formValue?.composeYaml;
    if (yamlText && yamlText.trim().length > 0) {
      try {
        compose = YAML.parse(yamlText);
      } catch (e) {
        compose = (this.entity?.config as any)?.compose;
      }
    }
    prepared.config = {
      ...((this.entity?.config as any) || { type: 'DOCKER_COMPOSE' }),
      compose
    };
    const profileId: string | null = formValue?.applicationProfileId ?? null;
    prepared.applicationProfileId = profileId
        ? { id: profileId, entityType: 'AGENT_APP_PROFILE' }
        : null;
    delete prepared.composeYaml;
    delete prepared.appType;
    delete prepared.currentVersion;
    return prepared;
  }

  onUpdate($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationById(this.entity.id.id).subscribe(full => {
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, boolean>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog'],
          data: {
            agentId: (full.agentId as any).id,
            agent: null as any,
            mode: 'update',
            application: full
          }
        }
      ).afterClosed().subscribe(confirmed => {
        if (confirmed) {
          this.reloadEntity();
        }
      });
    });
  }

  onRestart($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.dialogService.confirm(
      this.translate.instant('agent.app-restart-title', { name: this.entity.name }),
      this.translate.instant('agent.app-restart-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe(res => {
      if (res) {
        this.agentService.createAgentAppEvent(this.entity.id.id, {
          actionType: AgentAppEventActionType.RESTART
        }).subscribe(() => this.reloadEntity());
      }
    });
  }

  onUpgrade($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationById(this.entity.id.id).subscribe(full => {
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, boolean>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog'],
          data: {
            agentId: (full.agentId as any).id,
            agent: null as any,
            mode: 'upgrade',
            application: full
          }
        }
      ).afterClosed().subscribe(confirmed => {
        if (confirmed) {
          this.reloadEntity();
        }
      });
    });
  }

  onDelete($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationById(this.entity.id.id).pipe(
      mergeMap(full => this.dialog.open<AgentAppDeleteDialogComponent, AgentAppDeleteDialogData, boolean>(
        AgentAppDeleteDialogComponent, {
          disableClose: false,
          panelClass: ['tb-dialog'],
          data: { application: full }
        }
      ).afterClosed())
    ).subscribe(confirmed => {
      if (confirmed) {
        const agentId = (this.entity.agentId as any)?.id;
        if (agentId) {
          this.router.navigateByUrl(`/edgeManagement/agents/${agentId}/applications`);
        }
      }
    });
  }

  onAppIdCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('agent.app-id-copied-message'),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'left'
    }));
  }

  private reloadEntity() {
    // Ask the parent details page to refetch + reinject the entity. This is
    // critical because EntityDetailsPageComponent caches `this.entity` and
    // re-clones it on every edit-mode toggle — patching only our local copy
    // would be silently overwritten the next time the user clicks the pencil.
    this.entityAction.emit({ event: null, action: 'reload', entity: this.entity });
    return of(null);
  }

  private dumpCompose(entity: AgentApplicationInfo): string {
    const compose: any = entity?.config && (entity.config as any).compose;
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

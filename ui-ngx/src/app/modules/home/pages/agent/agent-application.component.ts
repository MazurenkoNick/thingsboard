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

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Inject, OnDestroy, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '@home/components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import {
  AgentApplication,
  AgentAppArgument,
  AgentAppEvent,
  AgentAppEventActionType,
  AgentAppProfile,
  AgentAppProfileInfo,
  AgentApplicationInfo,
  AgentApplicationType,
  agentApplicationTypeTranslationMap
} from '@shared/models/agent.models';
import { openAgentAppEventProgress } from '@home/pages/agent/util/agent-app-event-progress';
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
import { map, mergeMap, share, startWith } from 'rxjs/operators';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';
import {
  applyCredentialValuesToCompose,
  CredField,
  credentialSchemaFor,
  extractCredentialValues,
  normalizeCredValue,
  visibleCredentialFields as visibleCredFieldsFor
} from '@home/pages/agent/util/agent-credentials';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';

@Component({
  selector: 'tb-agent-application',
  templateUrl: './agent-application.component.html',
  styleUrls: ['./agent-application.component.scss'],
  standalone: false
})
export class AgentApplicationComponent extends EntityComponent<AgentApplicationInfo>
  implements AfterViewInit, OnDestroy {

  entityType = EntityType;
  private fullscreenHost: HTMLElement | null = null;

  agentApplicationTypes = Object.values(AgentApplicationType);
  agentApplicationTypeTranslationMap = agentApplicationTypeTranslationMap;

  templateVersion = '';

  relatedEntity: { name: string; url: string; typeLabel: string } | null = null;

  loadingProfiles = false;

  profileSearchCtrl: UntypedFormControl;
  filteredProfiles: Observable<AgentAppProfileInfo[]>;

  private availableProfiles$ = new BehaviorSubject<AgentAppProfileInfo[]>([]);
  private availableProfilesValue: AgentAppProfileInfo[] = [];

  get availableProfiles(): AgentAppProfileInfo[] {
    return this.availableProfilesValue;
  }

  set availableProfiles(profiles: AgentAppProfileInfo[]) {
    this.availableProfilesValue = profiles;
    this.availableProfiles$.next(profiles);
    this.syncProfileDisplay();
  }
  // Cached compose YAML of the previously non-profile state so the user can
  // restore it by clearing the profile selector. Captured on first switch to
  // a profile and cleared once they detach it.
  private detachedComposeYaml: string | null = null;
  // Same restore semantics as detachedComposeYaml, for the custom arguments list.
  private detachedArguments: AgentAppArgument[] | null = null;

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
    this.profileSearchCtrl = this.fb.control(null);
    this.profileSearchCtrl.disable({ emitEvent: false });
    const text$ = this.profileSearchCtrl.valueChanges.pipe(
      startWith(this.profileSearchCtrl.value),
      map(value => !value ? '' : (typeof value === 'string' ? value : (value as AgentAppProfileInfo).name))
    );
    this.filteredProfiles = combineLatest([this.availableProfiles$, text$]).pipe(
      map(([all, text]) => {
        if (!text || !text.length) {
          return all;
        }
        const lc = text.toLowerCase();
        return all.filter(p => p.name.toLowerCase().includes(lc));
      }),
      share()
    );
    this.profileSearchCtrl.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      const selected = (value && typeof value !== 'string') ? value as AgentAppProfileInfo : null;
      if (selected?.id?.id && selected.id.id !== this.selectedProfileId) {
        this.onProfileChange(selected.id.id);
      }
    });
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
    if (this.profileSearchCtrl) {
      if (this.isEdit) {
        this.profileSearchCtrl.enable({ emitEvent: false });
      } else {
        this.profileSearchCtrl.disable({ emitEvent: false });
      }
    }
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
        if (this.composeEditorSettingValue || editor.getReadOnly()) {
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

  displayProfileFn(profile?: AgentAppProfileInfo): string {
    return profile ? profile.name : '';
  }

  clearProfileField() {
    this.profileSearchCtrl.patchValue(null, { emitEvent: false });
    this.onProfileChange(null);
  }

  syncProfileDisplay() {
    if (!this.profileSearchCtrl) {
      return;
    }
    const id = this.selectedProfileId;
    if (!id) {
      this.profileSearchCtrl.patchValue(null, { emitEvent: false });
      return;
    }
    const match = this.availableProfiles.find(p => p.id.id === id);
    this.profileSearchCtrl.patchValue(match ?? { id: { id }, name: this.selectedProfileName } as any, { emitEvent: false });
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
      if (this.detachedArguments == null) {
        this.detachedArguments = this.entityForm.get('arguments')?.value ?? this.extractArguments(this.entity);
      }
      const profile = this.availableProfiles.find(p => p.id.id === profileId);
      if (profile) {
        this.applyProfileConfig(profile);
      } else if (this.entity?.appType) {
        this.agentService.getAgentAppProfilesByAppType(this.entity.appType).subscribe(list => {
          this.availableProfiles = this.filterProfilesByCurrentVersion(list);
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
      const restoredArgs = this.detachedArguments ?? this.extractArguments(this.entity);
      this.entityForm.get('arguments').setValue(restoredArgs);
      this.entityForm.get('arguments').markAsDirty();
      this.credentialValues = {};
      this.detachedComposeYaml = null;
      this.detachedArguments = null;
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
    this.entityForm.get('arguments').setValue((profile.config as any)?.arguments ?? []);
    this.entityForm.get('arguments').markAsDirty();
    this.credentialValues = extractCredentialValues(profileCompose, this.entity?.appType);
  }

  private loadProfilesForType(appType: AgentApplicationType | undefined) {
    if (!appType) { return; }
    this.loadingProfiles = true;
    this.agentService.getAgentAppProfilesByAppType(appType).subscribe({
      next: profiles => {
        this.availableProfiles = this.filterProfilesByCurrentVersion(profiles);
        this.loadingProfiles = false;
      },
      error: () => { this.loadingProfiles = false; }
    });
  }

  // Hide profiles whose template version doesn't match the application current version —
  // the backend rejects such switches on update. Keep the currently-assigned profile in the list so a profile-managed
  private filterProfilesByCurrentVersion(profiles: AgentAppProfileInfo[]): AgentAppProfileInfo[] {
    const appVersion = this.entity?.currentVersion;
    if (!appVersion) {
      return profiles;
    }
    const assignedId = this.entity?.applicationProfileId?.id;
    return profiles.filter(p =>
      p.templateCurrentVersion === appVersion || p.id?.id === assignedId
    );
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

  canUpgrade(): boolean {
    if (this.entity?.appType === AgentApplicationType.GENERIC) { return false; }
    return !!this.entity?.nextVersion;
  }

  canHaveRelatedEntity(): boolean {
    return this.entity?.appType === AgentApplicationType.EDGE
        || this.entity?.appType === AgentApplicationType.GATEWAY;
  }

  buildForm(entity: AgentApplicationInfo): UntypedFormGroup {
    const relatedEntityRequired = entity?.appType === AgentApplicationType.EDGE
      || entity?.appType === AgentApplicationType.GATEWAY;
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      applicationProfileId: [entity?.applicationProfileId?.id ?? null],
      composeYaml: [this.dumpCompose(entity)],
      arguments: [this.extractArguments(entity)],
      relatedEntityId: [entity?.relatedEntityId ?? null, relatedEntityRequired ? [Validators.required] : []]
    });
  }

  updateForm(entity: AgentApplicationInfo) {
    const yaml = this.dumpCompose(entity);
    const args = this.extractArguments(entity);
    this.entityForm.patchValue({
      name: entity.name,
      applicationProfileId: entity?.applicationProfileId?.id ?? null,
      composeYaml: yaml,
      arguments: args,
      relatedEntityId: entity?.relatedEntityId ?? null
    });
    this.pushComposeToEditor(yaml);
    this.initCredentialValues(entity);
    this.detachedComposeYaml = entity?.applicationProfileId ? null : yaml;
    this.detachedArguments = entity?.applicationProfileId ? null : args;
    this.syncProfileDisplay();
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
      compose,
      arguments: formValue?.arguments ?? []
    };
    const profileId: string | null = formValue?.applicationProfileId ?? null;
    prepared.applicationProfileId = profileId
        ? { id: profileId, entityType: 'AGENT_APP_PROFILE' }
        : null;
    // Round-trip the original + new relatedEntityId so saveEntity can chain
    // an assign/unassign after the application save.
    prepared.__relatedEntityIdNext = formValue?.relatedEntityId ?? null;
    prepared.__relatedEntityIdPrev = this.entity?.relatedEntityId ?? null;
    delete prepared.composeYaml;
    delete prepared.arguments;
    delete prepared.appType;
    delete prepared.currentVersion;
    delete prepared.relatedEntityId;
    return prepared;
  }

  onUpdate($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationById(this.entity.id.id).subscribe(full => {
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppEvent | null>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            agentId: (full.agentId as any).id,
            agent: null as any,
            mode: 'update',
            application: full
          }
        }
      ).afterClosed().subscribe(event => {
        if (event) {
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
      if (!res) { return; }
      // Need the full application for the progress dialog.
      this.agentService.getAgentApplicationById(this.entity.id.id).pipe(
        mergeMap(full =>
          this.agentService.createAgentAppEvent(this.entity.id.id, { actionType: AgentAppEventActionType.RESTART })
            .pipe(mergeMap(event => {
              this.reloadEntity();
              if (event) {
                return openAgentAppEventProgress(this.dialog, full as AgentApplication, event);
              }
              return of(null);
            }))
        )
      ).subscribe();
    });
  }

  onUpgrade($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationById(this.entity.id.id).subscribe(full => {
      this.dialog.open<AgentAppInstallWizardComponent, AgentAppInstallWizardData, AgentAppEvent | null>(
        AgentAppInstallWizardComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            agentId: (full.agentId as any).id,
            agent: null as any,
            mode: 'upgrade',
            application: full
          }
        }
      ).afterClosed().subscribe(event => {
        if (event) {
          this.reloadEntity();
        }
      });
    });
  }

  onDelete($event: Event) {
    if ($event) { $event.stopPropagation(); }
    this.agentService.getAgentApplicationById(this.entity.id.id).pipe(
      mergeMap(full => this.dialog.open<AgentAppDeleteDialogComponent, AgentAppDeleteDialogData, AgentAppEvent | null>(
        AgentAppDeleteDialogComponent, {
          disableClose: false,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: { application: full }
        }
      ).afterClosed().pipe(map(event => ({ full, event }))))
    ).subscribe(({ full, event }) => {
      if (!event) { return; }
      const agentId = (this.entity.agentId as any)?.id;
      openAgentAppEventProgress(this.dialog, full, event).subscribe(() => {
        if (agentId) {
          this.router.navigateByUrl(`/edgeManagement/agents/${agentId}/applications`);
        }
      });
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

  private extractArguments(entity: AgentApplicationInfo): AgentAppArgument[] {
    return ((entity?.config as any)?.arguments as AgentAppArgument[]) ?? [];
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

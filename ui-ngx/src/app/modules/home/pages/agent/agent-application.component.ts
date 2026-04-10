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
import {
  AgentAppDeleteDialogComponent,
  AgentAppDeleteDialogData
} from '@home/pages/agent/dialog/agent-app-delete-dialog.component';
import {
  AgentAppInstallWizardComponent,
  AgentAppInstallWizardData
} from '@home/pages/agent/wizard/agent-app-install-wizard.component';
import { mergeMap } from 'rxjs/operators';
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
      container?.addEventListener('wheel', (ev: WheelEvent) => {
        if (!this.isEdit || !editor.isFocused()) { return; }
        ev.preventDefault();
        const session = editor.getSession();
        session.setScrollTop(session.getScrollTop() + ev.deltaY);
        if (ev.deltaX) {
          session.setScrollLeft(session.getScrollLeft() + ev.deltaX);
        }
      }, { passive: false });
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
    const readOnly = !this.isEdit;
    this.composeEditor.setReadOnly(readOnly);
    const cursorLayer = (this.composeEditor.renderer as any).$cursorLayer;
    if (cursorLayer?.element?.style) {
      cursorLayer.element.style.display = readOnly ? 'none' : '';
    }
  }

  hideDelete() {
    return true;
  }

  /**
   * Upgrade is allowed only for standalone apps (no profile) that have a
   * linked template with a newer version available. Profile-bound apps are
   * upgraded through the bulk action in the profile section instead.
   */
  canUpgrade(): boolean {
    return !!this.entity?.nextVersion && !this.entity?.applicationProfileId;
  }

  buildForm(entity: AgentApplicationInfo): UntypedFormGroup {
    return this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      composeYaml: [this.dumpCompose(entity)]
    });
  }

  updateForm(entity: AgentApplicationInfo) {
    const yaml = this.dumpCompose(entity);
    this.entityForm.patchValue({
      name: entity.name,
      composeYaml: yaml
    });
    this.pushComposeToEditor(yaml);
    this.applyComposeEditorReadOnly();
    this.templateVersion = entity?.currentVersion || '';
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
        || value.includes(':') || value.includes('#') || value.includes('\n');
      return needsQuote ? `"${value.replace(/"/g, '\\"')}"` : value;
    }
    return String(value);
  }
}

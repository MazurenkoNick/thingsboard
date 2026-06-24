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

import { Component, ElementRef, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  AgentAppEventActionType,
  AgentAppProfile,
  AgentAppStepType,
  AgentAppTemplate,
} from '@shared/models/agent.models';
import * as YAML from 'yaml';
import { getAceDiff } from '@shared/models/ace/ace.models';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';
import { Ace } from 'ace-builds';

export interface AgentAppProfileUpgradeDialogData {
  profile: AgentAppProfile;
}

@Component({
  selector: 'tb-agent-app-profile-upgrade-dialog',
  templateUrl: './agent-app-profile-upgrade-dialog.component.html',
  styleUrls: ['../wizard/agent-app-install-wizard.component.scss'],
  standalone: false
})
export class AgentAppProfileUpgradeDialogComponent
  extends DialogComponent<AgentAppProfileUpgradeDialogComponent, AgentAppProfile | null>
  implements OnInit, OnDestroy {

  @ViewChild('diffViewer', { static: false })
  diffViewerElmRef: ElementRef<HTMLElement>;

  profile: AgentAppProfile;
  mergedProfile: AgentAppProfile | null = null;
  fromVersion: string | null = null;
  toVersion: string | null = null;
  template: AgentAppTemplate | null = null;

  proposedYaml = '';
  currentYaml = '';
  composeYaml = '';
  diffSyncScroll = false;
  private differ: any = null;

  loadingTemplate = true;
  loadError = '';
  submitting = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppProfileUpgradeDialogData,
              public dialogRef: MatDialogRef<AgentAppProfileUpgradeDialogComponent, AgentAppProfile | null>) {
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
    this.dialogRef.close(null);
  }

  canSubmit(): boolean {
    return !!this.template && !this.submitting && !this.loadError && !this.isComposeYamlInvalid();
  }

  isComposeYamlInvalid(): boolean {
    if (!this.composeYaml?.trim()) { return false; }
    try {
      YAML.parse(this.composeYaml);
      return false;
    } catch (_) {
      return true;
    }
  }

  submit() {
    if (!this.template || this.submitting || this.loadError) { return; }
    let compose: any;
    try {
      compose = this.parseComposeYaml(this.composeYaml);
    } catch (_) {
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('agent.app-compose-invalid-yaml'),
        type: 'error',
        duration: 3000,
        verticalPosition: 'bottom',
        horizontalPosition: 'left'
      }));
      return;
    }
    this.submitting = true;

    const updated: any = {
      ...this.profile,
      templateId: this.template!.id,
      config: {
        ...((this.profile.config as any) || { type: 'DOCKER_COMPOSE' }),
        compose
      }
    };

    this.agentService.saveAgentAppProfile(updated).subscribe({
      next: saved => this.dialogRef.close(saved),
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
    this.proposedYaml = this.dumpRawTemplateCompose(tpl, (this.profile.config as any)?.composeType);
    const draft = { ...this.profile, templateId: tpl.id } as AgentAppProfile;
    this.agentService.mergeProfileForPreview(
      tpl.id.id, draft, undefined, AgentAppEventActionType.UPGRADE
    ).subscribe({
      next: merged => {
        this.mergedProfile = merged;
        this.finishApplyTemplate(this.dumpCompose(merged));
      },
      error: () => this.failLoad('agent.app-upgrade-load-failed')
    });
  }

  private finishApplyTemplate(currentYaml: string) {
    this.currentYaml = currentYaml;
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
        lockScrolling: false,
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
      // Confine wheel/trackpad scroll so ace doesn't spill deltas it can't
      // consume into the page's default scroll chain — that's what causes
      // touchpad two-finger gestures to trigger browser back-navigation on
      // the diff viewer. Only confine while the target pane is focused so
      // the dialog body can still scroll past the diff when it isn't.
      confineWheelToAceEditor((leftEditor as any).container, leftEditor,
        () => leftEditor.isFocused());
      confineWheelToAceEditor((rightEditor as any).container, rightEditor,
        () => rightEditor.isFocused());
      confineWheelToAceEditor(this.diffViewerElmRef?.nativeElement, rightEditor,
        () => leftEditor.isFocused() || rightEditor.isFocused(), false);
      rightEditor.getSession().on('change', () => {
        this.composeYaml = rightEditor.getValue();
        this.realignDiff(this.differ, leftEditor);
      });
      this.bindDiffScrollSync(leftEditor, rightEditor);
      setTimeout(() => {
        leftEditor.resize(true);
        rightEditor.resize(true);
        this.realignDiff(this.differ, leftEditor);
      }, 50);
    });
  }

  private bindDiffScrollSync(leftEditor: Ace.Editor, rightEditor: Ace.Editor) {
    const leftSession = leftEditor.getSession();
    const rightSession = rightEditor.getSession();
    let syncing = false;
    const link = (a: Ace.EditSession, b: Ace.EditSession) => {
      a.on('changeScrollTop', () => {
        if (!this.diffSyncScroll || syncing) { return; }
        const next = a.getScrollTop();
        if (b.getScrollTop() === next) { return; }
        syncing = true;
        b.setScrollTop(next);
        syncing = false;
      });
      a.on('changeScrollLeft', () => {
        if (!this.diffSyncScroll || syncing) { return; }
        const next = a.getScrollLeft();
        if (b.getScrollLeft() === next) { return; }
        syncing = true;
        b.setScrollLeft(next);
        syncing = false;
      });
    };
    link(leftSession, rightSession);
    link(rightSession, leftSession);
  }

  onDiffSyncScrollChange() {
    if (!this.differ || !this.diffSyncScroll) { return; }
    const editors = this.differ.getEditors?.();
    if (!editors) { return; }
    const leftSession = editors.left?.getSession?.();
    const rightSession = editors.right?.getSession?.();
    if (!leftSession || !rightSession) { return; }
    rightSession.setScrollTop(leftSession.getScrollTop());
    rightSession.setScrollLeft(leftSession.getScrollLeft());
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

  private realignDiff(differ: any, editor: Ace.Editor) {
    if (!differ) { return; }
    const lineHeight = editor?.renderer?.lineHeight;
    if (lineHeight) { differ.lineHeight = lineHeight; }
    differ.diff();
  }

  private dumpRawTemplateCompose(template: AgentAppTemplate, composeType?: string): string {
    const steps = (template.startSteps || []);
    for (const step of steps) {
      const anyStep = step as any;
      if (step.type === AgentAppStepType.COMPOSE_TEMPLATE && anyStep.composeTemplates) {
        const keys = Object.keys(anyStep.composeTemplates);
        if (keys.length) {
          const key = (composeType && keys.includes(composeType)) ? composeType : keys[0];
          return this.dumpYaml(anyStep.composeTemplates[key], 0).trimEnd() + '\n';
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

  // Parses the edited compose. Throws on invalid YAML so submit() can surface
  // the error and abort instead of silently saving the previous/merged compose.
  private parseComposeYaml(yaml: string): any {
    if (yaml?.trim()) {
      return YAML.parse(yaml);
    }
    return (this.mergedProfile?.config as any)?.compose
      || (this.profile?.config as any)?.compose
      || { services: {} };
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

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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { StepperOrientation } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
} from '@shared/models/agent.models';
import {
  composeTemplateKeys,
  composeTypeLabelKey,
  dumpRawTemplateCompose,
  dumpYaml,
  parseComposeYaml,
  pickComposeType
} from '@home/pages/agent/util/agent-compose-yaml';

export interface AgentAppProfileWizardData {
  profile?: AgentAppProfile;
  lockedAppType?: AgentApplicationType;
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
  styleUrls: ['./agent-app-install-wizard.component.scss', './agent-app-profile-wizard.component.scss'],
  standalone: false
})
export class AgentAppProfileWizardComponent
  extends DialogComponent<AgentAppProfileWizardComponent, AgentAppProfile>
  implements OnInit, OnDestroy {

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
  composeTypeKeys: string[] = [];
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
  diffSyncScroll = false;

  submitting = false;

  stepperOrientation: Observable<StepperOrientation>;
  stepperLabelPosition: Observable<'bottom' | 'end'>;

  get lockedAppType(): AgentApplicationType | null {
    return this.data?.lockedAppType ?? this.data?.profile?.appType ?? null;
  }

  get editMode(): boolean {
    return !!this.data?.profile?.id?.id;
  }

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              private breakpointObserver: BreakpointObserver,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppProfileWizardData,
              public dialogRef: MatDialogRef<AgentAppProfileWizardComponent, AgentAppProfile>) {
    super(store, router, dialogRef);
    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'horizontal' : 'vertical'));
    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'end' : 'bottom'));
  }

  ngOnInit() {
    // Prefetch templates for all types so type selection is instant.
    this.typeCards.forEach(card => {
      this.agentService.getAgentAppTemplatesByAppType(card.type).subscribe({
        next: templates => this.templatesByTypeCache.set(card.type, templates),
        error: () => {}
      });
    });
    if (this.editMode) {
      this.initForEdit(this.data.profile);
    } else if (this.lockedAppType) {
      this.selectType(this.lockedAppType);
    }
  }

  get titleKey(): string {
    return this.editMode ? 'agent.app-profile-wizard-edit-title' : 'agent.app-profile-wizard-title';
  }

  get profileNamePlaceholder(): string {
    if (this.selectedType === AgentApplicationType.EDGE) {
      return this.translate.instant('agent.app-profile-name-placeholder-edge');
    }
    if (this.selectedType === AgentApplicationType.GATEWAY) {
      return this.translate.instant('agent.app-profile-name-placeholder-gateway');
    }
    return this.translate.instant('agent.app-profile-name-placeholder-generic');
  }

  private initForEdit(profile: AgentAppProfile) {
    this.selectedType = profile.appType;
    this.profileName = profile.name;
    this.profileDescription = profile.description || '';
    this.selectedTemplateId = profile.templateId?.id || null;
    this.loadTemplatesForType(profile.appType);
    if (!profile.templateId?.id) {
      return;
    }
    this.loadingTemplate = true;
    this.agentService.getAgentAppTemplateById(profile.templateId.id).subscribe({
      next: tpl => {
        this.template = tpl;
        this.composeType = (profile.config as any)?.composeType || pickComposeType(tpl);
        this.composeTypeKeys = composeTemplateKeys(tpl);
        this.proposedYaml = dumpRawTemplateCompose(tpl, this.composeType);
        const compose: any = (profile.config as any)?.compose;
        this.currentYaml = compose ? (dumpYaml(compose, 0).trimEnd() + '\n') : this.proposedYaml;
        this.composeYaml = this.currentYaml;
        this.loadingTemplate = false;
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingTemplate = false;
      }
    });
  }

  selectType(type: AgentApplicationType) {
    if (this.selectedType === type) {
      return;
    }
    this.selectedType = type;
    this.template = null;
    this.composeType = null;
    this.composeTypeKeys = [];
    this.composeYaml = '';
    this.selectedTemplateId = null;
    this.loadError = '';
    this.loadTemplatesForType(type);
  }

  private loadTemplatesForType(type: AgentApplicationType) {
    const cached = this.templatesByTypeCache.get(type);
    if (cached) {
      this.availableTemplates = [...cached]
        .sort((a, b) => (b.currentVersion || '').localeCompare(a.currentVersion || ''));
      this.selectLatestTemplate();
      return;
    }
    this.loadingTemplates = true;
    this.agentService.getAgentAppTemplatesByAppType(type).subscribe({
      next: templates => {
        this.templatesByTypeCache.set(type, templates);
        this.availableTemplates = [...templates]
          .sort((a, b) => (b.currentVersion || '').localeCompare(a.currentVersion || ''));
        this.loadingTemplates = false;
        this.selectLatestTemplate();
      },
      error: () => {
        this.loadError = this.translate.instant('agent.app-install-template-failed');
        this.loadingTemplates = false;
      }
    });
  }

  private selectLatestTemplate() {
    if (this.selectedTemplateId || !this.availableTemplates.length) {
      return;
    }
    const latest = this.availableTemplates.find(t => !t.nextVersion) || this.availableTemplates[0];
    this.selectedTemplateId = latest.id.id;
    this.applyTemplate(latest);
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
    this.composeType = pickComposeType(tpl);
    this.composeTypeKeys = composeTemplateKeys(tpl);
    this.rebuildComposePreview();
  }

  composeTypeLabel(composeType: string): string {
    const labelKey = composeTypeLabelKey(composeType);
    return labelKey ? this.translate.instant(labelKey) : composeType;
  }

  onComposeTypeChange(composeType: string) {
    if (!this.template || this.composeType === composeType) {
      return;
    }
    this.composeType = composeType;
    if (this.editMode) {
      this.proposedYaml = dumpRawTemplateCompose(this.template, this.composeType);
      return;
    }
    this.rebuildComposePreview();
  }

  private rebuildComposePreview() {
    const tpl = this.template;
    if (!tpl) {
      return;
    }
    this.proposedYaml = dumpRawTemplateCompose(tpl, this.composeType);
    this.currentYaml = this.proposedYaml;
    this.composeYaml = this.currentYaml;
    const draft: AgentAppProfile = {
      name: this.profileName?.trim() || 'preview',
      appType: this.selectedType
    } as any;
    this.agentService.mergeProfileForPreview(tpl.id.id, draft, this.composeType || undefined).subscribe({
      next: merged => {
        const compose: any = (merged as any)?.config?.compose;
        if (compose) {
          this.currentYaml = dumpYaml(compose, 0).trimEnd() + '\n';
          this.composeYaml = this.currentYaml;
        }
      },
      error: () => { /* keep raw template seed */ }
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
      ...(this.editMode ? this.data.profile : {}),
      name: this.profileName.trim(),
      appType: this.selectedType,
      templateId: this.template?.id,
      config: {
        type: 'DOCKER_COMPOSE',
        compose: parseComposeYaml(this.composeYaml),
        composeType: this.composeType || undefined
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
  }

  // --- Diff viewer ---

  // --- YAML utilities ---

}

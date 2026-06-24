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

import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { MatStepper, StepperOrientation } from '@angular/material/stepper';
import { Observable } from 'rxjs';
import { PageComponent } from '@shared/components/page.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppEventActionType,
  AgentAppProfile,
  AgentApplicationType,
  AgentAppTemplate,
  AgentInfo
} from '@shared/models/agent.models';
import { extractCredentialValues } from '@home/pages/agent/util/agent-credentials';
import { classifyStepsForAction } from '@home/pages/agent/util/agent-app-steps';
import { buildStepInputs, createStepBinding, StepBinding } from '@home/pages/agent/util/agent-app-step-inputs';
import { buildUpgradeApplication } from '@home/pages/agent/util/agent-app-payloads';
import { buildUpdateMergeDraft } from '@home/pages/agent/util/agent-app-compose-preview';
import { dumpCompose, dumpRawTemplateCompose, parseComposeYaml } from '@home/pages/agent/util/agent-compose-yaml';
import { AgentAppWizardLoaderService } from '@home/pages/agent/wizard/agent-app-wizard-loader.service';
import { AgentAppWizardSubmitService } from '@home/pages/agent/wizard/agent-app-wizard-submit.service';
import { AgentAppUpgradeResult, AgentAppWizardFinish } from '@home/pages/agent/wizard/agent-app-wizard.models';

@Component({
  selector: 'tb-agent-app-upgrade-flow',
  templateUrl: './agent-app-upgrade-flow.component.html',
  styleUrls: ['./agent-app-install-wizard.component.scss'],
  standalone: false
})
export class AgentAppUpgradeFlowComponent extends PageComponent implements OnInit {

  @Input() agentId: string;
  @Input() agent: AgentInfo;
  @Input() existingApplication: AgentApplication;
  @Input() lockedRelatedEntity: EntityId | null = null;
  @Input() embedded = false;
  @Input() showBack = false;
  @Input() stepperOrientation: Observable<StepperOrientation>;
  @Input() stepperLabelPosition: Observable<'bottom' | 'end'>;

  @Output() finished = new EventEmitter<AgentAppWizardFinish>();
  @Output() cancelled = new EventEmitter<AgentAppUpgradeResult | null>();
  @Output() back = new EventEmitter<void>();

  fromVersion: string | null = null;
  toVersion: string | null = null;

  profile: AgentAppProfile | null = null;
  mergedProfile: AgentAppProfile | null = null;
  profileProposedYaml = '';
  profileCurrentYaml = '';
  profileComposeYaml = '';
  private profileMergeRequested = false;
  loadingProfile = false;
  profileLoadError = '';
  profileUpgrading = false;
  profileUpgraded = false;
  showProfileUpgradeStep = false;

  @ViewChild('stepper', { static: false })
  stepper: MatStepper;

  bindings: StepBinding[] = [];

  proposedYaml = '';
  currentYaml = '';
  composeType: string | null = null;
  diffSyncScroll = false;

  selectedType: AgentApplicationType | null = null;
  template: AgentAppTemplate | null = null;
  loadingTemplate = false;
  loadError = '';

  appName = '';
  composeYaml = '';

  mergedApp: AgentApplication | null = null;
  submitting = false;

  credentialValues: Record<string, string> = {};

  relatedEntityId: EntityId | null = null;

  constructor(private translate: TranslateService,
              private agentService: AgentService,
              private loader: AgentAppWizardLoaderService,
              private submitSvc: AgentAppWizardSubmitService) {
    super();
  }

  // Profile-bound upgrade: compose comes from the profile, the user only edits
  // credentials. Steps (backup volumes, pull images) still apply.
  get isProfileBoundUpgrade(): boolean {
    return !!this.existingApplication?.applicationProfileId;
  }

  // True when the linked profile still points to the same template as the
  // application — the profile hasn't moved to nextVersion yet, so the wizard
  // surfaces a profile-upgrade step before Review & Customize.
  get needsProfileUpgrade(): boolean {
    return !!this.profile
      && !!this.existingApplication?.templateId?.id
      && this.profile.templateId?.id === this.existingApplication.templateId.id;
  }

  get canHaveRelatedEntity(): boolean {
    const type = this.selectedType || this.existingApplication?.appType;
    return type === AgentApplicationType.EDGE || type === AgentApplicationType.GATEWAY;
  }

  get relatedEntityShown(): boolean {
    return this.canHaveRelatedEntity && this.isProfileBoundUpgrade;
  }

  get relatedEntityRequired(): boolean {
    return this.relatedEntityShown;
  }

  ngOnInit() {
    this.relatedEntityId = this.lockedRelatedEntity || (this.existingApplication as any)?.relatedEntityId || null;
    this.appName = this.existingApplication.name;
    this.selectedType = this.existingApplication.appType;
    this.fromVersion = (this.existingApplication as any).currentVersion || null;
    this.loadingTemplate = true;
    this.resolveUpgradeTemplate();
    const profileId = this.existingApplication.applicationProfileId?.id;
    if (profileId) {
      this.resolveProfile(profileId);
    }
  }

  private resolveUpgradeTemplate() {
    this.loader.resolveUpgradeTemplate(this.existingApplication).subscribe({
      next: ({ template, fromVersion }) => {
        // The detail endpoint doesn't populate currentVersion on the
        // application, so fromVersion is usually null coming in from ngOnInit.
        // Adopt the value resolved from the linked template so the "from → to"
        // row doesn't show a dash.
        if (!this.fromVersion && fromVersion) {
          this.fromVersion = fromVersion;
        }
        this.applyUpgradeTemplate(template);
      },
      error: e => this.failUpgradeLoad(e?.messageKey || 'agent.app-upgrade-load-failed')
    });
  }

  private applyUpgradeTemplate(template: AgentAppTemplate) {
    this.template = template;
    this.toVersion = template.currentVersion || null;
    this.initBindings(template);

    this.composeType = (this.existingApplication?.config as any)?.composeType || null;
    this.proposedYaml = dumpRawTemplateCompose(template, this.composeType || undefined);
    // Profile-bound upgrade: compose comes from the profile and the user only
    // edits credentials, so no app-level merge — the image bump rides on the
    // profile-upgrade step.
    if (this.isProfileBoundUpgrade) {
      this.currentYaml = dumpCompose(this.existingApplication);
      this.composeYaml = this.currentYaml;
      const compose: any = (this.existingApplication?.config as any)?.compose;
      this.credentialValues = extractCredentialValues(compose, this.selectedType);
      this.loadingTemplate = false;
      this.maybeInitProfileDiff();
      return;
    }
    this.runMergeForPreview(template);
  }

  private runMergeForPreview(template: AgentAppTemplate) {
    const draft = buildUpdateMergeDraft(this.existingApplication, template);
    this.loader.merge(template.id.id, draft, undefined, undefined, AgentAppEventActionType.UPGRADE).subscribe({
      next: merged => {
        this.mergedApp = merged;
        this.currentYaml = dumpCompose(merged);
        this.composeYaml = this.currentYaml;
        this.loadingTemplate = false;
        this.maybeInitProfileDiff();
      },
      error: () => this.failUpgradeLoad('agent.app-upgrade-load-failed')
    });
  }

  private resolveProfile(profileId: string) {
    this.loadingProfile = true;
    this.loader.loadProfileById(profileId).subscribe({
      next: prof => {
        this.profile = prof;
        this.loadingProfile = false;
        this.maybeInitProfileDiff();
      },
      error: () => {
        this.profileLoadError = this.translate.instant('agent.app-profile-load-failed');
        this.loadingProfile = false;
      }
    });
  }

  private maybeInitProfileDiff() {
    if (!this.needsProfileUpgrade || !this.template || !this.profile || this.profileMergeRequested) { return; }
    this.profileMergeRequested = true;
    this.showProfileUpgradeStep = true;
    const profileComposeType = (this.profile.config as any)?.composeType || undefined;
    this.profileProposedYaml = dumpRawTemplateCompose(this.template, profileComposeType);
    const draft = { ...this.profile, templateId: this.template.id } as AgentAppProfile;
    this.agentService.mergeProfileForPreview(
      this.template.id.id, draft, undefined, AgentAppEventActionType.UPGRADE
    ).subscribe({
      next: merged => {
        this.mergedProfile = merged;
        this.profileCurrentYaml = dumpCompose(merged as any);
        this.profileComposeYaml = this.profileCurrentYaml;
      },
      error: () => {
        // Best-effort image bump; fall back to the existing profile compose.
        this.profileCurrentYaml = dumpCompose(this.profile as any);
        this.profileComposeYaml = this.profileCurrentYaml;
      }
    });
  }

  canSubmitProfileUpgrade(): boolean {
    return !!this.profile && !!this.template
      && !this.profileUpgrading && !this.profileUpgraded
      && !this.loadError && !this.profileLoadError;
  }

  submitProfileUpgrade() {
    if (!this.canSubmitProfileUpgrade()) { return; }
    this.profileUpgrading = true;
    const updated: any = {
      ...this.profile,
      templateId: this.template!.id,
      config: {
        ...((this.profile!.config as any) || { type: 'DOCKER_COMPOSE' }),
        compose: parseComposeYaml(this.profileComposeYaml, (this.mergedProfile?.config as any)?.compose)
      }
    };
    this.agentService.saveAgentAppProfile(updated).subscribe({
      next: saved => {
        this.profile = saved;
        // Reflect the committed compose, then lock the diff (its readOnly is
        // bound to profileUpgraded in the template).
        this.profileCurrentYaml = this.profileComposeYaml;
        this.profileUpgraded = true;
        this.profileUpgrading = false;
        // Defer so the linear stepper picks up the just-set [completed]
        // binding before stepper.next() decides whether it can advance.
        setTimeout(() => this.stepper?.next(), 0);
      },
      error: () => {
        this.profileUpgrading = false;
      }
    });
  }

  private failUpgradeLoad(messageKey: string) {
    this.loadError = this.translate.instant(messageKey);
    this.loadingTemplate = false;
  }

  private initBindings(template: AgentAppTemplate) {
    this.bindings = classifyStepsForAction(template, AgentAppEventActionType.UPGRADE)
      .map(cs => createStepBinding(cs, this.existingApplication));
  }

  get hasBackupVolumeInput(): boolean {
    return this.bindings.some(b => b.kind === 'backupVolume');
  }

  get selectedBackupVolumeCount(): number {
    let count = 0;
    for (const b of this.bindings) {
      if (b.kind === 'backupVolume' && b.backupVolumes) {
        count += b.backupVolumes.filter(v => v.selected).length;
      }
    }
    return count;
  }

  canSubmit(): boolean {
    if (this.relatedEntityRequired && !this.relatedEntityId?.id) {
      return false;
    }
    return !!this.template && !!this.existingApplication && !this.submitting && !this.loadError;
  }

  submit() {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting = true;
    const application = buildUpgradeApplication({
      existingApplication: this.existingApplication,
      template: this.template!,
      profileBound: this.isProfileBoundUpgrade,
      selectedType: this.selectedType,
      credentialValues: this.credentialValues,
      composeYaml: this.composeYaml,
      mergedApp: this.mergedApp
    });
    const stepInputs = buildStepInputs(this.bindings);
    this.submitSvc.upgrade(this.existingApplication.id.id, application, stepInputs).subscribe({
      next: event => this.finished.emit({ application: this.existingApplication, event, withProgress: true }),
      error: () => {
        this.submitting = false;
      }
    });
  }

  cancel() {
    // If the profile was already committed via the profile-upgrade step, let
    // the caller refresh even when the user backs out before the app upgrade.
    this.cancelled.emit(this.profileUpgraded ? { profileOnly: true } : null);
  }
}

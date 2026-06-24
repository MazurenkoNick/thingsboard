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

import { Component, Inject, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatStepper } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  AgentAppProfile,
  AgentApplicationType,
  AgentProfile,
  AgentProvisionType,
  agentProvisionTypeDescriptionMap,
  agentProvisionTypeTranslationMap
} from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { EntityType } from '@shared/models/entity-type.models';
import {
  AgentAppProfileWizardComponent,
  AgentAppProfileWizardData
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';

export interface AgentProfileAddWizardData {
  defaults?: Partial<AgentProfile>;
  lockedAppType?: AgentApplicationType;
}

@Component({
  selector: 'tb-agent-profile-add-wizard',
  templateUrl: './agent-profile-add-wizard.component.html',
  styleUrls: ['./agent-app-install-wizard.component.scss', './agent-profile-add-wizard.component.scss'],
  standalone: false
})
export class AgentProfileAddWizardComponent
  extends DialogComponent<AgentProfileAddWizardComponent, AgentProfile> {

  readonly EntityType = EntityType;
  readonly AgentProvisionType = AgentProvisionType;
  agentProvisionTypes = Object.values(AgentProvisionType);
  agentProvisionTypeTranslationMap = agentProvisionTypeTranslationMap;
  agentProvisionTypeDescriptionMap = agentProvisionTypeDescriptionMap;

  step1Form: UntypedFormGroup;
  step2Form: UntypedFormGroup;

  submitting = false;

  @ViewChild('stepper', { static: false }) stepper: MatStepper;

  stepperLabelPosition: Observable<'bottom' | 'end'>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private breakpointObserver: BreakpointObserver,
              @Inject(MAT_DIALOG_DATA) public data: AgentProfileAddWizardData,
              public dialogRef: MatDialogRef<AgentProfileAddWizardComponent, AgentProfile>) {
    super(store, router, dialogRef);
    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'end' : 'bottom'));
    const defaults = this.data?.defaults ?? {};
    this.step1Form = this.fb.group({
      name: [defaults.name ?? '', [Validators.required, Validators.maxLength(255)]],
      description: [defaults.description ?? ''],
      provisionType: [defaults.provisionType ?? AgentProvisionType.DISABLED]
    });
    this.step2Form = this.fb.group({
      appProfileIds: [[]]
    });
  }

  cancel() {
    this.dialogRef.close(undefined);
  }

  canSubmit(): boolean {
    return this.step1Form.valid && !this.submitting;
  }

  submit() {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting = true;
    const profile: AgentProfile = this.step1Form.value;
    const appProfileIds: string[] = this.step2Form.value?.appProfileIds ?? [];
    this.agentService.saveAgentProfile(profile, undefined, appProfileIds).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: () => {
        this.submitting = false;
      }
    });
  }

  createAppProfile() {
    this.openAppProfileWizard();
  }

  private openAppProfileWizard() {
    this.dialog.open<AgentAppProfileWizardComponent, AgentAppProfileWizardData, AgentAppProfile>(
      AgentAppProfileWizardComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { lockedAppType: this.data?.lockedAppType }
      }
    ).afterClosed().subscribe(saved => {
      if (saved) {
        const currentIds: string[] = this.step2Form.value?.appProfileIds ?? [];
        if (!currentIds.includes(saved.id.id)) {
          this.step2Form.get('appProfileIds').setValue([...currentIds, saved.id.id]);
        }
      }
    });
  }
}

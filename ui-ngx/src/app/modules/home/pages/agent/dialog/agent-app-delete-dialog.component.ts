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

import { Component, Inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AgentService } from '@core/http/agent.service';
import {
  AgentApplication,
  AgentAppEvent,
  AgentAppEventActionType,
  AgentAppStep
} from '@shared/models/agent.models';
import {
  buildComposeDownInput,
  extractComposeVolumeKeys,
  findComposeDownStep
} from '@home/pages/agent/util/agent-app-steps';

export interface AgentAppDeleteDialogData {
  application: AgentApplication;
  agentName?: string;
}

@Component({
  selector: 'tb-agent-app-delete-dialog',
  templateUrl: './agent-app-delete-dialog.component.html',
  styleUrls: ['./agent-app-delete-dialog.component.scss'],
  standalone: false
})
export class AgentAppDeleteDialogComponent
  extends DialogComponent<AgentAppDeleteDialogComponent, AgentAppEvent | null>
  implements OnInit {

  application: AgentApplication;
  agentName: string;
  removeVolumes = false;
  volumeKeys: string[] = [];
  loading = false;
  submitting = false;
  composeDownStep: AgentAppStep | null = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentAppDeleteDialogData,
              public dialogRef: MatDialogRef<AgentAppDeleteDialogComponent, AgentAppEvent | null>) {
    super(store, router, dialogRef);
    this.application = data.application;
    this.agentName = data.agentName || '';
    this.volumeKeys = extractComposeVolumeKeys(this.application);
  }

  ngOnInit() {
    if (this.application?.templateId?.id) {
      this.loading = true;
      this.agentService.getAgentAppTemplateById(this.application.templateId.id).subscribe({
        next: template => {
          this.composeDownStep = findComposeDownStep(template);
          this.loading = false;
        },
        error: () => {
          // Even without the template we can still dispatch DELETE — server uses defaults.
          this.composeDownStep = null;
          this.loading = false;
        }
      });
    }
  }

  cancel() {
    this.dialogRef.close(null);
  }

  confirm() {
    if (this.submitting) {
      return;
    }
    this.submitting = true;
    const stepInputs: { [stepId: string]: any } = {};
    if (this.composeDownStep) {
      stepInputs[this.composeDownStep.id] = buildComposeDownInput(this.composeDownStep, this.removeVolumes);
    }
    this.agentService.createAgentAppEvent(this.application.id.id, {
      actionType: AgentAppEventActionType.DELETE,
      stepInputs
    }).subscribe({
      next: event => this.dialogRef.close(event),
      error: () => {
        this.submitting = false;
      }
    });
  }
}

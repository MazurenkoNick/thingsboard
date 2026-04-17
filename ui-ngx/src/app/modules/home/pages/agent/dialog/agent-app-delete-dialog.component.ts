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
  styleUrls: ['./agent-app-delete-dialog.component.scss']
})
export class AgentAppDeleteDialogComponent
  extends DialogComponent<AgentAppDeleteDialogComponent, boolean>
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
              public dialogRef: MatDialogRef<AgentAppDeleteDialogComponent, boolean>) {
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
    this.dialogRef.close(false);
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
      next: () => this.dialogRef.close(true),
      error: () => {
        this.submitting = false;
      }
    });
  }
}

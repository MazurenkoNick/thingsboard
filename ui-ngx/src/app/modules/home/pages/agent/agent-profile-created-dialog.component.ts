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
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  AgentProfile,
  agentProvisionTypeDescriptionMap,
  agentProvisionTypeTranslationMap
} from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';

export interface AgentProfileCreatedDialogData {
  agentProfile: AgentProfile;
}

@Component({
  selector: 'tb-agent-profile-created-dialog',
  templateUrl: './agent-profile-created-dialog.component.html',
  styleUrls: ['./agent-profile-created-dialog.component.scss'],
  standalone: false
})
export class AgentProfileCreatedDialogComponent
  extends DialogComponent<AgentProfileCreatedDialogComponent>
  implements OnInit {

  agentProfile: AgentProfile;
  dockerCommand = '';

  agentProvisionTypeTranslationMap = agentProvisionTypeTranslationMap;
  agentProvisionTypeDescriptionMap = agentProvisionTypeDescriptionMap;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              @Inject(MAT_DIALOG_DATA) public data: AgentProfileCreatedDialogData,
              public dialogRef: MatDialogRef<AgentProfileCreatedDialogComponent>) {
    super(store, router, dialogRef);
    this.agentProfile = data.agentProfile;
  }

  ngOnInit() {
    if (this.agentProfile?.id?.id) {
      this.agentService.getAgentProvisionInstructions(this.agentProfile.id.id).subscribe({
        next: res => { this.dockerCommand = res?.instructions || ''; },
        error: () => { this.dockerCommand = ''; }
      });
    }
  }

  onCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('agent.install-command-copied-message'),
      type: 'success',
      duration: 1000,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  close() {
    this.dialogRef.close();
  }

  goToProfile() {
    if (this.agentProfile?.id?.id) {
      this.router.navigateByUrl(`/edgeManagement/profiles/agent/${this.agentProfile.id.id}`);
    }
    this.dialogRef.close();
  }
}

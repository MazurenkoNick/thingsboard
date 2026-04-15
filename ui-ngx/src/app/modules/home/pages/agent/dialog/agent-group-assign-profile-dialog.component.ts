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

import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { AgentAppProfile } from '@shared/models/agent.models';

export interface AgentGroupAssignProfileDialogData {
  profiles: AgentAppProfile[];
}

@Component({
  selector: 'tb-agent-group-assign-profile-dialog',
  templateUrl: './agent-group-assign-profile-dialog.component.html',
  styleUrls: ['./agent-group-assign-profile-dialog.component.scss']
})
export class AgentGroupAssignProfileDialogComponent
  extends DialogComponent<AgentGroupAssignProfileDialogComponent, AgentAppProfile> {

  profiles: AgentAppProfile[];
  selectedProfileId: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AgentGroupAssignProfileDialogData,
              public dialogRef: MatDialogRef<AgentGroupAssignProfileDialogComponent, AgentAppProfile>) {
    super(store, router, dialogRef);
    this.profiles = data.profiles || [];
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    if (!this.selectedProfileId) {
      return;
    }
    const profile = this.profiles.find(p => p.id.id === this.selectedProfileId);
    this.dialogRef.close(profile || null);
  }
}

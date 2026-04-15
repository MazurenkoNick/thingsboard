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
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  AgentGroup,
  agentProvisionTypeDescriptionMap,
  agentProvisionTypeTranslationMap
} from '@shared/models/agent.models';

export interface AgentGroupCreatedDialogData {
  group: AgentGroup;
}

@Component({
  selector: 'tb-agent-group-created-dialog',
  templateUrl: './agent-group-created-dialog.component.html',
  styleUrls: ['./agent-group-created-dialog.component.scss']
})
export class AgentGroupCreatedDialogComponent
  extends DialogComponent<AgentGroupCreatedDialogComponent> {

  group: AgentGroup;
  showSecret = false;

  agentProvisionTypeTranslationMap = agentProvisionTypeTranslationMap;
  agentProvisionTypeDescriptionMap = agentProvisionTypeDescriptionMap;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: AgentGroupCreatedDialogData,
              public dialogRef: MatDialogRef<AgentGroupCreatedDialogComponent>) {
    super(store, router, dialogRef);
    this.group = data.group;
  }

  get dockerCommand(): string {
    const key = this.group?.provisionKey || '';
    const secret = this.group?.provisionSecret || '';
    return `docker run -d \\
  --name=tb-agent \\
  --restart=always \\
  -v /var/run/docker.sock:/var/run/docker.sock:ro \\
  -v tb-agent-data:/root/.tb-agent \\
  -e TB_SERVER_ADDR=your-tb-server.com:7070 \\
  -e AUTO_PROVISION=true \\
  -e TB_PROVISION_KEY=${key} \\
  -e TB_PROVISION_SECRET=${secret} \\
  thingsboard/tb-agent:latest`;
  }

  toggleSecret() {
    this.showSecret = !this.showSecret;
  }

  onCopied(what: 'key' | 'secret' | 'command') {
    const msgMap = {
      key: 'agent.provision-key-copied-message',
      secret: 'agent.provision-secret-copied-message',
      command: 'agent.install-command-copied-message'
    };
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(msgMap[what]),
      type: 'success',
      duration: 1000,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  close() {
    this.dialogRef.close();
  }

  goToGroup() {
    if (this.group?.id?.id) {
      this.router.navigateByUrl(`/edgeManagement/agentGroups/${this.group.id.id}`);
    }
    this.dialogRef.close();
  }
}

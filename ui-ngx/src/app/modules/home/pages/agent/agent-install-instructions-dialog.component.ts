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
import { Agent } from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';

export interface AgentInstallInstructionsDialogData {
  agent: Agent;
  afterAdd: boolean;
}

@Component({
  selector: 'tb-agent-install-instructions-dialog',
  templateUrl: './agent-install-instructions-dialog.component.html',
  styleUrls: ['./agent-install-instructions-dialog.component.scss'],
  standalone: false
})
export class AgentInstallInstructionsDialogComponent
  extends DialogComponent<AgentInstallInstructionsDialogComponent> {

  agent: Agent;
  afterAdd: boolean;
  dialogTitle: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: AgentInstallInstructionsDialogData,
              public dialogRef: MatDialogRef<AgentInstallInstructionsDialogComponent>) {
    super(store, router, dialogRef);
    this.agent = data.agent;
    this.afterAdd = data.afterAdd;
    this.dialogTitle = this.afterAdd
      ? 'agent.agent-created-successfully'
      : 'agent.install-instructions';
  }

  get dockerCommand(): string {
    const key = this.agent?.routingKey || '';
    const secret = this.agent?.secret || '';
    return `docker run -d \\
  --name=tb-agent \\
  --restart=always \\
  -v /var/run/docker.sock:/var/run/docker.sock:ro \\
  -v tb-agent-data:/root/.tb-agent \\
  -e TB_SERVER_ADDR=your-tb-server.com:7070 \\
  -e TB_AGENT_ROUTING_KEY=${key} \\
  -e TB_AGENT_ROUTING_SECRET=${secret} \\
  thingsboard/tb-agent:latest`;
  }

  onCopied(what: 'key' | 'secret' | 'command') {
    const msgMap = {
      key: 'agent.routing-key-copied-message',
      secret: 'agent.secret-copied-message',
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

  goToAgent() {
    if (this.agent?.id?.id) {
      this.router.navigateByUrl(`/edgeManagement/agents/${this.agent.id.id}`);
    }
    this.dialogRef.close();
  }
}

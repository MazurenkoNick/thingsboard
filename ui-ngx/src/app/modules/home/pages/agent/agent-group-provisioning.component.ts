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

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  AgentGroupInfo,
  AgentProvisionType,
  agentProvisionTypeDescriptionMap,
  agentProvisionTypeTranslationMap
} from '@shared/models/agent.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
  selector: 'tb-agent-group-provisioning',
  templateUrl: './agent-group-provisioning.component.html',
  styleUrls: ['./agent-group-provisioning.component.scss'],
  standalone: false
})
export class AgentGroupProvisioningComponent implements OnChanges {

  @Input() group: AgentGroupInfo;
  @Input() active: boolean;

  showSecret = false;

  readonly agentProvisionTypeTranslationMap = agentProvisionTypeTranslationMap;
  readonly agentProvisionTypeDescriptionMap = agentProvisionTypeDescriptionMap;
  readonly AgentProvisionType = AgentProvisionType;

  constructor(private store: Store<AppState>,
              private translate: TranslateService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.group) {
      this.showSecret = false;
    }
  }

  get provisionType(): AgentProvisionType {
    return this.group?.provisionType || AgentProvisionType.DISABLED;
  }

  get isEnabled(): boolean {
    return this.provisionType === AgentProvisionType.ALLOW_CREATE_NEW_AGENTS;
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

  toggleSecret(): void {
    this.showSecret = !this.showSecret;
  }

  onCopied(what: 'key' | 'secret' | 'command'): void {
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
}

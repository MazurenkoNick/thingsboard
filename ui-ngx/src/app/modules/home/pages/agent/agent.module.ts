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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { AgentRoutingModule } from '@home/pages/agent/agent-routing.module';
import { AgentComponent } from '@home/pages/agent/agent.component';
import { AgentTabsComponent } from '@home/pages/agent/agent-tabs.component';
import { AgentGroupComponent } from '@home/pages/agent/agent-group.component';
import { AgentGroupTabsComponent } from '@home/pages/agent/agent-group-tabs.component';
import { AgentAppProfileComponent } from '@home/pages/agent/agent-app-profile.component';
import { AgentAppProfileTabsComponent } from '@home/pages/agent/agent-app-profile-tabs.component';
import {
  AgentInstallInstructionsDialogComponent
} from '@home/pages/agent/agent-install-instructions-dialog.component';
import {
  AgentAppDeleteDialogComponent
} from '@home/pages/agent/dialog/agent-app-delete-dialog.component';
import {
  AgentAppUpgradeWizardComponent
} from '@home/pages/agent/wizard/agent-app-upgrade-wizard.component';
import {
  AgentAppInstallWizardComponent
} from '@home/pages/agent/wizard/agent-app-install-wizard.component';
import { AgentApplicationComponent } from '@home/pages/agent/agent-application.component';
import { AgentApplicationTabsComponent } from '@home/pages/agent/agent-application-tabs.component';

@NgModule({
  declarations: [
    AgentComponent,
    AgentTabsComponent,
    AgentGroupComponent,
    AgentGroupTabsComponent,
    AgentAppProfileComponent,
    AgentAppProfileTabsComponent,
    AgentInstallInstructionsDialogComponent,
    AgentAppDeleteDialogComponent,
    AgentAppUpgradeWizardComponent,
    AgentAppInstallWizardComponent,
    AgentApplicationComponent,
    AgentApplicationTabsComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeDialogsModule,
    HomeComponentsModule,
    AgentRoutingModule,
  ]
})
export class AgentModule { }

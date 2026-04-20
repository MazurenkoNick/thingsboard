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
import { CommonModule, DatePipe } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { AgentRoutingModule } from '@home/pages/agent/agent-routing.module';
import { AgentComponent } from '@home/pages/agent/agent.component';
import { AgentTabsComponent } from '@home/pages/agent/agent-tabs.component';
import { AgentGroupComponent } from '@home/pages/agent/agent-group.component';
import { AgentGroupTabsComponent } from '@home/pages/agent/agent-group-tabs.component';
import { AgentAppProfileComponent } from '@home/pages/agent/agent-app-profile.component';
import {
  AgentInstallInstructionsDialogComponent
} from '@home/pages/agent/agent-install-instructions-dialog.component';
import {
  AgentGroupCreatedDialogComponent
} from '@home/pages/agent/agent-group-created-dialog.component';
import { AgentGroupProfilesComponent } from '@home/pages/agent/agent-group-profiles.component';
import {
  AgentGroupProvisioningComponent
} from '@home/pages/agent/agent-group-provisioning.component';
import {
  AgentGroupBulkActionsComponent
} from '@home/pages/agent/agent-group-bulk-actions.component';
import {
  AgentGroupAssignProfileDialogComponent
} from '@home/pages/agent/dialog/agent-group-assign-profile-dialog.component';
import {
  AgentGroupBulkActionDialogComponent
} from '@home/pages/agent/dialog/agent-group-bulk-action-dialog.component';
import {
  AgentGroupBulkActionDetailsDialogComponent
} from '@home/pages/agent/dialog/agent-group-bulk-action-details-dialog.component';
import {
  AgentAppDeleteDialogComponent
} from '@home/pages/agent/dialog/agent-app-delete-dialog.component';
import {
  AgentAppEventProgressDialogComponent
} from '@home/pages/agent/dialog/agent-app-event-progress-dialog.component';
import {
  AgentAppProfileUpgradeDialogComponent
} from '@home/pages/agent/dialog/agent-app-profile-upgrade-dialog.component';
import {
  AgentAppInstallWizardComponent
} from '@home/pages/agent/wizard/agent-app-install-wizard.component';
import {
  AgentAppProfileWizardComponent
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';
import { AgentApplicationComponent } from '@home/pages/agent/agent-application.component';
import { AgentApplicationTabsComponent } from '@home/pages/agent/agent-application-tabs.component';
import {
  AgentAppEventTableComponent
} from '@home/pages/agent/table/agent-app-event-table.component';
import {
  AgentAppEventFilterPanelComponent
} from '@home/pages/agent/table/agent-app-event-filter-panel.component';
import {
  AgentAppUnitTableComponent
} from '@home/pages/agent/table/agent-app-unit-table.component';
import {
  AgentAppUnitFilterPanelComponent
} from '@home/pages/agent/table/agent-app-unit-filter-panel.component';
import { AgentEventsPageComponent } from '@home/pages/agent/agent-events-page.component';

@NgModule({
  declarations: [
    AgentComponent,
    AgentTabsComponent,
    AgentGroupComponent,
    AgentGroupTabsComponent,
    AgentAppProfileComponent,
    AgentInstallInstructionsDialogComponent,
    AgentGroupCreatedDialogComponent,
    AgentGroupProfilesComponent,
    AgentGroupProvisioningComponent,
    AgentGroupBulkActionsComponent,
    AgentGroupAssignProfileDialogComponent,
    AgentGroupBulkActionDialogComponent,
    AgentGroupBulkActionDetailsDialogComponent,
    AgentAppDeleteDialogComponent,
    AgentAppEventProgressDialogComponent,
    AgentAppProfileUpgradeDialogComponent,
    AgentAppInstallWizardComponent,
    AgentAppProfileWizardComponent,
    AgentApplicationComponent,
    AgentApplicationTabsComponent,
    AgentAppEventTableComponent,
    AgentAppEventFilterPanelComponent,
    AgentAppUnitTableComponent,
    AgentAppUnitFilterPanelComponent,
    AgentEventsPageComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeDialogsModule,
    HomeComponentsModule,
    AgentRoutingModule,
  ],
  providers: [
    DatePipe,
  ]
})
export class AgentModule { }

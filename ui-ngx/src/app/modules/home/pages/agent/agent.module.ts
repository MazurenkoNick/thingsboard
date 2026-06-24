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

import { NgModule } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { AgentRoutingModule } from '@home/pages/agent/agent-routing.module';
import { AgentComponent } from '@home/pages/agent/agent.component';
import { AgentTabsComponent } from '@home/pages/agent/agent-tabs.component';
import { AgentProfileComponent } from '@home/pages/agent/agent-profile.component';
import { AgentProfileTabsComponent } from '@home/pages/agent/agent-profile-tabs.component';
import { AgentProfileDialogComponent } from '@home/pages/agent/agent-profile-dialog.component';
import { AgentProfileAutocompleteComponent } from '@home/pages/agent/agent-profile-autocomplete.component';
import { AgentAppProfileComponent } from '@home/pages/agent/agent-app-profile.component';
import {
  AgentInstallInstructionsDialogComponent
} from '@home/pages/agent/agent-install-instructions-dialog.component';
import {
  AgentProfileCreatedDialogComponent
} from '@home/pages/agent/agent-profile-created-dialog.component';
import {
  AgentProfileMergedProfilesComponent
} from '@home/pages/agent/agent-profile-merged-profiles.component';
import {
  AgentExecutionsSidePanelComponent
} from '@home/pages/agent/agent-executions-side-panel.component';
import {
  AgentProfileAssignProfileDialogComponent
} from '@home/pages/agent/dialog/agent-profile-assign-profile-dialog.component';
import {
  AgentProfileBulkActionDialogComponent
} from '@home/pages/agent/dialog/agent-profile-bulk-action-dialog.component';
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
  AgentAppProfileWizardComponent
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';
import {
  AgentProfileAddWizardComponent
} from '@home/pages/agent/wizard/agent-profile-add-wizard.component';
import { AgentApplicationComponent } from '@home/pages/agent/agent-application.component';
import { AgentApplicationTabsComponent } from '@home/pages/agent/agent-application-tabs.component';
import {
  AgentAppEventTableComponent
} from '@home/pages/agent/table/agent-app-event-table.component';
import {
  AgentBulkActionEventTableComponent
} from '@home/pages/agent/table/agent-bulk-action-event-table.component';
import {
  AgentEventsStatsHeaderComponent
} from '@home/pages/agent/table/agent-events-stats-header.component';
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
import { AgentBulkActionEventsPageComponent } from '@home/pages/agent/agent-bulk-action-events-page.component';
import {
  LogHighlightPipe,
  LogViewerComponent
} from '@home/pages/agent/log-viewer/log-viewer.component';
import {
  AgentAppUnitLogViewerPageComponent
} from '@home/pages/agent/log-viewer/agent-app-unit-log-viewer-page.component';
import {
  AgentMetricsStripComponent
} from '@home/pages/agent/metrics/agent-metrics-strip.component';
import {
  AgentMetricsChartsPanelComponent
} from '@home/pages/agent/metrics/agent-metrics-charts-panel.component';
import {
  AgentApplicationsPageComponent
} from '@home/pages/agent/metrics/agent-applications-page.component';
import {
  AgentApplicationDetailsPageComponent
} from '@home/pages/agent/metrics/agent-application-details-page.component';
import {
  AgentMultiEntityMetricsPanelComponent
} from '@home/pages/agent/metrics/agent-multi-entity-metrics-panel.component';
import { AGENT_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { AgentGroupConfigFactory } from '@home/pages/agent/agent-group-config.factory';
import { AgentAppArgumentsComponent } from '@home/pages/agent/component/agent-app-arguments.component';
import { AgentAppArgumentPanelComponent } from '@home/pages/agent/component/agent-app-argument-panel.component';

@NgModule({
  declarations: [
    AgentComponent,
    AgentTabsComponent,
    AgentProfileComponent,
    AgentProfileTabsComponent,
    AgentProfileDialogComponent,
    AgentProfileAutocompleteComponent,
    AgentAppProfileComponent,
    AgentInstallInstructionsDialogComponent,
    AgentProfileCreatedDialogComponent,
    AgentProfileMergedProfilesComponent,
    AgentExecutionsSidePanelComponent,
    AgentProfileAssignProfileDialogComponent,
    AgentProfileBulkActionDialogComponent,
    AgentAppDeleteDialogComponent,
    AgentAppEventProgressDialogComponent,
    AgentAppProfileUpgradeDialogComponent,
    AgentAppProfileWizardComponent,
    AgentProfileAddWizardComponent,
    AgentApplicationComponent,
    AgentApplicationTabsComponent,
    AgentAppEventTableComponent,
    AgentBulkActionEventTableComponent,
    AgentEventsStatsHeaderComponent,
    AgentAppEventFilterPanelComponent,
    AgentAppUnitTableComponent,
    AgentAppUnitFilterPanelComponent,
    AgentEventsPageComponent,
    AgentBulkActionEventsPageComponent,
    LogViewerComponent,
    LogHighlightPipe,
    AgentAppUnitLogViewerPageComponent,
    AgentMetricsStripComponent,
    AgentMetricsChartsPanelComponent,
    AgentApplicationsPageComponent,
    AgentApplicationDetailsPageComponent,
    AgentMultiEntityMetricsPanelComponent,
    AgentAppArgumentsComponent,
    AgentAppArgumentPanelComponent,
  ],
  imports: [
    CommonModule,
    ScrollingModule,
    SharedModule,
    HomeDialogsModule,
    HomeComponentsModule,
    AgentRoutingModule,
  ],
  providers: [
    DatePipe,
    {
      provide: AGENT_GROUP_CONFIG_FACTORY,
      useClass: AgentGroupConfigFactory
    }
  ]
})
export class AgentModule { }

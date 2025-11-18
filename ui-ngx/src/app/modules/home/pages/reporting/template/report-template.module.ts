///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ReportTemplatePageComponent } from '@home/pages/reporting/template/report-template-page.component';
import { ReportTemplateRoutingModule } from '@home/pages/reporting/template/report-template-routing.module';
import {
  ReportTemplateTableHeaderComponent
} from '@home/pages/reporting/template/report-template-table-header.component';
import { ReportTemplateTabsComponent } from '@home/pages/reporting/template/report-template-tabs.component';
import { ReportTemplateFormComponent } from '@home/pages/reporting/template/report-template-form.component';
import {
  ReportTemplateSettingsDialogComponent
} from '@home/pages/reporting/template/report-template-settings-dialog.component';
import {
  ReportTemplateComponentsModule
} from '@home/pages/reporting/template/components/report-template-components.module';
import { ReportTemplateSettingsComponent } from '@home/pages/reporting/template/report-template-settings.component';
import { WidgetSettingsCommonModule } from '@home/components/widget/lib/settings/common/widget-settings-common.module';
import { ReportTemplateFilterComponent } from '@home/pages/reporting/template/report-template-filter.component';
import { WidgetConfigComponentsModule } from '@home/components/widget/config/widget-config-components.module';
import {
  ReportTemplateHeaderFooterComponent
} from '@home/pages/reporting/template/report-template-header-footer.component';

@NgModule({
  declarations: [
    ReportTemplateFilterComponent,
    ReportTemplateTableHeaderComponent,
    ReportTemplateTabsComponent,
    ReportTemplateFormComponent,
    ReportTemplateHeaderFooterComponent,
    ReportTemplatePageComponent,
    ReportTemplateSettingsComponent,
    ReportTemplateSettingsDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    ReportTemplateComponentsModule,
    ReportTemplateRoutingModule,
    WidgetSettingsCommonModule,
    WidgetConfigComponentsModule
  ]
})
export class ReportTemplateModule { }

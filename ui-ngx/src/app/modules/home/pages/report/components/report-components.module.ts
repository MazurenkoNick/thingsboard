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
import {
  EditReportComponentTooltipComponent,
  ReportComponentComponent
} from '@home/pages/report/components/report-component.component';
import { ReportComponentsComponent } from '@home/pages/report/components/report-components.component';
import { HeadingPreviewComponent } from '@home/pages/report/components/heading-preview.component';
import { RichTextPreviewComponent } from '@home/pages/report/components/rich-text-preview.component';
import { ReportComponentLibraryComponent } from '@home/pages/report/components/report-component-library.component';
import { ReportComponentConfigComponent } from '@home/pages/report/components/report-component-config.component';
import { HeadingConfigComponent } from '@home/pages/report/components/heading-config.component';
import { RichTextConfigComponent } from '@home/pages/report/components/rich-text-config.component';
import { WidgetConfigComponentsModule } from '@home/components/widget/config/widget-config-components.module';
import { ReportInsetsComponent } from '@home/pages/report/components/report-insets.component';
import { PageBreakPreviewComponent } from '@home/pages/report/components/page-break-preview.component';
import { EmptyReportConfigComponent } from '@home/pages/report/components/empty-report-config.component';
import { EntityTablePreviewComponent } from '@home/pages/report/components/entity-table-preview.component';
import { EntityTableConfigComponent } from '@home/pages/report/components/entity-table-config.component';
import { BasicWidgetConfigModule } from '@home/components/widget/config/basic/basic-widget-config.module';
import { SubReportPreviewComponent } from '@home/pages/report/components/sub-report-preview.component';
import { SubReportConfigComponent } from '@home/pages/report/components/sub-report-config.component';
import { ImagePreviewComponent } from '@home/pages/report/components/image-preview.component';
import { ImageConfigComponent } from '@home/pages/report/components/image-config.component';
import { ReportImageDialogComponent } from '@home/pages/report/components/report-image-dialog.component';
import { ReportRichTextComponent } from '@home/pages/report/components/report-rich-text.component';
import { DashboardPreviewComponent } from '@home/pages/report/components/dashboard-preview.component';
import { DashboardConfigComponent } from '@home/pages/report/components/dashboard-config.component';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { AlarmTablePreviewComponent } from '@home/pages/report/components/alarm-table-preview.component';
import { AlarmTableConfigComponent } from '@home/pages/report/components/alarm-table-config.component';
import { TimeseriesTablePreviewComponent } from '@home/pages/report/components/timeseries-table-preview.component';
import { TimeseriesTableConfigComponent } from '@home/pages/report/components/timeseries-table-config.component';
import { ReportHeadingComponent } from '@home/pages/report/components/report-heading.component';

@NgModule({
  declarations: [
    EditReportComponentTooltipComponent,
    ReportComponentComponent,
    ReportComponentsComponent,
    ReportComponentLibraryComponent,
    ReportInsetsComponent,
    ReportHeadingComponent,
    ReportImageDialogComponent,
    ReportRichTextComponent,
    EmptyReportConfigComponent,
    HeadingPreviewComponent,
    HeadingConfigComponent,
    RichTextPreviewComponent,
    RichTextConfigComponent,
    PageBreakPreviewComponent,
    EntityTablePreviewComponent,
    EntityTableConfigComponent,
    AlarmTablePreviewComponent,
    AlarmTableConfigComponent,
    TimeseriesTablePreviewComponent,
    TimeseriesTableConfigComponent,
    ImagePreviewComponent,
    ImageConfigComponent,
    DashboardPreviewComponent,
    DashboardConfigComponent,
    SubReportPreviewComponent,
    SubReportConfigComponent,
    ReportComponentConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule,
    WidgetConfigComponentsModule,
    BasicWidgetConfigModule
  ],
  exports: [
    ReportComponentsComponent,
    ReportComponentLibraryComponent,
    ReportComponentConfigComponent,
    ReportInsetsComponent
  ]
})
export class ReportComponentsModule { }

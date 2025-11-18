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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Route } from '@angular/router';

import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { Observable } from 'rxjs';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { ReportTemplateService } from '@core/http/report-template.service';
import { ReportTemplatePageComponent } from '@home/pages/reporting/template/report-template-page.component';
import {
  ReportTemplatesTableConfigResolver
} from '@home/pages/reporting/template/report-templates-table-config.resolver';
import { ReportTemplate } from '@shared/models/report.models';
import { MenuId } from '@core/services/menu.models';

@Injectable()
export class ReportTemplateResolver  {

  constructor(private reportTemplateService: ReportTemplateService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<ReportTemplate> {
    const reportTemplateId = route.params.reportTemplateId;
    return this.reportTemplateService.getReportTemplate(reportTemplateId);
  }
}

export const reportTemplateBreadcumbLabelFunction: BreadCrumbLabelFunction<ReportTemplatePageComponent>
  = ((_route, _translate, component) => {
  return component.reportTemplate.name;
});

export const reportTemplatesRoute: Route = {
  path: 'templates',
  data: {
    breadcrumb: {
      menuId: MenuId.report_templates
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN],
        title: 'report-template.report-templates'
      },
      resolve: {
        entitiesTableConfig: ReportTemplatesTableConfigResolver
      }
    },
    {
      path: ':reportTemplateId',
      component: ReportTemplatePageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: reportTemplateBreadcumbLabelFunction,
          icon: 'mdi:chart-box-outline'
        } as BreadCrumbConfig<ReportTemplatePageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'report-template.report-template',
        hideTabs: true
      },
      resolve: {
        reportTemplate: ReportTemplateResolver
      }
    }
  ]
}

@NgModule({
  providers: [
    ReportTemplatesTableConfigResolver,
    ReportTemplateResolver
  ]
})
export class ReportTemplateRoutingModule { }

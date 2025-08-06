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

import { Injectable } from '@angular/core';

import { ActivatedRouteSnapshot } from '@angular/router';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityColumn,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { ReportFilter, ReportInfo, ReportQuery } from '@shared/models/report.models';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getEntityDetailsPageURL } from '@core/utils';
import { ReportService } from '@core/http/report.service';
import { ReportTableHeaderComponent } from '@home/pages/reporting/report/report-table-header.component';

@Injectable()
export class ReportsTableConfigResolver  {

  constructor(private store: Store<AppState>,
              private reportService: ReportService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe) {}

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<ReportInfo> {
    const config = new EntityTableConfig<ReportInfo>();
    this.configDefaults(config);
    const authUser = getCurrentAuthUser(this.store);
    config.componentsData = {
      reportFilter: {
        reportTemplateId: null,
        userId: null
      },
      reportFilterChanged: (filter: ReportFilter) => {
        config.componentsData.reportFilter = filter;
        config.getTable().resetSortAndFilter(true);
      }
    };

    config.columns = this.configureColumns(authUser, config);
    this.configureEntityFunctions(config);
    config.cellActionDescriptors = this.configureCellActions(config);
    config.groupActionDescriptors = this.configureGroupActions(config);

    defaultEntityTablePermissions(this.userPermissionsService, config);
    return config;
  }

  private configDefaults(config: EntityTableConfig<ReportInfo>) {
    config.entityType = EntityType.REPORT;
    config.addEnabled = false;
    config.entityTranslations = entityTypeTranslations.get(EntityType.REPORT);
    config.entityResources = entityTypeResources.get(EntityType.REPORT);

    config.entityTitle = (report) => report.name;

    config.rowPointer = false;

    config.deleteEntityTitle = report =>
      this.translate.instant('report.delete-report-title', {reportName: report.name});
    config.deleteEntityContent = () => this.translate.instant('report.delete-report-text');
    config.deleteEntitiesTitle = count => this.translate.instant('report.delete-reports-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('report.delete-reports-text');

    config.onEntityAction = action => this.onReportAction(action, config);
    config.detailsPanelEnabled = false;
    config.headerComponent = ReportTableHeaderComponent;
  }

  private configureColumns(_authUser: AuthUser, config: EntityTableConfig<ReportInfo>): Array<EntityColumn<ReportInfo>> {
    return [
      new DateEntityTableColumn<ReportInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<ReportInfo>('name', 'report.file-name', '25%', config.entityTitle),
      new EntityLinkTableColumn<ReportInfo>('reportTemplateName', 'report-template.report-template', '25%',
        (report) => report.templateInfo?.name ?? '',
        (report) => report.templateInfo ? getEntityDetailsPageURL(report.templateInfo?.id.id, EntityType.REPORT_TEMPLATE) : ''),
      new EntityTableColumn<ReportInfo>('userName', 'user.user', '25%', (report) => report.userName),
      new EntityTableColumn<ReportInfo>('format', 'report.format', '25%')
    ];
  }

  private configureEntityFunctions(config: EntityTableConfig<ReportInfo>): void {
    config.entitiesFetchFunction = pageLink => {
      const reportQuery = new ReportQuery(pageLink, {
        reportTemplateId: config.componentsData.reportFilter.reportTemplateId,
        userId: config.componentsData.reportFilter.userId
      });
      return this.reportService.getReportInfos(reportQuery);
    };

    config.deleteEntity = id => this.reportService.deleteReport(id.id);
  }

  private configureCellActions(_config: EntityTableConfig<ReportInfo>): Array<CellActionDescriptor<ReportInfo>> {
    const actions: Array<CellActionDescriptor<ReportInfo>> = [];
    actions.push(
      {
        name: this.translate.instant('report.download'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.downloadReport($event, entity)
      },
    );
    return actions;
  }

  private configureGroupActions(_config: EntityTableConfig<ReportInfo>): Array<GroupActionDescriptor<ReportInfo>> {
    return [];
  }

  private downloadReport($event: Event, report: ReportInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.reportService.downloadReport(report.id.id).subscribe();
  }

  onReportAction(action: EntityAction<ReportInfo>, _config: EntityTableConfig<ReportInfo>): boolean {
    switch (action.action) {
      case 'download':
        this.downloadReport(action.event, action.entity);
        return true;
    }
    return false;
  }
}

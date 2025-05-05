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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { ReportTemplateId } from '@shared/models/id/report-template-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';
import { SchedulerEventId } from '@shared/models/id/scheduler-event-id';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityAlias, EntityAliases } from '@shared/models/alias.models';
import {
  Filter, Filters,
  KeyFilter,
  keyFilterInfosToKeyFilters,
  keyFiltersToKeyFilterInfos
} from '@shared/models/query/query.models';
import { ReportComponentConfig, TableReportComponentConfig } from '@shared/models/report-component.models';

export enum ReportTemplateType {
  REPORT = 'REPORT',
  SUB_REPORT = 'SUB_REPORT'
}

export const reportTemplateTypes = Object.keys(ReportTemplateType) as ReportTemplateType[];

export const reportTemplateTypeTranslationMap = new Map<ReportTemplateType, string>(
  [
    [ReportTemplateType.REPORT, 'report-template.type-report'],
    [ReportTemplateType.SUB_REPORT, 'report-template.type-sub-report']
  ]
);

export interface BaseReportTemplate extends BaseData<ReportTemplateId>, HasTenantId, HasVersion, ExportableEntity<ReportTemplateId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  type: ReportTemplateType;
  description?: string;
  schedulerEventId?: SchedulerEventId;
}

export interface ReportTemplateInfo extends BaseReportTemplate {
  ownerId?: EntityId;
  ownerName?: string;
}

export interface HeaderFooter {
  enabled: boolean;
  components: ReportComponentConfig[];
  firstPage?: HeaderFooter;
}

export interface ReportFilter {
  id: string;
  filter: string;
  keyFilters: Array<KeyFilter>;
}

export const entityAliasesToList = (entityAliases: EntityAliases): EntityAlias[] => {
  const entityAliasesList: EntityAlias[] = [];
  for (const id of Object.keys(entityAliases)) {
    entityAliasesList.push(entityAliases[id]);
  }
  return entityAliasesList;
}

export const entityAliasesListToAliases = (entityAliasesList: EntityAlias[]): EntityAliases => {
  const entityAliases: EntityAliases = {};
  for (const entityAlias of entityAliasesList) {
    entityAliases[entityAlias.id] = entityAlias;
  }
  return entityAliases;
}

export const filtersToReportFilterList = (filters: Filters): ReportFilter[] => {
  const reportFilters: ReportFilter[] = [];
  for (const id of Object.keys(filters)) {
    reportFilters.push(filterToReportFilter(filters[id]));
  }
  return reportFilters;
}

export const reportFilterListToFilters = (reportFilters: ReportFilter[]): Filters => {
  const filters: Filters = {};
  for (const filter of reportFilters) {
    filters[filter.id] = reportFilterToFilter(filter);
  }
  return filters;
}

export const reportFilterToFilter = (reportFilter: ReportFilter): Filter => {
  const keyFilterInfos = keyFiltersToKeyFilterInfos(reportFilter.keyFilters);
  return {
    id: reportFilter.id,
    filter: reportFilter.filter,
    keyFilters: keyFilterInfos,
    editable: false
  };
}

export const filterToReportFilter = (filter: Filter): ReportFilter => {
  const keyFilters = keyFilterInfosToKeyFilters(filter.keyFilters);
  return {
    id: filter.id,
    filter: filter.filter,
    keyFilters
  };
}

export enum TbReportFormat {
  PDF = 'PDF',
  CSV = 'CSV'
}

export interface ReportTemplateConfig {
  format: TbReportFormat;
}

export interface AbstractReportTemplateConfig extends ReportTemplateConfig {
  namePattern: string;
}

export interface ReportTemplateSettings {
  name: string;
  namePattern: string;
  description?: string;
}

export interface PdfReportTemplateConfig extends AbstractReportTemplateConfig {
  entityAliases: EntityAlias[];
  filters: ReportFilter[];
  header: HeaderFooter;
  footer: HeaderFooter;
  components: ReportComponentConfig[];
}

export interface CsvReportTemplateConfig extends AbstractReportTemplateConfig {
  entityAlias: EntityAlias;
  filter: ReportFilter;
  component: TableReportComponentConfig;
}

export interface ReportTemplate<Config extends ReportTemplateConfig = ReportTemplateConfig> extends BaseReportTemplate {
  configuration: Config;
}

export const defaultReportTemplate: ReportTemplate<PdfReportTemplateConfig> = {
  name: '',
  type: ReportTemplateType.REPORT,
  configuration: {
    format: TbReportFormat.PDF,
    namePattern: 'report-%d{yyyy-MM-dd_HH:mm:ss}',
    header: {
      enabled: true,
      components: []
    },
    footer: {
      enabled: true,
      components: []
    },
    entityAliases: [],
    filters: [],
    components: []
  } as PdfReportTemplateConfig
};

export const validateAndUpdateReportTemplate =
  <Config extends ReportTemplateConfig>(reportTemplate: ReportTemplate<Config>): ReportTemplate<Config> => {
  if (!reportTemplate.configuration.format) {
    reportTemplate.configuration.format = TbReportFormat.PDF;
  }
  if (reportTemplate.configuration.format === TbReportFormat.PDF) {
    const configuration = reportTemplate.configuration as any as PdfReportTemplateConfig;
    configuration.header = validateAndUpdateReportTemplateHeaderFooter(configuration.header);
    configuration.footer = validateAndUpdateReportTemplateHeaderFooter(configuration.footer);
  } else {
    const configuration = reportTemplate.configuration as any as CsvReportTemplateConfig;
  }
  return reportTemplate;
}

const validateAndUpdateReportTemplateHeaderFooter = (headerFooter: HeaderFooter): HeaderFooter => {
  if (!headerFooter) {
    headerFooter = { enabled: true, components: [], firstPage: { enabled: false, components: [] } };
  }
  if (!headerFooter.components) {
    headerFooter.components = [];
  }
  if (!headerFooter.firstPage) {
    headerFooter.firstPage = { enabled: false, components: [] };
  }
  if (!headerFooter.firstPage.components) {
    headerFooter.firstPage.components = [];
  }
  return headerFooter;
}

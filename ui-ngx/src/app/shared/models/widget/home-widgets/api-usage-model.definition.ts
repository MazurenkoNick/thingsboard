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

import { EntityAliases, EntityAliasInfo, getEntityAliasId } from '@shared/models/alias.models';
import { FilterInfo, Filters } from '@shared/models/query/query.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { Datasource, DatasourceType, Widget } from '@shared/models/widget.models';
import { WidgetModelDefinition } from '@shared/models/widget/widget-model.definition';
import {
  ApiUsageWidgetSettings,
  getUniqueDataKeys
} from '@home/components/widget/lib/settings/cards/api-usage-settings.component.models';

interface AliasFilterPair {
  alias?: EntityAliasInfo;
  filter?: FilterInfo;
}

interface ApiUsageDatasourcesInfo {
  ds?: AliasFilterPair;
}

export const ApiUsageModelDefinition: WidgetModelDefinition<ApiUsageDatasourcesInfo> = {
  testWidget(widget: Widget): boolean {
    if (widget?.config?.settings) {
      const settings = widget.config.settings;
      if (settings.apiUsageDataKeys && Array.isArray(settings.apiUsageDataKeys)) {
        return true;
      }
    }
    return false;
  },
  prepareExportInfo(dashboard: Dashboard, widget: Widget): ApiUsageDatasourcesInfo {
    const settings: ApiUsageWidgetSettings = widget.config.settings as ApiUsageWidgetSettings;
    const info: ApiUsageDatasourcesInfo = {};
    if (settings.dsEntityAliasId) {
      info.ds = prepareExportDataSourcesInfo(dashboard, settings.dsEntityAliasId);
    }
    return info;
  },
  updateFromExportInfo(widget: Widget, entityAliases: EntityAliases, filters: Filters, info: ApiUsageDatasourcesInfo): void {
    const settings: ApiUsageWidgetSettings = widget.config.settings as ApiUsageWidgetSettings;
    if (info?.ds?.alias) {
      settings.dsEntityAliasId = getEntityAliasId(entityAliases, info.ds.alias);
    }
  },
  datasources(widget: Widget): Datasource[] {
    const settings: ApiUsageWidgetSettings = widget.config.settings as ApiUsageWidgetSettings;
    const datasources: Datasource[] = [];
    if (settings.apiUsageDataKeys?.length && settings.dsEntityAliasId) {
      datasources.push({
        type: DatasourceType.entity,
        name: '',
        entityAliasId: settings.dsEntityAliasId,
        dataKeys: getUniqueDataKeys(settings.apiUsageDataKeys)
      });
    }
    return datasources;
  },
  hasTimewindow(): boolean {
    return false;
  }
};

const prepareExportDataSourcesInfo = (dashboard: Dashboard, settings: string): AliasFilterPair => {
  const aliasAndFilter: AliasFilterPair = {};
  const entityAlias = dashboard.configuration.entityAliases[settings];
  if (entityAlias) {
    aliasAndFilter.alias = {
      alias: entityAlias.alias,
      filter: entityAlias.filter
    };
  }
  return aliasAndFilter;
}

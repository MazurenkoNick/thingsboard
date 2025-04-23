/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.data.util;

import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.Filter;

import java.util.ArrayList;
import java.util.List;

public class EntityDataQueryUtils {

    public static EntityDataQuery toEntityDataQuery(DataSource dataSource, List<EntityAlias> entityAliases, List<Filter> filters) {
        EntityAlias entityAlias = entityAliases.stream().filter(alias -> alias.getId().equals(dataSource.getEntityAliasId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entity alias not found: " + dataSource.getEntityAliasId()));
        Filter entityFilter = filters.stream().filter(filter -> filter.getId().equals(dataSource.getFilterId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Entity alias not found: " + dataSource.getFilterId()));
        List<EntityKey> entityFields = new ArrayList<>();
        List<EntityKey> latestValues = new ArrayList<>();
        for (DataKey dataKey : dataSource.getDataKeys()) {
            if (dataKey.getType().equals("attribute")) {
                latestValues.add(new EntityKey(EntityKeyType.ATTRIBUTE, dataKey.getType()));
            } else if (dataKey.getType().equals("timeseries")) {
                latestValues.add(new EntityKey(EntityKeyType.TIME_SERIES, dataKey.getType()));
            } else if (dataKey.getType().equals("entityField")) {
                entityFields.add(new EntityKey(EntityKeyType.ENTITY_FIELD, dataKey.getType()));
            }
        }
        EntityDataPageLink pageLink = new EntityDataPageLink(Integer.MAX_VALUE, 0, null, null);
        return new EntityDataQuery(entityAlias.getFilter(), pageLink, entityFields, latestValues, entityFilter.getKeyFilters());


    }
}

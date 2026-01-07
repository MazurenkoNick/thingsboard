/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.report.renderer;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;

import java.util.LinkedList;
import java.util.List;

@Component
public class TimeseriesTableRenderer extends TableWithLayoutComponentRenderer<TimeseriesTableComponent> {

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_TABLE;
    }

    @Override
    protected String noDataMessage() {
        return "No time series data found";
    }

    @Override
    protected List<DataKey> getColumns(TimeseriesTableComponent component, DataSource dataSource) {
        List<DataKey> columns = new LinkedList<>();
        if (component.isShowTimestamp()) {
            DataKey tsDataKey = DataKey.builder()
                    .name("ts")
                    .label(component.getTimestampLabel())
                    .settings(component.getTimestampColumnSettings())
                    .usePostProcessing(false)
                    .build();
            columns.add(tsDataKey);
        }
        columns.addAll(super.getColumns(component, dataSource));
        return columns;
    }

}

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
package org.thingsboard.server.report.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.style.Heading;
import org.thingsboard.server.report.context.ComponentData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.thingsboard.server.common.data.util.DataSourceUtils.getEntityLatestValue;

@Slf4j
public class ReportUtils {

    public static final Pattern REPORT_NAME_DATE_PATTERN = Pattern.compile("%d\\{([^\\}]*)\\}");
    public static final String DEFAULT_REPORT_NAME_PATTERN = "report-%d{yyyy-MM-dd_HH:mm:ss}";

    public static String prepareReportName(String namePattern, Date reportDate, TimeZone tz) {
        String name = (namePattern == null || namePattern.isEmpty()) ? DEFAULT_REPORT_NAME_PATTERN : namePattern;
        Matcher matcher = REPORT_NAME_DATE_PATTERN.matcher(name);
        while (matcher.find()) {
            String toReplace = matcher.group(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat(matcher.group(1));
            dateFormat.setTimeZone(tz);
            String replacement = dateFormat.format(reportDate);
            name = name.replace(toReplace, replacement);
        }
        return name;
    }

    public static void prepareReportComponent(ReportComponent component) {
        if (component instanceof DataReportComponent dataReportComponent) {
            List<DataSource> dataSources = dataReportComponent.getDataSources();
            if (dataSources != null && !dataSources.isEmpty()) {
                for (DataSource dataSource : dataSources) {
                    if (dataSource != null) {
                        prepareDataKeys(dataSource.getDataKeys());
                        prepareDataKeys(dataSource.getLatestDataKeys());
                    }
                }
            }
        }
    }

    private static void prepareDataKeys(List<DataKey> dataKeys) {
        if (dataKeys != null && !dataKeys.isEmpty()) {
            for (DataKey dataKey : dataKeys) {
                if (dataKey != null) {
                    if (StringUtils.isBlank(dataKey.getLabel())) {
                        dataKey.setLabel(dataKey.getName());
                    }
                }
            }
        }
    }

    public static Optional<DataSource> getSingleDataSource(DataReportComponent component) {
        DataSource dataSource = null;
        if (ReportComponentType.ALARM_TABLE.equals(component.getType())) {
            dataSource = ((AlarmTableComponent)component).getAlarmSource();
        } else {
            List<DataSource> dataSources = component.getDataSources();
            if (dataSources != null && !dataSources.isEmpty()) {
                dataSource = dataSources.get(0);
            }
        }
        if (dataSource == null) {
            return Optional.empty();
        }
        switch (dataSource.getType()) {
            case "device":
                if (dataSource.getDeviceId() == null) {
                    return Optional.empty();
                }
                break;
            case "entity":
                if (dataSource.getEntityAliasId() == null) {
                    return Optional.empty();
                }
        }
        return Optional.of(dataSource);
    }

    public static String updateDashboardReportStateParamsWithEntity(String state, EntityData stateEntity) {
        JsonNode stateObj = null;
        if (StringUtils.isNotBlank(state)) {
            try {
                String decoded = new String(Base64.getDecoder().decode(state));
                JsonNode parsed = JacksonUtil.toJsonNode(decoded);
                if (parsed.isArray() && !parsed.isEmpty()) {
                    stateObj = parsed;
                }
            } catch (Exception ignored) {}
        }
        if (stateObj == null) {
            stateObj = JacksonUtil.newArrayNode();
            ObjectNode stateData = JacksonUtil.newObjectNode();
            stateData.set("id", NullNode.getInstance());
            ((ArrayNode)stateObj).add(stateData);
        }

        ObjectNode stateParams;
        JsonNode stateData = stateObj.get(stateObj.size() - 1);
        if (stateData.has("params") && stateData.get("params").isObject()) {
            stateParams = (ObjectNode) stateData.get("params");
        } else {
            stateParams = JacksonUtil.newObjectNode();
            ((ObjectNode) stateData).set("params", stateParams);
        }
        stateParams.set("entityId", JacksonUtil.valueToTree(stateEntity.getEntityId()));
        Optional<String> entityName = getEntityLatestValue(stateEntity, EntityKeyType.ENTITY_FIELD, "name");
        Optional<String> entityLabel = getEntityLatestValue(stateEntity, EntityKeyType.ENTITY_FIELD, "label");
        stateParams.remove("entityName");
        stateParams.remove("entityLabel");
        entityName.ifPresent(s -> stateParams.put("entityName", s));
        entityLabel.ifPresent(s -> stateParams.put("entityLabel", s));
        String newStateJsonStr = JacksonUtil.toString(stateObj);
        return new String(Base64.getEncoder().encode(newStateJsonStr.getBytes()));
    }

    public static String formatNumericValue(String value, DataKey dataKey) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            if (dataKey.getDecimals() != null) {
                BigDecimal decimal = new BigDecimal(value);
                value = decimal.setScale(dataKey.getDecimals(), RoundingMode.HALF_UP).toPlainString();
            }
        } catch (NumberFormatException | ArithmeticException e) {
            log.warn("Failed to format value for data key '{}': {}", dataKey.getName(), e.getMessage());
        }
        if (dataKey.getUnits() != null) {
            value += dataKey.getUnits();
        }
        return value;
    }

}

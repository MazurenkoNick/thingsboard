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
package org.thingsboard.server.report.renderer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.CellSettings;
import org.thingsboard.server.common.data.report.configuration.ColumnSettings;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.TableWithLayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.common.data.report.configuration.style.Heading;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.report.util.ReportQueryUtils.mapLabelsToDataKeys;
import static org.thingsboard.server.report.util.ReportUtils.formatNumericValue;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;

@Slf4j
public abstract class TableWithLayoutComponentRenderer<C extends TableWithLayoutReportComponent> extends ReportComponentWithLayoutRenderer<C> {

    protected String dataSourceName() {
        return "data source";
    }

    protected String noDataMessage() {
        return "Table content is empty";
    }

    @Override
    protected String renderContent(C component, ComponentData reportDataSource) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", "No " + dataSourceName() + " is configured for table. " +
                    "Please check the " + dataSourceName() + " configuration."));
        }
        List<DataKey> columns = getColumns(component, dataSource.get());
        if (columns.isEmpty()) {
            return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", "No columns are configured for the table component. " +
                    "Please check the " + dataSourceName() + " configuration."));
        }
        Map<String, DataKey> labelToDataKey = mapLabelsToDataKeys(columns);

        HashMap<String, CellVariables> headerVariables = getCellVariablesMap(labelToDataKey, true);
        HashMap<String, CellVariables> cellVariables = getCellVariablesMap(labelToDataKey, false);

        List<LinkedHashMap<String, CellVariables>> rows = new ArrayList<>();
        for (Map<String, String> entityData : reportDataSource.getEntityDatas()) {
            LinkedHashMap<String, CellVariables> row = new LinkedHashMap<>();
            for (Map.Entry<String, CellVariables> column : headerVariables.entrySet()) {
                String label = column.getKey();
                String key = column.getValue().getKey();
                CellVariables baseStyles = cellVariables.getOrDefault(label, new CellVariables());
                CellVariables variables = baseStyles.toBuilder()
                        .fontSize(formatFontSize(key, entityData.get(label), baseStyles.getFontSize()))
                        .fontWeight(formatFontWeight(key, entityData.get(label), baseStyles.getFontWeight()))
                        .color(formatColor(key, entityData.get(label), baseStyles.getColor()))
                        .value(formatValue(key, entityData.get(label), labelToDataKey.get(label))).build();
                row.put(label, variables);
            }
            rows.add(row);
        }

        HashMap<String, Object> componentVariables = new HashMap<>();
        componentVariables.put("columns", headerVariables);
        componentVariables.put("rows", rows);
        componentVariables.put("noDataMessage", noDataMessage());

        if (component.isShowTableHeading() && component.getTableHeading() != null) {
            componentVariables.put("showTableHeading", true);
            Heading tableHeading = component.getTableHeading();
            String headingText = ThymeleafUtil.renderFromHtmlString(tableHeading.getText(), reportDataSource.getVariables());
            componentVariables.put("headingText", headingText);
            this.formatTableHeading(tableHeading, componentVariables);
        } else {
            componentVariables.put("showTableHeading", false);
        }
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/table-template", componentVariables);
    }

    protected List<DataKey> getColumns(C component, DataSource dataSource) {
        List<DataKey> allDataKeys = new LinkedList<>();
        allDataKeys.addAll(dataSource.getDataKeys());
        allDataKeys.addAll(dataSource.getLatestDataKeys());
        return allDataKeys;
    }

    private void formatTableHeading(Heading tableHeading, Map<String, Object> componentVariables) {
        componentVariables.put("headingColor", tableHeading.getColor() != null ? ColorUtils.normalizeCssColor(tableHeading.getColor()) : "#000");
        Font headingFont = tableHeading.getFont();
        if (headingFont == null) {
            headingFont = new Font();
            headingFont.setSize(20f);
            headingFont.setFamily("Roboto");
            headingFont.setStyle(FontStyle.NORMAL);
            headingFont.setWeight(FontWeight.NORMAL);
        }
        if (headingFont.getSize() != null && headingFont.getSize() > 0) {
            componentVariables.put("headingFontSize", headingFont.getSize());
        } else {
            componentVariables.put("headingFontSize", 10);
        }
        componentVariables.put("headingFontWeight", headingFont.getWeight() != null ? headingFont.getWeight().getValue() : FontWeight.NORMAL.getValue());
        componentVariables.put("headingFontStyle", headingFont.getStyle() != null ? headingFont.getStyle().getValue() : FontStyle.NORMAL.getValue());
        if (StringUtils.isNotBlank(headingFont.getFamily())) {
            componentVariables.put("headingFontFamily", headingFont.getFamily());
        } else {
            componentVariables.put("headingFontFamily", "Roboto");
        }
        TextAlignment textAlignment = tableHeading.getTextAlignment() != null ? tableHeading.getTextAlignment() : TextAlignment.CENTER;
        componentVariables.put("headingTextAlignment", textAlignment.getValue());
        VerticalAlignment verticalAlignment = tableHeading.getTextAlignment() != null ? tableHeading.getVerticalAlignment() : VerticalAlignment.MIDDLE;
        componentVariables.put("headingVerticalAlignment", verticalAlignment.getValue());
        if (tableHeading.getHeight() != null && tableHeading.getHeight() > 0) {
            componentVariables.put("headingHeight", tableHeading.getHeight() + "pt");
        } else {
            componentVariables.put("headingHeight", "100%");
        }
    }

    private Float formatFontSize(String key, String value, Float defaultSize) {
        if (defaultSize != null) {
            return defaultSize;
        }
        return defaultFontSize(key, value);
    }

    private String formatFontWeight(String key, String value, String defaultWeight) {
        if (defaultWeight != null) {
            return defaultWeight;
        }
        return defaultFontWeight(key, value);
    }

    private String formatColor(String key, String value, String defaultColor) {
        if (defaultColor != null) {
            return defaultColor;
        }
        return defaultColor(key, value);
    }

    private String formatValue(String key, String value, DataKey dataKey) {
        if (value == null || value.isBlank()) {
            return "";
        }
        value = defaultValue(key, value);
        if (dataKey != null && (dataKey.getDecimals() != null || dataKey.getUnits() != null)) {
            value = formatNumericValue(value, dataKey);
        }
        return value;
    }

    protected Float defaultFontSize(String key, String value) {
        return null;
    }

    protected String defaultFontWeight(String key, String value) {
        return null;
    }

    protected String defaultColor(String key, String value) {
        return null;
    }

    protected String defaultValue(String key, String value) {
        return value;
    }

    protected HashMap<String, CellVariables> getCellVariablesMap(Map<String, DataKey> labelToDataKey, boolean isHeader) {
        HashMap<String, CellVariables> result = new LinkedHashMap<>();
        labelToDataKey.forEach((label, dataKey) -> result.put(label, toCellVariables(dataKey, isHeader)));
        return result;
    }

    protected CellVariables toCellVariables(DataKey dataKey, boolean isHeader) {
        DataKeySettings settings = dataKey.getSettings();
        ColumnSettings columnSettings = null;
        if (settings instanceof ColumnSettings) {
            columnSettings = (ColumnSettings) settings;
        }
        return toCellVariables(dataKey.getName(), columnSettings, isHeader);
    }

    protected CellVariables toCellVariables(String key, ColumnSettings columnSettings, boolean isHeader) {
        if (columnSettings != null) {
            CellSettings cellSettings = isHeader ? columnSettings.getHeader() : columnSettings.getCell();
            if (cellSettings != null) {
                Font font = cellSettings.getFont();
                return CellVariables.builder()
                        .key(key)
                        .width(isHeader && !StringUtils.isBlank(columnSettings.getColumnWidth()) ? columnSettings.getColumnWidth() : null)
                        .color(cellSettings.getColor() != null ? ColorUtils.normalizeCssColor(cellSettings.getColor()) : null)
                        .backgroundColor(cellSettings.getBackgroundColor() != null ? ColorUtils.normalizeCssColor(cellSettings.getBackgroundColor()) : null)
                        .fontSize(font != null && font.getSize() != null && font.getSize() > 0 ? font.getSize() : null)
                        .fontWeight(font != null && font.getWeight() != null ? font.getWeight().name() : null)
                        .fontStyle(font != null && font.getStyle() != null ? font.getStyle().name() : null)
                        .fontFamily(font != null && font.getFamily() != null && !font.getFamily().isEmpty() ? font.getFamily() : null)
                        .textAlignment(cellSettings.getTextAlignment() != null ? cellSettings.getTextAlignment().name() : null)
                        .verticalAlignment(cellSettings.getTextAlignment() != null ? cellSettings.getVerticalAlignment().name() : null)
                        .build();
            }
        }
        return new CellVariables(isHeader ? key : "");
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder(toBuilder = true)
    static class CellVariables {
        private String key;
        private String value;
        private String width;
        private String color;
        private String backgroundColor;
        private Float fontSize;
        private String fontWeight;
        private String fontStyle;
        private String fontFamily;
        private String textAlignment;
        private String verticalAlignment;

        public CellVariables(String value) {
            this.value = value;
        }
    }

}

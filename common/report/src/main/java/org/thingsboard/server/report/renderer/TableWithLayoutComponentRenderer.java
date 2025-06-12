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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;

@Slf4j
public abstract class TableWithLayoutComponentRenderer<C extends TableWithLayoutReportComponent> extends ReportComponentWithLayoutRenderer<C> {

    protected String dataSourceName() {
        return "data source";
    }

    protected String noDataMessage() {
        return "Table content is empty";
    }

    protected String headingText(Heading tableHeading, ComponentData reportDataSource) {
        Map<String, Object> tableHeadingVariables = new HashMap<>();
        String entityName = "";
        String entityLabel = "";
        Integer rowCount = 0;
        List<Map<String, String>> entityDatas = reportDataSource.getEntityDatas();
        if (!entityDatas.isEmpty()) {
            rowCount = entityDatas.size();
            Map<String, String> row = entityDatas.get(0);
            entityName = row.get("entityName");
            entityLabel = row.get("entityLabel");
        }
        tableHeadingVariables.put("entityName", entityName);
        tableHeadingVariables.put("entityLabel", entityLabel);
        tableHeadingVariables.put("rowCount", String.valueOf(rowCount));
        return ThymeleafUtil.renderFromHtmlString(tableHeading.getText(), tableHeadingVariables);
    }

    @Override
    protected String renderContent(C component, ComponentData reportDataSource) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", "No " + dataSourceName() + " is configured for table. " +
                    "Please check the " + dataSourceName() + " configuration."));
        }
        List<DataKey> dataKeys = dataSource.stream().flatMap(ds -> Stream.of(Optional.ofNullable(ds.getDataKeys()).orElse(Collections.emptyList()),
                        Optional.ofNullable(ds.getLatestDataKeys()).orElse(Collections.emptyList())))
                .flatMap(Collection::stream)
                .toList();
        if (dataKeys.isEmpty()) {
            return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", "No columns are configured for the table component. " +
                    "Please check the " + dataSourceName() + " configuration."));
        }
        Map<String, DataKey> labelToDataKey = dataKeys.stream()
                .collect(Collectors.toMap(
                        DataKey::getLabel,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        HashMap<String, CellVariables> columns = getCellVariablesMap(component, labelToDataKey, true);
        HashMap<String, CellVariables> cellStyles = getCellVariablesMap(component, labelToDataKey, false);

        List<LinkedHashMap<String, CellVariables>> rows = new ArrayList<>();
        for (Map<String, String> entityData : reportDataSource.getEntityDatas()) {
            LinkedHashMap<String, CellVariables> row = new LinkedHashMap<>();
            for (Map.Entry<String, CellVariables> column : columns.entrySet()) {
                String label = column.getKey();
                String key = column.getValue().getValue();
                CellVariables baseStyles = cellStyles.getOrDefault(label, new CellVariables());
                CellVariables cellVariables = baseStyles.toBuilder()
                        .fontSize(formatFontSize(key, entityData.get(key), baseStyles.getFontSize()))
                        .fontWeight(formatFontWeight(key, entityData.get(key), baseStyles.getFontWeight()))
                        .color(formatColor(key, entityData.get(key), baseStyles.getColor()))
                        .value(formatValue(key, entityData.get(key), labelToDataKey.get(label))).build();
                row.put(label, cellVariables);
            }
            rows.add(row);
        }

        HashMap<String, Object> componentVariables = new HashMap<>();
        componentVariables.put("columns", columns);
        componentVariables.put("rows", rows);
        componentVariables.put("noDataMessage", noDataMessage());

        if (component.isShowTableHeading() && component.getTableHeading() != null) {
            componentVariables.put("showTableHeading", true);
            Heading tableHeading = component.getTableHeading();
            String headingText = headingText(tableHeading, reportDataSource);
            componentVariables.put("headingText", headingText);
            this.formatTableHeading(tableHeading, componentVariables);
        } else {
            componentVariables.put("showTableHeading", false);
        }
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/table-template", componentVariables);
    }

    private void formatTableHeading(Heading tableHeading, Map<String, Object> componentVariables) {
        componentVariables.put("headingColor", tableHeading.getColor() != null ? ColorUtils.normalizeCssColor(tableHeading.getColor()) : "#000");
        Font headingFont = tableHeading.getFont();
        if (headingFont == null) {
            headingFont = new Font();
            headingFont.setSize(20f);
            headingFont.setFamily("Roboto");
            headingFont.setStyle(FontStyle.normal);
            headingFont.setWeight(FontWeight.normal);
        }
        if (headingFont.getSize() != null && headingFont.getSize() > 0) {
            componentVariables.put("headingFontSize", headingFont.getSize());
        } else {
            componentVariables.put("headingFontSize", 10);
        }
        componentVariables.put("headingFontWeight", headingFont.getWeight() != null ? headingFont.getWeight() : FontWeight.normal);
        componentVariables.put("headingFontStyle", headingFont.getStyle() != null ? headingFont.getStyle() : FontStyle.normal);
        if (StringUtils.isNotBlank(headingFont.getFamily())) {
            componentVariables.put("headingFontFamily", headingFont.getFamily());
        } else {
            componentVariables.put("headingFontFamily", "Roboto");
        }
        TextAlignment textAlignment = tableHeading.getTextAlignment() != null ? tableHeading.getTextAlignment() : TextAlignment.center;
        componentVariables.put("headingTextAlignment", textAlignment.name());
        VerticalAlignment verticalAlignment = tableHeading.getTextAlignment() != null ? tableHeading.getVerticalAlignment() : VerticalAlignment.middle;
        componentVariables.put("headingVerticalAlignment", verticalAlignment.name());
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
        if (dataKey != null) {
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
        return defaultValue(key, value);
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

    protected HashMap<String, CellVariables> getCellVariablesMap(C component, Map<String, DataKey> labelToDataKey, boolean isHeader) {
        HashMap<String, CellVariables> result = new LinkedHashMap<>();
        labelToDataKey.forEach((key, dataKey) -> result.put(key, toCellVariables(dataKey, isHeader)));
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

    protected CellVariables toCellVariables(String name, ColumnSettings columnSettings, boolean isHeader) {
        if (columnSettings != null) {
            CellSettings cellSettings = isHeader ? columnSettings.getHeader() : columnSettings.getCell();
            if (cellSettings != null) {
                Font font = cellSettings.getFont();
                return CellVariables.builder()
                        .value(isHeader ? name : "")
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
        return new CellVariables(isHeader ? name : "");
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder(toBuilder = true)
    static class CellVariables {
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

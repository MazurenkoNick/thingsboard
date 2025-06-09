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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.CellSettings;
import org.thingsboard.server.common.data.report.configuration.ColumnSettings;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;


public abstract class TableComponentRenderer extends ReportComponentWithLayoutRenderer {

    protected String dataSourceName() {
        return "data source";
    }

    protected String noDataMessage() {
        return "Table content is empty";
    }

    protected boolean displayTitle(ComponentData reportDataSource) {
        return false;
    }

    protected String title(ComponentData reportDataSource) {
        return null;
    }

    @Override
    protected String renderContent(ReportComponent component, ComponentData reportDataSource) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", "No "+dataSourceName()+" is configured for table. " +
                    "Please check the "+dataSourceName()+" configuration."));
        }
        List<DataKey> dataKeys  = dataSource.get().getDataKeys();
        if (dataKeys == null || dataKeys.isEmpty()) {
            return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", "No columns are configured for the table component. " +
                    "Please check the "+dataSourceName()+" configuration."));
        }

        HashMap<String, CellVariables> headerStyles = getCellVariablesMap(component, dataSource.get(), true);
        HashMap<String, CellVariables> cellStyles = getCellVariablesMap(component, dataSource.get(), false);

        List<LinkedHashMap<String, CellVariables>> rows = reportDataSource.getEntityDatas().stream()
                .map(row -> row.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> {
                                    CellVariables baseStyles = cellStyles.getOrDefault(entry.getKey(), new CellVariables());
                                    return baseStyles.toBuilder()
                                            .fontSize(formatFontSize(entry, baseStyles.getFontSize()))
                                            .fontWeight(formatFontWeight(entry, baseStyles.getFontWeight()))
                                            .color(formatColor(entry, baseStyles.getColor()))
                                            .value(formatValue(entry, row)).build();
                                },
                                (v1, v2) -> v1,
                                LinkedHashMap::new
                        ))
                ).toList();

        HashMap<String, Object> componentVariables = new HashMap<>();
        componentVariables.put("columns", headerStyles);
        componentVariables.put("rows", rows);
        componentVariables.put("noDataMessage", noDataMessage());
        componentVariables.put("displayTitle", displayTitle(reportDataSource));
        componentVariables.put("title", title(reportDataSource));
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/table-template", componentVariables);
    }

    private Float formatFontSize(Map.Entry<String, String> entry, Float defaultSize) {
        if (defaultSize != null) {
            return defaultSize;
        }
        return defaultFontSize(entry);
    }

    private String formatFontWeight(Map.Entry<String, String> entry, String defaultWeight) {
        if (defaultWeight != null) {
            return defaultWeight;
        }
        return defaultFontWeight(entry);
    }

    private String formatColor(Map.Entry<String, String> entry, String defaultColor) {
        if (defaultColor != null) {
            return defaultColor;
        }
        return defaultColor(entry);
    }

    private String formatValue(Map.Entry<String, String> entry, Map<String, String> row) {
        return defaultValue(entry, row);
    }

    protected Float defaultFontSize(Map.Entry<String, String> entry) {
        return null;
    }

    protected String defaultFontWeight(Map.Entry<String, String> entry) {
        return null;
    }

    protected String defaultColor(Map.Entry<String, String> entry) {
        return null;
    }

    protected String defaultValue(Map.Entry<String, String> entry, Map<String, String> row) {
        return entry.getValue();
    }

    protected HashMap<String, CellVariables> getCellVariablesMap(ReportComponent component, DataSource dataSource, boolean isHeader) {
        List<DataKey> dataKeys = dataSource.getDataKeys();
        return dataKeys.stream()
                .collect(Collectors.toMap(
                        DataKey::getName,
                        dataKey -> toCellVariables(dataKey, isHeader),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    protected CellVariables toCellVariables(DataKey dataKey, boolean isHeader) {
        DataKeySettings settings = dataKey.getSettings();
        ColumnSettings columnSettings = null;
        if (settings instanceof ColumnSettings) {
            columnSettings = (ColumnSettings) settings;
        }
        return toCellVariables(dataKey.getLabel(), columnSettings, isHeader);
    }

    protected CellVariables toCellVariables(String label, ColumnSettings columnSettings, boolean isHeader) {
        if (columnSettings != null) {
            CellSettings cellSettings = isHeader ? columnSettings.getHeader() : columnSettings.getCell();
            if (cellSettings != null) {
                Font font = cellSettings.getFont();
                return CellVariables.builder()
                        .value(isHeader ? label : "")
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
        return new CellVariables(isHeader ? label : "");
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

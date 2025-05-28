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

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.CellSettings;
import org.thingsboard.server.common.data.report.configuration.ColumnSettings;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataKeySettings;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;
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

    private static final String TABLE_IS_NOT_CONFIGURED = "<table class=\"tb-report-table\">\n" +
            "    <thead>\n" +
            "    <tr>\n" +
            "        <td>Table columns are not configured</td>\n" +
            "    </tr>\n" +
            "    </thead>\n" +
            "</table>";

    @Override
    protected String renderContent(ReportComponent component, ComponentData reportDataSource) {
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return ThymeleafUtil.render("html/components/error-template", Map.of("errorMessage", "No columns are configured for the table component. " +
                    "Please check the data source configuration."));
        }
        List<DataKey> dataKeys  = dataSource.get().getDataKeys();
        if (dataKeys == null || dataKeys.isEmpty()) {
            return ThymeleafUtil.render("html/components/error-template", Map.of("errorMessage", "No columns are configured for the table component. " +
                    "Please check the data source configuration."));
        }

        HashMap<String, CellVariables> headerStyles = getCellVariablesMap(dataKeys, true);
        HashMap<String, CellVariables> cellStyles = getCellVariablesMap(dataKeys, false);

        List<LinkedHashMap<String, CellVariables>> rows = reportDataSource.getEntityDatas().stream()
                .map(row -> row.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> {
                                    CellVariables base = cellStyles.getOrDefault(entry.getKey(), defaultCellStyle(false));
                                    return base.toBuilder().value(entry.getValue()).build();
                                },
                                (v1, v2) -> v1,
                                LinkedHashMap::new
                        ))
                ).toList();

        HashMap<String, Object> componentVariables = new HashMap<>();
        componentVariables.put("columns", headerStyles);
        componentVariables.put("rows", rows);

        return ThymeleafUtil.render("html/components/table-template", componentVariables);
    }

    private HashMap<String, CellVariables> getCellVariablesMap(List<DataKey> dataKeys, boolean isHeader) {
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
        if (settings instanceof ColumnSettings columnSettings) {
            CellSettings cellSettings = isHeader ? columnSettings.getHeaderSettings() : columnSettings.getCellSettings();
            if (cellSettings != null) {
                Font font = cellSettings.getFont();
                return CellVariables.builder()
                        .value(isHeader ? dataKey.getLabel() : "")
                        .color(cellSettings.getColor() != null ? ColorUtils.normalizeCssColor(cellSettings.getColor()) : "#000")
                        .fontSize(font.getSize() != null && font.getSize() > 0 ? font.getSize() : 10)
                        .fontWeight(font.getWeight().name())
                        .fontStyle(font.getStyle().name())
                        .fontFamily(font.getFamily() != null && font.getFamily().length() > 0 ? font.getFamily() : "Roboto")
                        .textAlignment(cellSettings.getTextAlignment() != null ? cellSettings.getTextAlignment().name() : TextAlignment.center.name())
                        .verticalAlignment(cellSettings.getTextAlignment() != null ? cellSettings.getVerticalAlignment().name() : VerticalAlignment.middle.name())
                        .build();
            }
        }
        CellVariables cellVariables = defaultCellStyle(isHeader);
        cellVariables.setValue(isHeader ? dataKey.getLabel() : "");
        return cellVariables;
    }

    private CellVariables defaultCellStyle(boolean isHeader) {
        return CellVariables.builder()
                .color("#000")
                .fontSize(isHeader ? 14 : 10)
                .fontWeight(isHeader ? "bold" : "normal")
                .fontStyle("normal")
                .fontFamily("Roboto")
                .textAlignment(isHeader ? TextAlignment.center.name() : TextAlignment.left.name())
                .verticalAlignment(VerticalAlignment.middle.name())
                .build();
    }

    @Data
    @Builder(toBuilder = true)
    static class CellVariables {
        private String value;
        private String color;
        private float fontSize;
        private String fontWeight;
        private String fontStyle;
        private String fontFamily;
        private String textAlignment;
        private String verticalAlignment;
    }

}

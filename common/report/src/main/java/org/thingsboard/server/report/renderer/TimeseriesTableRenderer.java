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

import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.ModeEnum;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.report.context.ReportLayout;
import org.thingsboard.server.report.context.TbReportCtx;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.report.util.JasperReportUtils.createTextField;
import static org.thingsboard.server.report.context.ReportLayout.getSingleDataSource;

@Component
public class TimeseriesTableRenderer implements ReportComponentRenderer {

    @Override
    public void render(TbReportCtx ctx, ReportLayout layoutCtx, ReportComponent component) {
        TimeseriesTableComponent tsTableComponent = (TimeseriesTableComponent) component;
        List<DataKey> dataKeys = getSingleDataSource(tsTableComponent).getDataKeys();
        List<String> entityKeys = dataKeys.stream().map(DataKey::getName).collect(Collectors.toList());
        List<String> columnsHeaders = dataKeys.stream().map(DataKey::getLabel).collect(Collectors.toList());

        entityKeys.add(0, "ts");
        columnsHeaders.add(0, "Timestamp");

        addColumnHeader(layoutCtx, columnsHeaders);
        addTableDetailBand(layoutCtx, entityKeys);
    }

    public void addColumnHeader(ReportLayout layoutCtx, List<String> titles) {
        JasperDesign jasperDesign = layoutCtx.getJasperDesign();
        JRDesignBand columnHeader = new JRDesignBand();
        columnHeader.setHeight(20);
        int x = 0;
        int columnWidth = Math.min(jasperDesign.getColumnWidth(), layoutCtx.getUsablePageWidth() /titles.size());
        for (String title : titles) {
            columnHeader.addElement(createHeaderText(title, x, columnWidth));
            x += columnWidth;
        }
        jasperDesign.setColumnHeader(columnHeader);
    }

    private JRDesignStaticText createHeaderText(String text, int x, int width) {
        JRDesignStaticText header = new JRDesignStaticText();
        header.setX(x);
        header.setY(0);
        header.setWidth(width);
        header.setHeight(20);
        //header.setHorizontalAlignment(HorizontalAlignEnum.CENTER);
        header.setText(text);
        header.setForecolor(Color.WHITE);
        header.setBackcolor(Color.GRAY);
        header.setMode(ModeEnum.OPAQUE);
        return header;
    }

    public void addTableDetailBand(ReportLayout builder, List<String> entityKeys)  {

        JasperDesign jasperDesign = builder.getJasperDesign();

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(20);
        int x = 0;
        int columnWidth = Math.min(jasperDesign.getColumnWidth(), builder.getUsablePageWidth() /entityKeys.size());
        for (String entityKey : entityKeys) {
            detailBand.addElement(createTextField("$F{" + entityKey + "}", x, 0));
            x += columnWidth;
        }
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(detailBand);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.TIME_SERIES_TABLE;
    }

}

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

import lombok.Data;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignRectangle;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignSubreport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Margins;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.common.data.report.configuration.style.PageSize.A4;

@Data
public class ReportLayoutContext {

    private static final int DEFAULT_PAGE_MARGIN_SIZE = 20;
    private static final int DEFAULT_COMPONENT_MARGIN_SIZE = 0;
    private static final Map<String, String> FONT_MAP = Map.of(
            "Roboto", "Roboto",
            "monospace", "Monospaced",
            "sans-serif", "SansSerif",
            "serif", "Serif"
    );

    private JasperDesign jasperDesign;
    private int usablePageWidth;
    private int usablePageHeight;

    public ReportLayoutContext(PdfReportTemplateConfig configuration) {
        this.jasperDesign = new JasperDesign();
        this.jasperDesign.setName("MainReport");
        this.jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        PageSize pageSize = configuration.getPageSize();
        if (pageSize != null) {
            this.jasperDesign.setPageWidth(pageSize.getWidth());
            this.jasperDesign.setPageHeight(pageSize.getHeight());
        } else {
            this.jasperDesign.setPageWidth(A4.getWidth());
            this.jasperDesign.setPageHeight(A4.getHeight());
        }
        setMargins(configuration.getPageMargins(), DEFAULT_PAGE_MARGIN_SIZE);
        this.usablePageWidth = jasperDesign.getPageWidth() - jasperDesign.getLeftMargin() - jasperDesign.getRightMargin();
        this.usablePageHeight = jasperDesign.getPageHeight() - jasperDesign.getTopMargin() - jasperDesign.getBottomMargin();
        setBackground(configuration.getPageBackground());
    }

    public ReportLayoutContext(ReportComponent component, ReportLayoutContext parentBuilder) throws JRException {
        this.jasperDesign = new JasperDesign();
        this.jasperDesign.setName(StringUtils.randomAlphabetic(8));
        this.jasperDesign.setPageWidth(parentBuilder.getJasperDesign().getPageWidth());
        this.jasperDesign.setPageHeight(parentBuilder.getJasperDesign().getPageHeight());
        setMargins(component.getMargins(), DEFAULT_COMPONENT_MARGIN_SIZE);
        this.usablePageWidth = parentBuilder.getUsablePageWidth() - jasperDesign.getLeftMargin() - jasperDesign.getRightMargin();
        this.usablePageHeight = parentBuilder.getUsablePageHeight() - jasperDesign.getTopMargin() - jasperDesign.getBottomMargin();
        setBackground(component.getBackground());

        // define report fields
        switch (component.getType()) {
            case DASHBOARD:
                jasperDesign.addField(createByteField("data"));
                break;
            case TIME_SERIES_TABLE: {
                jasperDesign.addField(createField("ts", String.class));
            }
            case HEADING, RICH_TEXT, PAGE_BREAK, ENTITY_TABLE, ALARM_TABLE: {
                List<DataSource> dataSources = component.getDataSources();
                if (dataSources != null && !dataSources.isEmpty()) {
                    List<DataKey> dataKeys = dataSources.stream()
                            .map(DataSource::getDataKeys)
                            .flatMap(Collection::stream)
                            .toList();
                    for (DataKey dataKey : dataKeys) {
                        jasperDesign.addField(createField(dataKey.getName(), String.class));
                    }
                }
            }
        }
    }

    private void setBackground(String background) {
        if (background != null) {
            JRDesignBand backgroundBand = new JRDesignBand();
            backgroundBand.setHeight(usablePageHeight);

            JRDesignRectangle backgroundRect = new JRDesignRectangle();
            backgroundRect.setX(0);
            backgroundRect.setY(0);
            backgroundRect.setWidth(usablePageWidth);
            backgroundRect.setHeight(usablePageHeight);
            backgroundRect.setBackcolor(Color.decode(background));
            backgroundRect.setMode(ModeEnum.OPAQUE);

            backgroundBand.addElement(backgroundRect);
            jasperDesign.setBackground(backgroundBand);
        }
    }

    private void setMargins(Margins margins, int defaultPageMarginSize) {
        if (margins != null) {
            this.jasperDesign.setLeftMargin(margins.getLeft());
            this.jasperDesign.setRightMargin(margins.getRight());
            this.jasperDesign.setTopMargin(margins.getTop());
            this.jasperDesign.setBottomMargin(margins.getBottom());
        } else {
            this.jasperDesign.setLeftMargin(defaultPageMarginSize);
            this.jasperDesign.setRightMargin(defaultPageMarginSize);
            this.jasperDesign.setTopMargin(defaultPageMarginSize);
            this.jasperDesign.setBottomMargin(defaultPageMarginSize);
        }
    }

    public void addPageHeader(HeaderFooter header) {
        JRDesignBand pageHeader = createHeaderFooterBand(header);
        jasperDesign.setPageHeader(pageHeader);
    }

    public void addPageFooter(HeaderFooter footer) {
        JRDesignBand pageHeader = createHeaderFooterBand(footer);
        jasperDesign.setPageFooter(pageHeader);
    }

    public JRDesignBand createHeaderFooterBand(HeaderFooter header) {
        JRDesignBand pageHeader = new JRDesignBand();

        pageHeader.setHeight(20);
        HeaderFooter firstPage = header.getFirstPage();
        if (firstPage != null) {
            JRDesignStaticText firstPageHeader = createStaticTextElement(firstPage.getText(),  "$V{PAGE_NUMBER} == 1");
            pageHeader.addElement(firstPageHeader);
        }
        JRDesignStaticText otherPagesHeader = createStaticTextElement(header.getText(),  "$V{PAGE_NUMBER} > 1");
        pageHeader.addElement(otherPagesHeader);
        return pageHeader;
    }

    public JRDesignStaticText createStaticTextElement(String text, String conditionExpression) {
        JRDesignStaticText firstPageText = new JRDesignStaticText();
        firstPageText.setX(0);
        firstPageText.setY(0);
        firstPageText.setWidth(515);
        firstPageText.setHeight(20);
        firstPageText.setText(text);
        firstPageText.setPrintWhenExpression(
                new JRDesignExpression(conditionExpression)
        );
        return firstPageText;
    }

    public JRDesignField createField(String name, Class<?> type) {
        JRDesignField field = new JRDesignField();
        field.setName(name);
        field.setValueClass(type);
        return field;
    }

    public JRDesignField createByteField(String name) {
        JRDesignField imageField = new JRDesignField();
        imageField.setName(name);
        imageField.setValueClassName("byte[]");
        return imageField;
    }

    public void addSubReportBand(String subReportExpression, String subReportDSExpression) throws JRException {
        jasperDesign.addParameter(createParameter(subReportExpression, net.sf.jasperreports.engine.JasperReport.class));
        jasperDesign.addParameter(createParameter(subReportDSExpression, JRDataSource.class));

        JRDesignBand detailBand = new JRDesignBand();

        JRDesignSubreport subReport = new JRDesignSubreport(jasperDesign);

        JRDesignExpression subExpr = new JRDesignExpression();
        subExpr.setText("$P{" + subReportExpression + "}");
        subReport.setExpression(subExpr);

        JRDesignExpression dsExpr = new JRDesignExpression();
        dsExpr.setText("$P{" + subReportDSExpression + "}");
        subReport.setDataSourceExpression(dsExpr);

        detailBand.addElement(subReport);
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(detailBand);
    }

    private JRDesignParameter createParameter(String name, Class<?> valueClass) {
        JRDesignParameter param = new JRDesignParameter();
        param.setName(name);
        param.setValueClass(valueClass);
        return param;
    }

    public static DataSource getSingleDataSource(ReportComponent component) {
        List<DataSource> dataSources = component.getDataSources();
        if (dataSources == null || dataSources.isEmpty()) {
            throw new IllegalArgumentException("Data source is required for component: " + component.getType());
        }
        return component.getDataSources().get(0);
    }
}

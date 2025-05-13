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
package org.thingsboard.server.report.context;

import lombok.Data;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRElementGroup;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignElementGroup;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignRectangle;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignSubreport;
import net.sf.jasperreports.engine.design.JRDesignSubreportParameter;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.BorderSplitType;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Margins;
import org.thingsboard.server.common.data.report.configuration.style.PageOrientation;
import org.thingsboard.server.common.data.report.configuration.style.PageSize;
import org.thingsboard.server.report.util.ColorUtils;

import java.util.Collection;
import java.util.List;

import static org.thingsboard.server.common.data.report.configuration.style.PageSize.A4;
import static org.thingsboard.server.report.util.JasperReportUtils.createParameter;
import static org.thingsboard.server.report.util.JasperReportUtils.createSubReportParameter;

@Data
public class ReportLayout {

    private static final int DEFAULT_PAGE_MARGIN_SIZE = 20;
    private static final String PAGE_NUMBER_PARAMETER = "PAGE_NO";
    private static final String PAGE_NUMBER_TOTAL_PARAMETER = "PAGE_TOTAL";

    private static final int DEFAULT_COMPONENT_MARGIN_SIZE = 0;

    private JasperDesign jasperDesign;
    private int usablePageWidth;
    private int leftMargin;
    private int rightMargin;
    private int topMargin;
    private int bottomMargin;

    private ReportLayout reportLayout;

    public ReportLayout(PdfReportTemplateConfig configuration) throws JRException {
        this.jasperDesign = new JasperDesign();
        this.jasperDesign.setName("MainReport");
        this.jasperDesign.setIgnorePagination(false);
        this.jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        PageSize pageSize = configuration.getPageSize();
        if (pageSize == null) {
            pageSize = A4;
        }
        if (configuration.getPageOrientation() == PageOrientation.LANDSCAPE) {
            this.jasperDesign.setPageWidth(pageSize.getHeight());
            this.jasperDesign.setPageHeight(pageSize.getWidth());
        } else {
            this.jasperDesign.setPageWidth(pageSize.getWidth());
            this.jasperDesign.setPageHeight(pageSize.getHeight());
        }
        setMargins(configuration.getPageMargins(), DEFAULT_PAGE_MARGIN_SIZE);
        this.usablePageWidth = jasperDesign.getPageWidth() - leftMargin - rightMargin;
        setBackground(configuration.getPageBackground());

        JRDesignParameter param = new JRDesignParameter();
        param.setName("IMAGE");
        param.setValueClassName("byte[]");
        jasperDesign.addParameter(param);
    }

    public ReportLayout(ReportComponent component, ReportLayout parentLayout) throws JRException {
        this.reportLayout = parentLayout;
        this.jasperDesign = new JasperDesign();
        this.jasperDesign.setIgnorePagination(true);
        this.jasperDesign.setName(StringUtils.randomAlphabetic(8));
        this.jasperDesign.setPageWidth(parentLayout.getJasperDesign().getPageWidth());
        this.jasperDesign.setPageHeight(0);
        this.usablePageWidth = parentLayout.getUsablePageWidth();

        setMargins(component.getMargins(), DEFAULT_COMPONENT_MARGIN_SIZE, parentLayout);

        // add parameters
        JRDesignParameter param = new JRDesignParameter();
        param.setName("IMAGE");
        param.setValueClassName("byte[]");
        jasperDesign.addParameter(param);
        jasperDesign.addParameter(createParameter(PAGE_NUMBER_PARAMETER, Integer.class));
        jasperDesign.addParameter(createParameter(PAGE_NUMBER_TOTAL_PARAMETER, Integer.class));

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
            backgroundBand.setHeight(this.jasperDesign.getPageHeight());

            JRDesignRectangle backgroundRect = new JRDesignRectangle();
            backgroundRect.setX(0);
            backgroundRect.setY(0);
            backgroundRect.setWidth(this.jasperDesign.getPageWidth());
            backgroundRect.setHeight(this.jasperDesign.getPageHeight());
            backgroundRect.setBackcolor(ColorUtils.parseCssColor(background));
            backgroundRect.setMode(ModeEnum.OPAQUE);
            backgroundRect.getLinePen().setLineWidth(0f);

            backgroundBand.addElement(backgroundRect);
            jasperDesign.setBackground(backgroundBand);
        }
    }

    private void setMargins(Margins margins, int defaultPageMarginSize) {
        this.setMargins(margins, defaultPageMarginSize, null);
    }

    private void setMargins(Margins margins, int defaultPageMarginSize, ReportLayout parentBuilder) {
        if (margins != null) {
            this.leftMargin = margins.getLeft();
            this.rightMargin = margins.getRight();
            this.topMargin  = margins.getTop();
            this.bottomMargin = margins.getBottom();
        } else {
            this.leftMargin = defaultPageMarginSize;
            this.rightMargin = defaultPageMarginSize;
            this.topMargin = defaultPageMarginSize;
            this.bottomMargin = defaultPageMarginSize;
        }
        if (parentBuilder != null) {
            this.jasperDesign.setLeftMargin(parentBuilder.getLeftMargin());
            this.jasperDesign.setRightMargin(parentBuilder.getRightMargin());
            this.jasperDesign.setTopMargin(0);
            this.jasperDesign.setBottomMargin(0);
            this.jasperDesign.setColumnWidth(this.usablePageWidth);
        } else {
            this.jasperDesign.setLeftMargin(0);
            this.jasperDesign.setRightMargin(0);
            this.jasperDesign.setTopMargin(0);
            this.jasperDesign.setBottomMargin(0);
        }
    }

    public JRDesignFrame createHeaderFooter(boolean headerElseFooter) {
        JRDesignBand pageHeader = new JRDesignBand();
        JRDesignFrame frame = new JRDesignFrame();
        frame.setWidth(usablePageWidth);
        JRDesignFrame empty = new JRDesignFrame();
        frame.addElement(empty);
        pageHeader.addElement(frame);
        if (headerElseFooter) {
            frame.getLineBox().setTopPadding(this.topMargin);
        } else {
            frame.getLineBox().setBottomPadding(this.bottomMargin);
            pageHeader.setHeight(this.bottomMargin);
        }
        if (headerElseFooter) {
            jasperDesign.setPageHeader(pageHeader);
        } else {
            jasperDesign.setPageFooter(pageHeader);
        }
/*
        pageHeader.setHeight(20);
        HeaderFooter firstPage = header.getFirstPage();
        if (firstPage != null) {
            JRDesignStaticText firstPageHeader = createStaticTextElement(firstPage.getText(),  "$V{PAGE_NUMBER} == 1");
            pageHeader.addElement(firstPageHeader);
        }
        JRDesignStaticText otherPagesHeader = createStaticTextElement(header.getText(),  "$V{PAGE_NUMBER} > 1");
        pageHeader.addElement(otherPagesHeader);*/
        return frame;
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

    public void addSubReport(String subReportExpression, String subReportDSExpression, JRElementGroup container, JRExpression printWhenExpression) throws JRException {
        jasperDesign.addParameter(createParameter(subReportExpression, net.sf.jasperreports.engine.JasperReport.class));
        jasperDesign.addParameter(createParameter(subReportDSExpression, JRDataSource.class));

        JRDesignSubreport subReport = new JRDesignSubreport(jasperDesign);
        subReport.addParameter(createSubReportParameter(PAGE_NUMBER_PARAMETER, "$V{PAGE_NUMBER}"));
        subReport.addParameter(createSubReportParameter(PAGE_NUMBER_TOTAL_PARAMETER, "$V{MASTER_TOTAL_PAGES}"));
        subReport.addParameter(createSubReportParameter("IMAGE", "$P{IMAGE}"));

        if (printWhenExpression != null) {
            subReport.setPrintWhenExpression(printWhenExpression);
        }

        JRDesignExpression subExpr = new JRDesignExpression();
        subExpr.setText("$P{" + subReportExpression + "}");
        subReport.setExpression(subExpr);

        JRDesignExpression dsExpr = new JRDesignExpression();
        dsExpr.setText("$P{" + subReportDSExpression + "}");
        subReport.setDataSourceExpression(dsExpr);

        JRDesignFrame frame = new JRDesignFrame();
        frame.setX(0);
        frame.setY(0);
        frame.setWidth(usablePageWidth);
        frame.setPositionType(PositionTypeEnum.FLOAT);
        frame.setBorderSplitType(BorderSplitType.NO_BORDERS);
        frame.addElement(subReport);

        if (container instanceof JRDesignElementGroup) {
            ((JRDesignElementGroup)container).addElement(frame);
        } else if (container instanceof JRDesignFrame) {
            ((JRDesignFrame)container).addElement(frame);
        }
    }

    public JRDesignBand createDetailsBand() {
        JRDesignBand detailBand = new JRDesignBand();
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(detailBand);
        return detailBand;
    }

    public static DataSource getSingleDataSource(ReportComponent component) {
        List<DataSource> dataSources = component.getDataSources();
        if (dataSources == null || dataSources.isEmpty()) {
            throw new IllegalArgumentException("Data source is required for component: " + component.getType());
        }
        return component.getDataSources().get(0);
    }
}

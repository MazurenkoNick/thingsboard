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
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignBreak;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignSubreport;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.BreakTypeEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.DashboardComponent;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.HeadingComponent;
import org.thingsboard.server.common.data.report.configuration.components.PageBreakComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.RichTextComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class JasperReportBuilder {

    private JasperDesign jasperDesign;
    private int usablePageWidth;

    public JasperReportBuilder(PdfReportTemplateConfig configuration) {
        this.jasperDesign = new JasperDesign();
        this.jasperDesign.setName("MainReport");
        this.jasperDesign.setPageWidth(595);
        this.jasperDesign.setPageHeight(842);
        this.jasperDesign.setColumnWidth(515);
        this.jasperDesign.setLeftMargin(40);
        this.jasperDesign.setRightMargin(40);
        this.jasperDesign.setTopMargin(50);
        this.jasperDesign.setBottomMargin(50);
        this.jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        this.usablePageWidth= jasperDesign.getPageWidth() - jasperDesign.getLeftMargin() - jasperDesign.getRightMargin();
    }

    public JasperReportBuilder(ReportComponent component, int usablePageWidth) throws JRException {
        this.usablePageWidth = usablePageWidth;
        this.jasperDesign = new JasperDesign();
        this.jasperDesign.setName(StringUtils.randomAlphabetic(8));
        this.jasperDesign.setPageWidth(595);
        this.jasperDesign.setPageHeight(842);
        this.jasperDesign.setColumnWidth(200);
        this.jasperDesign.setLeftMargin(0);
        this.jasperDesign.setRightMargin(0);
        this.jasperDesign.setTopMargin(0);
        this.jasperDesign.setBottomMargin(0);

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

    public void addHeading(HeadingComponent component) {
        JRDesignTextField textField = new JRDesignTextField();
        textField.setX(0);
        textField.setY(0);
        textField.setWidth(500);
        textField.setHeight(20);

        // Set text color if provided
        if (component.getColor() != null) {
            Color color = Color.decode(component.getColor());
            textField.setForecolor(color);
        }

        // Set font style if defined
        Font font = component.getFont();
        if (font != null) {
            textField.setBold(font.getWeight() == FontWeight.bold);
            textField.setItalic(font.getStyle() == FontStyle.italic);
            if (font.getSize() != null) {
                textField.setFontSize(font.getSize());
            }
        }

        // Set horizontal and vertical alignment
        TextAlignment textAlignment = component.getTextAlignment();
        if (textAlignment != null) {
            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.valueOf(textAlignment.getValue()));
        }
        VerticalAlignment verticalAlignment = component.getVerticalAlignment();
        if (verticalAlignment != null) {
            textField.setVerticalTextAlign(VerticalTextAlignEnum.valueOf(verticalAlignment.getValue()));
        }

        // Set the text content as a string literal
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("\"" + component.getValue() + "\"");
        textField.setExpression(expression);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(30);
        detailBand.addElement(textField);

        JRDesignSection detailSection = (JRDesignSection) jasperDesign.getDetailSection();
        detailSection.addBand(detailBand);
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

    private void addImage() {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(500); // Make sure it’s tall enough for the image

        JRDesignImage image = new JRDesignImage(jasperDesign);
        image.setX(0);
        image.setY(0);
        image.setWidth(500);
        image.setHeight(500);
        image.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);

        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("new java.io.ByteArrayInputStream($F{data})");
        expression.setValueClass(java.io.InputStream.class);

        image.setExpression(expression);
        //image.setExpression(new JRDesignExpression("net.sf.jasperreports.renderers.SimpleDataRenderer.getInstance($F{data})"));
        //image.setExpression(new JRDesignExpression("$F{data}"));


        detailBand.addElement(image);

        // Set the detail band into the design
        JRDesignSection detailSection = (JRDesignSection) jasperDesign.getDetailSection();
        detailSection.addBand(detailBand);
    }

    public void addRichText(String richText) {
        JRDesignTextField htmlField = new JRDesignTextField();
        htmlField.setX(0);
        htmlField.setY(0);
        htmlField.setWidth(500);
        htmlField.setHeight(100);
        htmlField.setMarkup("html");

        JRDesignExpression expression = new JRDesignExpression();
        expression.setText(escapeHtmlForJasperExpression(richText));
        htmlField.setExpression(expression);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(100);
        detailBand.addElement(htmlField);

        JRDesignSection detailSection = (JRDesignSection) jasperDesign.getDetailSection();
        detailSection.addBand(detailBand);
    }

    private String escapeHtmlForJasperExpression(String rawHtml) {
        String escaped = rawHtml
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "\"" + escaped + "\"";
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

    public void addColumnHeader(List<String> titles) {
        JRDesignBand columnHeader = new JRDesignBand();
        columnHeader.setHeight(20);
        int x = 0;
        int columnWidth = Math.min(jasperDesign.getColumnWidth(), getUsablePageWidth() /titles.size());
        for (String title : titles) {
            columnHeader.addElement(createHeaderText(title, x, columnWidth));
            x += columnWidth;
        }
        jasperDesign.setColumnHeader(columnHeader);
    }

    public void addTableDetailBand(List<String> entityKeys)  {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(20);
        int x = 0;
        int columnWidth = Math.min(jasperDesign.getColumnWidth(), getUsablePageWidth() /entityKeys.size());
        for (String entityKey : entityKeys) {
            detailBand.addElement(createTextField("$F{" + entityKey + "}", x, 0));
            x += columnWidth;
        }
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(detailBand);
    }

    private JRDesignTextField createTextField(String expression, int x, int y) {
        JRDesignTextField field = new JRDesignTextField();
        field.setX(x);
        field.setY(y);
        field.setWidth(180);
        field.setHeight(20);
        field.setBlankWhenNull(true);
        field.setExpression(new JRDesignExpression(expression));
        return field;
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

    public void buildSubReportBand(String subReportExpression, String subReportDSExpression) throws JRException {
        jasperDesign.addParameter(createParameter(subReportExpression, JasperReport.class));
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

    public void addPageBreak() {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(1);

        JRDesignBreak pageBreak = new JRDesignBreak();
        pageBreak.setType(BreakTypeEnum.PAGE);
        detailBand.addElement(pageBreak);
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(detailBand);
    }

    public JasperReport buildComponent(ReportComponent component) throws JRException {
        return switch (component.getType()) {
            case HEADING -> buildHeading((HeadingComponent) component);
            case RICH_TEXT -> buildRichText((RichTextComponent) component);
            case PAGE_BREAK -> buildPageBreak((PageBreakComponent) component);
            case ENTITY_TABLE -> buildEntityTable((EntityTableComponent) component);
            case TIME_SERIES_TABLE -> buildTimeSeriesTable((TimeseriesTableComponent) component);
            case ALARM_TABLE -> buildAlarmTable((AlarmTableComponent) component);
            case DASHBOARD -> buildDashboard((DashboardComponent) component);
            default -> throw new IllegalArgumentException("Unknown report component type: " + component.getType());
        };
    }

    public JasperReport buildHeading(HeadingComponent component) throws JRException {
        JasperReportBuilder heading = new JasperReportBuilder(component, getUsablePageWidth());
        heading.addHeading(component);
        return JasperCompileManager.compileReport(heading.getJasperDesign());
    }

    public JasperReport buildRichText(RichTextComponent component) throws JRException {
        JasperReportBuilder richText = new JasperReportBuilder(component, getUsablePageWidth());
        richText.addRichText(component.getValue());
        return JasperCompileManager.compileReport(richText.getJasperDesign());
    }

    public JasperReport buildEntityTable(EntityTableComponent component) throws JRException {
        JasperReportBuilder table = new JasperReportBuilder(component, getUsablePageWidth());

        List<DataKey> dataKeys = getSingleDataSource(component).getDataKeys();
        List<String> entityKeys = dataKeys.stream().map(DataKey::getName).toList();
        List<String> columnsHeaders = dataKeys.stream().map(DataKey::getLabel).toList();

        table.addColumnHeader(columnsHeaders);
        table.addTableDetailBand(entityKeys);
        return JasperCompileManager.compileReport(table.getJasperDesign());
    }

    public JasperReport buildTimeSeriesTable(TimeseriesTableComponent component) throws JRException {
        JasperReportBuilder table = new JasperReportBuilder(component, getUsablePageWidth());

        List<DataKey> dataKeys = getSingleDataSource(component).getDataKeys();
        List<String> entityKeys = dataKeys.stream().map(DataKey::getName).collect(Collectors.toList());
        List<String> columnsHeaders = dataKeys.stream().map(DataKey::getLabel).collect(Collectors.toList());

        entityKeys.add(0, "ts");
        columnsHeaders.add(0, "Timestamp");

        table.addColumnHeader(columnsHeaders);
        table.addTableDetailBand(entityKeys);
        return JasperCompileManager.compileReport(table.getJasperDesign());
    }

    public JasperReport buildAlarmTable(AlarmTableComponent component) throws JRException {
        JasperReportBuilder table = new JasperReportBuilder(component, getUsablePageWidth());

        List<DataKey> dataKeys = component.getAlarmSource().getDataKeys();
        List<String> entityKeys = dataKeys.stream().map(DataKey::getName).collect(Collectors.toList());
        List<String> columnsHeaders = dataKeys.stream().map(DataKey::getLabel).collect(Collectors.toList());

        table.addColumnHeader(columnsHeaders);
        table.addTableDetailBand(entityKeys);
        return JasperCompileManager.compileReport(table.getJasperDesign());
    }

    private JasperReport buildDashboard(DashboardComponent component) throws JRException {
        JasperReportBuilder imageDesign = new JasperReportBuilder(component, getUsablePageWidth());
        imageDesign.addImage();
        return JasperCompileManager.compileReport(imageDesign.getJasperDesign());
    }

    public JasperReport buildPageBreak(PageBreakComponent component) throws JRException {
        JasperReportBuilder pageBreak = new JasperReportBuilder(component, getUsablePageWidth());
        pageBreak.addPageBreak();
        return JasperCompileManager.compileReport(pageBreak.getJasperDesign());
    }

    public static DataSource getSingleDataSource(ReportComponent component) {
        List<DataSource> dataSources = component.getDataSources();
        if (dataSources == null || dataSources.isEmpty()) {
            throw new IllegalArgumentException("Data source is required for component: " + component.getType());
        }
        return component.getDataSources().get(0);
    }
}

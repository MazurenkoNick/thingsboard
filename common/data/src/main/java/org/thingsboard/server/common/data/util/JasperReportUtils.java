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
package org.thingsboard.server.common.data.util;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignSubreport;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.HeaderFooter;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfiguration;

import java.awt.*;
import java.util.List;


public class JasperReportUtils {

    public static JasperDesign initMainDesign(ReportTemplateConfiguration configuration) {
        JasperDesign design = new JasperDesign();
        design.setName("MainReport");
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setColumnWidth(515);
        design.setLeftMargin(40);
        design.setRightMargin(40);
        design.setTopMargin(50);
        design.setBottomMargin(50);
        design.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        return design;
    }

    public static JasperDesign initComponentDesign() {
        JasperDesign design = new JasperDesign();
        design.setName(StringUtils.randomAlphabetic(8));
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setColumnWidth(200);
        design.setLeftMargin(0);
        design.setRightMargin(0);
        design.setTopMargin(0);
        design.setBottomMargin(0);
        return design;
    }

    public static void addHeading(JasperDesign mainDesign, String htmlText) {
        JRDesignTextField htmlField = new JRDesignTextField();
        htmlField.setX(0);
        htmlField.setY(0);
        htmlField.setWidth(500);
        htmlField.setHeight(30);
        //htmlField.setHorizontalTextAlign(100);
        htmlField.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        htmlField.setMarkup("html");

        JRDesignExpression expression = new JRDesignExpression();
        expression.setText(htmlText);
        htmlField.setExpression(expression);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(30);
        detailBand.addElement(htmlField);

        JRDesignSection detailSection = (JRDesignSection) mainDesign.getDetailSection();
        detailSection.addBand(detailBand);
    }

    public static void addPageHeader(JasperDesign jasperDesign, HeaderFooter header) {
        JRDesignBand pageHeader = new JRDesignBand();
        pageHeader.setHeight(20);
        JRDesignStaticText headerText = new JRDesignStaticText();
        headerText.setX(0);
        headerText.setY(0);
        headerText.setWidth(515);
        headerText.setHeight(20);
        headerText.setText(header.getText());
        pageHeader.addElement(headerText);
        jasperDesign.setPageHeader(pageHeader);
    }

    public static void addPageFooter(JasperDesign jasperDesign, HeaderFooter footer) {
        JRDesignBand pageFooter = new JRDesignBand();
        pageFooter.setHeight(20);
        JRDesignStaticText footerText = new JRDesignStaticText();
        footerText.setX(0);
        footerText.setY(0);
        footerText.setWidth(515);
        footerText.setHeight(20);
        footerText.setText(footer.getText());
        pageFooter.addElement(footerText);
        jasperDesign.setPageFooter(pageFooter);
    }

    public static String addImageBand(JasperDesign mainDesign) {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(500); // Make sure it’s tall enough for the image

        JRDesignParameter imageParam = new JRDesignParameter();
        imageParam.setName("image");
        imageParam.setValueClass(java.io.InputStream.class);
        try {
            mainDesign.addParameter(imageParam);
        } catch (JRException e) {
            throw new RuntimeException(e);
        }

        JRDesignImage image = new JRDesignImage(mainDesign);
        image.setX(0);
        image.setY(0);
        image.setWidth(500);
        image.setHeight(500);
        image.setScaleImage(net.sf.jasperreports.engine.type.ScaleImageEnum.RETAIN_SHAPE);

        // Set expression to read from parameter
        JRDesignExpression imgExpr = new JRDesignExpression();
        imgExpr.setText("$P{image}");
        image.setExpression(imgExpr);

        detailBand.addElement(image);

        // Set the detail band into the design
        JRDesignSection detailSection = (JRDesignSection) mainDesign.getDetailSection();
        detailSection.addBand(detailBand);
        return "image";
    }

    public static void addRichText(JasperDesign mainDesign, String richText) {
        JRDesignTextField htmlField = new JRDesignTextField();
        htmlField.setX(0);
        htmlField.setY(0);
        htmlField.setWidth(500);
        htmlField.setHeight(100);
        htmlField.setMarkup("html");

        JRDesignExpression expression = new JRDesignExpression();
        expression.setText(richText);
        htmlField.setExpression(expression);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(100);
        detailBand.addElement(htmlField);

        JRDesignSection detailSection = (JRDesignSection) mainDesign.getDetailSection();
        detailSection.addBand(detailBand);
    }

    public static JRDesignField createField(String name, Class<?> type) {
        JRDesignField field = new JRDesignField();
        field.setName(name);
        field.setValueClass(type);
        return field;
    }

    public static void addColumnHeader(JasperDesign tableDesign, List<String> titles) {
        JRDesignBand columnHeader = new JRDesignBand();
        columnHeader.setHeight(20);
        int x = 0;
        int columnWidth = Math.min(tableDesign.getColumnWidth(), tableDesign.getPageWidth()/titles.size());
        for (String title : titles) {
            columnHeader.addElement(createHeaderText(title, x, columnWidth));
            x += columnWidth;
        }
        tableDesign.setColumnHeader(columnHeader);
    }

    public static void addTableDetailBand(JasperDesign tableDesign, List<String> entityKeys) throws JRException {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(20);
        int x = 0;
        int columnWidth = Math.min(tableDesign.getColumnWidth(), tableDesign.getPageWidth()/entityKeys.size());
        for (String entityKey : entityKeys) {
            tableDesign.addField(createField(entityKey, String.class));
            detailBand.addElement(createTextField("$F{" + entityKey + "}", x, 0));
            x += columnWidth;
        }
        ((JRDesignSection) tableDesign.getDetailSection()).addBand(detailBand);
    }

    public static JRDesignTextField createTextField(String expression, int x, int y) {
        JRDesignTextField field = new JRDesignTextField();
        field.setX(x);
        field.setY(y);
        field.setWidth(180);
        field.setHeight(20);
        field.setExpression(new JRDesignExpression(expression));
        return field;
    }

    public static JRDesignStaticText createHeaderText(String text, int x, int width) {
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

    public static void addSubReport(JasperDesign jasperDesign, String subReportExpression, String subReportDSExpression) throws JRException {
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

    public static JRDesignParameter createParameter(String name, Class<?> valueClass) {
        JRDesignParameter param = new JRDesignParameter();
        param.setName(name);
        param.setValueClass(valueClass);
        return param;
    }
}

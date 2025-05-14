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

import java.awt.Dimension;
import java.io.ByteArrayInputStream;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

/*import net.sf.jasperreports.components.html.HtmlComponent;
import net.sf.jasperreports.engine.JRComponentElement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRGenericPrintElement;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.base.JRBasePrintImage;
import net.sf.jasperreports.engine.type.HorizontalImageAlignEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.type.VerticalImageAlignEnum;
import net.sf.jasperreports.engine.util.HtmlPrintElement;
import net.sf.jasperreports.engine.util.JRExpressionUtil;
import net.sf.jasperreports.renderers.FlyingSaucerXhtmlToImageRenderer;*/

public class RichTextHtmlPrintElement /*implements HtmlPrintElement*/ {

    public RichTextHtmlPrintElement(){
    }
/*
    @Override
    public JRPrintImage createImageFromElement(JRGenericPrintElement element) throws JRException {
        String htmlContent = (String) element.getParameterValue(HtmlPrintElement.PARAMETER_HTML_CONTENT);
        String scaleType = (String) element.getParameterValue(HtmlPrintElement.PARAMETER_SCALE_TYPE);
        String horizontalAlignment = (String) element.getParameterValue(HtmlPrintElement.PARAMETER_HORIZONTAL_ALIGN);
        String verticalAlignment = (String) element.getParameterValue(HtmlPrintElement.PARAMETER_VERTICAL_ALIGN);
        Boolean hasOverflowed = (Boolean) element.getParameterValue(HtmlPrintElement.BUILTIN_PARAMETER_HAS_OVERFLOWED);
        Boolean clipOnOverflow = (Boolean) element.getParameterValue(HtmlPrintElement.PARAMETER_CLIP_ON_OVERFLOW);

        JRBasePrintImage printImage = new JRBasePrintImage(element.getDefaultStyleProvider());
        printImage.setStyle(element.getStyle());
        printImage.setMode(element.getModeValue());
        printImage.setBackcolor(element.getBackcolor());
        printImage.setForecolor(element.getForecolor());
        printImage.setX(element.getX());
        printImage.setY(element.getY());
        printImage.setWidth(element.getWidth());
        printImage.setScaleImage(ScaleImageEnum.getByName(scaleType));
        printImage.setHorizontalImageAlign(HorizontalImageAlignEnum.getByName(horizontalAlignment));
        printImage.setVerticalImageAlign(VerticalImageAlignEnum.getByName(verticalAlignment));

        FlyingSaucerXhtmlToImageRenderer renderer = new FlyingSaucerXhtmlToImageRenderer(getHtmlDocument(htmlContent), element.getWidth(), element.getHeight());

        if (printImage.getScaleImageValue() == ScaleImageEnum.REAL_HEIGHT || printImage.getScaleImageValue() == ScaleImageEnum.REAL_SIZE) {
            boolean canClip = hasOverflowed != null ? hasOverflowed : false;
            if (canClip) {
                printImage.setHeight(element.getHeight());
                if (clipOnOverflow) {
                    printImage.setScaleImage(ScaleImageEnum.CLIP);
                }
            } else {
                printImage.setHeight(renderer.getComputedSize().height);
            }
        } else {
            printImage.setHeight(element.getHeight());
        }

        printImage.setRenderer(renderer);
        return printImage;
    }

    @Override
    public JRPrintImage createImageFromComponentElement(JRComponentElement componentElement) throws JRException {
        HtmlComponent html = (HtmlComponent) componentElement.getComponent();

        String htmlContent = "";

        if (html.getHtmlContentExpression() != null) {
            htmlContent = JRExpressionUtil.getExpressionText(html.getHtmlContentExpression());
        }

        JRBasePrintImage printImage = new JRBasePrintImage(componentElement.getDefaultStyleProvider());

        printImage.setStyle(componentElement.getStyle());
        printImage.setMode(componentElement.getModeValue());
        printImage.setBackcolor(componentElement.getBackcolor());
        printImage.setForecolor(componentElement.getForecolor());
        printImage.setX(componentElement.getX());
        printImage.setY(componentElement.getY());
        printImage.setWidth(componentElement.getWidth());
        printImage.setHeight(componentElement.getHeight());
        printImage.setScaleImage(html.getScaleType());
        printImage.setHorizontalImageAlign(html.getHorizontalImageAlign());
        printImage.setVerticalImageAlign(html.getVerticalImageAlign());

        FlyingSaucerXhtmlToImageRenderer renderer = new FlyingSaucerXhtmlToImageRenderer(getHtmlDocument(htmlContent), componentElement.getWidth(), componentElement.getHeight());
        printImage.setRenderer(renderer);
        return printImage;
    }

    @Override
    public Dimension getComputedSize(JRGenericPrintElement element) {
        String htmlContent = (String) element.getParameterValue(HtmlPrintElement.PARAMETER_HTML_CONTENT);

        FlyingSaucerXhtmlToImageRenderer renderer = new FlyingSaucerXhtmlToImageRenderer(getHtmlDocument(htmlContent), element.getWidth(), element.getHeight());

        return renderer.getComputedSize();
    }

    private Document getHtmlDocument(String htmlContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<head><style language='text/css'>");
        sb.append("@page{ margin: 0; }");
        sb.append("body{ margin:0; font-family: Roboto;}");
        sb.append("p { font-weight: 400; letter-spacing: 0.12pt; margin: 12pt 0; font-size: 12pt; line-height: 1.4; }");
        sb.append("</style></head>");
        sb.append("<body>");
        sb.append(htmlContent);
        sb.append("</body>");
        sb.append("</html>");

        Tidy tidy = new Tidy();
        tidy.setXHTML(true);
        tidy.setQuiet(true);
        tidy.setShowWarnings(false);

        return tidy.parseDOM(new ByteArrayInputStream(sb.toString().getBytes()), null);
    }*/

}

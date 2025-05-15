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

import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.lowagie.text.pdf.BaseFont.IDENTITY_H;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.xhtmlrenderer.pdf.ITextRenderer.DEFAULT_DOTS_PER_PIXEL;
import static org.xhtmlrenderer.pdf.ITextRenderer.DEFAULT_DOTS_PER_POINT;


public class HtmlRenderUtils {

    public static ITextRenderer createRenderer() throws Exception {
        ITextRenderer renderer = new ITextRenderer(new ITextOutputDevice(DEFAULT_DOTS_PER_POINT), new PdfReportUserAgent());
        renderer.getFontResolver().addFont("fonts/roboto/roboto.ttf",
                "Roboto", IDENTITY_H, true, null);
        renderer.getFontResolver().addFont("fonts/roboto/robotoitalic.ttf",
                "Roboto", IDENTITY_H, true, null);
        renderer.getFontResolver().addFont("fonts/monospace/dejavusansmono.ttf",
                "monospace", IDENTITY_H, true, null);
        renderer.getFontResolver().addFont("fonts/monospace/dejavusansmonobold.ttf",
                "monospace", IDENTITY_H, true, null);
        return renderer;
    }

    public static String convertToXhtml(String html) throws UnsupportedEncodingException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        parseDom(html, outputStream);
        return outputStream.toString(UTF_8);
    }

    public static org.w3c.dom.Document parseDom(String html) throws UnsupportedEncodingException {
        return parseDom(html, null);
    }

    public static org.w3c.dom.Document parseDom(String html, OutputStream out) throws UnsupportedEncodingException {
        Tidy tidy = new Tidy();
        tidy.setInputEncoding(UTF_8);
        tidy.setOutputEncoding(UTF_8);
        tidy.setXHTML(true);
        tidy.setTrimEmptyElements(false);
        tidy.setShowWarnings(false);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(html.getBytes(UTF_8));
        return tidy.parseDOM(inputStream, out);
    }

    public static int measureHtmlHeight(ITextRenderer renderer, String htmlContent, int width) throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("htmlContent", htmlContent);
        variables.put("pageWidth", width + "px");
        variables.put("pageHeight", "1000px");
        String renderedHtmlContent = ThymeleafUtil.render("html/measure-template", variables);
        org.w3c.dom.Document document = parseDom(renderedHtmlContent);
        renderer.setDocument(document);
        renderer.layout();
        return renderer.getRootBox().getHeight() / DEFAULT_DOTS_PER_PIXEL;
    }

}

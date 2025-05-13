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

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.type.ParameterEvaluationTimeEnum;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.RichTextComponent;
import org.thingsboard.server.report.context.ReportLayout;
import org.thingsboard.server.report.context.TbReportCtx;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.simple.Graphics2DRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;

import static org.thingsboard.server.report.util.JasperReportUtils.createJRTextField;
import static org.thingsboard.server.report.util.JasperReportUtils.createParameter;
import static org.thingsboard.server.report.util.JasperReportUtils.toJRExpression;

@Slf4j
@Component
public class RichTextRenderer extends ReportComponentWithLayoutRenderer {

    @Override
    public void render(TbReportCtx ctx, ReportLayout layoutCtx, JRDesignFrame frame, ReportComponent component) throws JRException {
        RichTextComponent richTextComponent = (RichTextComponent) component;

        Graphics2DRenderer renderer = new Graphics2DRenderer();
        XMLResource resource = XMLResource.load(new StringReader(richTextComponent.getValue()));
        renderer.setDocument(resource.getDocument(), null);
        renderer.getSharedContext().getTextRenderer().setSmoothingThreshold(0);

        Dimension dim = new Dimension((int)(this.layoutWidth * (4f/3f)), 1000);
        BufferedImage buff = new BufferedImage((int) dim.getWidth(), (int) dim.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) buff.getGraphics();
        renderer.layout(g, new Dimension((int)(this.layoutWidth * (4f/3f)), 1000));
        g.dispose();

        Rectangle rect = renderer.getMinimumSize();
        buff = new BufferedImage((int) rect.getWidth(), (int) rect.getHeight(), BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) buff.getGraphics();
        renderer.render(g);
        g.dispose();

        JRDesignImage image = new JRDesignImage(layoutCtx.getJasperDesign());
        image.setX(0);
        image.setY(0);
        image.setWidth(this.layoutWidth);
        image.setHeight((int)(rect.getHeight() * (3f/4f)));

        String htmlImageId = "IMAGE";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(buff, "png", baos);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        byte[] bytes = baos.toByteArray();
        /*try {
            java.nio.file.Files.write(Path.of("/data/test/report/" + StringUtils.randomAlphabetic(10) + ".png"), bytes);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }*/

        ctx.getParams().put(htmlImageId, bytes);

        image.setExpression(new JRDesignExpression("new java.io.ByteArrayInputStream($P{"+htmlImageId+"})"));


        /*JRDesignTextField htmlField = createJRTextField(this.layoutWidth);
        htmlField.setMarkup("rtf");

        JRDesignExpression expression = new JRDesignExpression();
        String testRtf = "{\\rtf1\\ansi{\\fonttbl\\f0\\fswiss Helvetica;}\\f0\\pard\n" +
                " This is some {\\b bold} text.\\par\n" +
                " }";
        expression.setText(toJRExpression(testRtf));
        htmlField.setExpression(expression);*/

        frame.addElement(image);
    }

    private String escapeHtmlForJasperExpression(String rawHtml) {
        String escaped = rawHtml
                .replace("\\", "\\\\")
                //.replace("\"", "\\\"")
                .replace("\n", "<br>");
        return escaped;
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.RICH_TEXT;
    }

}

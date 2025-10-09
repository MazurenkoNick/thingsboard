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

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.HeadingComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.HashMap;
import java.util.Map;

@Component
public class HeadingRenderer extends ReportComponentWithLayoutRenderer<HeadingComponent> {

    @Override
    public String renderContent(HeadingComponent component, ComponentData reportDataSource) {
        String processedText = ThymeleafUtil.renderFromTextString(
                component.getValue(), reportDataSource.getVariables());

        Map<String, Object> templateVars = new HashMap<>();
        templateVars.put("color", resolveColor(component));
        templateVars.put("fontSize", resolveFontSize(component));
        templateVars.put("fontWeight", component.getFont().getWeight().getValue());
        templateVars.put("fontStyle", component.getFont().getStyle().getValue());
        templateVars.put("fontFamily", resolveFontFamily(component));
        templateVars.put("textAlignment", resolveTextAlignment(component));
        templateVars.put("verticalAlignment", resolveVerticalAlignment(component));
        templateVars.put("height", resolveHeight(component));
        templateVars.put("value", processedText);

        return ThymeleafUtil.renderFromHtmlTemplate("html/components/heading-template", templateVars);
    }

    private String resolveColor(HeadingComponent component) {
        return component.getColor() != null ? ColorUtils.normalizeCssColor(component.getColor()) : "#000";
    }

    private Float resolveFontSize(HeadingComponent component) {
        Float size = component.getFont().getSize();
        return (size != null && size > 0) ? size : 10;
    }

    private String resolveFontFamily(HeadingComponent component) {
        String family = component.getFont().getFamily();
        return (family != null && !family.isEmpty()) ? family : "Roboto";
    }

    private String resolveTextAlignment(HeadingComponent component) {
        TextAlignment alignment = component.getTextAlignment();
        return (alignment != null ? alignment : TextAlignment.CENTER).getValue();
    }

    private String resolveVerticalAlignment(HeadingComponent component) {
        VerticalAlignment vertical = component.getVerticalAlignment();
        return (vertical != null ? vertical : VerticalAlignment.MIDDLE).getValue();
    }

    private String resolveHeight(HeadingComponent component) {
        Integer height = component.getHeight();
        return (height != null && height > 0) ? height + "pt" : "100%";
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.HEADING;
    }

}

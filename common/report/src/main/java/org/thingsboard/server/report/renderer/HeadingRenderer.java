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
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.HashMap;

@Component
public class HeadingRenderer extends ReportComponentWithLayoutRenderer {

    @Override
    public String renderContent(ReportComponent component, ComponentData reportDataSource) {
        HeadingComponent headingComponent = (HeadingComponent) component;
        String processedText = ThymeleafUtil.renderFromHtmlString(headingComponent.getValue(), reportDataSource.getVariables());

        HashMap<String, Object> componentVariables = new HashMap<>();
        componentVariables.put("color", headingComponent.getColor() != null ? ColorUtils.normalizeCssColor(headingComponent.getColor()) : "#000");
        if (headingComponent.getFont().getSize() != null && headingComponent.getFont().getSize() > 0) {
            componentVariables.put("fontSize", headingComponent.getFont().getSize());
        } else {
            componentVariables.put("fontSize", 10);
        }
        componentVariables.put("fontWeight", headingComponent.getFont().getWeight());
        componentVariables.put("fontStyle", headingComponent.getFont().getStyle());
        if (headingComponent.getFont().getFamily() != null && headingComponent.getFont().getFamily().length() > 0) {
            componentVariables.put("fontFamily", headingComponent.getFont().getFamily());
        } else {
            componentVariables.put("fontFamily", "Roboto");
        }
        TextAlignment textAlignment = headingComponent.getTextAlignment() != null ? headingComponent.getTextAlignment() : TextAlignment.center;
        componentVariables.put("textAlignment", textAlignment.name());
        VerticalAlignment verticalAlignment = headingComponent.getTextAlignment() != null ? headingComponent.getVerticalAlignment() : VerticalAlignment.middle;
        componentVariables.put("verticalAlignment", verticalAlignment.name());
        if (headingComponent.getHeight() != null && headingComponent.getHeight() > 0) {
            componentVariables.put("height", headingComponent.getHeight() + "pt");
        } else {
            componentVariables.put("height", "100%");
        }
        componentVariables.put("value", processedText);
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/heading-template", componentVariables);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.HEADING;
    }

}

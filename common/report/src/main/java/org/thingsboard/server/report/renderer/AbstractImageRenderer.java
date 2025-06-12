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

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.components.AbstractImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.HashMap;

import static org.thingsboard.server.report.util.ImageUtils.EMPTY_IMAGE_URI;

public abstract class AbstractImageRenderer<C extends AbstractImageComponent> extends ReportComponentWithLayoutRenderer<C> {

    @Override
    public String renderContent(C imageComponent, ComponentData reportDataSource) {
        String imageUrl = this.getImageUrl(imageComponent, reportDataSource);
        HashMap<String, Object> componentVariables = new HashMap<>();
        componentVariables.put("layoutWidth", this.layoutWidthPx + "px");
        componentVariables.put("imageUrl", StringUtils.isBlank(imageUrl) ? EMPTY_IMAGE_URI : imageUrl);
        String imageWidth = this.layoutWidthPx + "px";
        if (ImageWidthType.original.equals(imageComponent.getWidthType())) {
            imageWidth = "auto";
        } else if (ImageWidthType.custom.equals(imageComponent.getWidthType())) {
            int customWidth = 100;
            if (imageComponent.getCustomWidth() >= 1) {
                customWidth = imageComponent.getCustomWidth();
            }
            imageWidth = customWidth + "px";
        }
        componentVariables.put("imageWidth", imageWidth);
        String imageAlign = "center";
        if (imageComponent.getAlignment() != null) {
            imageAlign = imageComponent.getAlignment().name();
        }
        componentVariables.put("imageAlign", imageAlign);
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/image", componentVariables);
    }

    protected abstract String getImageUrl(C component, ComponentData reportDataSource);
}

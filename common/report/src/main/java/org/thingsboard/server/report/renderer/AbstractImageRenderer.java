/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
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
import org.thingsboard.server.common.data.report.configuration.image.ImageAlignment;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.Map;

import static org.thingsboard.server.report.util.ImageUtils.EMPTY_IMAGE_URI;

public abstract class AbstractImageRenderer<C extends AbstractImageComponent> extends ReportComponentWithLayoutRenderer<C> {

    @Override
    public String renderContent(C imageComponent, ComponentData reportDataSource) {
        String imageUrl = this.getImageUrl(imageComponent, reportDataSource);
        String layoutWidth = this.layoutWidthPx + "px";

        String imageWidth;
        if (ImageWidthType.ORIGINAL == imageComponent.getWidthType()) {
            imageWidth = "auto";
        } else if (ImageWidthType.CUSTOM == imageComponent.getWidthType()) {
            int customWidth = imageComponent.getCustomWidth() >= 1 ? imageComponent.getCustomWidth() : 100;
            imageWidth = customWidth + "px";
        } else {
            imageWidth = layoutWidth;
        }

        String imageAlign = imageComponent.getAlignment() != null ?
                imageComponent.getAlignment().getValue() :
                ImageAlignment.CENTER.getValue();

        Map<String, Object> componentVariables = Map.of(
                "layoutWidth", layoutWidth,
                "imageUrl", StringUtils.isBlank(imageUrl) ? EMPTY_IMAGE_URI : imageUrl,
                "imageWidth", imageWidth,
                "imageAlign", imageAlign
        );

        return ThymeleafUtil.renderFromHtmlTemplate("html/components/image", componentVariables);
    }

    protected abstract String getImageUrl(C component, ComponentData reportDataSource);
}

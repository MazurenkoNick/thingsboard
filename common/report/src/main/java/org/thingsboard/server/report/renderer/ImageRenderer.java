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

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.components.ImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.image.ImageSourceType;
import org.thingsboard.server.report.context.ComponentData;

import java.util.List;

import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;

@Component
public class ImageRenderer extends AbstractImageRenderer<ImageComponent> {

    @Override
    protected String getImageUrl(ImageComponent component, ComponentData reportDataSource) {
        String imageUrl = "";
        if (ImageSourceType.ENTITY_KEY == component.getSourceType()) {
            if (!reportDataSource.getEntityDatas().isEmpty()) {
                var entityData = reportDataSource.getEntityDatas().get(0);
                var dataSource = getSingleDataSource(component);
                if (dataSource.isPresent()) {
                    List<DataKey> dataKeys = dataSource.get().getDataKeys();
                    if (dataKeys != null && !dataKeys.isEmpty()) {
                        var dataKey = dataKeys.get(0);
                        imageUrl = entityData.get(dataKey.getLabel());
                    }
                }
            }
        } else {
            imageUrl = component.getImageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = "/assets/report/components/image-placeholder.svg";
            }
        }
        return imageUrl;
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.IMAGE;
    }

}

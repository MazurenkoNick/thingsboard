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
package org.thingsboard.server.report.context;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Margins;

import java.util.List;

@Data
public class ComponentLayout {

    private static final int DEFAULT_PAGE_MARGIN_SIZE = 20;
    private static final int DEFAULT_COMPONENT_MARGIN_SIZE = 0;
    private int usablePageWidth;
    private int leftMargin;
    private int rightMargin;
    private int topMargin;
    private int bottomMargin;

    private ComponentLayout reportLayout;

    public ComponentLayout() {
    }

    public ComponentLayout(ReportComponent component, ComponentLayout parentLayout) {
        this.reportLayout = parentLayout;
        this.usablePageWidth = parentLayout.getUsablePageWidth();

        Margins margins = component.getMargins();
        if (margins != null) {
            this.leftMargin = margins.getLeft();
            this.rightMargin = margins.getRight();
            this.topMargin  = margins.getTop();
            this.bottomMargin = margins.getBottom();
        } else {
            this.leftMargin = DEFAULT_COMPONENT_MARGIN_SIZE;
            this.rightMargin = DEFAULT_COMPONENT_MARGIN_SIZE;
            this.topMargin = DEFAULT_COMPONENT_MARGIN_SIZE;
            this.bottomMargin = DEFAULT_COMPONENT_MARGIN_SIZE;
        }
    }

    public static DataSource getSingleDataSource(ReportComponent component) {
        List<DataSource> dataSources = component.getDataSources();
        if (dataSources == null || dataSources.isEmpty()) {
            throw new IllegalArgumentException("Data source is required for component: " + component.getType());
        }
        return component.getDataSources().get(0);
    }
}

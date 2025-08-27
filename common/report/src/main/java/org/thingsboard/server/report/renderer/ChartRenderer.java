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

import org.jfree.chart.ChartTheme;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.thingsboard.server.common.data.report.configuration.components.AbstractChartComponent;
import org.thingsboard.server.common.data.report.configuration.image.ImageWidthType;
import org.thingsboard.server.report.context.ComponentData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public abstract class ChartRenderer<C extends AbstractChartComponent> extends AbstractImageRenderer<C> {

    protected static ChartTheme currentChartTheme = new StandardChartTheme("TbChartTheme");

    private int width;
    private int height;

    @Override
    protected String getImageUrl(C component, ComponentData reportDataSource) {

        if (ImageWidthType.CUSTOM == component.getWidthType()) {
            this.width = component.getCustomWidth() >= 1 ? component.getCustomWidth() : 100;
        } else {
            this.width  = this.layoutWidthPx;
        }
        this.height = component.getHeight() >= 1 ? component.getHeight() : 400;

        JFreeChart chart = createChart(component, reportDataSource);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ChartUtils.writeChartAsPNG(baos, chart, width, height);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] imageData = baos.toByteArray();

        return encodeImage(imageData);
    }

    protected int getWidth() {
        return width;
    }

    protected int getHeight() {
        return height;
    }

    protected abstract JFreeChart createChart(C component, ComponentData reportDataSource);

    private String encodeImage(byte[] imageBytes) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64;
    }
}

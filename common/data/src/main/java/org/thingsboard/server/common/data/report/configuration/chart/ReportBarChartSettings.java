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
package org.thingsboard.server.common.data.report.configuration.chart;

import lombok.Data;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

@Data
public class ReportBarChartSettings extends ReportLatestChartSettings {

    private Double axisMin;
    private Double axisMax;
    private Font axisTickLabelFont;
    private String axisTickLabelColor;
    private BarSeriesSettings barSettings;

    public ReportBarChartSettings() {}

    public ReportBarChartSettings(ReportBarChartSettings input) {
        super(input);
        if (input == null) {
            input = new ReportBarChartSettings();
        }
        this.setTitle(input.getTitle() != null ? input.getTitle() : "Bars");
        this.axisMin = input.getAxisMin();
        this.axisMax = input.getAxisMax();
        this.axisTickLabelFont = input.getAxisTickLabelFont() != null ? input.getAxisTickLabelFont() : Font.builder().family("Roboto")
                .size(12f)
                .weight(FontWeight.NORMAL)
                .style(FontStyle.NORMAL)
                .build();
        this.axisTickLabelColor = input.getAxisTickLabelColor() != null ? input.getAxisTickLabelColor() : "rgba(0, 0, 0, 0.54)";
        BarSeriesSettings inputBarSettings = input.getBarSettings();
        this.barSettings = new BarSeriesSettings(inputBarSettings);
        this.barSettings.setBarWidth(inputBarSettings != null && inputBarSettings.getBarWidth() != null ? inputBarSettings.getBarWidth() : 80.0);
        this.barSettings.setShowLabel(inputBarSettings != null && inputBarSettings.getShowLabel() != null ? inputBarSettings.getShowLabel() : true);
    }

}

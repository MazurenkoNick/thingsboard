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
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class LegendConfig {
    private LegendPosition position;
    private Boolean sortDataKeys;
    private Boolean showMin;
    private Boolean showMax;
    private Boolean showAvg;
    private Boolean showTotal;
    private Boolean showLatest;

    public LegendConfig() {}

    public LegendConfig(LegendConfig input) {
        if (input == null) {
            input = new LegendConfig();
        }
        this.position = input.getPosition() != null ? input.getPosition() : LegendPosition.top;
        this.sortDataKeys = input.getSortDataKeys() != null ? input.getSortDataKeys() : Boolean.FALSE;
        this.showMin = input.getShowMin() != null ? input.getShowMin() : Boolean.FALSE;
        this.showMax = input.getShowMax() != null ? input.getShowMax() : Boolean.FALSE;
        this.showAvg = input.getShowAvg() != null ? input.getShowAvg() : Boolean.FALSE;
        this.showTotal = input.getShowTotal() != null ? input.getShowTotal() : Boolean.FALSE;
        this.showLatest = input.getShowLatest() != null ? input.getShowLatest() : Boolean.FALSE;
    }

    @JsonIgnore
    public boolean isSimpleLegend() {
        return !this.showMin && !this.showMax && !this.showAvg && !this.showTotal && !this.showLatest;
    }
}

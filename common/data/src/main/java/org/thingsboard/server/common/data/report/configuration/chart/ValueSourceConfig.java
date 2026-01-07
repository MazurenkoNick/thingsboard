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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.DataKey;

@Data
public abstract class ValueSourceConfig {

    private ValueSourceType type;
    private Double value;
    private String latestKeyType;
    private String latestKey;
    private String entityKeyType;
    private String entityAlias;
    private String entityKey;

    @JsonIgnore
    public boolean isValidSource() {
        if (type == null) {
            return false;
        }
        switch (type) {
            case constant -> {
                return value != null;
            }
            case latestKey -> {
                return ("attribute".equals(latestKeyType) || "timeseries".equals(latestKeyType)) && StringUtils.isNotBlank(latestKey);
            }
            case entity -> {
                return ("attribute".equals(entityKeyType) || "timeseries".equals(entityKeyType)) && StringUtils.isNotBlank(entityAlias) && StringUtils.isNotBlank(entityKey);
            }
        }
        return false;
    }

    @JsonIgnore
    public DataKey toEntityDataKey() {
        DataKey key = new DataKey();
        key.setName(entityKey);
        key.setType(entityKeyType);
        return key;
    }

}

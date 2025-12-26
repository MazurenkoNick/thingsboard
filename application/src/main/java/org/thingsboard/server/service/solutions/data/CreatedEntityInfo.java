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
package org.thingsboard.server.service.solutions.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CreatedEntityInfo {

    private String name;
    private EntityType type;
    private String owner;

    public String getEntityPageLink(UUID key) {
        return switch (type) {
            case DEVICE -> "/entities/devices/all/" + key.toString();
            case ASSET -> "/entities/assets/all/" + key.toString();
            case DEVICE_PROFILE -> "/profiles/deviceProfiles/" + key.toString();
            case ASSET_PROFILE -> "/profiles/assetProfiles/" + key.toString();
            case USER -> "/users/all/" + key.toString();
            case CUSTOMER -> "/customers/all/" + key.toString();
            case DASHBOARD -> "/dashboards/all/" + key.toString();
            case RULE_CHAIN -> "/ruleChains/" + key.toString();
            case ROLE -> "/security-settings/roles/" + key.toString();
            case EDGE -> "/edgeManagement/instances/all/" + key.toString();
            default -> null;
        };
    }

}

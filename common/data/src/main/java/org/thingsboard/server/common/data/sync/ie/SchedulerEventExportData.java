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
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;

import java.util.UUID;
import java.util.function.Function;

@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulerEventExportData extends EntityExportData<SchedulerEvent> {

    public void prepareConfiguration(JsonNode configuration, String type, Function<EntityId, EntityId> idMapper, String userId) {
        switch (type) {
            case "updateFirmware", "updateSoftware" -> {
                ObjectNode msgBody = configuration.withObject("msgBody");
                String oldId = msgBody.path("id").asText(null);
                if (oldId != null) {
                    OtaPackageId otaPackageId = new OtaPackageId(UUID.fromString(oldId));
                    msgBody.put("id", idMapper.apply(otaPackageId).getId().toString());
                }
            }
            case "generateReport" -> {
                ObjectNode reportConfig = configuration.withObject("msgBody").withObject("reportConfig");
                reportConfig.put("userId", userId);
                String oldId = reportConfig.path("dashboardId").asText(null);
                if (oldId != null) {
                    DashboardId dashboardId = new DashboardId(UUID.fromString(oldId));
                    reportConfig.put("dashboardId", idMapper.apply(dashboardId).getId().toString());
                }
            }
        }
    }

}

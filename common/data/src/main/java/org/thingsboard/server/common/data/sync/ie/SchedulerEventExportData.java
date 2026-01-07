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
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulerEventExportData extends EntityExportData<SchedulerEvent> {

    public static final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    public JsonNode prepareConfiguration(JsonNode configuration, String type, Function<EntityId, EntityId> idMapper, UserId userId) {
        return switch (type) {
            case "updateFirmware", "updateSoftware" -> {
                ObjectNode msgBody = configuration.withObject("msgBody");
                String oldId = msgBody.path("id").asText(null);
                if (oldId != null) {
                    OtaPackageId otaPackageId = new OtaPackageId(UUID.fromString(oldId));
                    msgBody.put("id", idMapper.apply(otaPackageId).getId().toString());
                }
                yield configuration;
            }
            case "generateDashboardReport" -> {
                ObjectNode reportConfig = configuration.withObject("msgBody").withObject("reportConfig");
                reportConfig.put("userId", userId.getId().toString());
                String oldId = reportConfig.path("dashboardId").asText(null);
                if (oldId != null) {
                    DashboardId dashboardId = new DashboardId(UUID.fromString(oldId));
                    reportConfig.put("dashboardId", idMapper.apply(dashboardId).getId().toString());
                }
                yield configuration;
            }
            case "generateReport" -> {
                ReportConfig reportConfig = mapper.treeToValue(configuration, ReportConfig.class);
                reportConfig.setUserId(userId);
                reportConfig.setReportTemplateId((ReportTemplateId) idMapper.apply(reportConfig.getReportTemplateId()));
                reportConfig.setTargets(null);
                reportConfig.setNotificationTemplateId(null);
                yield mapper.valueToTree(reportConfig);
            }
            default -> configuration;
        };
    }

}

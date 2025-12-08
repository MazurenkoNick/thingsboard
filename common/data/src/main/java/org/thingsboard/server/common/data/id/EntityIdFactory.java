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
package org.thingsboard.server.common.data.id;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventType;

import java.util.UUID;

public class EntityIdFactory {

    public static EntityId getByTypeAndUuid(int type, String uuid) {
        return getByTypeAndUuid(EntityType.values()[type], UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndUuid(int type, UUID uuid) {
        return getByTypeAndUuid(EntityType.values()[type], uuid);
    }

    public static EntityId getByTypeAndUuid(String type, String uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndId(String type, String uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndId(EntityType type, String uuid) {
        return getByTypeAndUuid(type, UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndUuid(String type, UUID uuid) {
        return getByTypeAndUuid(EntityType.valueOf(type), uuid);
    }

    public static EntityId getByTypeAndUuid(EntityType type, String uuid) {
        return getByTypeAndUuid(type, UUID.fromString(uuid));
    }

    public static EntityId getByTypeAndUuid(EntityType type, UUID uuid) {
        return switch (type) {
            case TENANT -> TenantId.fromUUID(uuid);
            case CUSTOMER -> new CustomerId(uuid);
            case USER -> new UserId(uuid);
            case DASHBOARD -> new DashboardId(uuid);
            case DEVICE -> new DeviceId(uuid);
            case ASSET -> new AssetId(uuid);
            case CONVERTER -> new ConverterId(uuid);
            case INTEGRATION -> new IntegrationId(uuid);
            case ALARM -> new AlarmId(uuid);
            case ENTITY_GROUP -> new EntityGroupId(uuid);
            case RULE_CHAIN -> new RuleChainId(uuid);
            case RULE_NODE -> new RuleNodeId(uuid);
            case SCHEDULER_EVENT -> new SchedulerEventId(uuid);
            case BLOB_ENTITY -> new BlobEntityId(uuid);
            case REPORT_TEMPLATE -> new ReportTemplateId(uuid);
            case REPORT -> new ReportId(uuid);
            case ENTITY_VIEW -> new EntityViewId(uuid);
            case ROLE -> new RoleId(uuid);
            case GROUP_PERMISSION -> new GroupPermissionId(uuid);
            case WIDGETS_BUNDLE -> new WidgetsBundleId(uuid);
            case WIDGET_TYPE -> new WidgetTypeId(uuid);
            case DEVICE_PROFILE -> new DeviceProfileId(uuid);
            case ASSET_PROFILE -> new AssetProfileId(uuid);
            case TENANT_PROFILE -> new TenantProfileId(uuid);
            case API_USAGE_STATE -> new ApiUsageStateId(uuid);
            case TB_RESOURCE -> new TbResourceId(uuid);
            case OTA_PACKAGE -> new OtaPackageId(uuid);
            case EDGE -> new EdgeId(uuid);
            case RPC -> new RpcId(uuid);
            case QUEUE -> new QueueId(uuid);
            case NOTIFICATION_TARGET -> new NotificationTargetId(uuid);
            case NOTIFICATION_REQUEST -> new NotificationRequestId(uuid);
            case NOTIFICATION_RULE -> new NotificationRuleId(uuid);
            case NOTIFICATION_TEMPLATE -> new NotificationTemplateId(uuid);
            case NOTIFICATION -> new NotificationId(uuid);
            case QUEUE_STATS -> new QueueStatsId(uuid);
            case OAUTH2_CLIENT -> new OAuth2ClientId(uuid);
            case MOBILE_APP -> new MobileAppId(uuid);
            case DOMAIN -> new DomainId(uuid);
            case MOBILE_APP_BUNDLE -> new MobileAppBundleId(uuid);
            case CALCULATED_FIELD -> new CalculatedFieldId(uuid);
            case JOB -> new JobId(uuid);
            case SECRET -> new SecretId(uuid);
            case ADMIN_SETTINGS -> new AdminSettingsId(uuid);
            case AI_MODEL -> new AiModelId(uuid);
            case API_KEY -> new ApiKeyId(uuid);
        };
    }

    public static EntityId getByEdgeEventTypeAndUuid(EdgeEventType edgeEventType, UUID uuid) {
        return switch (edgeEventType) {
            case TENANT -> TenantId.fromUUID(uuid);
            case CUSTOMER -> new CustomerId(uuid);
            case USER -> new UserId(uuid);
            case DASHBOARD -> new DashboardId(uuid);
            case DEVICE -> new DeviceId(uuid);
            case ASSET -> new AssetId(uuid);
            case ALARM -> new AlarmId(uuid);
            case RULE_CHAIN -> new RuleChainId(uuid);
            case ENTITY_VIEW -> new EntityViewId(uuid);
            case WIDGETS_BUNDLE -> new WidgetsBundleId(uuid);
            case WIDGET_TYPE -> new WidgetTypeId(uuid);
            case DEVICE_PROFILE -> new DeviceProfileId(uuid);
            case ASSET_PROFILE -> new AssetProfileId(uuid);
            case TENANT_PROFILE -> new TenantProfileId(uuid);
            case OTA_PACKAGE -> new OtaPackageId(uuid);
            case EDGE -> EdgeId.fromUUID(uuid);
            case SCHEDULER_EVENT -> new SchedulerEventId(uuid);
            case ENTITY_GROUP, DEVICE_GROUP_OTA -> new EntityGroupId(uuid);
            case ROLE -> new RoleId(uuid);
            case GROUP_PERMISSION -> new GroupPermissionId(uuid);
            case INTEGRATION -> new IntegrationId(uuid);
            case CONVERTER -> new ConverterId(uuid);
            case QUEUE -> new QueueId(uuid);
            case TB_RESOURCE -> new TbResourceId(uuid);
            case NOTIFICATION_RULE -> new NotificationRuleId(uuid);
            case NOTIFICATION_TARGET -> new NotificationTargetId(uuid);
            case NOTIFICATION_TEMPLATE -> new NotificationTemplateId(uuid);
            case OAUTH2_CLIENT -> new OAuth2ClientId(uuid);
            case DOMAIN -> new DomainId(uuid);
            case CALCULATED_FIELD -> new CalculatedFieldId(uuid);
            case SECRET -> new SecretId(uuid);
            case REPORT_TEMPLATE -> new ReportTemplateId(uuid);
            case AI_MODEL -> new AiModelId(uuid);
            default -> throw new IllegalArgumentException("EdgeEventType " + edgeEventType + " is not supported!");
        };
    }

}

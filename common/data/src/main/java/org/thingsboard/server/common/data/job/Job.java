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
package org.thingsboard.server.common.data.job;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Job extends BaseData<JobId> implements TenantEntity {

    @NotNull
    private TenantId tenantId;
    @NotNull
    private JobType type;
    @NotBlank
    private String key;
    @NotNull
    private EntityId entityId;
    private String entityName; // read-only
    @NotNull
    private JobStatus status;
    @NotNull
    @Valid
    private JobConfiguration configuration;
    @NotNull
    private JobResult result;

    public static final Set<EntityType> SUPPORTED_ENTITY_TYPES = Set.of(
            EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE,
            EntityType.REPORT_TEMPLATE
    );

    @Builder(toBuilder = true)
    public Job(TenantId tenantId, JobType type, String key, EntityId entityId, JobConfiguration configuration) {
        this.tenantId = tenantId;
        this.type = type;
        this.key = key;
        this.entityId = entityId;
        this.configuration = configuration;
        this.configuration.setTasksKey(UUID.randomUUID().toString());
        presetResult();
    }

    public void presetResult() {
        this.result = switch (type) {
            case CF_REPROCESSING -> new CfReprocessingJobResult();
            case REPORT -> new ReportJobResult();
            case DUMMY -> new DummyJobResult();
        };
    }

    @SuppressWarnings("unchecked")
    public <C extends JobConfiguration> C getConfiguration() {
        return (C) configuration;
    }

    public static ReportJobBuilder newReportJob() {
        return new ReportJobBuilder();
    }

    public static class ReportJobBuilder {

        private TenantId tenantId;
        private EntityId entityId;
        private final ReportJobConfiguration configuration = new ReportJobConfiguration();

        public ReportJobBuilder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public ReportJobBuilder reportTemplateId(ReportTemplateId reportTemplateId) {
            this.entityId = reportTemplateId;
            this.configuration.setReportTemplateId(reportTemplateId);
            return this;
        }

        public ReportJobBuilder userId(UserId userId) {
            this.configuration.setUserId(userId);
            return this;
        }

        public ReportJobBuilder timezone(String timezone) {
            this.configuration.setTimezone(timezone);
            return this;
        }

        public ReportJobBuilder originator(EntityId originator) {
            this.configuration.setOriginator(originator);
            return this;
        }

        public ReportJobBuilder targets(List<UUID> targets) {
            this.configuration.setTargets(targets);
            return this;
        }

        public ReportJobBuilder notificationTemplateId(NotificationTemplateId notificationTemplateId) {
            this.configuration.setNotificationTemplateId(notificationTemplateId);
            return this;
        }

        public ReportJobBuilder notificationRequests(List<NotificationRequest> notificationRequests) {
            this.configuration.setNotificationRequests(notificationRequests);
            return this;
        }

        public Job build() {
            String key = UUID.randomUUID().toString(); // we can submit multiple report jobs at once regardless of the configuration
            return new Job(tenantId, JobType.REPORT, key, entityId, configuration);
        }

    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

}

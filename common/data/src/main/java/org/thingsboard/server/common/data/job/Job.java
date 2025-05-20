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
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.ReportTemplate;

import java.util.UUID;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Job extends BaseData<JobId> implements HasTenantId {

    @NotNull
    private TenantId tenantId;
    @NotNull
    private JobType type;
    @NotBlank
    private String key;
    @NotBlank
    private String description;
    @NotNull
    private JobStatus status;
    @NotNull
    @Valid
    private JobConfiguration configuration;
    @NotNull
    private JobResult result;

    @Builder(toBuilder = true)
    public Job(TenantId tenantId, JobType type, String key, String description, JobConfiguration configuration) {
        this.tenantId = tenantId;
        this.type = type;
        this.key = key;
        this.description = description;
        this.configuration = configuration;
        this.configuration.setTasksKey(UUID.randomUUID().toString());
        presetResult();
    }

    public void presetResult() {
        this.result = switch (type) {
            case REPORT -> new ReportJobResult();
            case DUMMY -> new DummyJobResult();
        };
    }

    @SuppressWarnings("unchecked")
    public <C extends JobConfiguration> C getConfiguration() {
        return (C) configuration;
    }

    public static Job newReportJob(ReportTemplate reportTemplate,
                                   UserId userId,
                                   CustomerId customerId,
                                   String timezone) {
        return Job.builder()
                .tenantId(reportTemplate.getTenantId())
                .type(JobType.REPORT)
                .key(UUID.randomUUID().toString()) // we can submit multiple report jobs at once regardless of the configuration
                .description("Report generation for template '" + reportTemplate.getName() + "'") // todo: hyperlink to template
                .configuration(ReportJobConfiguration.builder()
                        .reportTemplateId(reportTemplate.getId()) // todo: also get from msg body
                        .reportFormat(reportTemplate.getConfiguration().getFormat())
                        .userId(userId) // todo: also get from msg body
                        .customerId(customerId) // todo: also get from msg body
                        .timezone(timezone) // todo: also get from msg body
                        .build())
                .build();
    }

}

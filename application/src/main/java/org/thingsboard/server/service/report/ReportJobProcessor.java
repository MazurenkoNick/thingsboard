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
package org.thingsboard.server.service.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.ReportJobConfiguration;
import org.thingsboard.server.common.data.job.ReportJobResult;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.job.task.TaskResult;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.ReportGeneratedTrigger;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.job.JobProcessor;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ReportJobProcessor implements JobProcessor {

    private final ReportTemplateService reportTemplateService;
    private final SystemSecurityService systemSecurityService;
    private final NotificationRuleProcessor notificationRuleProcessor;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;

    @Override
    public int process(Job job, Consumer<Task<?>> taskConsumer) throws Exception {
        ReportJobConfiguration configuration = job.getConfiguration();
        if (configuration.getReportTemplateId() == null) {
            throw new IllegalArgumentException("Report template must be specified");
        }
        ReportTemplate reportTemplate = reportTemplateService.findReportTemplateById(job.getTenantId(), configuration.getReportTemplateId());
        AccessJwtToken accessToken = systemSecurityService.createUserAccessToken(job.getTenantId(), configuration.getUserId());

        ReportTask task = ReportTask.builder()
                .tenantId(job.getTenantId())
                .jobId(job.getId())
                .key(configuration.getTasksKey())
                .reportTemplateConfig(reportTemplate.getConfiguration())
                .customerId(configuration.getCustomerId())
                .timezone(configuration.getTimezone())
                .accessToken(accessToken.getToken())
                .accessTokenExpirationTs(accessToken.getClaims().getExpiration().getTime())
                .build();
        taskConsumer.accept(task);
        return 1;
    }

    @Override
    public void reprocess(Job job, List<TaskResult> failures, Consumer<Task<?>> taskConsumer) throws Exception {
        process(job, taskConsumer);
    }

    @Override
    public void onJobFinished(Job job) {
        ReportJobResult result = (ReportJobResult) job.getResult();
        ReportJobConfiguration configuration = job.getConfiguration();

        if (configuration.getRuleNodeId() != null) {
            /*
             * fixme:
             *  from scheduler event, do we produce any message to rule engine?
             * */
            if (job.getStatus() == JobStatus.COMPLETED) {
                TbMsg tbMsg = TbMsg.newMsg()
                        .type(TbMsgType.REPORT_GENERATED)
                        .originator(configuration.getUserId())
                        .customerId(configuration.getCustomerId())
                        .data(JacksonUtil.toString(configuration))
                        .metaData(new TbMsgMetaData(Map.of(
                                "reportBlobEntityId", result.getReportBlobId().toString()
                        )))
                        .ruleChainId(configuration.getRuleChainId())
                        .ruleNodeId(configuration.getRuleNodeId())
                        .build();
//                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, job.getTenantId(), )
//                clusterService.pushMsgToRuleEngine(job.getTenantId(), configuration.getReportTemplateId(), tbMsg, TbQueueCallback.EMPTY);
            }
        }
        notificationRuleProcessor.process(ReportGeneratedTrigger.builder()
                .tenantId(job.getTenantId())
                .customerId(configuration.getCustomerId())
                .reportBlobId(result.getReportBlobId())
                .reportName(result.getReportName())
                .build());
    }

    @Override
    public JobType getType() {
        return JobType.REPORT;
    }

}

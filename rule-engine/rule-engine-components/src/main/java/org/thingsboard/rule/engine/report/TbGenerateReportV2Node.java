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
package org.thingsboard.rule.engine.report;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.ReportJobConfiguration;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Base64;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "generate report",
        configClazz = TbGenerateReportV2NodeConfiguration.class,
        nodeDescription = "Requests report generation",
        nodeDetails = "Requests report generation. When report is ready - new message with type REPORT_GENERATED arrives, " +
                      "with report blob entity id in the metadata (reportBlobEntityId)",
//        configDirective = "tbActionNodeGenerateReportConfig", // TODO: add UI
        icon = "description"
)
public class TbGenerateReportV2Node implements TbNode {

    private TbGenerateReportV2NodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGenerateReportV2NodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        TenantId tenantId = ctx.getTenantId();
        ReportConfig reportConfig;
        if (config.isUseConfigFromMessage()) {
            reportConfig = JacksonUtil.fromString(msg.getData(), ReportConfig.class);
        } else {
            reportConfig = config.getConfig();
        }
        if (reportConfig == null) {
            throw new IllegalArgumentException("Report configuration is missing");
        }

        Job job = Job.newReportJob()
                .tenantId(tenantId)
                .reportTemplateId(reportConfig.getReportTemplateId())
                .userId(reportConfig.getUserId())
                .timezone(reportConfig.getTimezone())
                .recipientId(reportConfig.getRecipientId())
                .notificationTemplateId(reportConfig.getNotificationTemplateId())
                .build();
        ReportJobConfiguration configuration = job.getConfiguration();

        TbMsg outputMsg = TbMsg.newMsg(msg, msg.getQueueName(), ctx.getSelf().getRuleChainId(), ctx.getSelfId());
        configuration.setRuleNode(ctx.getSelf());
        configuration.setOutputTbMsgProto(Base64.getEncoder().encodeToString(TbMsg.toProto(outputMsg).toByteArray()));
        configuration.setQueueName(msg.getQueueName());

        DonAsynchron.withCallback(ctx.getJobManager().submitJob(job), result -> {
            // do nothing, tellSuccess will be done when the job is completed
        }, error -> {
            ctx.tellFailure(msg, error);
        });
    }

}

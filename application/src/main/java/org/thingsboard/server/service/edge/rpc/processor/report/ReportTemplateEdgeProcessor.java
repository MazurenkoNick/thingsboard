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
package org.thingsboard.server.service.edge.rpc.processor.report;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.ReportTemplateUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class ReportTemplateEdgeProcessor extends BaseReportTemplateProcessor implements ReportTemplateProcessor {

    @Override
    public ListenableFuture<Void> processReportTemplateMsgFromEdge(TenantId tenantId, Edge edge, ReportTemplateUpdateMsg reportTemplateUpdateMsg) {
        log.trace("[{}] executing processReportTemplateMsgFromEdge [{}] from edge [{}]", tenantId, reportTemplateUpdateMsg, edge.getId());
        ReportTemplateId reportTemplateId = new ReportTemplateId(new UUID(reportTemplateUpdateMsg.getIdMSB(), reportTemplateUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (reportTemplateUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateReportTemplate(tenantId, reportTemplateId, reportTemplateUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(reportTemplateUpdateMsg.getMsgType());
            };
        } catch (DataValidationException e) {
            log.warn("[{}] Failed to process ReportTemplateUpdateMsg from Edge [{}]", tenantId, reportTemplateUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private void saveOrUpdateReportTemplate(TenantId tenantId, ReportTemplateId reportTemplateId, ReportTemplateUpdateMsg reportTemplateUpdateMsg, Edge edge) {
        Boolean created = super.saveOrUpdateReportTemplate(tenantId, reportTemplateId, reportTemplateUpdateMsg);
        if (created) {
            pushReportTemplateCreatedEventToRuleEngine(tenantId, edge, reportTemplateId);
        }
    }

    private void pushReportTemplateCreatedEventToRuleEngine(TenantId tenantId, Edge edge, ReportTemplateId reportTemplateId) {
        try {
            ReportTemplate reportTemplate = edgeCtx.getReportTemplateService().findReportTemplateById(tenantId, reportTemplateId);
            String reportTemplateAsString = JacksonUtil.toString(reportTemplate);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, null);
            pushEntityEventToRuleEngine(tenantId, reportTemplateId, null, TbMsgType.ENTITY_CREATED, reportTemplateAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push report template action to rule engine: {}", tenantId, reportTemplateId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        ReportTemplateId reportTemplateId = new ReportTemplateId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                ReportTemplate reportTemplate = edgeCtx.getReportTemplateService().findReportTemplateById(edgeEvent.getTenantId(), reportTemplateId);
                if (reportTemplate != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    ReportTemplateUpdateMsg reportTemplateUpdateMsg = EdgeMsgConstructorUtils.constructReportTemplateUpdatedMsg(msgType, reportTemplate);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addReportTemplateUpdateMsg(reportTemplateUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                ReportTemplateUpdateMsg reportTemplateUpdateMsg = EdgeMsgConstructorUtils.constructReportTemplateDeleteMsg(reportTemplateId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addReportTemplateUpdateMsg(reportTemplateUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.REPORT_TEMPLATE;
    }

}

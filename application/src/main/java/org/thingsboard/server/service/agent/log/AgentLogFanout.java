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
package org.thingsboard.server.service.agent.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.agent.util.AgentProtoUtils;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.gen.agent.v1.AgentLogChunk;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.agent.AgentGrpcService;
import org.thingsboard.server.service.agent.session.AgentSessionState;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;

@Component
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class AgentLogFanout {

    @Lazy
    private final AgentGrpcService agentGrpcService;
    private final AgentAppUnitService unitService;
    private final LogChunkBuffer logChunkBuffer;
    private final SubscriptionManagerService subscriptionManagerService;
    private final PartitionService partitionService;
    private final TbClusterService clusterService;

    public void fanout(AgentSessionState session, AgentLogChunk chunk) {
        TenantId tenantId = session.getTenantId();
        AgentAppUnit unit = unitService.findByAgentAndProjectAndIdentifier(
                tenantId, session.getAgentId(), chunk.getProjectName(), chunk.getUnitId());
        if (unit == null) {
            log.debug("[{}] No AgentAppUnit for agent {} project {} unit {}",
                    tenantId, session.getAgentId(), chunk.getProjectName(), chunk.getUnitId());
            agentGrpcService.stopLogStream(session.getAgentId(), chunk.getProjectName(), chunk.getUnitId());
            return;
        }
        if (chunk.getLinesCount() == 0 && chunk.getDropped() <= 0) {
            return;
        }
        long seq = logChunkBuffer.append(tenantId, unit.getId(), AgentProtoUtils.fromProto(chunk));
        forwardWatermark(tenantId, unit.getId(), seq);
    }

    private void forwardWatermark(TenantId tenantId, EntityId unitId, long latestSeq) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, unitId);
        if (tpi.isMyPartition()) {
            subscriptionManagerService.onLogStreamUpdate(tenantId, unitId, latestSeq, TbCallback.EMPTY);
        } else {
            clusterService.pushMsgToCore(tpi, unitId.getId(),
                    TbSubscriptionUtils.toLogStreamUpdateProto(tenantId, unitId, latestSeq), null);
        }
    }
}

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
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.gen.agent.v1.AgentLogChunk;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.log.AgentLogFanout;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class AgentLogChunkMessageHandler implements AgentInboundMessageHandler {

    private final AgentLogFanout logFanout;
    private final RateLimitService rateLimitService;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        return msgCtx.msg().hasLogChunk();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        AgentLogChunk chunk = msgCtx.msg().getLogChunk();
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] LogChunk unit={} lines={} dropped={}",
                    tenantId, agentId, chunk.getUnitId(), chunk.getLinesCount(), chunk.getDropped());
        }
        if (!rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS, tenantId)
                || !rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS_PER_AGENT, tenantId, agentId)) {
            log.debug("[{}][{}] Dropping log chunk for unit {}, rate limit reached", tenantId, agentId, chunk.getUnitId());
            return;
        }
        logFanout.fanout(msgCtx.sessionState(), chunk);
    }
}

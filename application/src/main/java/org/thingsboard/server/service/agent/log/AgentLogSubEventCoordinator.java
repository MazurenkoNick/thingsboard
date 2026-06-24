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
import org.springframework.stereotype.Component;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.subscription.SubEventObserver;
import org.thingsboard.server.service.subscription.TbAgentUnitRemoteSubsInfo.TbAgentUnitSubsUpdateInfo;
import org.thingsboard.server.service.subscription.TbEntityRemoteSubsInfo.TbEntitySubsUpdateInfo;
import org.thingsboard.server.service.subscription.TbEntitySubEvent;

@Slf4j
@TbCoreComponent
@Component
@RequiredArgsConstructor
public class AgentLogSubEventCoordinator implements SubEventObserver {

    private final AgentAppUnitService unitService;
    private final TbClusterService tbClusterService;

    @Override
    public EntityType entityType() {
        return EntityType.AGENT_APP_UNIT;
    }

    @Override
    public void onSubEvent(TbEntitySubEvent event, TbEntitySubsUpdateInfo subsUpdateInfo) {
        if (!(subsUpdateInfo instanceof TbAgentUnitSubsUpdateInfo updInfo)) {
            log.warn("[{}] Expected TbAgentUnitSubsUpdateInfo but got {}",
                    event.getEntityId(), subsUpdateInfo.getClass().getSimpleName());
            return;
        }
        if (updInfo.isDuplicate()) {
            return;
        }
        boolean isEmptyLogSubsBeforeEvent = updInfo.isEmptyLogSubsBeforeEvent();
        boolean isEmptyLogSubsAfterEvent = updInfo.isEmptyLogSubsAfterEvent();
        boolean firstSub = isEmptyLogSubsBeforeEvent && !isEmptyLogSubsAfterEvent;
        boolean lastSub = !isEmptyLogSubsBeforeEvent && isEmptyLogSubsAfterEvent;
        if (!firstSub && !lastSub) {
            return;
        }
        AgentAppUnitId unitId = (AgentAppUnitId) event.getEntityId();
        AgentAppUnitInfo info = unitService.findAgentAppUnitInfoById(event.getTenantId(), unitId);
        if (info == null || info.getAgentId() == null) {
            log.warn("[{}] Cannot resolve unit info for {}; dropping log stream {}",
                    event.getTenantId(), unitId, lastSub ? "Stop" : "Start");
            return;
        }
        pushNfToGrpcOwningCore(event, info, lastSub);
    }

    private void pushNfToGrpcOwningCore(TbEntitySubEvent event, AgentAppUnitInfo info, boolean lastSub) {
        tbClusterService.onAgentLogStreamRequest(event.getTenantId(), info.getAgentId(), info, lastSub);
    }
}

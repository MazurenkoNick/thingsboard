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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.service.subscription.TbAgentUnitRemoteSubsInfo.TbAgentUnitSubsUpdateInfo;
import org.thingsboard.server.service.subscription.TbEntitySubEvent;
import org.thingsboard.server.service.subscription.TbSubscriptionsInfo;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentLogSubEventCoordinatorTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentAppUnitId UNIT_ID = new AgentAppUnitId(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final String PROJECT_NAME = "my-project";
    private static final String UNIT_IDENTIFIER = "my-service";

    @Mock
    private AgentAppUnitService unitService;
    @Mock
    private TbClusterService tbClusterService;

    @InjectMocks
    private AgentLogSubEventCoordinator coordinator;

    @Test
    void firstSubProducesStartRequest() {
        AgentAppUnitInfo info = unitInfo(UNIT_IDENTIFIER, PROJECT_NAME, AGENT_ID);
        when(unitService.findAgentAppUnitInfoById(TENANT_ID, UNIT_ID)).thenReturn(info);

        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(false, false, true, false));

        verify(tbClusterService).onAgentLogStreamRequest(TENANT_ID, AGENT_ID, info, false);
    }

    @Test
    void lastSubEmitsStop() {
        AgentAppUnitInfo info = unitInfo(UNIT_IDENTIFIER, PROJECT_NAME, AGENT_ID);
        when(unitService.findAgentAppUnitInfoById(TENANT_ID, UNIT_ID)).thenReturn(info);

        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.DELETED), updInfo(false, true, false, true));

        verify(tbClusterService).onAgentLogStreamRequest(TENANT_ID, AGENT_ID, info, true);
    }

    @Test
    void noTransitionDoesNothing() {
        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(false, false, false, false));

        verifyNoInteractions(tbClusterService);
    }

    @Test
    void stillEmptyAfterEventDoesNothing() {
        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(false, false, true, true));

        verifyNoInteractions(tbClusterService);
    }

    @Test
    void duplicateEventIgnored() {
        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(true, false, true, false));

        verifyNoInteractions(tbClusterService);
    }

    @Test
    void unitNotResolvableSilentlyDropped() {
        when(unitService.findAgentAppUnitInfoById(TENANT_ID, UNIT_ID)).thenReturn(null);

        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(false, false, true, false));

        verify(tbClusterService, never()).onAgentLogStreamRequest(any(), any(), any(), anyBoolean());
    }

    private TbEntitySubEvent subEvent(ComponentLifecycleEvent type) {
        return TbEntitySubEvent.builder()
                .tenantId(TENANT_ID)
                .entityId(UNIT_ID)
                .type(type)
                .info(new TbSubscriptionsInfo(false, false, false,false, null, false, null, 1))
                .seqNumber(1)
                .build();
    }

    private TbAgentUnitSubsUpdateInfo updInfo(boolean duplicate, boolean empty,
                                              boolean emptyLogSubsBeforeEvent, boolean emptyLogSubsAfterEvent) {
        return new TbAgentUnitSubsUpdateInfo(duplicate, empty, emptyLogSubsBeforeEvent, emptyLogSubsAfterEvent);
    }

    private AgentAppUnitInfo unitInfo(String identifier, String projectName, AgentId agentId) {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setIdentifier(identifier);
        return new AgentAppUnitInfo(unit, agentId, projectName);
    }
}

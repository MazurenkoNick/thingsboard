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
package org.thingsboard.server.dao.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.owner.OwnerService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppArgumentSourceResolverTest {

    @Mock
    private OwnerService ownerService;
    @Mock
    private AgentAppRelationService agentAppRelationService;

    @InjectMocks
    private AgentAppArgumentSourceResolver resolver;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final AgentId agentId = new AgentId(UUID.randomUUID());
    private AgentApplication application;

    @BeforeEach
    void setUp() {
        application = new AgentApplication();
        application.setTenantId(tenantId);
        application.setAgentId(agentId);
    }

    @Test
    void agentResolvesToAgentId() {
        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.AGENT)).isEqualTo(agentId);
    }

    @Test
    void tenantResolvesToTenantId() {
        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.TENANT)).isEqualTo(tenantId);
    }

    @Test
    void ownerResolvesViaOwnerService() {
        EntityId ownerId = new CustomerId(UUID.randomUUID());
        when(ownerService.getOwner(tenantId, agentId)).thenReturn(ownerId);

        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.OWNER)).isEqualTo(ownerId);
    }

    @Test
    void relatedEntityUsesSavedRelationWhenPresent() {
        EntityId edgeId = new EdgeId(UUID.randomUUID());
        when(agentAppRelationService.findRelatedEntity(tenantId, application)).thenReturn(edgeId);

        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.RELATED_ENTITY)).isEqualTo(edgeId);
        verify(agentAppRelationService, never()).findRelatedEntityByConfig(tenantId, application);
    }

    @Test
    void relatedEntityFallsBackToConfigWhenNoSavedRelation() {
        EntityId edgeId = new EdgeId(UUID.randomUUID());
        when(agentAppRelationService.findRelatedEntity(tenantId, application)).thenReturn(null);
        when(agentAppRelationService.findRelatedEntityByConfig(tenantId, application)).thenReturn(edgeId);

        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.RELATED_ENTITY)).isEqualTo(edgeId);
    }

    @Test
    void relatedEntityIsNullWhenNeitherResolves() {
        when(agentAppRelationService.findRelatedEntity(tenantId, application)).thenReturn(null);
        when(agentAppRelationService.findRelatedEntityByConfig(tenantId, application)).thenReturn(null);

        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.RELATED_ENTITY)).isNull();
    }

    @Test
    void concreteSourceResolvesToNull() {
        assertThat(resolver.resolveContextSource(tenantId, application, AgentAppArgumentSource.DEVICE)).isNull();
    }
}

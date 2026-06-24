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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.config.AgentAppArgument;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentValueType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppArgumentReferenceValidatorTest {

    @Mock
    private EntityServiceRegistry entityServiceRegistry;
    @Mock
    private EntityDaoService entityDaoService;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @Test
    void passesWhenReferencedEntityExists() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Optional<HasId<?>> found = Optional.of(new Device(deviceId));
        lenient().when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE)).thenReturn(entityDaoService);
        when(entityDaoService.findEntity(eq(tenantId), any())).thenReturn(found);

        assertThatNoException().isThrownBy(() ->
                AgentAppArgumentReferenceValidator.validate(tenantId, configWith(deviceId), entityServiceRegistry));
    }

    @Test
    void rejectsWhenReferencedEntityMissingOrFromOtherTenant() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE)).thenReturn(entityDaoService);
        when(entityDaoService.findEntity(eq(tenantId), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                AgentAppArgumentReferenceValidator.validate(tenantId, configWith(deviceId), entityServiceRegistry))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("non-existent");
    }

    @Test
    void skipsContextDerivedSources() {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(argument(AgentAppArgumentSource.AGENT, null)));

        assertThatNoException().isThrownBy(() ->
                AgentAppArgumentReferenceValidator.validate(tenantId, config, entityServiceRegistry));
    }

    private DockerComposeConfig configWith(DeviceId deviceId) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(argument(AgentAppArgumentSource.DEVICE, deviceId)));
        return config;
    }

    private AgentAppArgument argument(AgentAppArgumentSource source, DeviceId entityId) {
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName("ref");
        argument.setSourceType(source);
        argument.setSourceEntityId(entityId);
        argument.setValueType(AgentAppArgumentValueType.ATTRIBUTE);
        argument.setScope(AttributeScope.SERVER_SCOPE);
        argument.setKey("k");
        return argument;
    }

}

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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppArgument;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentValueType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileDao;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppProfileDataValidatorTest {

    @Mock
    private AgentAppProfileDao profileDao;
    @Mock
    private TenantService tenantService;
    @Mock
    private EntityServiceRegistry entityServiceRegistry;
    @Mock
    private EntityDaoService entityDaoService;

    private AgentAppProfileDataValidator validator;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        validator = new AgentAppProfileDataValidator(profileDao, tenantService, entityServiceRegistry);
    }

    @Test
    void validateUpdate_nonExisting_throws() {
        AgentAppProfileId id = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setId(id);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);
        when(profileDao.findById(any(), eq(id.getId()))).thenReturn(null);

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Can't update non existing agent application profile");
    }

    @Test
    void validateUpdate_appTypeChanged_throws() {
        AgentAppProfileId id = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setId(id);
        AgentAppProfile old = validProfile(AgentApplicationType.GATEWAY);
        old.setId(id);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);
        when(profileDao.findById(any(), eq(id.getId()))).thenReturn(old);

        assertThatThrownBy(() -> validator.validate(profile, AgentAppProfile::getTenantId))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("type cannot be changed");
    }

    @Test
    void validateUpdate_sameAppType_passes() {
        AgentAppProfileId id = new AgentAppProfileId(UUID.randomUUID());
        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setId(id);
        AgentAppProfile old = validProfile(AgentApplicationType.EDGE);
        old.setId(id);
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);
        when(profileDao.findById(any(), eq(id.getId()))).thenReturn(old);

        assertDoesNotThrow(() -> validator.validate(profile, AgentAppProfile::getTenantId));
    }

    @Test
    void validateDataImpl_concreteEntityArgument_invokesReferenceValidator() {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        DockerComposeConfig config = mock(DockerComposeConfig.class);
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName("dev");
        argument.setSourceType(AgentAppArgumentSource.DEVICE);
        argument.setSourceEntityId(deviceId);
        argument.setValueType(AgentAppArgumentValueType.ATTRIBUTE);
        argument.setScope(AttributeScope.SERVER_SCOPE);
        argument.setKey("k");
        when(config.getArguments()).thenReturn(List.of(argument));

        AgentAppProfile profile = validProfile(AgentApplicationType.EDGE);
        profile.setConfig(config);

        when(entityServiceRegistry.getServiceByEntityType(EntityType.DEVICE)).thenReturn(entityDaoService);
        when(entityDaoService.findEntity(eq(TENANT_ID), any())).thenReturn(Optional.of(new Device(deviceId)));
        when(tenantService.tenantExists(TENANT_ID)).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(profile, AgentAppProfile::getTenantId));
        verify(entityServiceRegistry).getServiceByEntityType(EntityType.DEVICE);
        verify(entityDaoService).findEntity(eq(TENANT_ID), eq(deviceId));
    }

    private AgentAppProfile validProfile(AgentApplicationType appType) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setName("Test App Profile");
        profile.setTenantId(TENANT_ID);
        profile.setAppType(appType);
        profile.setTemplateId(new AgentAppTemplateId(UUID.randomUUID()));
        profile.setConfig(mock(DockerComposeConfig.class));
        return profile;
    }
}

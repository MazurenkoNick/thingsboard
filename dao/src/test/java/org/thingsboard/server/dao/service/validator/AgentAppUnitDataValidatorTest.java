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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentAppUnitDao;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = AgentAppUnitDataValidator.class)
class AgentAppUnitDataValidatorTest {

    @MockitoBean
    AgentApplicationService agentApplicationService;
    @MockitoBean
    AgentAppUnitDao agentAppUnitDao;
    @Autowired
    AgentAppUnitDataValidator validator;

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    AgentApplicationId applicationId = new AgentApplicationId(UUID.fromString("7ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    AgentAppUnitId unitId = new AgentAppUnitId(UUID.fromString("6ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        AgentApplication application = new AgentApplication(applicationId);
        application.setName("Test Application");
        willReturn(application).given(agentApplicationService).findById(eq(tenantId), eq(applicationId));
    }

    @Test
    void testValidateDataImpl_nullAgentApplicationId_thenException() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setIdentifier("id");
        unit.setType(AgentAppUnitType.CONTAINER);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, unit));
        assertThat(exception.getMessage()).containsIgnoringCase("assigned to agent application");
    }

    @Test
    void testValidateDataImpl_nonExistentAgentApplication_thenException() {
        AgentApplicationId nonExistentId = new AgentApplicationId(UUID.randomUUID());
        willReturn(null).given(agentApplicationService).findById(eq(tenantId), eq(nonExistentId));

        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(nonExistentId);
        unit.setIdentifier("id");
        unit.setType(AgentAppUnitType.CONTAINER);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, unit));
        assertThat(exception.getMessage()).contains("non-existent agent application");
    }

    @Test
    void testValidateDataImpl_blankIdentifier_thenException() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(applicationId);
        unit.setIdentifier("  ");
        unit.setType(AgentAppUnitType.CONTAINER);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, unit));
        assertThat(exception.getMessage()).contains("identifier");
    }

    @Test
    void testValidateDataImpl_nullType_thenException() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(applicationId);
        unit.setIdentifier("id");
        unit.setType(null);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateDataImpl(tenantId, unit));
        assertThat(exception.getMessage()).contains("type");
    }

    @Test
    void testValidateDataImpl_valid_thenOK() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setAgentApplicationId(applicationId);
        unit.setIdentifier("unit-1");
        unit.setType(AgentAppUnitType.CONTAINER);

        Assertions.assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, unit));
    }

    @Test
    void testValidateUpdate_nonExistentUnit_thenException() {
        AgentAppUnit unit = new AgentAppUnit(unitId);
        unit.setAgentApplicationId(applicationId);
        unit.setIdentifier("id");
        unit.setType(AgentAppUnitType.CONTAINER);
        willReturn(null).given(agentAppUnitDao).findById(eq(tenantId), eq(unitId.getId()));

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, unit));
        assertThat(exception.getMessage()).contains("non existing agent app unit");
    }

    @Test
    void testValidateUpdate_existingUnit_thenReturnsOld() {
        AgentAppUnit unit = new AgentAppUnit(unitId);
        unit.setAgentApplicationId(applicationId);
        unit.setIdentifier("new-id");
        unit.setType(AgentAppUnitType.VOLUME);
        AgentAppUnit oldUnit = new AgentAppUnit(unitId);
        oldUnit.setIdentifier("old-id");
        oldUnit.setType(AgentAppUnitType.CONTAINER);
        willReturn(oldUnit).given(agentAppUnitDao).findById(eq(tenantId), eq(unitId.getId()));

        AgentAppUnit result = validator.validateUpdate(tenantId, unit);
        assertThat(result).isSameAs(oldUnit);
    }
}

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
package org.thingsboard.server.dao.sql.agent;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentAppUnitDao;
import org.thingsboard.server.dao.agent.AgentDao;
import org.thingsboard.server.dao.agent.AgentProfileDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JpaAgentAppUnitDaoTest extends AbstractJpaDaoTest {

    UUID tenantId1;
    UUID agentId1;
    UUID applicationId1;
    Agent agent1;
    AgentProfile agentProfile1;
    AgentApplication application1;

    @Autowired
    private AgentDao agentDao;
    @Autowired
    private AgentProfileDao agentProfileDao;
    @Autowired
    private AgentApplicationDao agentApplicationDao;
    @Autowired
    private AgentAppUnitDao agentAppUnitDao;

    @Before
    public void setUp() {
        tenantId1 = Uuids.timeBased();
        agentId1 = Uuids.timeBased();
        agentProfile1 = saveAgentProfile(tenantId1, "AGENT_APP_UNIT_TEST_PROFILE");
        agent1 = saveAgent(agentId1, tenantId1, Uuids.timeBased(), "AGENT_APP_UNIT_TEST");
        application1 = saveApplication("APP_UNIT_TEST");
        applicationId1 = application1.getId().getId();
    }

    @After
    public void tearDown() {
        if (application1 != null) {
            List<AgentAppUnit> units = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
            for (AgentAppUnit unit : units) {
                agentAppUnitDao.removeById(TenantId.fromUUID(tenantId1), unit.getId().getId());
            }
            agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), applicationId1);
        }
        if (agent1 != null) {
            agentDao.removeById(TenantId.fromUUID(tenantId1), agentId1);
        }
        if (agentProfile1 != null) {
            agentProfileDao.removeById(TenantId.fromUUID(tenantId1), agentProfile1.getId().getId());
        }
    }

    @Test
    public void testSaveFindByIdFindByAgentApplicationId() {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setTenantId(TenantId.fromUUID(tenantId1));
        unit.setAgentApplicationId(new AgentApplicationId(applicationId1));
        unit.setIdentifier("unit-1");
        unit.setType(AgentAppUnitType.CONTAINER);

        AgentAppUnit saved = agentAppUnitDao.save(TenantId.fromUUID(tenantId1), unit);
        assertNotNull(saved.getId());

        AgentAppUnit found = agentAppUnitDao.findById(TenantId.fromUUID(tenantId1), saved.getId().getId());
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
        assertEquals(applicationId1, found.getAgentApplicationId().getId());
        assertEquals("unit-1", found.getIdentifier());
        assertEquals(AgentAppUnitType.CONTAINER, found.getType());

        List<AgentAppUnit> byApp = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertEquals(1, byApp.size());
        assertEquals(saved.getId(), byApp.get(0).getId());

        agentAppUnitDao.removeById(TenantId.fromUUID(tenantId1), saved.getId().getId());
    }

    @Test
    public void testRemoveById() {
        AgentAppUnit unit = saveUnit("unit-2", AgentAppUnitType.VOLUME);
        agentAppUnitDao.removeById(TenantId.fromUUID(tenantId1), unit.getId().getId());
        AgentAppUnit found = agentAppUnitDao.findById(TenantId.fromUUID(tenantId1), unit.getId().getId());
        assertNull(found);
    }

    @Test
    public void testRemoveByAgentApplicationId() {
        saveUnit("u1", AgentAppUnitType.CONTAINER);
        saveUnit("u2", AgentAppUnitType.NETWORK);
        List<AgentAppUnit> before = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertEquals(2, before.size());

        agentAppUnitDao.removeByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        List<AgentAppUnit> after = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertTrue(after.isEmpty());
    }

    @Test
    public void testRemoveAgentApplicationRemovesAgentAppUnits() {
        saveUnit("cascade1", AgentAppUnitType.CONTAINER);
        saveUnit("cascade2", AgentAppUnitType.VOLUME);
        List<AgentAppUnit> before = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertEquals(2, before.size());

        agentApplicationDao.removeById(TenantId.fromUUID(tenantId1), applicationId1);
        application1 = null;

        List<AgentAppUnit> after = agentAppUnitDao.findByAgentApplicationId(TenantId.fromUUID(tenantId1), applicationId1);
        assertTrue(after.isEmpty());
    }

    private AgentProfile saveAgentProfile(UUID tenantId, String name) {
        AgentProfile profile = new AgentProfile();
        profile.setTenantId(TenantId.fromUUID(tenantId));
        profile.setName(name);
        profile.setProvisionType(AgentProvisionType.DISABLED);
        return agentProfileDao.save(TenantId.fromUUID(tenantId), profile);
    }

    private Agent saveAgent(UUID id, UUID tenantId, UUID customerId, String name) {
        Agent agent = new Agent();
        agent.setId(new AgentId(id));
        agent.setTenantId(TenantId.fromUUID(tenantId));
        agent.setCustomerId(new CustomerId(customerId));
        agent.setName(name);
        agent.setAgentProfileId(agentProfile1.getId());
        return agentDao.save(TenantId.fromUUID(tenantId), agent);
    }

    private AgentApplication saveApplication(String name) {
        AgentApplication app = new AgentApplication();
        app.setTenantId(TenantId.fromUUID(tenantId1));
        app.setName(name);
        app.setAgentId(new AgentId(agentId1));
        app.setAppType(AgentApplicationType.GENERIC);
        app.setConfig(new DockerComposeConfig());
        return agentApplicationDao.save(TenantId.fromUUID(tenantId1), app);
    }

    private AgentAppUnit saveUnit(String identifier, AgentAppUnitType type) {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setTenantId(TenantId.fromUUID(tenantId1));
        unit.setAgentApplicationId(new AgentApplicationId(applicationId1));
        unit.setIdentifier(identifier);
        unit.setType(type);
        return agentAppUnitDao.save(TenantId.fromUUID(tenantId1), unit);
    }
}

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.agent;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JpaAgentAppTemplateDaoTest extends AbstractJpaDaoTest {

    private final List<AgentAppTemplateId> savedIds = new ArrayList<>();

    @Autowired
    private AgentAppTemplateDao agentAppTemplateDao;

    @After
    public void tearDown() {
        for (AgentAppTemplateId id : savedIds) {
            agentAppTemplateDao.removeById(TenantId.SYS_TENANT_ID, id.getId());
        }
    }

    @Test
    public void testSaveAndFindById() {
        AgentAppTemplate template = saveTemplate(AgentApplicationType.EDGE, "1.0.0");

        assertNotNull(template.getId());
        assertTrue(template.getCreatedTime() > 0);

        AgentAppTemplate found = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, template.getId().getId());
        assertNotNull(found);
        assertEquals(template.getId(), found.getId());
        assertEquals(AgentApplicationType.EDGE, found.getAppType());
        assertEquals("1.0.0", found.getCurrentVersion());
        assertNull(found.getNextVersion());
    }

    @Test
    public void testFindByIdNonExistent() {
        AgentAppTemplate found = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, UUID.randomUUID());
        assertNull(found);
    }

    @Test
    public void testFindByAppTypeAndConfigTypeAndVersion() {
        AgentAppTemplate saved = saveTemplate(AgentApplicationType.GATEWAY, "2.0.0");

        AgentAppTemplate found = agentAppTemplateDao.findByAppTypeAndConfigTypeAndVersion(
                TenantId.SYS_TENANT_ID, AgentApplicationType.GATEWAY, AgentAppConfigType.DOCKER_COMPOSE, "2.0.0");
        assertNotNull(found);
        assertEquals(saved.getId(), found.getId());
    }

    @Test
    public void testFindByAppTypeAndConfigTypeAndVersionNoMatch() {
        saveTemplate(AgentApplicationType.EDGE, "3.0.0");

        AgentAppTemplate found = agentAppTemplateDao.findByAppTypeAndConfigTypeAndVersion(
                TenantId.SYS_TENANT_ID, AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, "999.0.0");
        assertNull(found);
    }

    @Test
    public void testFindByAppTypeAndConfigTypeAndVersionWrongAppType() {
        saveTemplate(AgentApplicationType.EDGE, "4.0.0");

        AgentAppTemplate found = agentAppTemplateDao.findByAppTypeAndConfigTypeAndVersion(
                TenantId.SYS_TENANT_ID, AgentApplicationType.GATEWAY, AgentAppConfigType.DOCKER_COMPOSE, "4.0.0");
        assertNull(found);
    }

    @Test
    public void testFindByAppTypeAndConfigTypeAndVersionNoConfig() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.EDGE);
        template.setCurrentVersion("4.5.0");
        template.setStartSteps(Collections.emptyList());
        template.setUpgradeSteps(Collections.emptyList());
        AgentAppTemplate saved = agentAppTemplateDao.save(TenantId.SYS_TENANT_ID, template);
        savedIds.add(saved.getId());

        AgentAppTemplate found = agentAppTemplateDao.findByAppTypeAndConfigTypeAndVersion(
                TenantId.SYS_TENANT_ID, AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, "4.5.0");
        assertNull(found);
    }

    @Test
    public void testFindAll() {
        AgentAppTemplate t1 = saveTemplate(AgentApplicationType.EDGE, "5.0.0");
        AgentAppTemplate t2 = saveTemplate(AgentApplicationType.GATEWAY, "5.1.0");

        List<AgentAppTemplate> all = agentAppTemplateDao.findAll(TenantId.SYS_TENANT_ID);
        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(t -> t.getId().equals(t1.getId())));
        assertTrue(all.stream().anyMatch(t -> t.getId().equals(t2.getId())));
    }

    @Test
    public void testRemoveById() {
        AgentAppTemplate saved = saveTemplate(AgentApplicationType.EDGE, "6.0.0");
        UUID id = saved.getId().getId();
        savedIds.remove(saved.getId());

        agentAppTemplateDao.removeById(TenantId.SYS_TENANT_ID, id);

        AgentAppTemplate found = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, id);
        assertNull(found);
    }

    @Test
    public void testUpdate() {
        AgentAppTemplate saved = saveTemplate(AgentApplicationType.EDGE, "7.0.0");

        saved.setCurrentVersion("7.1.0");
        saved.setNextVersion("8.0.0");
        agentAppTemplateDao.save(TenantId.SYS_TENANT_ID, saved);

        AgentAppTemplate found = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, saved.getId().getId());
        assertNotNull(found);
        assertEquals("7.1.0", found.getCurrentVersion());
        assertEquals("8.0.0", found.getNextVersion());
    }

    private AgentAppTemplate saveTemplate(AgentApplicationType appType, String version) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(appType);
        template.setConfig(new DockerComposeConfig());
        template.setCurrentVersion(version);
        template.setNextVersion(null);
        template.setStartSteps(Collections.emptyList());
        template.setUpgradeSteps(Collections.emptyList());
        AgentAppTemplate saved = agentAppTemplateDao.save(TenantId.SYS_TENANT_ID, template);
        savedIds.add(saved.getId());
        return saved;
    }
}

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
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@DaoSqlTest
public class AgentAppTemplateServiceTest extends AbstractServiceTest {

    @Autowired
    AgentAppTemplateService agentAppTemplateService;

    @Test
    public void testSave() {
        AgentAppTemplate template = createTemplate(AgentApplicationType.EDGE, "1.0.0");

        AgentAppTemplate saved = agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);
        Assert.assertNotNull(saved);
        Assert.assertNotNull(saved.getId());
        Assert.assertTrue(saved.getCreatedTime() > 0);
        Assert.assertEquals(AgentApplicationType.EDGE, saved.getAppType());
        Assert.assertEquals("1.0.0", saved.getCurrentVersion());

        agentAppTemplateService.delete(TenantId.SYS_TENANT_ID, saved.getId());
    }

    @Test
    public void testSaveWithNullAppType() {
        AgentAppTemplate template = createTemplate(null, "1.0.0");

        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template));
    }

    @Test
    public void testSaveWithNullCurrentVersion() {
        AgentAppTemplate template = createTemplate(AgentApplicationType.EDGE, null);

        Assertions.assertThrows(DataValidationException.class, () ->
                agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template));
    }

    @Test
    public void testFindById() {
        AgentAppTemplate saved = agentAppTemplateService.save(TenantId.SYS_TENANT_ID,
                createTemplate(AgentApplicationType.EDGE, "2.0.0"));

        AgentAppTemplate found = agentAppTemplateService.findById(TenantId.SYS_TENANT_ID, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());
        Assert.assertEquals(AgentApplicationType.EDGE, found.getAppType());
        Assert.assertEquals("2.0.0", found.getCurrentVersion());

        agentAppTemplateService.delete(TenantId.SYS_TENANT_ID, saved.getId());
    }

    @Test
    public void testFindByIdNonExistent() {
        AgentAppTemplate found = agentAppTemplateService.findById(TenantId.SYS_TENANT_ID,
                new AgentAppTemplateId(UUID.randomUUID()));
        Assert.assertNull(found);
    }

    @Test
    public void testFindByAppTypeAndConfigTypeAndVersion() {
        AgentAppTemplate saved = agentAppTemplateService.save(TenantId.SYS_TENANT_ID,
                createTemplate(AgentApplicationType.GATEWAY, "3.0.0"));

        AgentAppTemplate found = agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(
                AgentApplicationType.GATEWAY, AgentAppConfigType.DOCKER_COMPOSE, "3.0.0");
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());

        agentAppTemplateService.delete(TenantId.SYS_TENANT_ID, saved.getId());
    }

    @Test
    public void testFindByAppTypeAndConfigTypeAndVersionNoMatch() {
        AgentAppTemplate saved = agentAppTemplateService.save(TenantId.SYS_TENANT_ID,
                createTemplate(AgentApplicationType.EDGE, "4.0.0"));

        AgentAppTemplate found = agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(
                AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, "999.0.0");
        Assert.assertNull(found);

        agentAppTemplateService.delete(TenantId.SYS_TENANT_ID, saved.getId());
    }

    @Test
    public void testFindAll() {
        AgentAppTemplate t1 = agentAppTemplateService.save(TenantId.SYS_TENANT_ID,
                createTemplate(AgentApplicationType.EDGE, "5.0.0"));
        AgentAppTemplate t2 = agentAppTemplateService.save(TenantId.SYS_TENANT_ID,
                createTemplate(AgentApplicationType.GATEWAY, "5.1.0"));

        List<AgentAppTemplate> all = agentAppTemplateService.findAll(TenantId.SYS_TENANT_ID);
        Assert.assertTrue(all.size() >= 2);
        Assert.assertTrue(all.stream().anyMatch(t -> t.getId().equals(t1.getId())));
        Assert.assertTrue(all.stream().anyMatch(t -> t.getId().equals(t2.getId())));

        agentAppTemplateService.delete(TenantId.SYS_TENANT_ID, t1.getId());
        agentAppTemplateService.delete(TenantId.SYS_TENANT_ID, t2.getId());
    }

    @Test
    public void testDelete() {
        AgentAppTemplate saved = agentAppTemplateService.save(TenantId.SYS_TENANT_ID,
                createTemplate(AgentApplicationType.EDGE, "6.0.0"));

        agentAppTemplateService.delete(TenantId.SYS_TENANT_ID, saved.getId());

        AgentAppTemplate found = agentAppTemplateService.findById(TenantId.SYS_TENANT_ID, saved.getId());
        Assert.assertNull(found);
    }

    @Test
    public void testDeleteNonExistent() {
        // delete of a non-existent template should not throw
        Assertions.assertDoesNotThrow(() ->
                agentAppTemplateService.delete(TenantId.SYS_TENANT_ID, new AgentAppTemplateId(UUID.randomUUID())));
    }

    @Test
    public void testUpdate() {
        AgentAppTemplate saved = agentAppTemplateService.save(TenantId.SYS_TENANT_ID,
                createTemplate(AgentApplicationType.EDGE, "7.0.0"));

        saved.setCurrentVersion("7.1.0");
        saved.setNextVersion("8.0.0");
        AgentAppTemplate updated = agentAppTemplateService.save(TenantId.SYS_TENANT_ID, saved);

        Assert.assertEquals(saved.getId(), updated.getId());
        Assert.assertEquals("7.1.0", updated.getCurrentVersion());
        Assert.assertEquals("8.0.0", updated.getNextVersion());

        AgentAppTemplate found = agentAppTemplateService.findById(TenantId.SYS_TENANT_ID, saved.getId());
        Assert.assertEquals("7.1.0", found.getCurrentVersion());
        Assert.assertEquals("8.0.0", found.getNextVersion());

        agentAppTemplateService.delete(TenantId.SYS_TENANT_ID, saved.getId());
    }

    private AgentAppTemplate createTemplate(AgentApplicationType appType, String version) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(appType);
        template.setConfig(new DockerComposeConfig());
        template.setCurrentVersion(version);
        template.setNextVersion(null);
        template.setStartSteps(Collections.emptyList());
        template.setUpgradeSteps(Collections.emptyList());
        return template;
    }
}

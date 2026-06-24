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

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
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
        template.setConfigType(AgentAppConfigType.DOCKER_COMPOSE);
        template.setCurrentVersion(version);
        template.setNextVersion(null);
        template.setStartSteps(Collections.emptyList());
        template.setUpgradeSteps(Collections.emptyList());
        AgentAppTemplate saved = agentAppTemplateDao.save(TenantId.SYS_TENANT_ID, template);
        savedIds.add(saved.getId());
        return saved;
    }
}

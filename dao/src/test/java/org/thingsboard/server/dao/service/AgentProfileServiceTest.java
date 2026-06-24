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
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

@DaoSqlTest
public class AgentProfileServiceTest extends AbstractServiceTest {

    @Autowired
    AgentProfileService agentProfileService;
    @Autowired
    AgentAppProfileService agentAppProfileService;
    @Autowired
    AgentAppTemplateService agentAppTemplateService;

    @Test
    public void testSaveFindDelete() {
        AgentProfile profile = agentProfileService.saveProfile(newProfile("Profile 1"));
        Assert.assertNotNull(profile.getId());

        AgentProfile found = agentProfileService.findProfileById(tenantId, profile.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals("Profile 1", found.getName());

        agentProfileService.deleteProfile(tenantId, profile.getId());
        Assert.assertNull(agentProfileService.findProfileById(tenantId, profile.getId()));
    }

    @Test
    public void testSetDefaultAgentProfile_switchesDefault() {
        AgentProfile p1 = agentProfileService.saveProfile(newProfile("Default P1"));
        AgentProfile p2 = agentProfileService.saveProfile(newProfile("Default P2"));

        Assert.assertTrue(agentProfileService.setDefaultAgentProfile(tenantId, p1.getId()));
        Assert.assertEquals(p1.getId(), agentProfileService.findDefaultAgentProfile(tenantId).getId());

        Assert.assertTrue(agentProfileService.setDefaultAgentProfile(tenantId, p2.getId()));
        Assert.assertEquals(p2.getId(), agentProfileService.findDefaultAgentProfile(tenantId).getId());
        Assert.assertFalse("previous default must be cleared",
                agentProfileService.findProfileById(tenantId, p1.getId()).isDefault());
        // default profiles cannot be deleted directly; tenant teardown cleans them up
    }

    @Test
    public void testSaveSecondDefault_throws() {
        AgentProfile p1 = agentProfileService.saveProfile(newProfile("Uniqueness P1"));
        agentProfileService.setDefaultAgentProfile(tenantId, p1.getId());

        AgentProfile p2 = newProfile("Uniqueness P2");
        p2.setDefault(true);
        Assertions.assertThrows(DataValidationException.class, () -> agentProfileService.saveProfile(p2));
        // p1 is now the default and cannot be deleted directly; tenant teardown cleans it up
    }

    @Test
    public void testUpdateNonExisting_throws() {
        AgentProfile profile = newProfile("Ghost Profile");
        profile.setId(new AgentProfileId(UUID.randomUUID()));
        Assertions.assertThrows(DataValidationException.class, () -> agentProfileService.saveProfile(profile));
    }

    @Test
    public void testAssignAndUnassignAppProfile() {
        AgentProfile agentProfile = agentProfileService.saveProfile(newProfile("Relation Profile"));
        AgentAppProfile appProfile = createAppProfile("Related App Profile");

        agentProfileService.assignAppProfileToAgentProfile(tenantId, agentProfile.getId(), appProfile.getId());
        Assert.assertEquals(1, agentAppProfileService
                .findProfileRelationInfosByAgentProfileId(tenantId, agentProfile.getId()).size());

        agentProfileService.unassignAppProfileFromAgentProfile(tenantId, agentProfile.getId(), appProfile.getId());
        Assert.assertTrue(agentAppProfileService
                .findProfileRelationInfosByAgentProfileId(tenantId, agentProfile.getId()).isEmpty());

        agentAppProfileService.deleteProfile(tenantId, appProfile.getId());
        agentProfileService.deleteProfile(tenantId, agentProfile.getId());
    }

    // ==================== helpers ====================

    private AgentProfile newProfile(String name) {
        AgentProfile profile = new AgentProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        return profile;
    }

    private AgentAppProfile createAppProfile(String name) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        AgentAppTemplate savedTemplate = agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);

        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setAppType(AgentApplicationType.GENERIC);
        profile.setTemplateId(savedTemplate.getId());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        profile.setConfig(config);
        return agentAppProfileService.saveProfile(profile);
    }
}

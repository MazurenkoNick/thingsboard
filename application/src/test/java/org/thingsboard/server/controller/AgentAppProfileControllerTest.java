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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AgentAppProfileControllerTest extends AbstractControllerTest {

    @Autowired
    AgentAppTemplateService agentAppTemplateService;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveGetAndDeleteAppProfile() throws Exception {
        AgentAppProfile saved = createAppProfile("Controller App Profile");
        Assert.assertNotNull(saved.getId());

        AgentAppProfile found = doGet("/api/agent/app/profile/" + saved.getId().getId(), AgentAppProfile.class);
        Assert.assertEquals(saved.getId(), found.getId());

        doDelete("/api/agent/app/profile/" + saved.getId().getId()).andExpect(status().isOk());
        doGet("/api/agent/app/profile/" + saved.getId().getId()).andExpect(status().is4xxClientError());
    }

    @Test
    public void testSaveAppProfile_customerForbidden() throws Exception {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setName("Customer Denied App Profile");
        profile.setAppType(AgentApplicationType.GENERIC);

        loginCustomerUser();
        doPost("/api/agent/app/profile", profile).andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testMergeForPreview() throws Exception {
        AgentAppTemplateId templateId = createTemplate();

        AgentAppProfile profile = new AgentAppProfile();
        profile.setName("Merge Preview Profile");
        profile.setAppType(AgentApplicationType.GENERIC);
        profile.setTemplateId(templateId);
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        profile.setConfig(config);

        AgentAppProfile merged = doPost("/api/agent/app/profiles/merge/" + templateId.getId() + "/preview",
                profile, AgentAppProfile.class);
        Assert.assertNotNull(merged);
        Assert.assertEquals(AgentApplicationType.GENERIC, merged.getAppType());
    }

    @Test
    public void testGetAgentAppProfilesByAppType() throws Exception {
        AgentAppProfile saved = createAppProfile("By Type Profile");

        List<AgentAppProfileInfo> infos = doGetTyped(
                "/api/agent/app/profiles/" + AgentApplicationType.GENERIC.name(), new TypeReference<>() {});
        Assert.assertTrue(infos.stream().anyMatch(p -> p.getId().equals(saved.getId())));
    }

    @Test
    public void testGetTenantAgentAppProfiles_paging() throws Exception {
        createAppProfile("Paging App Profile A");
        createAppProfile("Paging App Profile B");

        PageData<AgentAppProfile> page = doGetTypedWithPageLink("/api/tenant/agent/app/profiles?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    public void testGetAppProfile_crossTenantDenied() throws Exception {
        AgentAppProfile saved = createAppProfile("Cross Tenant App Profile");

        loginDifferentTenant();
        doGet("/api/agent/app/profile/" + saved.getId().getId()).andExpect(status().is4xxClientError());

        loginTenantAdmin();
    }

    private AgentAppTemplateId createTemplate() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        return agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template).getId();
    }

    private AgentAppProfile createAppProfile(String name) throws Exception {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setName(name);
        profile.setAppType(AgentApplicationType.GENERIC);
        profile.setTemplateId(createTemplate());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        profile.setConfig(config);
        return doPost("/api/agent/app/profile", profile, AgentAppProfile.class);
    }
}

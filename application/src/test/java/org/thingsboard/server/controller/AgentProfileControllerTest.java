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
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.BulkOperationPreview;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AgentProfileControllerTest extends AbstractControllerTest {

    @Autowired
    AgentAppTemplateService agentAppTemplateService;

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveGetAndDeleteProfile() throws Exception {
        AgentProfile saved = createProfile("Controller Profile");
        Assert.assertNotNull(saved.getId());

        AgentProfile found = doGet("/api/agent/profile/" + saved.getId().getId(), AgentProfile.class);
        Assert.assertEquals(saved.getId(), found.getId());

        doDelete("/api/agent/profile/" + saved.getId().getId()).andExpect(status().isOk());
        doGet("/api/agent/profile/" + saved.getId().getId()).andExpect(status().is4xxClientError());
    }

    @Test
    public void testSaveProfile_customerForbidden() throws Exception {
        AgentProfile profile = new AgentProfile();
        profile.setName("Customer Denied Profile");

        loginCustomerUser();
        doPost("/api/agent/profile", profile).andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testSetDefaultAgentProfile() throws Exception {
        AgentProfile profile = createProfile("Default Profile");

        AgentProfile result = doPost("/api/agent/profile/" + profile.getId().getId() + "/default",
                "", AgentProfile.class);
        Assert.assertTrue(result.isDefault());
    }

    @Test
    public void testAssignAndUnassignAppProfile() throws Exception {
        AgentProfile agentProfile = createProfile("Relation Profile");
        AgentAppProfile appProfile = createAppProfile("Related App Profile");

        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        List<AgentAppProfileRelationInfo> infos = doGetTyped(
                "/api/agent/profile/" + agentProfile.getId().getId() + "/appProfilesInfo", new TypeReference<>() {});
        Assert.assertEquals(1, infos.size());

        doDelete("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        List<AgentAppProfileRelationInfo> afterUnassign = doGetTyped(
                "/api/agent/profile/" + agentProfile.getId().getId() + "/appProfilesInfo", new TypeReference<>() {});
        Assert.assertTrue(afterUnassign.isEmpty());
    }

    @Test
    public void testPreviewBulkOperation_noApps() throws Exception {
        AgentProfile agentProfile = createProfile("Preview Profile");
        AgentAppProfile appProfile = createAppProfile("Preview App Profile");
        doPost("/api/agent/profile/" + agentProfile.getId().getId()
                + "/appProfile/" + appProfile.getId().getId()).andExpect(status().isOk());

        BulkOperationRequest request = new BulkOperationRequest();
        request.setActionType(AgentAppEventActionType.UPDATE);

        BulkOperationPreview preview = doPost("/api/agent/profile/" + agentProfile.getId().getId()
                        + "/appProfile/" + appProfile.getId().getId() + "/bulk/preview",
                request, BulkOperationPreview.class);
        Assert.assertEquals(0, preview.getTotal());
        Assert.assertEquals(0, preview.getEligible());
    }

    @Test
    public void testGetTenantAgentProfiles_paging() throws Exception {
        createProfile("Paging Profile A");
        createProfile("Paging Profile B");

        PageData<AgentProfile> page = doGetTypedWithPageLink("/api/tenant/agent/profiles?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    public void testGetProfile_crossTenantDenied() throws Exception {
        AgentProfile saved = createProfile("Cross Tenant Profile");

        loginDifferentTenant();
        doGet("/api/agent/profile/" + saved.getId().getId()).andExpect(status().is4xxClientError());

        loginTenantAdmin();
    }

    private AgentProfile createProfile(String name) throws Exception {
        AgentProfile profile = new AgentProfile();
        profile.setName(name);
        return doPost("/api/agent/profile", profile, AgentProfile.class);
    }

    private AgentAppProfile createAppProfile(String name) throws Exception {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        AgentAppTemplate savedTemplate = agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);

        AgentAppProfile profile = new AgentAppProfile();
        profile.setName(name);
        profile.setAppType(AgentApplicationType.GENERIC);
        profile.setTemplateId(savedTemplate.getId());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        profile.setConfig(config);
        return doPost("/api/agent/app/profile", profile, AgentAppProfile.class);
    }
}

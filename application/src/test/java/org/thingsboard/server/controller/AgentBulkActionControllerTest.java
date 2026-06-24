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
import org.thingsboard.server.common.data.agent.AgentAppEventInfo;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AgentBulkActionControllerTest extends AbstractControllerTest {

    @Autowired
    AgentAppTemplateService agentAppTemplateService;
    @Autowired
    AgentBulkActionService agentBulkActionService;

    private AgentBulkAction bulkAction;

    @Before
    public void setUpBulkAction() throws Exception {
        loginTenantAdmin();
        AgentProfile agentProfile = doPost("/api/agent/profile", named(new AgentProfile(), "Bulk Ctl Profile"), AgentProfile.class);
        AgentAppProfile appProfile = createAppProfile("Bulk Ctl App Profile");

        AgentBulkAction action = new AgentBulkAction();
        action.setTenantId(tenantId);
        action.setAgentProfileId(agentProfile.getId().getId());
        action.setApplicationProfileId(appProfile.getId().getId());
        action.setActionType(AgentAppEventActionType.UPDATE);
        action.setStatus(AgentBulkActionStatus.STARTED);
        bulkAction = agentBulkActionService.save(tenantId, action);
    }

    @Test
    public void testGetAgentBulkAction() throws Exception {
        AgentBulkAction found = doGet("/api/agent/bulk/" + bulkAction.getId().getId(), AgentBulkAction.class);
        Assert.assertEquals(bulkAction.getId(), found.getId());
        Assert.assertEquals(AgentAppEventActionType.UPDATE, found.getActionType());
    }

    @Test
    public void testGetAgentBulkActionEvents_paging() throws Exception {
        PageData<AgentAppEventInfo> events = doGetTypedWithPageLink(
                "/api/agent/bulk/" + bulkAction.getId().getId() + "/events?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertEquals(0, events.getTotalElements());
    }

    @Test
    public void testGetAgentBulkAction_customerForbidden() throws Exception {
        loginCustomerUser();
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentBulkAction_crossTenantDenied() throws Exception {
        loginDifferentTenant();
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().is4xxClientError());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentBulkAction_allowedWithProfileReads() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(
                Resource.AGENT_PROFILE, List.of(Operation.READ),
                Resource.AGENT_APP_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().isOk());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentBulkAction_deniedWithoutAgentProfileRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT_APP_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    @Test
    public void testGetAgentBulkAction_deniedWithoutAppProfileRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/bulk/" + bulkAction.getId().getId()).andExpect(status().isForbidden());
        loginTenantAdmin();
    }

    private AgentProfile named(AgentProfile profile, String name) {
        profile.setName(name);
        return profile;
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

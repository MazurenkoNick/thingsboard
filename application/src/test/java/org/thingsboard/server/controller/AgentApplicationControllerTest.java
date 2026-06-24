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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Mechanism-A permission model: agent applications and app events carry no
 * Resource of their own and are gated through the parent AGENT (checkAgentAppId / checkAgentAppEventId
 * delegate to checkAgentId).
 */
@DaoSqlTest
public class AgentApplicationControllerTest extends AbstractControllerTest {

    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    AgentAppEventService agentAppEventService;
    @Autowired
    AgentAppTemplateService agentAppTemplateService;

    private AgentApplication application;
    private AgentAppEvent event;

    @Before
    public void setUpAgentApp() throws Exception {
        loginTenantAdmin();

        Agent newAgent = new Agent();
        newAgent.setName("Perm Test Agent");
        newAgent.setRoutingKey(StringUtils.randomAlphanumeric(20));
        newAgent.setSecret(StringUtils.randomAlphanumeric(20));
        Agent agent = doPost("/api/agent", newAgent, Agent.class);

        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        template = agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setName("Perm Test App");
        app.setTemplateId(template.getId());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(genericCompose());
        app.setConfig(config);
        application = agentApplicationService.save(tenantId, app);

        AgentAppEvent appEvent = new AgentAppEvent();
        appEvent.setTenantId(tenantId);
        appEvent.setApplicationId(application.getId());
        appEvent.setAgentId(agent.getId());
        appEvent.setApplicationName(application.getName());
        appEvent.setActionType(AgentAppEventActionType.INSTALL);
        appEvent.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        appEvent.setStatus(AgentAppEventStatus.PENDING);
        appEvent.setUpdatedTime(System.currentTimeMillis());
        event = agentAppEventService.save(tenantId, appEvent);
    }

    @Test
    public void testGetAgentApplicationById_allowedWithAgentRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT, List.of(Operation.READ)));
        doGet("/api/agent/app/" + application.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testGetAgentApplicationById_deniedWithoutAgentRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/app/" + application.getId().getId()).andExpect(status().isForbidden());
    }

    @Test
    public void testGetAgentAppEventById_allowedWithAgentRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT, List.of(Operation.READ)));
        doGet("/api/agent/app/event/" + event.getId().getId()).andExpect(status().isOk());
    }

    @Test
    public void testGetAgentAppEventById_deniedWithoutAgentRead() throws Exception {
        loginAsRestrictedTenantAdmin(tenantId, Map.of(Resource.AGENT_PROFILE, List.of(Operation.READ)));
        doGet("/api/agent/app/event/" + event.getId().getId()).andExpect(status().isForbidden());
    }

    private ObjectNode genericCompose() {
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "nginx:alpine");
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("generic", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

}

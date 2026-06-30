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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventFilter;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.agent.step.state.ComposeStepState;
import org.thingsboard.server.common.data.agent.step.state.StepField;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.agent.AgentService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@DaoSqlTest
public class AgentAppEventServiceTest extends AbstractServiceTest {

    @Autowired
    AgentService agentService;
    @Autowired
    AgentApplicationService agentApplicationService;
    @Autowired
    AgentAppTemplateService agentAppTemplateService;
    @Autowired
    AgentAppEventService agentAppEventService;
    @Autowired
    AgentBulkActionService agentBulkActionService;
    @Autowired
    AgentProfileService agentProfileService;
    @Autowired
    AgentAppProfileService agentAppProfileService;

    @Test
    public void testSaveAndFind_withStepStatesJsonRoundTrip() {
        Agent agent = createAgent("Agent events round trip");
        AgentApplication app = createApp(agent, "round-trip-app");

        UUID stepId = UUID.randomUUID();
        ComposeStepState composeStepState = new ComposeStepState();
        composeStepState.setPullImages(new StepField<>(true, true));
        Map<UUID, AgentAppStepState> stepStates = Map.of(stepId, composeStepState);

        AgentAppEvent event = newEvent(agent, app, AgentAppEventActionType.INSTALL,
                AgentAppEventDeliveryState.PENDING, AgentAppEventStatus.QUEUED);
        event.setStepStates(stepStates);
        AgentAppEvent saved = agentAppEventService.save(tenantId, event, false);
        Assert.assertNotNull(saved.getId());

        AgentAppEvent found = agentAppEventService.findById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertNotNull(found.getStepStates());
        Assert.assertTrue(found.getStepStates().containsKey(stepId));
        Assert.assertTrue("step state should round-trip to its concrete type",
                found.getStepStates().get(stepId) instanceof ComposeStepState);

        cleanup(agent, app);
    }

    @Test
    public void testFindByFilter_filtersByApplicationAndStatus() {
        Agent agent = createAgent("Agent filter");
        AgentApplication app1 = createApp(agent, "filter-app-1");
        AgentApplication app2 = createApp(agent, "filter-app-2");

        saveEvent(agent, app1, AgentAppEventActionType.INSTALL, AgentAppEventDeliveryState.DELIVERED, AgentAppEventStatus.FINISHED);
        saveEvent(agent, app1, AgentAppEventActionType.UPDATE, AgentAppEventDeliveryState.PENDING, AgentAppEventStatus.QUEUED);
        saveEvent(agent, app2, AgentAppEventActionType.INSTALL, AgentAppEventDeliveryState.PENDING, AgentAppEventStatus.QUEUED);

        // by application
        PageData<AgentAppEvent> byApp1 = agentAppEventService.findByFilter(
                AgentAppEventFilter.builder().tenantId(tenantId).applicationId(app1.getId()).build(), new PageLink(100));
        Assert.assertEquals(2, byApp1.getTotalElements());
        Assert.assertTrue(byApp1.getData().stream().allMatch(e -> e.getApplicationId().equals(app1.getId())));

        // by application + status (applicationId is mandatory for findByFilter)
        PageData<AgentAppEvent> app1Finished = agentAppEventService.findByFilter(
                AgentAppEventFilter.builder().tenantId(tenantId).applicationId(app1.getId())
                        .status(AgentAppEventStatus.FINISHED).build(), new PageLink(100));
        Assert.assertEquals(1, app1Finished.getTotalElements());
        Assert.assertEquals(AgentAppEventStatus.FINISHED, app1Finished.getData().get(0).getStatus());

        // by application + actionType
        PageData<AgentAppEvent> app1Update = agentAppEventService.findByFilter(
                AgentAppEventFilter.builder().tenantId(tenantId).applicationId(app1.getId())
                        .actionType(AgentAppEventActionType.UPDATE).build(), new PageLink(100));
        Assert.assertEquals(1, app1Update.getTotalElements());

        cleanup(agent, app1, app2);
    }

    @Test
    public void testFindByBulkActionId() {
        Agent agent = createAgent("Agent bulk events");
        AgentApplication app = createApp(agent, "bulk-app");

        AgentBulkActionId bulkActionId = createBulkAction().getId();

        AgentAppEvent inBulk1 = newEvent(agent, app, AgentAppEventActionType.UPDATE, AgentAppEventDeliveryState.PENDING, AgentAppEventStatus.QUEUED);
        inBulk1.setBulkActionId(bulkActionId.getId());
        agentAppEventService.save(tenantId, inBulk1, false);

        AgentAppEvent inBulk2 = newEvent(agent, app, AgentAppEventActionType.UPDATE, AgentAppEventDeliveryState.DELIVERED, AgentAppEventStatus.FINISHED);
        inBulk2.setBulkActionId(bulkActionId.getId());
        agentAppEventService.save(tenantId, inBulk2, false);

        // an event not part of the bulk action
        saveEvent(agent, app, AgentAppEventActionType.RESTART, AgentAppEventDeliveryState.PENDING, AgentAppEventStatus.QUEUED);

        PageData<AgentAppEvent> all = agentAppEventService.findByBulkActionId(bulkActionId, null, null, new PageLink(100));
        Assert.assertEquals(2, all.getTotalElements());
        Assert.assertTrue(all.getData().stream().allMatch(e -> bulkActionId.getId().equals(e.getBulkActionId())));

        // filter the bulk results by status
        PageData<AgentAppEvent> finishedOnly = agentAppEventService.findByBulkActionId(
                bulkActionId, null, AgentAppEventStatus.FINISHED, new PageLink(100));
        Assert.assertEquals(1, finishedOnly.getTotalElements());

        cleanup(agent, app);
    }

    @Test
    public void testHasActiveOrPendingEventForApplication() {
        Agent agent = createAgent("Agent pending check");
        AgentApplication withEvent = createApp(agent, "with-event");
        AgentApplication withoutEvent = createApp(agent, "without-event");

        saveEvent(agent, withEvent, AgentAppEventActionType.INSTALL, AgentAppEventDeliveryState.PENDING, AgentAppEventStatus.QUEUED);

        Assert.assertTrue(agentAppEventService.hasActiveOrPendingEventForApplication(withEvent.getId()));
        Assert.assertFalse(agentAppEventService.hasActiveOrPendingEventForApplication(withoutEvent.getId()));

        cleanup(agent, withEvent, withoutEvent);
    }

    @Test
    public void testDeleteAllPendingByApplicationId() {
        Agent agent = createAgent("Agent delete pending");
        AgentApplication app = createApp(agent, "delete-pending-app");

        saveEvent(agent, app, AgentAppEventActionType.INSTALL, AgentAppEventDeliveryState.PENDING, AgentAppEventStatus.QUEUED);
        Assert.assertTrue(agentAppEventService.hasActiveOrPendingEventForApplication(app.getId()));

        agentAppEventService.deleteAllPendingByApplicationId(app.getId());

        Assert.assertFalse(agentAppEventService.hasActiveOrPendingEventForApplication(app.getId()));

        cleanup(agent, app);
    }

    // ==================== helpers ====================

    private void saveEvent(Agent agent, AgentApplication app, AgentAppEventActionType actionType,
                           AgentAppEventDeliveryState deliveryState, AgentAppEventStatus status) {
        agentAppEventService.save(tenantId, newEvent(agent, app, actionType, deliveryState, status), false);
    }

    private AgentAppEvent newEvent(Agent agent, AgentApplication app, AgentAppEventActionType actionType,
                                   AgentAppEventDeliveryState deliveryState, AgentAppEventStatus status) {
        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(app.getId());
        event.setAgentId(agent.getId());
        event.setApplicationName(app.getName());
        event.setActionType(actionType);
        event.setDeliveryState(deliveryState);
        event.setStatus(status);
        event.setUpdatedTime(System.currentTimeMillis());
        return event;
    }

    private AgentBulkAction createBulkAction() {
        AgentProfile agentProfile = new AgentProfile();
        agentProfile.setTenantId(tenantId);
        agentProfile.setName("Bulk Agent Profile " + UUID.randomUUID());
        agentProfile = agentProfileService.saveProfile(agentProfile);

        AgentAppTemplate template = createTemplate();
        AgentAppProfile appProfile = new AgentAppProfile();
        appProfile.setTenantId(tenantId);
        appProfile.setName("Bulk App Profile " + UUID.randomUUID());
        appProfile.setAppType(AgentApplicationType.GENERIC);
        appProfile.setTemplateId(template.getId());
        DockerComposeConfig cfg = new DockerComposeConfig();
        cfg.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        appProfile.setConfig(cfg);
        appProfile = agentAppProfileService.saveProfile(appProfile);

        AgentBulkAction action = new AgentBulkAction();
        action.setTenantId(tenantId);
        action.setAgentProfileId(agentProfile.getId().getId());
        action.setApplicationProfileId(appProfile.getId().getId());
        action.setActionType(AgentAppEventActionType.UPDATE);
        action.setStatus(AgentBulkActionStatus.STARTED);
        return agentBulkActionService.save(tenantId, action);
    }

    private Agent createAgent(String name) {
        Agent agent = new Agent();
        agent.setTenantId(tenantId);
        agent.setName(name);
        agent.setRoutingKey(UUID.randomUUID().toString());
        agent.setSecret(StringUtils.randomAlphanumeric(20));
        return agentService.saveAgent(agent);
    }

    private AgentAppTemplate createTemplate() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        return agentAppTemplateService.save(TenantId.SYS_TENANT_ID, template);
    }

    private AgentApplication createApp(Agent agent, String name) {
        AgentAppTemplate template = createTemplate();
        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setAppType(AgentApplicationType.GENERIC);
        app.setName(name);
        app.setTemplateId(template.getId());
        app.setOrigin(AgentApplicationOrigin.INSTALLED);
        app.setProjectName(AgentApplication.generateProjectName());
        DockerComposeConfig config = new DockerComposeConfig();
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "nginx:alpine");
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("generic", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        config.setCompose(compose);
        app.setConfig(config);
        return agentApplicationService.save(tenantId, app);
    }

    private void cleanup(Agent agent, AgentApplication... apps) {
        for (AgentApplication app : apps) {
            agentApplicationService.delete(tenantId, app.getId());
        }
        agentService.deleteAgent(tenantId, agent.getId());
    }
}

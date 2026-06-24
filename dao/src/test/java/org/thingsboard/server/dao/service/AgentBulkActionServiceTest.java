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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.dao.agent.AgentProfileService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@DaoSqlTest
public class AgentBulkActionServiceTest extends AbstractServiceTest {

    @Autowired
    AgentBulkActionService agentBulkActionService;
    @Autowired
    AgentProfileService agentProfileService;
    @Autowired
    AgentAppProfileService agentAppProfileService;
    @Autowired
    AgentAppTemplateService agentAppTemplateService;

    private AgentProfileId agentProfileId;
    private AgentAppProfileId applicationProfileId;

    @Before
    public void setUpProfiles() {
        agentProfileId = createAgentProfile("Bulk Agent Profile").getId();
        applicationProfileId = createAppProfile("Bulk App Profile").getId();
    }

    @Test
    public void testSaveFindAndUpdate() {
        AgentBulkAction action = agentBulkActionService.save(tenantId, newAction(AgentBulkActionStatus.QUEUED));
        Assert.assertNotNull(action.getId());

        AgentBulkAction found = agentBulkActionService.findById(tenantId, action.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(AgentBulkActionStatus.QUEUED, found.getStatus());

        // status / counters round-trip
        found.setStatus(AgentBulkActionStatus.STARTED);
        found.setTotal(10);
        found.setSubmitted(7);
        found.setSkipCounts(Map.of(SkipReason.VERSION_MISMATCH, 2, SkipReason.ACTIVE_EVENT, 1));
        agentBulkActionService.save(tenantId, found);

        AgentBulkAction reloaded = agentBulkActionService.findById(tenantId, found.getId());
        Assert.assertEquals(AgentBulkActionStatus.STARTED, reloaded.getStatus());
        Assert.assertEquals(10, reloaded.getTotal());
        Assert.assertEquals(7, reloaded.getSubmitted());
        Assert.assertEquals(Integer.valueOf(2), reloaded.getSkipCounts().get(SkipReason.VERSION_MISMATCH));
        Assert.assertEquals(Integer.valueOf(1), reloaded.getSkipCounts().get(SkipReason.ACTIVE_EVENT));
    }

    @Test
    public void testFindByAgentProfileIdAndApplicationProfileId() {
        AgentAppProfileId otherAppProfileId = createAppProfile("Other Bulk App Profile").getId();
        agentBulkActionService.save(tenantId, newAction(AgentBulkActionStatus.QUEUED));
        AgentBulkAction other = newAction(AgentBulkActionStatus.QUEUED);
        other.setApplicationProfileId(otherAppProfileId.getId());
        agentBulkActionService.save(tenantId, other);

        PageData<AgentBulkAction> byProfile = agentBulkActionService.findByAgentProfileId(tenantId, agentProfileId, new PageLink(100));
        Assert.assertEquals(2, byProfile.getTotalElements());

        PageData<AgentBulkAction> byProfileAndApp = agentBulkActionService.findByAgentProfileIdAndApplicationProfileId(
                tenantId, agentProfileId, applicationProfileId, new PageLink(100));
        Assert.assertEquals(1, byProfileAndApp.getTotalElements());
        Assert.assertEquals(applicationProfileId.getId(), byProfileAndApp.getData().get(0).getApplicationProfileId());
    }

    @Test
    public void testFindStuckBulkActions_onlyQueuedOrInProgressBelowThreshold() {
        AgentBulkAction queued = agentBulkActionService.save(tenantId, newAction(AgentBulkActionStatus.QUEUED));

        AgentBulkAction inProgress = newAction(AgentBulkActionStatus.IN_PROGRESS);
        inProgress.setProcessingStartedTime(System.currentTimeMillis());
        inProgress = agentBulkActionService.save(tenantId, inProgress);

        AgentBulkAction started = agentBulkActionService.save(tenantId, newAction(AgentBulkActionStatus.STARTED));

        long futureThreshold = System.currentTimeMillis() + 3_600_000L;
        Set<AgentBulkActionId> stuck = agentBulkActionService.findStuckBulkActions(futureThreshold, new PageLink(100))
                .getData().stream().map(AgentBulkAction::getId).collect(Collectors.toSet());

        Assert.assertTrue("QUEUED below threshold is stuck", stuck.contains(queued.getId()));
        Assert.assertTrue("IN_PROGRESS below threshold is stuck", stuck.contains(inProgress.getId()));
        Assert.assertFalse("STARTED is terminal, never stuck", stuck.contains(started.getId()));
    }

    private AgentBulkAction newAction(AgentBulkActionStatus status) {
        AgentBulkAction action = new AgentBulkAction();
        action.setTenantId(tenantId);
        action.setAgentProfileId(agentProfileId.getId());
        action.setApplicationProfileId(applicationProfileId.getId());
        action.setActionType(AgentAppEventActionType.UPDATE);
        action.setStatus(status);
        return action;
    }

    private AgentProfile createAgentProfile(String name) {
        AgentProfile profile = new AgentProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        return agentProfileService.saveProfile(profile);
    }

    private AgentAppProfile createAppProfile(String name) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        AgentAppTemplate savedTemplate = agentAppTemplateService.save(
                org.thingsboard.server.common.data.id.TenantId.SYS_TENANT_ID, template);

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


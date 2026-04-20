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
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentGroupService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.agent.config.AgentAppConfigMergeOrchestrator;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

@Service
@Slf4j
@TbCoreComponent
@RequiredArgsConstructor
public class AgentAutoInstallService {

    private final AgentService agentService;
    private final AgentAppProfileService profileService;
    private final AgentApplicationService applicationService;
    private final AgentAppEventService appEventService;
    private final AgentAppConfigMergeOrchestrator configMergeOrchestrator;
    private final EdgeService edgeService;
    private final DeviceService deviceService;

    public void autoInstall(TenantId tenantId, AgentId agentId) {
        Agent agent = agentService.findAgentById(tenantId, agentId);
        if (agent == null || agent.getAgentGroupId() == null) {
            log.trace("[{}][{}] Skipping auto-install: agent not found or has no group", tenantId, agentId);
            return;
        }
        List<AgentAppProfile> pendingProfiles = profileService.findUninstalledProfilesForAgent(
                tenantId, agent.getAgentGroupId(), agent.getId());

        if (pendingProfiles.isEmpty()) {
            log.trace("[{}][{}] No profiles pending auto-install", tenantId, agentId);
            return;
        }

        log.info("[{}][{}] Auto-installing {} profile(s)", tenantId, agentId, pendingProfiles.size());
        for (AgentAppProfile profile : pendingProfiles) {
            try {
                provisionForProfile(tenantId, agent, profile);
            } catch (Exception e) {
                log.error("[{}][{}] Failed to auto-install profile [{}]", tenantId, agentId, profile.getId(), e);
            }
        }
    }

    private void provisionForProfile(TenantId tenantId, Agent agent, AgentAppProfile profile) {
        EntityId relatedEntityId = createRelatedEntityIfNeeded(tenantId, profile);

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setName(profile.getName());
        app.setAppType(profile.getAppType());
        app.setTemplateId(profile.getTemplateId());
        app.setApplicationProfileId(profile.getId());
        app.setOrigin(AgentApplicationOrigin.AUTO_PROVISIONED);
        setConfigCredentials(app, profile, relatedEntityId);
        AgentApplication savedApp = applicationService.save(tenantId, app);
        log.info("[{}][{}] Auto-provisioned application [{}] for profile [{}]",
                tenantId, agent.getId(), savedApp.getId(), profile.getId());

        AgentAppEvent event = new AgentAppEvent();
        event.setTenantId(tenantId);
        event.setApplicationId(savedApp.getId());
        event.setAgentId(savedApp.getAgentId());
        event.setApplicationName(savedApp.getName());
        event.setActionType(AgentAppEventActionType.INSTALL);
        event.setDeliveryState(AgentAppEventDeliveryState.PENDING);
        event.setUpdatedTime(System.currentTimeMillis());
        appEventService.save(tenantId, event);
    }

    private void setConfigCredentials(AgentApplication app, AgentAppProfile profile, EntityId relatedEntityId) {
        if (profile.getConfig() == null) {
            return;
        }
        AgentAppConfig configCopy = profile.getConfig().copy();
        app.setConfig(configCopy);
        if (relatedEntityId == null) {
            return;
        }
        configMergeOrchestrator.merge(app, AppConfigMergeCtx.builder()
                .relatedEntityId(relatedEntityId)
                .build());
    }

    private EntityId createRelatedEntityIfNeeded(TenantId tenantId, AgentAppProfile profile) {
        if (profile.getAppType() == null) {
            return null;
        }
        var relatedType = profile.getAppType().getRelatedEntityType();
        if (relatedType == null) {
            return null;
        }
        String suffix = AgentApplication.generateProjectName();
        return switch (relatedType) {
            case EDGE -> createEdge(tenantId, profile, suffix);
            case DEVICE -> createGatewayDevice(tenantId, profile, suffix);
            default -> null;
        };
    }

    private EdgeId createEdge(TenantId tenantId, AgentAppProfile profile, String suffix) {
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName(profile.getName() + "-" + suffix);
        edge.setType("default");
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        Edge saved = edgeService.saveEdge(edge);
        log.info("[{}] Auto-created Edge [{}] for profile [{}]", tenantId, saved.getId(), profile.getId());
        return saved.getId();
    }

    /**
     * Reverse-engineers credentials type from the profile's compose env vars so that the
     * device credentials match what MergeCredentialsToConfigRule expects to inject into compose.
     */
    private DeviceId createGatewayDevice(TenantId tenantId, AgentAppProfile profile, String suffix) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(profile.getName() + "-" + suffix);
        // deviceProfileId auto-resolved to default by DeviceServiceImpl when null

        String securityType = extractGatewaySecurityType(profile);
        if ("usernamePassword".equals(securityType)) {
            DeviceCredentials creds = new DeviceCredentials();
            creds.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
            BasicMqttCredentials basic = new BasicMqttCredentials();
            basic.setClientId(StringUtils.randomAlphanumeric(20));
            basic.setUserName(StringUtils.randomAlphanumeric(20));
            basic.setPassword(StringUtils.randomAlphanumeric(20));
            creds.setCredentialsValue(JacksonUtil.toString(basic));
            // credentialsId for MQTT_BASIC is computed by DeviceCredentialsService
            Device saved = deviceService.saveDeviceWithCredentials(device, creds);
            log.info("[{}] Auto-created gateway Device [{}] (MQTT_BASIC) for profile [{}]", tenantId, saved.getId(), profile.getId());
            return saved.getId();
        }

        // Default: ACCESS_TOKEN — null lets DeviceServiceImpl generate a 20-char random token
        Device saved = deviceService.saveDeviceWithAccessToken(device, null);
        log.info("[{}] Auto-created gateway Device [{}] (ACCESS_TOKEN) for profile [{}]", tenantId, saved.getId(), profile.getId());
        return saved.getId();
    }

    private String extractGatewaySecurityType(AgentAppProfile profile) {
        if (!(profile.getConfig() instanceof DockerComposeConfig cfg) || cfg.getCompose() == null) {
            return null;
        }
        return DockerComposeUtils.getEnvVariable(
                cfg.getCompose(),
                AgentApplicationType.GATEWAY.getMainImagePattern(),
                "TB_GW_SECURITY_TYPE");
    }
}

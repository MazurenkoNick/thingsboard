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
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
@TbCoreComponent
@RequiredArgsConstructor
public class AgentAutoInstallService {

    private final AgentService agentService;
    private final AgentAppProfileService profileService;
    private final AgentAppProvisioner appProvisioner;
    private final AgentEventRateLimiter agentEventRateLimiter;
    private final TbEdgeService tbEdgeService;
    private final RuleChainService ruleChainService;
    private final DeviceService deviceService;
    private final SystemSecurityService systemSecurityService;

    public void autoInstall(TenantId tenantId, AgentId agentId) {
        Agent agent = agentService.findAgentById(tenantId, agentId);
        if (agent == null || agent.getAgentProfileId() == null) {
            log.trace("[{}][{}] Skipping auto-install: agent not found or has no profile", tenantId, agentId);
            return;
        }
        List<AgentAppProfile> pendingProfiles = profileService.findUninstalledAppProfilesForAgentProfile(
                tenantId, agent.getAgentProfileId(), agent.getId());

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
        // rate-limit before creating any related entity so an over-quota reject doesn't orphan an Edge/Device
        agentEventRateLimiter.checkOrThrow(tenantId, agent.getId());
        String projectName = AgentApplication.generateProjectName();
        GroupEntity<? extends EntityId> relatedEntity = createRelatedEntityIfNeeded(tenantId, profile, projectName);
        EntityId relatedEntityId = relatedEntity == null ? null : relatedEntity.getId();

        AgentApplication app = new AgentApplication();
        app.setTenantId(tenantId);
        app.setAgentId(agent.getId());
        app.setName(resolveAppName(profile, relatedEntity, projectName));
        app.setAppType(profile.getAppType());
        app.setTemplateId(profile.getTemplateId());
        app.setApplicationProfileId(profile.getId());
        app.setOrigin(AgentApplicationOrigin.AUTO_PROVISIONED);
        app.setProjectName(projectName);
        // profile's config will be set to the application implicitly inside applicationService#saveWithRelatedEntity
        AgentApplication savedApp = appProvisioner.saveWithLifecycleEvent(tenantId, app, relatedEntityId, AgentAppEventActionType.INSTALL);
        log.info("[{}][{}] Auto-provisioned application [{}] for profile [{}]",
                tenantId, agent.getId(), savedApp.getId(), profile.getId());
    }

    private String resolveAppName(AgentAppProfile profile, GroupEntity<? extends EntityId> relatedEntity, String projectName) {
        return relatedEntity == null
                ? profile.getAppType() + " " + projectName
                : relatedEntity.getName() + " application";
    }

    private GroupEntity<? extends EntityId> createRelatedEntityIfNeeded(TenantId tenantId, AgentAppProfile profile, String projectName) {
        if (profile.getAppType() == null) {
            return null;
        }
        var relatedType = profile.getAppType().getRelatedEntityType();
        if (relatedType == null) {
            return null;
        }
        return switch (relatedType) {
            case EDGE -> createEdge(tenantId, profile, projectName);
            case DEVICE -> createGatewayDevice(tenantId, profile, projectName);
            default -> null;
        };
    }

    private Edge createEdge(TenantId tenantId, AgentAppProfile profile, String projectName) {
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(tenantId);
        if (edgeTemplateRootRuleChain == null) {
            throw new NoSuchElementException("Root edge rule chain is not available!");
        }
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName("Edge-" + projectName);
        edge.setType("default");
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setEdgeLicenseKey(EdgeUtils.DEFAULT_EDGE_LICENSE_KEY);
        edge.setCloudEndpoint(systemSecurityService.getBaseUrl(tenantId, null, null));
        Edge saved;
        try {
            saved = tbEdgeService.save(edge, edgeTemplateRootRuleChain, Collections.emptyList(), null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to auto-create Edge for profile " + profile.getId(), e);
        }
        log.info("[{}] Auto-created Edge [{}] for profile [{}]", tenantId, saved.getId(), profile.getId());
        return saved;
    }

    /**
     * Reverse-engineers credentials type from the profile's compose env vars so that the
     * device credentials match what MergeCredentialsToConfigRule expects to inject into compose.
     */
    private Device createGatewayDevice(TenantId tenantId, AgentAppProfile profile, String projectName) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Gateway-" + projectName);
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
            return saved;
        }

        // Default: ACCESS_TOKEN — null lets DeviceServiceImpl generate a 20-char random token
        Device saved = deviceService.saveDeviceWithAccessToken(device, null);
        log.info("[{}] Auto-created gateway Device [{}] (ACCESS_TOKEN) for profile [{}]", tenantId, saved.getId(), profile.getId());
        return saved;
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

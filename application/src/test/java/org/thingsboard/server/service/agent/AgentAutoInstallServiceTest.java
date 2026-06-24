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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAutoInstallServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final AgentProfileId AGENT_PROFILE_ID = new AgentProfileId(UUID.randomUUID());
    private static final AgentAppProfileId APPLICATION_PROFILE_ID = new AgentAppProfileId(UUID.randomUUID());
    private static final AgentAppTemplateId TEMPLATE_ID = new AgentAppTemplateId(UUID.randomUUID());

    @Mock
    private AgentService agentService;
    @Mock
    private AgentAppProfileService profileService;
    @Mock
    private AgentAppProvisioner appProvisioner;
    @Mock
    private AgentEventRateLimiter agentEventRateLimiter;
    @Mock
    private TbEdgeService tbEdgeService;
    @Mock
    private RuleChainService ruleChainService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private SystemSecurityService systemSecurityService;

    @InjectMocks
    private AgentAutoInstallService autoInstallService;

    private Agent agentWithProfile;

    @BeforeEach
    void setUp() {
        agentWithProfile = new Agent();
        agentWithProfile.setId(AGENT_ID);
        agentWithProfile.setTenantId(TENANT_ID);
        agentWithProfile.setAgentProfileId(AGENT_PROFILE_ID);
        agentWithProfile.setName("test-agent");
    }

    @Test
    void whenAgentNotFound_shouldSkip() {
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(null);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(profileService, appProvisioner, tbEdgeService, deviceService);
    }

    @Test
    void whenAgentHasNoProfile_shouldSkip() {
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setAgentProfileId(null);
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agent);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(profileService, appProvisioner, tbEdgeService, deviceService);
    }

    @Test
    void whenNoPendingProfiles_shouldNotCreateAnything() {
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(Collections.emptyList());

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(appProvisioner, tbEdgeService, deviceService);
    }

    @Test
    void whenGenericProfile_shouldCreateAppAndEvent_noRelatedEntity() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GENERIC, null);
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        ArgumentCaptor<EntityId> relatedCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), appCaptor.capture(), relatedCaptor.capture(), eq(AgentAppEventActionType.INSTALL));
        AgentApplication saved = appCaptor.getValue();
        assertThat(saved.getAgentId()).isEqualTo(AGENT_ID);
        assertThat(saved.getApplicationProfileId()).isEqualTo(APPLICATION_PROFILE_ID);
        assertThat(saved.getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(saved.getAppType()).isEqualTo(AgentApplicationType.GENERIC);
        assertThat(saved.getOrigin()).isEqualTo(AgentApplicationOrigin.AUTO_PROVISIONED);
        assertThat(relatedCaptor.getValue()).isNull();

        verifyNoInteractions(tbEdgeService, deviceService);
    }

    @Test
    void whenEdgeProfile_shouldCreateEdgeWithRandomCredentials() throws Exception {
        AgentAppProfile profile = newProfile(AgentApplicationType.EDGE, emptyComposeConfig());
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        when(systemSecurityService.getBaseUrl(eq(TENANT_ID), isNull(), isNull()))
                .thenReturn("http://thingsboard.local");

        RuleChain edgeTemplateRootRuleChain = new RuleChain();
        edgeTemplateRootRuleChain.setId(new RuleChainId(UUID.randomUUID()));
        when(ruleChainService.getEdgeTemplateRootRuleChain(TENANT_ID)).thenReturn(edgeTemplateRootRuleChain);

        EdgeId edgeId = new EdgeId(UUID.randomUUID());
        Edge createdEdge = new Edge();
        createdEdge.setId(edgeId);
        when(tbEdgeService.save(any(Edge.class), eq(edgeTemplateRootRuleChain), eq(Collections.<EntityGroup>emptyList()), isNull()))
                .thenReturn(createdEdge);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<Edge> edgeCaptor = ArgumentCaptor.forClass(Edge.class);
        verify(tbEdgeService).save(edgeCaptor.capture(), eq(edgeTemplateRootRuleChain), eq(Collections.emptyList()), isNull());
        Edge edge = edgeCaptor.getValue();
        assertThat(edge.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(edge.getName()).startsWith("Edge-");
        assertThat(edge.getType()).isEqualTo("default");
        assertThat(edge.getRoutingKey()).isNotBlank().hasSize(20);
        assertThat(edge.getSecret()).isNotBlank().hasSize(20);
        assertThat(edge.getEdgeLicenseKey()).isNotBlank();
        assertThat(edge.getCloudEndpoint()).isEqualTo("http://thingsboard.local");

        ArgumentCaptor<EntityId> relatedCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), relatedCaptor.capture(), eq(AgentAppEventActionType.INSTALL));
        assertThat(relatedCaptor.getValue()).isEqualTo(edgeId);
    }

    @Test
    void whenGatewayProfileWithAccessToken_shouldCreateDeviceWithAutoToken() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GATEWAY, gatewayComposeConfig("accessToken"));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device createdDevice = new Device();
        createdDevice.setId(deviceId);
        when(deviceService.saveDeviceWithAccessToken(any(Device.class), isNull())).thenReturn(createdDevice);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceService).saveDeviceWithAccessToken(deviceCaptor.capture(), isNull());
        assertThat(deviceCaptor.getValue().getName()).startsWith("Gateway-");
        verify(deviceService, never()).saveDeviceWithCredentials(any(), any());

        ArgumentCaptor<EntityId> relatedCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), relatedCaptor.capture(), eq(AgentAppEventActionType.INSTALL));
        assertThat(relatedCaptor.getValue()).isEqualTo(deviceId);
    }

    @Test
    void whenGatewayProfileWithUsernamePassword_shouldCreateDeviceWithMqttBasicCredentials() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GATEWAY, gatewayComposeConfig("usernamePassword"));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device createdDevice = new Device();
        createdDevice.setId(deviceId);
        when(deviceService.saveDeviceWithCredentials(any(Device.class), any(DeviceCredentials.class)))
                .thenReturn(createdDevice);
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<DeviceCredentials> credsCaptor = ArgumentCaptor.forClass(DeviceCredentials.class);
        verify(deviceService).saveDeviceWithCredentials(any(Device.class), credsCaptor.capture());
        DeviceCredentials creds = credsCaptor.getValue();
        assertThat(creds.getCredentialsType()).isEqualTo(DeviceCredentialsType.MQTT_BASIC);
        assertThat(creds.getCredentialsValue()).contains("clientId").contains("userName").contains("password");
        verify(deviceService, never()).saveDeviceWithAccessToken(any(), any());

        ArgumentCaptor<EntityId> relatedCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(appProvisioner).saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), relatedCaptor.capture(), eq(AgentAppEventActionType.INSTALL));
        assertThat(relatedCaptor.getValue()).isEqualTo(deviceId);
    }

    @Test
    void whenOneProfileFails_shouldContinueWithOthers() {
        AgentAppProfile failingProfile = newProfile(AgentApplicationType.GENERIC, null);
        failingProfile.setId(new AgentAppProfileId(UUID.randomUUID()));
        AgentAppProfile okProfile = newProfile(AgentApplicationType.GENERIC, null);

        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithProfile);
        when(profileService.findUninstalledAppProfilesForAgentProfile(TENANT_ID, AGENT_PROFILE_ID, AGENT_ID))
                .thenReturn(List.of(failingProfile, okProfile));
        when(appProvisioner.saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL)))
                .thenThrow(new RuntimeException("boom"))
                .thenAnswer(inv -> inv.getArgument(1));

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(appProvisioner, org.mockito.Mockito.times(2))
                .saveWithLifecycleEvent(eq(TENANT_ID), any(AgentApplication.class), any(), eq(AgentAppEventActionType.INSTALL));
    }

    private AgentAppProfile newProfile(AgentApplicationType appType, DockerComposeConfig config) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setId(APPLICATION_PROFILE_ID);
        profile.setTenantId(TENANT_ID);
        profile.setName("profile-" + appType.name().toLowerCase());
        profile.setAppType(appType);
        profile.setTemplateId(TEMPLATE_ID);
        profile.setConfig(config);
        return profile;
    }

    private DockerComposeConfig emptyComposeConfig() {
        DockerComposeConfig cfg = new DockerComposeConfig();
        cfg.setCompose(JacksonUtil.newObjectNode());
        return cfg;
    }

    private DockerComposeConfig gatewayComposeConfig(String securityType) {
        DockerComposeConfig cfg = new DockerComposeConfig();
        String json = "{\n" +
                "  \"services\": {\n" +
                "    \"tb-gateway\": {\n" +
                "      \"image\": \"thingsboard/tb-gateway:latest\",\n" +
                "      \"environment\": {\n" +
                "        \"TB_GW_SECURITY_TYPE\": \"" + securityType + "\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonNode node = JacksonUtil.toJsonNode(json);
        cfg.setCompose(node);
        return cfg;
    }
}

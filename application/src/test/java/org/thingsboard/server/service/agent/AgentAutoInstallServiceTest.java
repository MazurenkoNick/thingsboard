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
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentGroupService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;

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
    private static final AgentGroupId GROUP_ID = new AgentGroupId(UUID.randomUUID());
    private static final AgentAppProfileId PROFILE_ID = new AgentAppProfileId(UUID.randomUUID());
    private static final AgentAppTemplateId TEMPLATE_ID = new AgentAppTemplateId(UUID.randomUUID());

    @Mock
    private AgentService agentService;
    @Mock
    private AgentGroupService agentGroupService;
    @Mock
    private AgentAppProfileService profileService;
    @Mock
    private AgentApplicationService applicationService;
    @Mock
    private AgentAppEventService appEventService;
    @Mock
    private EdgeService edgeService;
    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private AgentAutoInstallService autoInstallService;

    private Agent agentWithGroup;

    @BeforeEach
    void setUp() {
        agentWithGroup = new Agent();
        agentWithGroup.setId(AGENT_ID);
        agentWithGroup.setTenantId(TENANT_ID);
        agentWithGroup.setAgentGroupId(GROUP_ID);
        agentWithGroup.setName("test-agent");
    }

    @Test
    void whenAgentNotFound_shouldSkip() {
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(null);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(profileService, applicationService, appEventService, edgeService, deviceService);
    }

    @Test
    void whenAgentHasNoGroup_shouldSkip() {
        Agent agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setTenantId(TENANT_ID);
        agent.setAgentGroupId(null);
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agent);

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(profileService, applicationService, appEventService, edgeService, deviceService);
    }

    @Test
    void whenNoPendingProfiles_shouldNotCreateAnything() {
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithGroup);
        when(profileService.findUninstalledProfilesForAgent(TENANT_ID, GROUP_ID, AGENT_ID))
                .thenReturn(Collections.emptyList());

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verifyNoInteractions(applicationService, appEventService, edgeService, deviceService);
    }

    @Test
    void whenGenericProfile_shouldCreateAppAndEvent_noRelatedEntity() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GENERIC, null);
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithGroup);
        when(profileService.findUninstalledProfilesForAgent(TENANT_ID, GROUP_ID, AGENT_ID))
                .thenReturn(List.of(profile));
        when(applicationService.save(eq(TENANT_ID), any(AgentApplication.class)))
                .thenAnswer(inv -> {
                    AgentApplication a = inv.getArgument(1);
                    a.setId(new org.thingsboard.server.common.data.id.AgentApplicationId(UUID.randomUUID()));
                    return a;
                });

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(applicationService).save(eq(TENANT_ID), appCaptor.capture());
        AgentApplication saved = appCaptor.getValue();
        assertThat(saved.getAgentId()).isEqualTo(AGENT_ID);
        assertThat(saved.getApplicationProfileId()).isEqualTo(PROFILE_ID);
        assertThat(saved.getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(saved.getAppType()).isEqualTo(AgentApplicationType.GENERIC);
        assertThat(saved.getOrigin()).isEqualTo(AgentApplicationOrigin.AUTO_PROVISIONED);
        assertThat(saved.getRelatedEntityId()).isNull();

        ArgumentCaptor<AgentAppEvent> eventCaptor = ArgumentCaptor.forClass(AgentAppEvent.class);
        verify(appEventService).save(eq(TENANT_ID), eventCaptor.capture());
        AgentAppEvent event = eventCaptor.getValue();
        assertThat(event.getActionType()).isEqualTo(AgentAppEventActionType.INSTALL);
        assertThat(event.getDeliveryState()).isEqualTo(AgentAppEventDeliveryState.PENDING);
        assertThat(event.getApplicationId()).isEqualTo(saved.getId());

        verifyNoInteractions(edgeService, deviceService);
    }

    @Test
    void whenEdgeProfile_shouldCreateEdgeWithRandomCredentials() {
        AgentAppProfile profile = newProfile(AgentApplicationType.EDGE, emptyComposeConfig());
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithGroup);
        when(profileService.findUninstalledProfilesForAgent(TENANT_ID, GROUP_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        EdgeId edgeId = new EdgeId(UUID.randomUUID());
        Edge createdEdge = new Edge();
        createdEdge.setId(edgeId);
        when(edgeService.saveEdge(any(Edge.class))).thenReturn(createdEdge);
        when(applicationService.save(eq(TENANT_ID), any(AgentApplication.class)))
                .thenAnswer(inv -> {
                    AgentApplication a = inv.getArgument(1);
                    a.setId(new org.thingsboard.server.common.data.id.AgentApplicationId(UUID.randomUUID()));
                    return a;
                });

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<Edge> edgeCaptor = ArgumentCaptor.forClass(Edge.class);
        verify(edgeService).saveEdge(edgeCaptor.capture());
        Edge edge = edgeCaptor.getValue();
        assertThat(edge.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(edge.getName()).startsWith(profile.getName() + "-");
        assertThat(edge.getType()).isEqualTo("default");
        assertThat(edge.getRoutingKey()).isNotBlank().hasSize(20);
        assertThat(edge.getSecret()).isNotBlank().hasSize(20);

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(applicationService).save(eq(TENANT_ID), appCaptor.capture());
        assertThat(appCaptor.getValue().getRelatedEntityId()).isEqualTo(edgeId);
    }

    @Test
    void whenGatewayProfileWithAccessToken_shouldCreateDeviceWithAutoToken() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GATEWAY, gatewayComposeConfig("accessToken"));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithGroup);
        when(profileService.findUninstalledProfilesForAgent(TENANT_ID, GROUP_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device createdDevice = new Device();
        createdDevice.setId(deviceId);
        when(deviceService.saveDeviceWithAccessToken(any(Device.class), isNull())).thenReturn(createdDevice);
        when(applicationService.save(eq(TENANT_ID), any(AgentApplication.class)))
                .thenAnswer(inv -> {
                    AgentApplication a = inv.getArgument(1);
                    a.setId(new org.thingsboard.server.common.data.id.AgentApplicationId(UUID.randomUUID()));
                    return a;
                });

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceService).saveDeviceWithAccessToken(deviceCaptor.capture(), isNull());
        assertThat(deviceCaptor.getValue().getName()).startsWith(profile.getName() + "-");
        verify(deviceService, never()).saveDeviceWithCredentials(any(), any());

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(applicationService).save(eq(TENANT_ID), appCaptor.capture());
        assertThat(appCaptor.getValue().getRelatedEntityId()).isEqualTo(deviceId);
    }

    @Test
    void whenGatewayProfileWithUsernamePassword_shouldCreateDeviceWithMqttBasicCredentials() {
        AgentAppProfile profile = newProfile(AgentApplicationType.GATEWAY, gatewayComposeConfig("usernamePassword"));
        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithGroup);
        when(profileService.findUninstalledProfilesForAgent(TENANT_ID, GROUP_ID, AGENT_ID))
                .thenReturn(List.of(profile));

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        Device createdDevice = new Device();
        createdDevice.setId(deviceId);
        when(deviceService.saveDeviceWithCredentials(any(Device.class), any(DeviceCredentials.class)))
                .thenReturn(createdDevice);
        when(applicationService.save(eq(TENANT_ID), any(AgentApplication.class)))
                .thenAnswer(inv -> {
                    AgentApplication a = inv.getArgument(1);
                    a.setId(new org.thingsboard.server.common.data.id.AgentApplicationId(UUID.randomUUID()));
                    return a;
                });

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        ArgumentCaptor<DeviceCredentials> credsCaptor = ArgumentCaptor.forClass(DeviceCredentials.class);
        verify(deviceService).saveDeviceWithCredentials(any(Device.class), credsCaptor.capture());
        DeviceCredentials creds = credsCaptor.getValue();
        assertThat(creds.getCredentialsType()).isEqualTo(DeviceCredentialsType.MQTT_BASIC);
        assertThat(creds.getCredentialsValue()).contains("clientId").contains("userName").contains("password");
        verify(deviceService, never()).saveDeviceWithAccessToken(any(), any());

        ArgumentCaptor<AgentApplication> appCaptor = ArgumentCaptor.forClass(AgentApplication.class);
        verify(applicationService).save(eq(TENANT_ID), appCaptor.capture());
        assertThat(appCaptor.getValue().getRelatedEntityId()).isEqualTo(deviceId);
    }

    @Test
    void whenOneProfileFails_shouldContinueWithOthers() {
        AgentAppProfile failingProfile = newProfile(AgentApplicationType.GENERIC, null);
        failingProfile.setId(new AgentAppProfileId(UUID.randomUUID()));
        AgentAppProfile okProfile = newProfile(AgentApplicationType.GENERIC, null);

        when(agentService.findAgentById(TENANT_ID, AGENT_ID)).thenReturn(agentWithGroup);
        when(profileService.findUninstalledProfilesForAgent(TENANT_ID, GROUP_ID, AGENT_ID))
                .thenReturn(List.of(failingProfile, okProfile));
        when(applicationService.save(eq(TENANT_ID), any(AgentApplication.class)))
                .thenThrow(new RuntimeException("boom"))
                .thenAnswer(inv -> {
                    AgentApplication a = inv.getArgument(1);
                    a.setId(new org.thingsboard.server.common.data.id.AgentApplicationId(UUID.randomUUID()));
                    return a;
                });

        autoInstallService.autoInstall(TENANT_ID, AGENT_ID);

        verify(applicationService, org.mockito.Mockito.times(2)).save(eq(TENANT_ID), any(AgentApplication.class));
        verify(appEventService, org.mockito.Mockito.times(1)).save(eq(TENANT_ID), any(AgentAppEvent.class));
    }

    private AgentAppProfile newProfile(AgentApplicationType appType, DockerComposeConfig config) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setId(PROFILE_ID);
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

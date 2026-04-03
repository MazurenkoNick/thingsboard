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
package org.thingsboard.server.service.agent.template;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.sync.GitSyncService;
import org.thingsboard.server.service.sync.vc.GitRepository.FileType;
import org.thingsboard.server.service.sync.vc.GitRepository.RepoFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentAppTemplateSyncServiceTest {

    @Mock
    private GitSyncService gitSyncService;
    @Mock
    private AgentAppTemplateService agentAppTemplateService;
    @Mock
    private PartitionService partitionService;

    @InjectMocks
    private AgentAppTemplateSyncService syncService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(syncService, "repoUri", "https://github.com/test/repo.git");
        ReflectionTestUtils.setField(syncService, "branch", "main");
        ReflectionTestUtils.setField(syncService, "basePath", "templates");
        ReflectionTestUtils.setField(syncService, "fetchFrequencyMs", 3600000L);
    }

    // ==================== init() tests ====================

    @Test
    void init_shouldSkipRegistration_whenRepoUriIsBlank() throws Exception {
        ReflectionTestUtils.setField(syncService, "repoUri", "");

        syncService.init();

        verifyNoInteractions(gitSyncService);
    }

    @Test
    void init_shouldDefaultBranchToMain_whenBranchIsBlank() throws Exception {
        ReflectionTestUtils.setField(syncService, "branch", "");

        syncService.init();

        verify(gitSyncService).registerSync(eq("agent-app-templates"),
                eq("https://github.com/test/repo.git"), eq("main"), eq(3600000L), any());
    }

    @Test
    void init_shouldRegisterSync_withConfiguredValues() throws Exception {
        ReflectionTestUtils.setField(syncService, "branch", "develop");

        syncService.init();

        verify(gitSyncService).registerSync(eq("agent-app-templates"),
                eq("https://github.com/test/repo.git"), eq("develop"), eq(3600000L), any());
    }

    // ==================== update() - partition check ====================

    @Test
    void update_shouldSkip_whenNotMyPartition() {
        when(partitionService.isMyPartition(any(), any(), any())).thenReturn(false);

        invokeUpdate();

        verify(agentAppTemplateService, never()).save(any(), any());
    }

    // ==================== update() - happy path ====================

    @Test
    void update_shouldSaveTemplate_withFieldsFromFileNameAndContent() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        String filePath = "templates/" + fileName;
        RepoFile file = new RepoFile(filePath, fileName, FileType.FILE);
        when(gitSyncService.listFiles(eq("agent-app-templates"), eq("templates"), anyInt(), eq(FileType.FILE)))
                .thenReturn(List.of(file));
        when(gitSyncService.getFileContent("agent-app-templates", filePath))
                .thenReturn(templateJson("DOCKER_COMPOSE", "1.1.0", "[]"));
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(
                AgentApplicationType.EDGE, AgentAppConfigType.DOCKER_COMPOSE, "1.0.0"))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        AgentAppTemplate saved = captor.getValue();
        assertThat(saved.getAppType()).isEqualTo(AgentApplicationType.EDGE);
        assertThat(saved.getConfig()).isNotNull();
        assertThat(saved.getConfig().getType()).isEqualTo(AgentAppConfigType.DOCKER_COMPOSE);
        assertThat(saved.getCurrentVersion()).isEqualTo("1.0.0");
        assertThat(saved.getNextVersion()).isEqualTo("1.1.0");
        assertThat(saved.getTenantId()).isEqualTo(TenantId.SYS_TENANT_ID);
    }

    @Test
    void update_shouldUpdateExistingTemplate_whenFound() {
        setupPartitionOwnership();
        String fileName = "template-GATEWAY-DOCKER_COMPOSE-2.0.0.json";
        String filePath = "templates/" + fileName;
        RepoFile file = new RepoFile(filePath, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));
        when(gitSyncService.getFileContent("agent-app-templates", filePath))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]"));

        AgentAppTemplate existing = new AgentAppTemplate();
        existing.setAppType(AgentApplicationType.GATEWAY);
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(
                AgentApplicationType.GATEWAY, AgentAppConfigType.DOCKER_COMPOSE, "2.0.0"))
                .thenReturn(existing);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(existing.getCurrentVersion()).isEqualTo("2.0.0");
    }

    @Test
    void update_shouldProcessMultipleFiles() {
        setupPartitionOwnership();
        RepoFile file1 = new RepoFile("templates/template-EDGE-DOCKER_COMPOSE-1.0.0.json",
                "template-EDGE-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        RepoFile file2 = new RepoFile("templates/template-GATEWAY-DOCKER_COMPOSE-1.0.0.json",
                "template-GATEWAY-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file1, file2));
        when(gitSyncService.getFileContent("agent-app-templates", file1.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, null, "[]"));
        when(gitSyncService.getFileContent("agent-app-templates", file2.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, null, "[]"));
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(any(), any(), any()))
                .thenReturn(null);

        invokeUpdate();

        verify(agentAppTemplateService, times(2)).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    // ==================== update() - error isolation ====================

    @Test
    void update_shouldContinueProcessing_whenOneFileFails() {
        setupPartitionOwnership();
        RepoFile badFile = new RepoFile("templates/template-EDGE-DOCKER_COMPOSE-1.0.0.json",
                "template-EDGE-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        RepoFile goodFile = new RepoFile("templates/template-GATEWAY-DOCKER_COMPOSE-2.0.0.json",
                "template-GATEWAY-DOCKER_COMPOSE-2.0.0.json", FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(badFile, goodFile));
        when(gitSyncService.getFileContent("agent-app-templates", badFile.path()))
                .thenThrow(new RuntimeException("git read error"));
        when(gitSyncService.getFileContent("agent-app-templates", goodFile.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, null, "[]"));
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(any(), any(), any()))
                .thenReturn(null);

        invokeUpdate();

        verify(agentAppTemplateService, times(1)).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    // ==================== config type validation ====================

    @Test
    void update_shouldSkipFile_whenConfigTypeMismatchesBetweenFilenameAndContent() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";

        RepoFile mismatchFile = new RepoFile("templates/template-EDGE-INVALID_TYPE-1.0.0.json",
                "template-EDGE-INVALID_TYPE-1.0.0.json", FileType.FILE);
        RepoFile goodFile = new RepoFile("templates/template-GATEWAY-DOCKER_COMPOSE-1.0.0.json",
                "template-GATEWAY-DOCKER_COMPOSE-1.0.0.json", FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(mismatchFile, goodFile));
        when(gitSyncService.getFileContent("agent-app-templates", mismatchFile.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, null, "[]"));
        when(gitSyncService.getFileContent("agent-app-templates", goodFile.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, null, "[]"));
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(any(), any(), any()))
                .thenReturn(null);

        invokeUpdate();

        // Only the good file should be saved — mismatch file fails with IllegalArgumentException on valueOf
        verify(agentAppTemplateService, times(1)).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    // ==================== file pattern matching ====================

    @Test
    void update_shouldIgnoreFilesThatDontMatchPattern() {
        setupPartitionOwnership();
        RepoFile nonMatching = new RepoFile("templates/readme.md", "readme.md", FileType.FILE);
        RepoFile wrongFormat = new RepoFile("templates/template-edge-1.0.0.json",
                "template-edge-1.0.0.json", FileType.FILE); // lowercase, missing configType
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(nonMatching, wrongFormat));

        invokeUpdate();

        verify(gitSyncService, never()).getFileContent(any(), any());
        verify(agentAppTemplateService, never()).save(any(), any());
    }

    @Test
    void update_shouldMatchPreReleaseVersions() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0-beta1.json";
        RepoFile file = new RepoFile("templates/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, null, "[]"));
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(any(), any(), any()))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        assertThat(captor.getValue().getCurrentVersion()).isEqualTo("1.0.0-beta1");
    }

    // ==================== YAML compose conversion ====================

    @Test
    void update_shouldConvertYamlComposeTemplatesToJson() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));

        UUID stepId = UUID.randomUUID();
        String yamlCompose = "services:\n  tb-core:\n    image: thingsboard/tb-core";
        String startStepsJson = """
                [{"type": "COMPOSE_TEMPLATE", "id": "%s", "title": "Choose type", "composeTemplates": {"monolith": "%s"}}]
                """.formatted(stepId, yamlCompose.replace("\n", "\\n"));
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, startStepsJson, "[]"));
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(any(), any(), any()))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        AgentAppTemplate saved = captor.getValue();
        assertThat(saved.getStartSteps()).hasSize(1);
        ComposeTypeChoiceStep step = (ComposeTypeChoiceStep) saved.getStartSteps().get(0);
        JsonNode composeJson = step.getComposeTemplates().get("monolith");
        assertThat(composeJson).isNotNull();
        assertThat(composeJson.isObject()).isTrue();
        assertThat(composeJson.get("services").get("tb-core").get("image").asText())
                .isEqualTo("thingsboard/tb-core");
    }

    @Test
    void update_shouldReadComposeFromYamlFileReference() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));

        UUID stepId = UUID.randomUUID();
        String startStepsJson = """
                [{"type": "COMPOSE_TEMPLATE", "id": "%s", "title": "Choose type", "composeTemplates": {"monolith": "compose/edge/1.0.0/monolith.yml"}}]
                """.formatted(stepId);
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, startStepsJson, "[]"));
        when(gitSyncService.getFileContent("agent-app-templates", "compose/edge/1.0.0/monolith.yml"))
                .thenReturn("services:\n  tb-core:\n    image: thingsboard/tb-core".getBytes());
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(any(), any(), any()))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        AgentAppTemplate saved = captor.getValue();
        assertThat(saved.getStartSteps()).hasSize(1);
        ComposeTypeChoiceStep step = (ComposeTypeChoiceStep) saved.getStartSteps().get(0);
        JsonNode composeJson = step.getComposeTemplates().get("monolith");
        assertThat(composeJson).isNotNull();
        assertThat(composeJson.isObject()).isTrue();
        assertThat(composeJson.get("services").get("tb-core").get("image").asText())
                .isEqualTo("thingsboard/tb-core");
    }

    @Test
    void update_shouldHandleEmptyComposeTemplatesGracefully() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));

        UUID stepId = UUID.randomUUID();
        String startStepsJson = """
                [{"type": "COMPOSE_TEMPLATE", "id": "%s", "title": "Choose type", "composeTemplates": {}}]
                """.formatted(stepId);
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, null, startStepsJson));
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(any(), any(), any()))
                .thenReturn(null);

        invokeUpdate();

        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), any());
    }

    @Test
    void update_shouldNotConvertComposeTemplatesInUpgradeSteps() {
        setupPartitionOwnership();
        String fileName = "template-EDGE-DOCKER_COMPOSE-1.0.0.json";
        RepoFile file = new RepoFile("templates/" + fileName, fileName, FileType.FILE);
        when(gitSyncService.listFiles(any(), any(), anyInt(), any())).thenReturn(List.of(file));

        UUID stepId = UUID.randomUUID();
        String infoStepJson = """
                [{"type": "COMPOSE_START", "id": "%s", "title": "Upgrade info"}]
                """.formatted(stepId);
        when(gitSyncService.getFileContent("agent-app-templates", file.path()))
                .thenReturn(templateJson("DOCKER_COMPOSE", null, "[]", infoStepJson));
        when(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(any(), any(), any()))
                .thenReturn(null);

        invokeUpdate();

        ArgumentCaptor<AgentAppTemplate> captor = ArgumentCaptor.forClass(AgentAppTemplate.class);
        verify(agentAppTemplateService).save(eq(TenantId.SYS_TENANT_ID), captor.capture());
        assertThat(captor.getValue().getUpgradeSteps()).hasSize(1);
    }

    // ==================== Helper methods ====================

    private void setupPartitionOwnership() {
        when(partitionService.isMyPartition(eq(ServiceType.TB_CORE), eq(TenantId.SYS_TENANT_ID), eq(TenantId.SYS_TENANT_ID)))
                .thenReturn(true);
    }

    private void invokeUpdate() {
        try {
            syncService.init();
            ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
            verify(gitSyncService).registerSync(any(), any(), any(), anyLong(), callbackCaptor.capture());
            callbackCaptor.getValue().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] templateJson(String configType, String nextVersion, String startStepsJson) {
        return templateJson(configType, nextVersion, startStepsJson, "[]");
    }

    private byte[] templateJson(String configType, String nextVersion,
                                String startStepsJson, String upgradeStepsJson) {
        return """
                {
                  "config": {"type": "%s"},
                  "nextVersion": %s,
                  "startSteps": %s,
                  "upgradeSteps": %s
                }
                """.formatted(
                configType,
                nextVersion != null ? "\"" + nextVersion + "\"" : "null",
                startStepsJson,
                upgradeStepsJson
        ).getBytes();
    }
}

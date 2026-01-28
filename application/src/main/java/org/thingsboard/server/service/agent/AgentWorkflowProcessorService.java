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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.edge.utils.EdgeProtoUtils;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.CommandResult;
import org.thingsboard.server.gen.agent.v1.InstallCommand;
import org.thingsboard.server.gen.agent.v1.Metadata;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.workflow.StepPayload;
import org.thingsboard.server.service.install.InstallScripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@TbCoreComponent
@Slf4j
public class AgentWorkflowProcessorService { // todo: rename to specify that it's install-only. create separate for upgrade

    // Keep it simple: one active workflow per agent.
    private final Map<AgentId, WorkflowState> workflows = new ConcurrentHashMap<>();

    private final InstallScripts installScripts;

    public AgentWorkflowProcessorService(InstallScripts installScripts) {
        this.installScripts = installScripts;
    }

    public void startMockInstallIfPossible(AgentSession session) {
        AgentId agentId = session.getState().getAgentId();
        if (agentId == null) {
            return;
        }
        workflows.computeIfAbsent(agentId, ignored -> {
            WorkflowState wf = new WorkflowState(buildMockSteps(), UUID.randomUUID(), 0);
            log.info("[{}] Starting mock install workflow with {} steps", agentId, wf.steps.size());
            pushCurrentStep(session, wf);
            return wf;
        });
    }

    public void onRejectedAck(AgentSession session) {
        return;
    }

    public void onStepResult(AgentSession session, CommandResult res) {
        AgentId agentId = session.getState().getAgentId();
        if (agentId == null) {
            return;
        }
        WorkflowState wf = workflows.get(agentId);
        if (wf == null) {
            return;
        }
        if (!sameCommandId(wf.commandId, res.getCommandId())) {
            log.debug("[{}] Ignoring ack for unknown commandId, step={}", agentId, res.getStep());
            return;
        }

        long expectedStep = wf.currentStepIndex + 1L;
        if (res.getStep() != expectedStep) {
            log.debug("[{}] Ignoring out-of-order ack. expectedStep={}, gotStep={}", agentId, expectedStep, res.getStep());
            return;
        }

        if (!res.getSuccess()) {
            log.warn("[{}] Step {} rejected by agent. message={}", agentId, res.getStep(), res.getMessage());
            workflows.remove(agentId);
            return;
        }

        wf.currentStepIndex++;
        if (wf.currentStepIndex >= wf.steps.size()) {
            log.info("[{}] Mock install workflow completed successfully", agentId);
            workflows.remove(agentId);
            return;
        }
        pushCurrentStep(session, wf);
    }

    private void pushCurrentStep(AgentSession session, WorkflowState wf) {
        long stepNumber = wf.currentStepIndex + 1L;
        StepPayload payload = wf.steps.get(wf.currentStepIndex);

        Metadata.Builder metaBuilder = Metadata.newBuilder()
//                .putData("method", installationMethod)
                .putData("baseUrl", "host.docker.internal") // todo: retrieve from HttpServletRequest#getServerName
                .putData("id", payload.id() == null ? "" : payload.id())
                .putData("title", payload.title())
                .putData("type", payload.type())
                .putData("optional", String.valueOf(payload.optional()));

        // Handle different step types
        if (payload.compose() != null) {
            // Serialize compose structure to JSON
            String composeJson = JacksonUtil.toString(payload.compose());
            metaBuilder.putData("compose", composeJson);
        }

        if (payload.modifications() != null) {
            // Serialize modifications to JSON
            String modificationsJson = JacksonUtil.toString(payload.modifications());
            metaBuilder.putData("modifications", modificationsJson);
        }

        if (payload.projectName() != null) {
            metaBuilder.putData("projectName", payload.projectName());
        }

        if (payload.message() != null) {
            metaBuilder.putData("message", payload.message());
        }

        InstallCommand installCommand = InstallCommand.newBuilder()
                .setCommandId(toMsg(wf.commandId))
                .setStep(stepNumber)
                .setAppName("tb-edge")
                .setAppType(org.thingsboard.server.gen.agent.v1.AppType.DOCKER_COMPOSE)
                .setMeta(metaBuilder.build())
                .build();

        session.push(ServerToAgent.newBuilder().setInstallCommand(installCommand).build());
    }

    private List<StepPayload> buildMockSteps() {
        Path stepsFile = resolveStepsFile();

        try {
            if (!Files.exists(stepsFile)) {
                log.warn("Steps file not found: {}, falling back to empty steps", stepsFile);
                return new ArrayList<>();
            }

            String jsonContent = Files.readString(stepsFile);
            StepsDefinition stepsDef = JacksonUtil.fromString(jsonContent, StepsDefinition.class);

            if (stepsDef == null || stepsDef.steps == null || stepsDef.steps.isEmpty()) {
                log.warn("No steps found in file: {}", stepsFile);
                return new ArrayList<>();
            }

            String platformEdgeVersion = convertEdgeVersionToDocsFormat(EdgeProtoUtils.getNewestEdgeVersion().name());
            List<StepPayload> steps = new ArrayList<>();

            for (StepDefinition stepDef : stepsDef.steps) {
                String stepType = stepDef.type != null ? stepDef.type : "bash";
                boolean isOptional = stepDef.optional != null && stepDef.optional;

                // Handle different step types
                Object compose = null;
                Object modifications = null;
                String projectName = null;
                String message = null;

                if ("compose".equals(stepType) && stepDef.compose != null) {
                    // Substitute variables in compose structure
                    compose = substituteVariablesInCompose(stepDef.compose, "method", platformEdgeVersion); // todo: method ---- var
                } else if ("compose-modify".equals(stepType) && stepDef.modifications != null) {
                    modifications = stepDef.modifications;
                } else if ("compose-start".equals(stepType)) {
                    projectName = stepDef.projectName != null ? stepDef.projectName : "tb-edge";
                } else if ("info".equals(stepType)) {
                    message = stepDef.message;
                }

                steps.add(new StepPayload(stepDef.id, stepDef.title, stepType, isOptional,
                        compose, modifications, projectName, message));
            }

            return steps;
        } catch (IOException e) {
            log.warn("Failed to read steps file: {}", stepsFile, e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("Failed to parse steps file: {}", stepsFile, e);
            return new ArrayList<>();
        }
    }

    private String substituteVariables(String script, String method, String platformEdgeVersion) {
        if (script == null) {
            return "";
        }

//        script = script.replace("${CLOUD_ROUTING_KEY}", routingKey);
//        script = script.replace("${CLOUD_ROUTING_SECRET}", secret);
//        script = script.replace("${CLOUD_RPC_PORT}", Integer.toString(rpcPort));
//        script = script.replace("${CLOUD_RPC_SSL_ENABLED}", Boolean.toString(sslEnabled));

        if ("docker".equals(method)) {
            script = script.replace("${TB_EDGE_VERSION}", platformEdgeVersion + "EDGE");
            script = script.replace("${BASE_URL}", "host.docker.internal");
            script = script.replace("${EXTRA_HOSTS}", "");
        }

        return script;
    }

    private Object substituteVariablesInCompose(Object composeObj, String method, String platformEdgeVersion) {
        // Convert to JsonNode for recursive substitution
        JsonNode composeNode = JacksonUtil.valueToTree(composeObj);
        if (composeNode == null || !composeNode.isObject()) {
            return composeObj;
        }

        ObjectNode composeObject = (ObjectNode) composeNode;
        substituteVariablesInJsonNode(composeObject, method, platformEdgeVersion);

        // Convert back to Map/Object structure
        return JacksonUtil.treeToValue(composeObject, Object.class);
    }

    private void substituteVariablesInJsonNode(JsonNode node, String method, String platformEdgeVersion) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    String text = value.asText();
                    String substituted = substituteVariables(text, method, platformEdgeVersion);
                    if (!text.equals(substituted)) {
                        objectNode.put(entry.getKey(), substituted);
                    }
                } else if (value.isArray()) {
                    // Process array elements
                    for (int i = 0; i < value.size(); i++) {
                        JsonNode arrayElement = value.get(i);
                        if (arrayElement.isTextual()) {
                            String text = arrayElement.asText();
                            String substituted = substituteVariables(text, method, platformEdgeVersion);
                            if (!text.equals(substituted)) {
                                ((ArrayNode) value).set(i, JsonNodeFactory.instance.textNode(substituted));
                            }
                        } else {
                            substituteVariablesInJsonNode(arrayElement, method, platformEdgeVersion);
                        }
                    }
                } else {
                    substituteVariablesInJsonNode(value, method, platformEdgeVersion);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode arrayElement : node) {
                substituteVariablesInJsonNode(arrayElement, method, platformEdgeVersion);
            }
        }
    }

    private Path resolveStepsFile() {
        // <dataDir>/json/edge/instructions/install/<method>/install.json
        return Paths.get(installScripts.getDataDir(), InstallScripts.JSON_DIR, "edge", "instructions", "install", "docker", "install.json");
    }

    private static class StepsDefinition {
        public List<StepDefinition> steps;
    }

    private static class StepDefinition {
        public String id; // optional short stable identifier (max 4 symbols recommended)
        public String title;
        public String script;  // For backward compatibility with bash steps
        public String type;
        public Boolean optional;
        public Object compose;  // For "compose" type steps
        public Object modifications;  // For "compose-modify" type steps
        public String projectName;  // For "compose-start" type steps
        public String message;  // For "info" type steps
    }

    private static boolean sameCommandId(UUID expected, CommandId actual) {
        return expected.getMostSignificantBits() == actual.getIdMSB()
                && expected.getLeastSignificantBits() == actual.getIdLSB();
    }

    private static CommandId toMsg(UUID id) {
        return CommandId.newBuilder()
                .setIdMSB(id.getMostSignificantBits())
                .setIdLSB(id.getLeastSignificantBits())
                .build();
    }

    private static String convertEdgeVersionToDocsFormat(String edgeVersion) {
        return edgeVersion.replace("_", ".").substring(2);
    }

    private static class WorkflowState {
        final List<StepPayload> steps;
        final UUID commandId;
        volatile int currentStepIndex;

        WorkflowState(List<StepPayload> steps, UUID commandId, int currentStepIndex) {
            this.steps = steps;
            this.commandId = commandId;
            this.currentStepIndex = currentStepIndex;
        }
    }
}
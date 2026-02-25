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

import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.RollbackEventMeta;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.InfoStep;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.ConfigType;
import org.thingsboard.server.gen.agent.v1.HelloAck;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.agent.v1.StepId;

import java.util.Map;

public class AgentMsgConstructorUtils {

    private static final HelloAck HELLO_SUCCESS_MSG = HelloAck.newBuilder().setSuccess(true).build();

    public static ServerToAgent helloSuccessResponse() {
        return ServerToAgent.newBuilder()
                .setHelloAck(HELLO_SUCCESS_MSG)
                .build();
    }

    public static ServerToAgent buildAppCommand(AgentAppEvent event, AgentApplication application, AgentAppStep step, int totalSteps) {
        AppCommand.Builder builder = AppCommand.newBuilder()
                .setCommandId(CommandId.newBuilder()
                        .setIdMSB(event.getId().getId().getMostSignificantBits())
                        .setIdLSB(event.getId().getId().getLeastSignificantBits())
                        .build())
                .setAction(mapAction(event.getActionType()))
                .setAppName(application.getName() != null ? application.getName() : "")
                .setConfigType(mapConfigType(application.getConfig().getType()))
                .setTotalSteps(totalSteps);

        if (step != null) {
            builder.putAllMetadata(buildStepMetadata(step, application));
            builder.setStepId(StepId.newBuilder()
                    .setIdMSB(step.getId().getMostSignificantBits())
                    .setIdLSB(step.getId().getLeastSignificantBits())
                    .build());
        }
        if (event.getMetadata() instanceof RollbackEventMeta rollbackMeta && rollbackMeta.getFailedEventId() != null) {
            builder.putMetadata("failedCommandId", rollbackMeta.getFailedEventId().getId().toString());
        }
        return ServerToAgent.newBuilder()
                .setAppCommand(builder.build())
                .build();
    }

    private static AppCommandAction mapAction(AgentAppEventActionType actionType) {
        return switch (actionType) {
            case INSTALL -> AppCommandAction.APP_INSTALL;
            case UPDATE -> AppCommandAction.APP_UPDATE;
            case DELETE -> AppCommandAction.APP_DELETE;
            case RESTART -> AppCommandAction.APP_RESTART;
            case ROLLBACK -> AppCommandAction.APP_ROLLBACK;
        };
    }

    private static ConfigType mapConfigType(AgentAppConfigType appConfigType) {
        return ConfigType.DOCKER_COMPOSE;
    }

    private static Map<String, String> buildStepMetadata(AgentAppStep step, AgentApplication application) {
        Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("stepTitle", step.getTitle() != null ? step.getTitle() : "");
        metadata.put("stepType", step.getType() != null ? step.getType().name() : "");

        AgentAppStepType type = step.getType();

        if (application.getConfig() instanceof DockerComposeConfig cfg) {
            if (cfg.getProjectName() != null) {
                metadata.put("projectName", cfg.getProjectName());
            }
            if (type == AgentAppStepType.COMPOSE && cfg.getCompose() != null) {
                metadata.put("compose", cfg.getCompose().toString());
            }
            if (type == AgentAppStepType.COMPOSE_DOWN) {
                metadata.put("removeVolumes", String.valueOf(cfg.isRemoveVolumes()));
            }
        }

        if (type == AgentAppStepType.INFO && step instanceof InfoStep infoStep && infoStep.getMessage() != null) {
            metadata.put("message", infoStep.getMessage());
        }

        return metadata;
    }

}

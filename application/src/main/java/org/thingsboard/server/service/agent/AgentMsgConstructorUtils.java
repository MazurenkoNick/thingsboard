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

import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.AgentArgumentUtils;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.gen.agent.v1.AppCommand;
import org.thingsboard.server.gen.agent.v1.AppCommandAction;
import org.thingsboard.server.gen.agent.v1.CommandId;
import org.thingsboard.server.gen.agent.v1.ConfigType;
import org.thingsboard.server.gen.agent.v1.HelloAck;
import org.thingsboard.server.gen.agent.v1.ServerToAgent;
import org.thingsboard.server.gen.agent.v1.StepId;

import java.util.Map;
import java.util.UUID;

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
            builder.putAllMetadata(buildStepMetadata(event, step, application));
            builder.setStepId(StepId.newBuilder()
                    .setIdMSB(step.getId().getMostSignificantBits())
                    .setIdLSB(step.getId().getLeastSignificantBits())
                    .build());
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
            case UPGRADE -> AppCommandAction.APP_UPGRADE;
        };
    }

    private static ConfigType mapConfigType(AgentAppConfigType appConfigType) {
        return switch (appConfigType) {
            case DOCKER_COMPOSE -> ConfigType.DOCKER_COMPOSE;
        };
    }

    private static Map<String, String> buildStepMetadata(AgentAppEvent event, AgentAppStep step, AgentApplication application) {
        Map<UUID, AgentAppStepState> stepIdToUserStateSteps = event.getStepStates();
        Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("stepTitle", step.getTitle() != null ? step.getTitle() : "");
        metadata.put("stepType", step.getType() != null ? step.getType().name() : "");

        if (application.getProjectName() != null) {
            metadata.put("projectName", application.getProjectName());
        }

        AgentAppStepState resolvedUserState = CollectionUtils.isEmpty(stepIdToUserStateSteps)
                ? null : stepIdToUserStateSteps.get(step.getId());
        metadata.putAll(step.getCommandMetadata(application, resolvedUserState));

        var arguments = application.getConfig() != null ? application.getConfig().getArguments() : null;
        metadata.computeIfPresent("compose", (k, compose) ->
                AgentArgumentUtils.substitute(compose, event.getResolvedArguments(), arguments));
        // A RUN_JOB copies env/volumes from a compose service, which may carry ${tb.x} args — resolve them here too.
        metadata.computeIfPresent("job", (k, job) ->
                AgentArgumentUtils.substitute(job, event.getResolvedArguments(), arguments));

        return metadata;
    }

}

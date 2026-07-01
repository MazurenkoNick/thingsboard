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
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.state.RunJobStepState;
import org.thingsboard.server.common.data.agent.step.state.StepField;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunJobStepTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String COMPOSE = """
            {
              "services": {
                "mytbedge": {
                  "image": "thingsboard/tb-edge-pe:4.3.0EDGEPE",
                  "environment": { "SPRING_DATASOURCE_URL": "jdbc:postgresql://postgres:5432/tb" },
                  "volumes": ["tb-edge-data:/data", "tb-edge-logs:/var/log/tb-edge"]
                },
                "postgres": { "image": "postgres:16" }
              }
            }
            """;

    @Test
    void resolvesBindsEnvAndNetworkService() throws Exception {
        RunJobStepState state = new RunJobStepState();
        state.setImage(new StepField<>("thingsboard/tb-edge-pe:4.3.0EDGEPE", false));
        state.setBinds(new StepField<>(List.of("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}"), false));
        state.setEnv(new StepField<>(List.of("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).environment}"), false));
        state.setNetworkFromServiceImageRegexPattern(new StepField<>("postgres:.+", false));

        RunJobStep step = new RunJobStep();
        step.setState(state);

        Map<String, String> metadata = step.getCommandMetadata(application(), null);

        JsonNode job = MAPPER.readTree(metadata.get(RunJobStepState.JOB));
        assertThat(job.get("image").asText()).isEqualTo("thingsboard/tb-edge-pe:4.3.0EDGEPE");
        assertThat(job.get("binds")).extracting(JsonNode::asText)
                .containsExactly("tb-edge-data:/data", "tb-edge-logs:/var/log/tb-edge");
        assertThat(job.get("env")).extracting(JsonNode::asText)
                .containsExactly("SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/tb");
        assertThat(job.get("service").asText()).isEqualTo("postgres");
    }

    @Test
    void keepsNonRefEntriesAndOmitsServiceWhenNoRegex() throws Exception {
        RunJobStepState state = new RunJobStepState();
        state.setImage(new StepField<>("busybox", false));
        state.setBinds(new StepField<>(List.of("plain-vol:/mnt"), false));

        RunJobStep step = new RunJobStep();
        step.setState(state);

        JsonNode job = MAPPER.readTree(step.getCommandMetadata(application(), null).get(RunJobStepState.JOB));
        assertThat(job.get("binds")).extracting(JsonNode::asText).containsExactly("plain-vol:/mnt");
        assertThat(job.has("service")).isFalse();
    }

    private static AgentApplication application() throws Exception {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(MAPPER.readTree(COMPOSE));
        AgentApplication app = new AgentApplication();
        app.setConfig(config);
        return app;
    }
}

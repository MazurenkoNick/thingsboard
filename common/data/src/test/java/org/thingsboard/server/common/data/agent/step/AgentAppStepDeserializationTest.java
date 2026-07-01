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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.step.state.RunJobStepState;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAppStepDeserializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Mirrors the RUN_JOB step in template-EDGE-DOCKER_COMPOSE-4.3.0EDGEPE.json.
    private static final String RUN_JOB_STEP = """
            {
              "id": "580e8400-e29b-41d4-a716-446655440014",
              "type": "RUN_JOB",
              "templateOnly": false,
              "state": {
                "image": { "value": "thingsboard/tb-edge-pe:4.3.0EDGEPE", "userChoice": false },
                "binds": { "value": ["${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}"], "userChoice": false },
                "env": { "value": ["${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).environment}"], "userChoice": false },
                "networkFromServiceImageRegexPattern": { "value": "postgres:.+", "userChoice": false }
              }
            }
            """;

    // Mirrors the COMPOSE upgrade step in the same template.
    private static final String COMPOSE_STEP = """
            {
              "id": "580e8400-e29b-41d4-a716-446655440013",
              "type": "COMPOSE",
              "templateOnly": false,
              "state": {
                "serviceImageRegexPatterns": { "value": ["postgres:.+"], "userChoice": false }
              }
            }
            """;

    @Test
    void runJobStateBindsToTypedFields() throws Exception {
        AgentAppStep step = MAPPER.readValue(RUN_JOB_STEP, AgentAppStep.class);
        assertThat(step).isInstanceOf(RunJobStep.class);
        RunJobStepState state = ((RunJobStep) step).getState();
        assertThat(state.getImage().getValue()).isEqualTo("thingsboard/tb-edge-pe:4.3.0EDGEPE");
        assertThat(state.getBinds().getValue()).containsExactly("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}");
        assertThat(state.getEnv().getValue()).containsExactly("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).environment}");
        assertThat(state.getNetworkFromServiceImageRegexPattern().getValue()).isEqualTo("postgres:.+");
        assertThat(state.getImage().isUserChoice()).isFalse();
    }

    @Test
    void composeServiceRegexBindsViaJsonProperty() throws Exception {
        AgentAppStep step = MAPPER.readValue(COMPOSE_STEP, AgentAppStep.class);
        assertThat(step).isInstanceOf(ComposeStep.class);
        assertThat(((ComposeStep) step).getState().getServicesImagesRegexPatterns().getValue())
                .containsExactly("postgres:.+");
    }
}

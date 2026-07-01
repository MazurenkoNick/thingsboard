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
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentComposeRefUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String COMPOSE = """
            {
              "services": {
                "mytbedge": {
                  "image": "thingsboard/tb-edge-pe:4.3.0EDGEPE",
                  "environment": { "SPRING_DATASOURCE_URL": "jdbc:postgresql://postgres:5432/tb", "TB_QUEUE_TYPE": "in-memory" },
                  "volumes": ["tb-edge-data:/data", "tb-edge-logs:/var/log/tb-edge"]
                },
                "postgres": {
                  "image": "postgres:16"
                }
              }
            }
            """;

    private static JsonNode compose() {
        try {
            return MAPPER.readTree(COMPOSE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void isComposeRefRecognizesOnlyComposeRefs() {
        assertThat(AgentComposeRefUtils.isComposeRef("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}")).isTrue();
        assertThat(AgentComposeRefUtils.isComposeRef("${compose.services.postgres.image}")).isTrue();
        assertThat(AgentComposeRefUtils.isComposeRef("tb-edge-data:/data")).isFalse();
        assertThat(AgentComposeRefUtils.isComposeRef("${tb.device_uuid}")).isFalse();
        assertThat(AgentComposeRefUtils.isComposeRef(null)).isFalse();
    }

    @Test
    void resolvesServiceVolumesArrayViaImageRegex() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}"))
                .containsExactly("tb-edge-data:/data", "tb-edge-logs:/var/log/tb-edge");
    }

    @Test
    void resolvesServiceEnvironmentMapAsKeyValueList() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).environment}"))
                .containsExactlyInAnyOrder("SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/tb", "TB_QUEUE_TYPE=in-memory");
    }

    @Test
    void resolvesScalarViaPlainFieldNavigation() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.services.postgres.image}"))
                .containsExactly("postgres:16");
    }

    @Test
    void unmatchedRegexResolvesToEmptyListFailOpen() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.svcImgRegex(nope:.+).volumes}")).isEmpty();
    }

    @Test
    void missingPropertyResolvesToEmptyList() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.svcImgRegex(postgres:.+).volumes}")).isEmpty();
    }

    @Test
    void nonRefResolvesToNull() {
        assertThat(AgentComposeRefUtils.resolve(compose(), "tb-edge-data:/data")).isNull();
    }
}

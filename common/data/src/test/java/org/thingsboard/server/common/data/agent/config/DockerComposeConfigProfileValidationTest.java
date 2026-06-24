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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.exception.DataValidationException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockerComposeConfigProfileValidationTest {

    @Test
    void generic_noValidation() {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JsonNodeFactory.instance.objectNode());

        assertThatNoException().isThrownBy(() -> config.validateForProfile(AgentApplicationType.GENERIC));
    }

    // ==================== EDGE ====================

    @Test
    void edge_allKeysPresent_passes() {
        DockerComposeConfig config = createEdgeConfig("placeholder", "placeholder", "placeholder");

        assertThatNoException().isThrownBy(() -> config.validateForProfile(AgentApplicationType.EDGE));
    }

    @Test
    void edge_missingRoutingKey_throws() {
        ObjectNode env = JsonNodeFactory.instance.objectNode()
                .put("CLOUD_ROUTING_SECRET", "placeholder")
                .put("CLOUD_RPC_HOST", "placeholder");
        DockerComposeConfig config = createConfigWithService("thingsboard/tb-edge-pe:3.8.0", env);

        assertThatThrownBy(() -> config.validateForProfile(AgentApplicationType.EDGE))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("CLOUD_ROUTING_KEY");
    }

    @Test
    void edge_missingAllKeys_listsAll() {
        ObjectNode env = JsonNodeFactory.instance.objectNode();
        DockerComposeConfig config = createConfigWithService("thingsboard/tb-edge-pe:3.8.0", env);

        assertThatThrownBy(() -> config.validateForProfile(AgentApplicationType.EDGE))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("CLOUD_ROUTING_KEY")
                .hasMessageContaining("CLOUD_ROUTING_SECRET")
                .hasMessageContaining("CLOUD_RPC_HOST");
    }

    @Test
    void edge_noMatchingService_throws() {
        ObjectNode env = JsonNodeFactory.instance.objectNode().put("CLOUD_ROUTING_KEY", "x");
        DockerComposeConfig config = createConfigWithService("nginx:latest", env);

        assertThatThrownBy(() -> config.validateForProfile(AgentApplicationType.EDGE))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("image pattern");
    }

    // ==================== GATEWAY — accessToken ====================

    @Test
    void gateway_accessToken_allKeysPresent_passes() {
        ObjectNode env = JsonNodeFactory.instance.objectNode()
                .put("TB_GW_SECURITY_TYPE", "accessToken")
                .put("TB_GW_ACCESS_TOKEN", "placeholder");
        DockerComposeConfig config = createConfigWithService("thingsboard/tb-gateway:3.8.0", env);

        assertThatNoException().isThrownBy(() -> config.validateForProfile(AgentApplicationType.GATEWAY));
    }

    @Test
    void gateway_accessToken_missingToken_throws() {
        ObjectNode env = JsonNodeFactory.instance.objectNode()
                .put("TB_GW_SECURITY_TYPE", "accessToken");
        DockerComposeConfig config = createConfigWithService("thingsboard/tb-gateway:3.8.0", env);

        assertThatThrownBy(() -> config.validateForProfile(AgentApplicationType.GATEWAY))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("TB_GW_ACCESS_TOKEN");
    }

    // ==================== GATEWAY — usernamePassword ====================

    @Test
    void gateway_usernamePassword_allKeysPresent_passes() {
        ObjectNode env = JsonNodeFactory.instance.objectNode()
                .put("TB_GW_SECURITY_TYPE", "usernamePassword")
                .put("TB_GW_CLIENT_ID", "placeholder")
                .put("TB_GW_USERNAME", "placeholder")
                .put("TB_GW_PASSWORD", "placeholder");
        DockerComposeConfig config = createConfigWithService("thingsboard/tb-gateway:3.8.0", env);

        assertThatNoException().isThrownBy(() -> config.validateForProfile(AgentApplicationType.GATEWAY));
    }

    @Test
    void gateway_usernamePassword_missingClientId_throws() {
        ObjectNode env = JsonNodeFactory.instance.objectNode()
                .put("TB_GW_SECURITY_TYPE", "usernamePassword")
                .put("TB_GW_USERNAME", "placeholder")
                .put("TB_GW_PASSWORD", "placeholder");
        DockerComposeConfig config = createConfigWithService("thingsboard/tb-gateway:3.8.0", env);

        assertThatThrownBy(() -> config.validateForProfile(AgentApplicationType.GATEWAY))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("TB_GW_CLIENT_ID");
    }

    // ==================== GATEWAY — unsupported type ====================

    @Test
    void gateway_unsupportedSecurityType_throws() {
        ObjectNode env = JsonNodeFactory.instance.objectNode()
                .put("TB_GW_SECURITY_TYPE", "x509");
        DockerComposeConfig config = createConfigWithService("thingsboard/tb-gateway:3.8.0", env);

        assertThatThrownBy(() -> config.validateForProfile(AgentApplicationType.GATEWAY))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Unsupported gateway security type");
    }

    @Test
    void gateway_missingSecurityType_throws() {
        ObjectNode env = JsonNodeFactory.instance.objectNode()
                .put("TB_GW_ACCESS_TOKEN", "placeholder");
        DockerComposeConfig config = createConfigWithService("thingsboard/tb-gateway:3.8.0", env);

        assertThatThrownBy(() -> config.validateForProfile(AgentApplicationType.GATEWAY))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("TB_GW_SECURITY_TYPE");
    }

    // ==================== Helpers ====================

    private DockerComposeConfig createEdgeConfig(String routingKey, String routingSecret, String rpcHost) {
        ObjectNode env = JsonNodeFactory.instance.objectNode()
                .put("CLOUD_ROUTING_KEY", routingKey)
                .put("CLOUD_ROUTING_SECRET", routingSecret)
                .put("CLOUD_RPC_HOST", rpcHost);
        return createConfigWithService("thingsboard/tb-edge-pe:3.8.0", env);
    }

    private DockerComposeConfig createConfigWithService(String image, ObjectNode env) {
        ObjectNode service = JsonNodeFactory.instance.objectNode();
        service.put("image", image);
        service.set("environment", env);

        ObjectNode services = JsonNodeFactory.instance.objectNode();
        services.set("main", service);

        ObjectNode compose = JsonNodeFactory.instance.objectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        return config;
    }
}

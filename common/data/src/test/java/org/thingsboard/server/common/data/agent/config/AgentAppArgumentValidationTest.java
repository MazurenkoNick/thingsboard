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

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentAppArgumentValidationTest {

    @Test
    void acceptsValidArguments() {
        DockerComposeConfig config = configWith(
                argument("device_uuid", AgentAppArgumentSource.RELATED_ENTITY, AgentAppArgumentValueType.ATTRIBUTE, "id"));
        assertThatNoException().isThrownBy(config::validateArguments);
    }

    @Test
    void rejectsDuplicateNames() {
        DockerComposeConfig config = configWith(
                argument("dup", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, "a"),
                argument("dup", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, "b"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void rejectsInvalidName() {
        DockerComposeConfig config = configWith(
                argument("bad name", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, "a"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    void rejectsMissingKey() {
        DockerComposeConfig config = configWith(
                argument("name", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, null));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("key");
    }

    @Test
    void acceptsConcreteSourceWithMatchingEntityId() {
        AgentAppArgument argument = argument("dev", AgentAppArgumentSource.DEVICE, AgentAppArgumentValueType.ATTRIBUTE, "k");
        argument.setSourceEntityId(new DeviceId(UUID.randomUUID()));
        DockerComposeConfig config = configWith(argument);
        assertThatNoException().isThrownBy(config::validateArguments);
    }

    @Test
    void rejectsConcreteSourceWithoutEntityId() {
        DockerComposeConfig config = configWith(
                argument("dev", AgentAppArgumentSource.DEVICE, AgentAppArgumentValueType.ATTRIBUTE, "k"));
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("source entity id");
    }

    @Test
    void rejectsEntityIdTypeMismatch() {
        AgentAppArgument argument = argument("dev", AgentAppArgumentSource.DEVICE, AgentAppArgumentValueType.ATTRIBUTE, "k");
        argument.setSourceEntityId(new AssetId(UUID.randomUUID()));
        DockerComposeConfig config = configWith(argument);
        assertThatThrownBy(config::validateArguments)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("type must be");
    }

    private DockerComposeConfig configWith(AgentAppArgument... arguments) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setArguments(List.of(arguments));
        return config;
    }

    private AgentAppArgument argument(String name, AgentAppArgumentSource source,
                                      AgentAppArgumentValueType valueType, String key) {
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName(name);
        argument.setSourceType(source);
        argument.setValueType(valueType);
        argument.setScope(AttributeScope.SERVER_SCOPE);
        argument.setKey(key);
        return argument;
    }

}

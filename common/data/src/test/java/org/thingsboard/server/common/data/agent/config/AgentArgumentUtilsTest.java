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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentArgumentUtilsTest {

    @Test
    void substitutesKnownPlaceholder() {
        String compose = "{\"environment\":{\"DEVICE_ID\":\"${tb.device_uuid}\"}}";
        String result = AgentArgumentUtils.substitute(compose, Map.of("device_uuid", "abc-123"), null);
        assertThat(result).isEqualTo("{\"environment\":{\"DEVICE_ID\":\"abc-123\"}}");
    }

    @Test
    void leavesNativeDockerVariablesUntouched() {
        String compose = "{\"environment\":{\"PATH\":\"${PATH}\"}}";
        String result = AgentArgumentUtils.substitute(compose, Map.of("device_uuid", "abc-123"), null);
        assertThat(result).isEqualTo(compose);
    }

    @Test
    void leavesUnknownNamespacedPlaceholderUntouched() {
        String compose = "{\"x\":\"${tb.unknown}\"}";
        String result = AgentArgumentUtils.substitute(compose, Map.of("device_uuid", "abc-123"), null);
        assertThat(result).isEqualTo(compose);
    }

    @Test
    void jsonEscapesValue() {
        String compose = "{\"x\":\"${tb.token}\"}";
        String result = AgentArgumentUtils.substitute(compose, Map.of("token", "a\"b\\c"), null);
        assertThat(result).isEqualTo("{\"x\":\"a\\\"b\\\\c\"}");
    }

    @Test
    void substitutesMultiplePlaceholders() {
        String compose = "{\"a\":\"${tb.one}\",\"b\":\"${tb.two}\"}";
        String result = AgentArgumentUtils.substitute(compose, Map.of("one", "1", "two", "2"), null);
        assertThat(result).isEqualTo("{\"a\":\"1\",\"b\":\"2\"}");
    }

    @Test
    void jsonFormatInjectsRawArrayForWholeValue() {
        String compose = "{\"services\":{\"edge\":{\"ports\":\"${tb.edge_ports}\"}}}";
        String value = "[\"18080:8080\",\"11883:1883\"]";
        String result = AgentArgumentUtils.substitute(compose, Map.of("edge_ports", value),
                List.of(arg("edge_ports", AgentAppArgumentFormat.JSON)));
        assertThat(result).isEqualTo("{\"services\":{\"edge\":{\"ports\":[\"18080:8080\",\"11883:1883\"]}}}");
    }

    @Test
    void jsonFormatFallsBackToQuotedStringWhenNotJson() {
        String compose = "{\"x\":\"${tb.v}\"}";
        String result = AgentArgumentUtils.substitute(compose, Map.of("v", "not-json"),
                List.of(arg("v", AgentAppArgumentFormat.JSON)));
        assertThat(result).isEqualTo("{\"x\":\"not-json\"}");
    }

    @Test
    void jsonFormatEmbeddedPlaceholderStaysString() {
        String compose = "{\"image\":\"repo:${tb.tag}\"}";
        String result = AgentArgumentUtils.substitute(compose, Map.of("tag", "1.0"),
                List.of(arg("tag", AgentAppArgumentFormat.JSON)));
        assertThat(result).isEqualTo("{\"image\":\"repo:1.0\"}");
    }

    @Test
    void stringFormatWholeValueStaysQuotedEvenWhenJsonLike() {
        String compose = "{\"x\":\"${tb.v}\"}";
        String result = AgentArgumentUtils.substitute(compose, Map.of("v", "[\"a\"]"),
                List.of(arg("v", AgentAppArgumentFormat.STRING)));
        assertThat(result).isEqualTo("{\"x\":\"[\\\"a\\\"]\"}");
    }

    @Test
    void returnsContentUnchangedWhenNoArguments() {
        String compose = "{\"x\":\"${tb.one}\"}";
        assertThat(AgentArgumentUtils.substitute(compose, Map.of(), null)).isEqualTo(compose);
        assertThat(AgentArgumentUtils.substitute(compose, null, null)).isEqualTo(compose);
    }

    @Test
    void handlesNullAndEmptyContent() {
        assertThat(AgentArgumentUtils.substitute(null, Map.of("one", "1"), null)).isNull();
        assertThat(AgentArgumentUtils.substitute("", Map.of("one", "1"), null)).isEmpty();
    }

    private AgentAppArgument arg(String name, AgentAppArgumentFormat format) {
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName(name);
        argument.setFormat(format);
        return argument;
    }

}

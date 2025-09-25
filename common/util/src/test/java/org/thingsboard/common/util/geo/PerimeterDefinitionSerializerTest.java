/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.common.util.geo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;

public class PerimeterDefinitionSerializerTest {

    @Test
    void shouldSerializeCircle() {
        PerimeterDefinition circle = new CirclePerimeterDefinition(50.45, 30.52, 120.0);

        String json = JacksonUtil.writeValueAsString(circle);

        JsonNode actual = JacksonUtil.toJsonNode(json);
        assertThat(actual.get("latitude").asDouble()).isEqualTo(50.45);
        assertThat(actual.get("longitude").asDouble()).isEqualTo(30.52);
        assertThat(actual.get("radius").asDouble()).isEqualTo(120.0);
    }

    @Test
    void shouldSerializePolygon() throws Exception {
        String rawArray = "[[50.45,30.52],[50.46,30.53],[50.44,30.54]]";
        PerimeterDefinition polygon = new PolygonPerimeterDefinition(rawArray);

        String json = JacksonUtil.writeValueAsString(polygon);

        JsonNode actual = JacksonUtil.toJsonNode(json);
        JsonNode expected = JacksonUtil.toJsonNode(rawArray);
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.isArray()).isTrue();
        assertThat(actual.size()).isEqualTo(3);
    }

}

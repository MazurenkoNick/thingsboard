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

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;

public class PerimeterDefinitionDeserializerTest {

    @Test
    void shouldDeserializeCircle() {
        String json = """
                {"latitude":50.45,"longitude":30.52,"radius":100.0}""";

        PerimeterDefinition def = JacksonUtil.fromString(json, PerimeterDefinition.class);

        assertThat(def).isNotNull().isInstanceOf(CirclePerimeterDefinition.class);

        CirclePerimeterDefinition circle = (CirclePerimeterDefinition) def;
        assertThat(circle.getLatitude()).isEqualTo(50.45);
        assertThat(circle.getLongitude()).isEqualTo(30.52);
        assertThat(circle.getRadius()).isEqualTo(100.0);
    }

    @Test
    void shouldDeserializePolygon() {
        String json = "[[50.45,30.52],[50.46,30.53],[50.44,30.54]]";

        PerimeterDefinition def = JacksonUtil.fromString(json, PerimeterDefinition.class);

        assertThat(def).isInstanceOf(PolygonPerimeterDefinition.class);
        PolygonPerimeterDefinition poly = (PolygonPerimeterDefinition) def;
        assertThat(poly.getPolygonDefinition()).isEqualTo(json);
    }
}

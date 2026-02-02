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
package org.thingsboard.edge.rpc;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeVersionComparatorTest {

    @Test
    void compare_sameVersion_returnsZero() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_3_0, EdgeVersion.V_3_3_0)).isEqualTo(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_0_0, EdgeVersion.V_4_0_0)).isEqualTo(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_2_1_2, EdgeVersion.V_4_2_1_2)).isEqualTo(0);
    }

    @Test
    void compare_majorVersionDifference_returnsCorrectOrder() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_3_0, EdgeVersion.V_4_0_0)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_0_0, EdgeVersion.V_3_3_0)).isGreaterThan(0);
    }

    @Test
    void compare_minorVersionDifference_returnsCorrectOrder() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_3_0, EdgeVersion.V_3_6_0)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_0, EdgeVersion.V_3_3_0)).isGreaterThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_0_0, EdgeVersion.V_4_1_0)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_1_0, EdgeVersion.V_4_0_0)).isGreaterThan(0);
    }

    @Test
    void compare_patchVersionDifference_returnsCorrectOrder() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_0, EdgeVersion.V_3_6_1)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_1, EdgeVersion.V_3_6_0)).isGreaterThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_1, EdgeVersion.V_3_6_2)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_3_6_2, EdgeVersion.V_3_6_4)).isLessThan(0);
    }

    @Test
    void compare_fourPartVersion_returnsCorrectOrder() {
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_2_0, EdgeVersion.V_4_2_1_2)).isLessThan(0);
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_2_1_2, EdgeVersion.V_4_2_0)).isGreaterThan(0);
    }

    @Test
    void compare_threePartVsFourPart_treatsImplicitZero() {
        // V_4_2_0 should be less than V_4_2_1_2 (4.2.0.0 < 4.2.1.2)
        assertThat(EdgeVersionComparator.INSTANCE.compare(EdgeVersion.V_4_2_0, EdgeVersion.V_4_2_1_2)).isLessThan(0);
    }

    @Test
    void getNewestEdgeVersion_excludesLatestAndUnrecognized() {
        EdgeVersion newest = EdgeVersionComparator.getNewestEdgeVersion();
        assertThat(newest).isNotNull();
        assertThat(newest).isNotEqualTo(EdgeVersion.V_LATEST);
        assertThat(newest).isNotEqualTo(EdgeVersion.UNRECOGNIZED);
    }
}

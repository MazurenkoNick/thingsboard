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
package org.thingsboard.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NumberUtilsTest {

    private final Float floatVal = 29.29824f;
    private final double doubleVal = 1729.1729;

    @Test
    public void isNaN() {
        assertThat(NumberUtils.isNaN(doubleVal)).isFalse();
        assertThat(NumberUtils.isNaN(Double.NaN)).isTrue();
    }

    @Test
    public void toFixedFloat() {
        float actualF = NumberUtils.toFixed(floatVal, 3);
        assertThat(Float.compare(floatVal, actualF)).isEqualTo(1);
        assertThat(Float.compare(29.298f, actualF)).isEqualTo(0);
    }

    @Test
    public void toFixedDouble() {
        double actualD = NumberUtils.toFixed(doubleVal, 3);
        assertThat(Double.compare(doubleVal, actualD)).isEqualTo(-1);
        assertThat(Double.compare(1729.173, actualD)).isEqualTo(0);
    }

    @Test
    public void toInt() {
        assertThat(NumberUtils.toInt(doubleVal)).isEqualTo(1729);
        assertThat(NumberUtils.toInt(12.8)).isEqualTo(13);
        assertThat(NumberUtils.toInt(28.0)).isEqualTo(28);
    }

    @Test
    public void roundResult() {
        assertThat(NumberUtils.roundResult(doubleVal, null)).isEqualTo(1729.1729);
        assertThat(NumberUtils.roundResult(doubleVal, 0)).isEqualTo(1729);
        assertThat(NumberUtils.roundResult(doubleVal, 2)).isEqualTo(1729.17);
    }

}

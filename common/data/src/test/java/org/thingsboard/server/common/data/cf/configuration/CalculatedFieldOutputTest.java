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
package org.thingsboard.server.common.data.cf.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class CalculatedFieldOutputTest {

    @Test
    public void testHasContextOnlyChanges_whenTypeChanged_shouldReturnTrue() {
        TimeSeriesOutput output = new TimeSeriesOutput();
        AttributesOutput newOutput = new AttributesOutput();

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenNameChanged_shouldReturnTrue() {
        TimeSeriesOutput output = new TimeSeriesOutput();
        TimeSeriesOutput newOutput = new TimeSeriesOutput();
        newOutput.setName("new");

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenScopeChanged_shouldReturnTrue() {
        AttributesOutput output = new AttributesOutput();
        output.setScope(AttributeScope.SHARED_SCOPE);
        AttributesOutput newOutput = new AttributesOutput();
        newOutput.setScope(AttributeScope.SERVER_SCOPE);

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenDecimalsByDefaultChanged_shouldReturnTrue() {
        AttributesOutput output = new AttributesOutput();
        AttributesOutput newOutput = new AttributesOutput();
        newOutput.setDecimalsByDefault(2);

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenStrategyHasContextOnlyChanges_shouldReturnTrue() {
        AttributesOutputStrategy outputStrategy = mock(AttributesRuleChainOutputStrategy.class);
        given(outputStrategy.hasContextOnlyChanges(any())).willReturn(true);

        AttributesOutput output = new AttributesOutput();
        output.setStrategy(outputStrategy);
        AttributesOutput newOutput = new AttributesOutput();

        assertThat(output.hasContextOnlyChanges(newOutput)).isTrue();
    }

    @Test
    public void testHasContextOnlyChanges_whenStrategyDoesNotHaveContextOnlyChanges_shouldReturnTrue() {
        AttributesOutputStrategy outputStrategy = mock(AttributesRuleChainOutputStrategy.class);
        given(outputStrategy.hasContextOnlyChanges(any())).willReturn(false);

        AttributesOutput output = new AttributesOutput();
        output.setStrategy(outputStrategy);
        AttributesOutput newOutput = new AttributesOutput();

        assertThat(output.hasContextOnlyChanges(newOutput)).isFalse();
    }

    /* <strategy>.hasContextOnlyChanges() tests*/

    @Test
    public void testAttributesImmediateOutputStrategyHasContextOnlyChanges_whenTypeChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, true, false, true, true);
        AttributesRuleChainOutputStrategy newStrategy = new AttributesRuleChainOutputStrategy();

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesImmediateOutputStrategyHasContextOnlyChanges_whenSaveAttributesChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, true, false, true, true);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesImmediateOutputStrategyHasContextOnlyChanges_whenSendWsUpdateChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, true, true, false, true);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesImmediateOutputStrategyHasContextOnlyChanges_whenProcessCfsChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, true, true, false, false);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, false, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesRuleChainOutputStrategyHasContextOnlyChanges_whenTypeChanged_shouldReturnTrue() {
        AttributesRuleChainOutputStrategy strategy = new AttributesRuleChainOutputStrategy();
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, false, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasContextOnlyChanges_whenTypeChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, false, true, true, true);
        TimeSeriesRuleChainOutputStrategy newStrategy = new TimeSeriesRuleChainOutputStrategy();

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasContextOnlyChanges_whenSaveLatestChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, false, false, true, true);
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(0, false, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasContextOnlyChanges_whenSendWsUpdateChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, true, true, false, true);
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasContextOnlyChanges_whenProcessCfsChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, false);
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesRuleChainOutputStrategyHasContextOnlyChanges_whenProcessCfsChanged_shouldReturnTrue() {
        TimeSeriesRuleChainOutputStrategy strategy = new TimeSeriesRuleChainOutputStrategy();
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, false);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isTrue();
    }

    /* <strategy>.hasRefreshContextOnlyChanges() tests*/

    @Test
    public void testAttributesImmediateOutputStrategyHasRefreshContextOnlyChanges_whenUpdateAttrOnValueChangedChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(true, false, true, false, true);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, false, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isFalse();
        assertThat(strategy.hasRefreshContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testAttributesImmediateOutputStrategyHasRefreshContextOnlyChanges_whenSendAttrUpdatedNotificationChanged_shouldReturnTrue() {
        AttributesImmediateOutputStrategy strategy = new AttributesImmediateOutputStrategy(false, true, true, false, true);
        AttributesImmediateOutputStrategy newStrategy = new AttributesImmediateOutputStrategy(true, true, true, false, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isFalse();
        assertThat(strategy.hasRefreshContextOnlyChanges(newStrategy)).isTrue();
    }

    @Test
    public void testTimeSeriesImmediateOutputStrategyHasRefreshContextOnlyChanges_whenTtlChanged_shouldReturnTrue() {
        TimeSeriesImmediateOutputStrategy strategy = new TimeSeriesImmediateOutputStrategy(0, true, true, true, true);
        TimeSeriesImmediateOutputStrategy newStrategy = new TimeSeriesImmediateOutputStrategy(300, true, true, true, true);

        assertThat(strategy.hasContextOnlyChanges(newStrategy)).isFalse();
        assertThat(strategy.hasRefreshContextOnlyChanges(newStrategy)).isTrue();
    }

}

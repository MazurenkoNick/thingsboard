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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.tbel.DefaultTbelInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.stats.DefaultStatsFactory;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.service.cf.PropagationCalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationCalculatedFieldState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;

@SpringBootTest(classes = {SimpleMeterRegistry.class, DefaultStatsFactory.class, DefaultTbelInvokeService.class})
public class PropagationCalculatedFieldStateTest {

    private static final String TEMPERATURE_ARGUMENT_NAME = "t";
    private static final String TEST_RESULT_EXPRESSION_KEY = "testResult";
    private static final double TEMPERATURE_VALUE = 12.5;

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("6c3513cb-85e7-4510-8746-1ba01859a8ce"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("be960a50-c029-4698-b2ec-c56a543c561c"));
    private final AssetId ASSET_ID_1 = new AssetId(UUID.fromString("d26f0e5b-7d7d-4a61-9f5e-08ab97b30734"));
    private final AssetId ASSET_ID_2 = new AssetId(UUID.fromString("1933a317-4df5-4d36-9800-68aded74579b"));

    private final SingleValueArgumentEntry singleValueArgEntry =
            new SingleValueArgumentEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", TEMPERATURE_VALUE), 99L);

    private final PropagationArgumentEntry propagationArgEntry =
            new PropagationArgumentEntry(new ArrayList<>(List.of(ASSET_ID_2, ASSET_ID_1)));

    private PropagationCalculatedFieldState state;
    private CalculatedFieldCtx ctx;

    @Autowired
    private TbelInvokeService tbelInvokeService;

    @MockitoBean
    private ApiLimitService apiLimitService;

    @MockitoBean
    private ActorSystemContext actorSystemContext;

    @BeforeEach
    void setUp() {
        when(actorSystemContext.getTbelInvokeService()).thenReturn(tbelInvokeService);
        when(actorSystemContext.getApiLimitService()).thenReturn(apiLimitService);
        when(apiLimitService.getLimit(any(), any())).thenReturn(1000L);
    }

    void initCtxAndState(boolean applyExpressionToResolvedArguments) {
        ctx = new CalculatedFieldCtx(getCalculatedField(applyExpressionToResolvedArguments), actorSystemContext);
        ctx.init();

        state = new PropagationCalculatedFieldState(ctx.getEntityId());
        state.setCtx(ctx, null);
        state.init(false);
    }

    @Test
    void testType() {
        initCtxAndState(false);
        assertThat(state.getType()).isEqualTo(CalculatedFieldType.PROPAGATION);
    }

    @Test
    void testInitAddsRequiredArgument() {
        initCtxAndState(false);
        assertThat(state.getRequiredArguments()).containsExactlyInAnyOrder(TEMPERATURE_ARGUMENT_NAME, PROPAGATION_CONFIG_ARGUMENT);
    }

    @Test
    void testIsReadyReturnFalseWhenNoArgumentsSet() {
        initCtxAndState(false);
        assertThat(state.isReady()).isFalse();
    }

    @Test
    void testIsReadyWhenPropagationArgIsNull() {
        initCtxAndState(false);
        state.update(Map.of(TEMPERATURE_ARGUMENT_NAME, singleValueArgEntry), ctx);
        assertThat(state.isReady()).isFalse();
        assertThat(state.getReadinessStatus().errorMsg()).contains(PROPAGATION_CONFIG_ARGUMENT);
    }

    @Test
    void testIsReadyWhenPropagationArgIsEmpty() {
        initCtxAndState(false);
        state.update(Map.of(TEMPERATURE_ARGUMENT_NAME, singleValueArgEntry,
                PROPAGATION_CONFIG_ARGUMENT, new PropagationArgumentEntry(Collections.emptyList())), ctx);
        assertThat(state.isReady()).isFalse();
        assertThat(state.getReadinessStatus().errorMsg()).contains(PROPAGATION_CONFIG_ARGUMENT);
    }

    @Test
    void testIsReadyWhenPropagationArgHasEntities() {
        initCtxAndState(false);
        state.update(Map.of(TEMPERATURE_ARGUMENT_NAME, singleValueArgEntry, PROPAGATION_CONFIG_ARGUMENT, propagationArgEntry), ctx);
        assertThat(state.isReady()).isTrue();
        assertThat(state.getReadinessStatus().errorMsg()).isNull();
    }


    @Test
    void testPerformCalculationWithEmptyPropagationArg() throws Exception {
        initCtxAndState(false);
        state.getArguments().put(PROPAGATION_CONFIG_ARGUMENT, new PropagationArgumentEntry(Collections.emptyList()));

        PropagationCalculatedFieldResult result = performCalculation();

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getPropagationEntityIds()).isNullOrEmpty();
    }

    @Test
    void testPerformCalculationWithArgumentsOnlyMode() throws Exception {
        initCtxAndState(false);
        state.getArguments().put(PROPAGATION_CONFIG_ARGUMENT, propagationArgEntry);
        state.getArguments().put(TEMPERATURE_ARGUMENT_NAME, singleValueArgEntry);

        PropagationCalculatedFieldResult propagationResult = performCalculation();

        assertThat(propagationResult).isNotNull();
        assertThat(propagationResult.isEmpty()).isFalse();
        assertThat(propagationResult.getPropagationEntityIds()).containsExactly(ASSET_ID_2, ASSET_ID_1);

        TelemetryCalculatedFieldResult result = propagationResult.getResult();
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(OutputType.ATTRIBUTES);
        assertThat(result.getScope()).isEqualTo(AttributeScope.SERVER_SCOPE);

        ObjectNode expectedNode = JacksonUtil.newObjectNode();
        JacksonUtil.addKvEntry(expectedNode, singleValueArgEntry.getKvEntryValue(), TEMPERATURE_ARGUMENT_NAME);

        assertThat(result.getResult()).isEqualTo(expectedNode);
    }

    @Test
    void testPerformCalculationWithExpressionResultMode() throws Exception {
        initCtxAndState(true);
        state.getArguments().put(PROPAGATION_CONFIG_ARGUMENT, propagationArgEntry);
        state.getArguments().put(TEMPERATURE_ARGUMENT_NAME, singleValueArgEntry);

        PropagationCalculatedFieldResult propagationResult = performCalculation();

        assertThat(propagationResult).isNotNull();
        assertThat(propagationResult.isEmpty()).isFalse();
        assertThat(propagationResult.getPropagationEntityIds()).containsExactly(ASSET_ID_2, ASSET_ID_1);

        TelemetryCalculatedFieldResult result = propagationResult.getResult();
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(OutputType.ATTRIBUTES);
        assertThat(result.getScope()).isEqualTo(AttributeScope.SERVER_SCOPE);

        ObjectNode expectedNode = JacksonUtil.newObjectNode();
        expectedNode.put(TEST_RESULT_EXPRESSION_KEY, TEMPERATURE_VALUE * 2);

        assertThat(result.getResult()).isEqualTo(expectedNode);
    }

    private CalculatedField getCalculatedField(boolean applyExpressionToResolvedArguments) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(TENANT_ID);
        calculatedField.setEntityId(DEVICE_ID);
        calculatedField.setType(CalculatedFieldType.PROPAGATION);
        calculatedField.setName("Test Propagation CF");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(getCalculatedFieldConfig(applyExpressionToResolvedArguments));
        calculatedField.setVersion(1L);
        return calculatedField;
    }

    private CalculatedFieldConfiguration getCalculatedFieldConfig(boolean applyExpressionToResolvedArguments) {
        var config = new PropagationCalculatedFieldConfiguration();

        config.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));
        config.setApplyExpressionToResolvedArguments(applyExpressionToResolvedArguments);

        Argument temperatureArg = new Argument();
        ReferencedEntityKey tempKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        temperatureArg.setRefEntityKey(tempKey);

        config.setArguments(Map.of(TEMPERATURE_ARGUMENT_NAME, temperatureArg));
        config.setExpression("{" + TEST_RESULT_EXPRESSION_KEY + ": " + TEMPERATURE_ARGUMENT_NAME + " * 2}");

        Output output = new Output();
        output.setType(OutputType.ATTRIBUTES);
        output.setScope(AttributeScope.SERVER_SCOPE);
        config.setOutput(output);

        return config;
    }

    private PropagationCalculatedFieldResult performCalculation() throws ExecutionException, InterruptedException {
        return (PropagationCalculatedFieldResult) state.performCalculation(Collections.emptyMap(), ctx).get();
    }
}

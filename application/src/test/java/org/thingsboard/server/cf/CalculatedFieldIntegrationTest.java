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
package org.thingsboard.server.cf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.AttributesOutput;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ScriptCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesImmediateOutputStrategy;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.task.CfReprocessingTaskResult;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.controller.AbstractWebTest;
import org.thingsboard.server.controller.CalculatedFieldControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.entitiy.cf.CalculatedFieldReprocessingValidator;
import org.thingsboard.server.service.entitiy.cf.CalculatedFieldReprocessingValidator.CfReprocessingValidationResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_ONLY;
import static org.thingsboard.server.common.data.job.JobStatus.PENDING;
import static org.thingsboard.server.common.data.job.JobStatus.QUEUED;
import static org.thingsboard.server.common.data.job.JobStatus.RUNNING;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "queue.tasks.partitioning_strategy=entity",
        "queue.tasks.partitions_per_type=CF_REPROCESSING:24",
        "queue.calculated_fields.pack_processing_timeout=10000"
})
public class CalculatedFieldIntegrationTest extends CalculatedFieldControllerTest {

    public static final int TIMEOUT = 60;
    public static final int POLL_INTERVAL = 1;

    private final String exampleScript =
            "var avgTemperature = temperature.mean(); // Get average temperature\n" +
            "  var temperatureK = (avgTemperature - 32) * (5 / 9) + 273.15; // Convert Fahrenheit to Kelvin\n" +
            "\n" +
            "  // Estimate air pressure based on altitude\n" +
            "  var pressure = 101325 * Math.pow((1 - 2.25577e-5 * altitude), 5.25588);\n" +
            "\n" +
            "  // Air density formula\n" +
            "  var airDensity = pressure / (287.05 * temperatureK);\n" +
            "\n" +
            "  return {\n" +
            "    \"airDensity\": toFixed(airDensity, 2)\n" +
            "  };";

    @SpyBean
    private TimeseriesService timeseriesService;

    @Test
    public void testSimpleCalculatedFieldWhenAllTelemetryPresent() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        postTelemetry(testDevice.getId(), "{\"temperature\":25}");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"deviceTemperature\":40}"));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        argument.setDefaultValue("12"); // not used because real telemetry value in db is present
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("fahrenheitTemp");
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("77.0");
                });

        postTelemetry(testDevice.getId(), "{\"temperature\":30}");

        await().alias("update telemetry -> recalculate state").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });

        AttributesOutput newOutput = new AttributesOutput();
        newOutput.setScope(AttributeScope.SERVER_SCOPE);
        newOutput.setName("temperatureF");
        config.setOutput(newOutput);
        savedCalculatedField.setConfiguration(config);

        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        await().alias("update CF output -> perform calculation with updated output").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode temperatureF = getServerAttributes(testDevice.getId(), "temperatureF");
                    assertThat(temperatureF).isNotNull();
                    assertThat(temperatureF.get(0)).isNotNull();
                    assertThat(temperatureF.get(0).get("value").asText()).isEqualTo("86.0");
                });

        Argument savedArgument = ((SimpleCalculatedFieldConfiguration) savedCalculatedField.getConfiguration()).getArguments().get("T");
        savedArgument.setRefEntityKey(new ReferencedEntityKey("deviceTemperature", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        await().alias("update CF argument -> perform calculation with new argument").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode temperatureF = getServerAttributes(testDevice.getId(), "temperatureF");
                    assertThat(temperatureF).isNotNull();
                    assertThat(temperatureF.get(0).get("value").asText()).isEqualTo("104.0");
                });

        ((SimpleCalculatedFieldConfiguration) savedCalculatedField.getConfiguration()).setExpression("1.8 * T + 32");
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);

        await().alias("update CF expression -> perform calculation with new expression").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode temperatureF = getServerAttributes(testDevice.getId(), "temperatureF");
                    assertThat(temperatureF).isNotNull();
                    assertThat(temperatureF.get(0).get("value").asText()).isEqualTo("104.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenNotAllTelemetryPresent() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("fahrenheitTemp");
        config.setOutput(output);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> state is not ready -> no calculation performed").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").isNull()).isTrue();
                });

        postTelemetry(testDevice.getId(), "{\"temperature\":30}");

        await().alias("update telemetry -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenNotAllTelemetryPresentButDefaultValueIsSet() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        argument.setDefaultValue("12");
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("fahrenheitTemp");
        config.setOutput(output);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation with default value").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("53.6");
                });

        postTelemetry(testDevice.getId(), "{\"temperature\":30}");

        await().alias("update telemetry -> recalculate state").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenEntityIdIsProfile() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"x\":40}"));

        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);

        Asset asset1 = createAsset("Test asset 1", assetProfile.getId());
        doPost("/api/plugins/telemetry/ASSET/" + asset1.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":11}"));

        Asset asset2 = createAsset("Test asset 2", assetProfile.getId());
        doPost("/api/plugins/telemetry/ASSET/" + asset2.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":12}"));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(assetProfile.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("z = x + y");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument1 = new Argument();
        ReferencedEntityKey refEntityKey1 = new ReferencedEntityKey("y", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE);
        argument1.setRefEntityKey(refEntityKey1);

        Argument argument2 = new Argument();
        argument2.setRefEntityId(testDevice.getId());
        ReferencedEntityKey refEntityKey2 = new ReferencedEntityKey("x", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE);
        argument2.setRefEntityKey(refEntityKey2);

        config.setArguments(Map.of("x", argument2, "y", argument1));

        config.setExpression("x + y");

        AttributesOutput output = new AttributesOutput();
        output.setName("z");
        output.setScope(AttributeScope.SERVER_SCOPE);

        config.setOutput(output);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);

        doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF and perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("51.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("52.0");
                });

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"x\":25}"));

        await().alias("update device telemetry -> recalculate state for all assets").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("36.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("37.0");
                });

        doPost("/api/plugins/telemetry/ASSET/" + asset1.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":15}"));

        await().alias("update asset 1 telemetry -> recalculate state only for asset 1").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("40.0");

                    // result of asset 2 (no changes)
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("37.0");
                });

        doPost("/api/plugins/telemetry/ASSET/" + asset2.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":5}"));

        await().alias("update asset 2 telemetry -> recalculate state only for asset 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1 (no changes)
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("40.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("30.0");
                });

        Asset asset3 = createAsset("Test asset 3", assetProfile.getId());
        doPost("/api/plugins/telemetry/ASSET/" + asset3.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":13}"));

        Asset finalAsset3 = asset3;
        await().alias("add new entity to profile -> calculate state for new entity").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 3
                    ArrayNode z3 = getServerAttributes(finalAsset3.getId(), "z");
                    assertThat(z3).isNotNull();
                    assertThat(z3.get(0).get("value").asText()).isEqualTo("38.0");
                });

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"x\":20}"));

        await().alias("update device telemetry -> recalculate state for all assets").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("35.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("25.0");

                    // result of asset 3
                    ArrayNode z3 = getServerAttributes(finalAsset3.getId(), "z");
                    assertThat(z3).isNotNull();
                    assertThat(z3.get(0).get("value").asText()).isEqualTo("33.0");
                });

        // update profile for asset 3 -> delete state for asset 3
        AssetProfile newAssetProfile = doPost("/api/assetProfile", createAssetProfile("New Asset Profile"), AssetProfile.class);
        asset3.setAssetProfileId(newAssetProfile.getId());
        asset3 = doPost("/api/asset", asset3, Asset.class);

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"x\":15}"));

        Asset updatedAsset3 = asset3;
        await().alias("update device telemetry -> recalculate state for asset 1 and asset 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // result of asset 1
                    ArrayNode z1 = getServerAttributes(asset1.getId(), "z");
                    assertThat(z1).isNotNull();
                    assertThat(z1.get(0).get("value").asText()).isEqualTo("30.0");

                    // result of asset 2
                    ArrayNode z2 = getServerAttributes(asset2.getId(), "z");
                    assertThat(z2).isNotNull();
                    assertThat(z2.get(0).get("value").asText()).isEqualTo("20.0");

                    // no changes for asset 3
                    ArrayNode z3 = getServerAttributes(updatedAsset3.getId(), "z");
                    assertThat(z3).isNotNull();
                    assertThat(z3.get(0).get("value").asText()).isEqualTo("33.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenExpressionIsInvalid() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        postTelemetry(testDevice.getId(), "{\"temperature\":25}");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        argument.setDefaultValue("12"); // not used because real telemetry value in db is present
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/0) + 32");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("fahrenheitTemp");
        config.setOutput(output);

        calculatedField.setConfiguration(config);
        calculatedField.setVersion(1L);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> ctx is not initialized -> no calculation perform").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").isNull()).isTrue();
                });

        postTelemetry(testDevice.getId(), "{\"temperature\":30}");

        await().alias("update telemetry -> ctx is not initialized -> no calculation perform").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").isNull()).isTrue();
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenUseLatestTsIsTrue() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        long ts = System.currentTimeMillis() - 300000L;
        postTelemetry(testDevice.getId(), String.format("{\"ts\": %s, \"values\": {\"temperature\":30}}", ts));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("fahrenheitTemp");
        config.setOutput(output);

        config.setUseLatestTs(true);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("ts").asText()).isEqualTo(Long.toString(ts));
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenUseLatestTsIsTrueAndTelemetryBeforeLatest() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        long ts = System.currentTimeMillis();

        long tsA = ts - 300000L;
        postTelemetry(testDevice.getId(), String.format("{\"ts\": %s, \"values\": {\"a\":1}}", tsA));

        long tsB = ts - 300L;
        postTelemetry(testDevice.getId(), String.format("{\"ts\": %s, \"values\": {\"b\":5}}", tsB));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("a + b");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument1 = new Argument();
        ReferencedEntityKey refEntityKey1 = new ReferencedEntityKey("a", ArgumentType.TS_LATEST, null);
        argument1.setRefEntityKey(refEntityKey1);
        Argument argument2 = new Argument();
        ReferencedEntityKey refEntityKey2 = new ReferencedEntityKey("b", ArgumentType.TS_LATEST, null);
        argument2.setRefEntityKey(refEntityKey2);
        config.setArguments(Map.of("a", argument1, "b", argument2));
        config.setExpression("a + b");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("c");
        config.setOutput(output);

        config.setUseLatestTs(true);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode c = getLatestTelemetry(testDevice.getId(), "c");
                    assertThat(c).isNotNull();
                    assertThat(c.get("c").get(0).get("ts").asText()).isEqualTo(Long.toString(tsB));
                    assertThat(c.get("c").get(0).get("value").asText()).isEqualTo("6.0");
                });

        long tsABeforeTsB = tsB - 300L;
        postTelemetry(testDevice.getId(), String.format("{\"ts\": %s, \"values\": {\"a\":10}}", tsABeforeTsB));

        await().alias("update telemetry with ts less than latest -> save result with latest ts").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode c = getLatestTelemetry(testDevice.getId(), "c");
                    assertThat(c).isNotNull();
                    assertThat(c.get("c").get(0).get("ts").asText()).isEqualTo(Long.toString(tsB));// also tsB, since this is the latest timestamp
                    assertThat(c.get("c").get(0).get("value").asText()).isEqualTo("15.0");
                });
    }

    @Test
    public void testScriptCalculatedFieldWhenUsedLatestTsInScript() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        long ts = System.currentTimeMillis() - 300000L;
        postTelemetry(testDevice.getId(), String.format("{\"ts\": %s, \"values\": {\"temperature\":30}}", ts));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SCRIPT);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        ScriptCalculatedFieldConfiguration config = new ScriptCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        config.setArguments(Map.of("T", argument));
        config.setExpression("return {\"ts\": ctx.latestTs, \"values\": {\"fahrenheitTemp\": (T * 1.8) + 32}};");

        config.setOutput(new TimeSeriesOutput());

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("ts").asText()).isEqualTo(Long.toString(ts));
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("86.0");
                });
    }

    @Test
    public void testReprocessCalculatedField() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);
        Asset testAsset = createAsset("Test asset", assetProfile.getId());

        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(120);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(45);

        long a1Ts = currentTime - TimeUnit.SECONDS.toMillis(140); // outside the TW
        long a2b1Ts = currentTime - TimeUnit.SECONDS.toMillis(130); // outside the TW (but telemetry will be used for initial processing)
        long b2Ts = currentTime - TimeUnit.SECONDS.toMillis(80); // inside the TW
        long b3Ts = currentTime - TimeUnit.SECONDS.toMillis(70); // inside the TW
        long a3Ts = currentTime - TimeUnit.SECONDS.toMillis(60); // inside the TW
        long a4Ts = currentTime - TimeUnit.SECONDS.toMillis(50); // inside the TW
        long b4Ts = currentTime - TimeUnit.SECONDS.toMillis(40); // outside the TW

        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":1}}", a1Ts)));
        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":2}}", a2b1Ts)));
        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":3}}", a3Ts)));
        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":4}}", a4Ts)));

        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":10}}", a2b1Ts)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":20}}", b2Ts)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":30}}", b3Ts)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":40}}", b4Ts)));

        /*      telemetry flow:
                             startTs                endTs
                               |______________________|
               |  ts  | 1    2 |   3     4     5    6 |   7
               |device| 1 -> 2 |->    ->    -> 3 -> 4 |
               |asset |      10|-> 20 -> 30 ->   ->   |-> 40
                               |______________________|
                                          |--- reprocessing time window
               the result should be: 12 -> 22 -> 32 -> 33 -> 34
        */

        CalculatedField savedCalculatedField = createCalculatedField(testDevice.getId(), testAsset.getId());

        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":10}}", currentTime)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":100}}", currentTime)));

        reprocessCalculatedField(savedCalculatedField, startTs, endTs);

        await().alias("reprocess -> perform calculation for time window").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getTimeSeries(testDevice.getId(), startTs, endTs, "result");
                    assertThat(result).isNotNull();

                    assertThat(result.get("result").get(0).get("ts").asText()).isEqualTo(Long.toString(a4Ts));
                    assertThat(result.get("result").get(0).get("value").asText()).isEqualTo("34.0");

                    assertThat(result.get("result").get(1).get("ts").asText()).isEqualTo(Long.toString(a3Ts));
                    assertThat(result.get("result").get(1).get("value").asText()).isEqualTo("33.0");

                    assertThat(result.get("result").get(2).get("ts").asText()).isEqualTo(Long.toString(b3Ts));
                    assertThat(result.get("result").get(2).get("value").asText()).isEqualTo("32.0");

                    assertThat(result.get("result").get(3).get("ts").asText()).isEqualTo(Long.toString(b2Ts));
                    assertThat(result.get("result").get(3).get("value").asText()).isEqualTo("22.0");

                    assertThat(result.get("result").get(4).get("ts").asText()).isEqualTo(Long.toString(startTs)); // we use reprocessing startTs instead of telemetry ts for initial calculation
                    assertThat(result.get("result").get(4).get("value").asText()).isEqualTo("12.0");

                    ObjectNode resultLatest = getLatestTelemetry(testDevice.getId(), "result");
                    assertThat(resultLatest).isNotNull();
                    assertThat(resultLatest.get("result").get(0).get("value").asText()).isEqualTo("110.0"); // reprocessing result did not overwrite the actual latest value
                });

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = findJobs(List.of(JobType.CF_REPROCESSING), List.of(testDevice.getUuidId())).stream().findFirst().orElseThrow();
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(cfReprocessingJob.getResult().getSuccessfulCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getEntityId()).isEqualTo(testDevice.getId());
            assertThat(cfReprocessingJob.getEntityName()).isEqualTo(testDevice.getName());
        });
    }

    @Test
    public void testReprocessCalculatedFieldWhenUseLatestTsTrue() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");

        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(120);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(45);

        long x1Ts = currentTime - TimeUnit.SECONDS.toMillis(80); // inside the TW
        long x2Ts = currentTime - TimeUnit.SECONDS.toMillis(60); // inside the TW
        long x3y1Ts = currentTime - TimeUnit.SECONDS.toMillis(50); // inside the TW
        long x4Ts = currentTime - TimeUnit.SECONDS.toMillis(47); // inside the TW

        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"x\":1}}", x1Ts)));
        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"x\":2}}", x2Ts)));
        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"x\":3}}", x3y1Ts)));
        pushTelemetry(testDevice.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"x\":4}}", x4Ts)));
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"y\":10}"));

    /*      telemetry flow:
             startTs          endTs
                |____________________|
           | ts |   1    2    3    4 |
           | x  |-> 1 -> 2 -> 3 -> 4 |
           | y  |->   ->   -> 10 ->  |
                |____________________|
                        |--- reprocessing time window
           the result should be: 11 -> 12 -> 13 -> 14
    */

        CalculatedField savedCalculatedField = createCalculatedFieldWhenUseLatestTs(testDevice.getId());

        reprocessCalculatedField(savedCalculatedField, startTs, endTs);

        await().alias("reprocess -> perform calculation for time window").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode z = getTimeSeries(testDevice.getId(), startTs, endTs, "z");
                    assertThat(z).isNotNull();

                    assertThat(z.get("z").get(0).get("ts").asText()).isEqualTo(Long.toString(x4Ts));
                    assertThat(z.get("z").get(0).get("value").asText()).isEqualTo("14");

                    assertThat(z.get("z").get(1).get("ts").asText()).isEqualTo(Long.toString(x3y1Ts));
                    assertThat(z.get("z").get(1).get("value").asText()).isEqualTo("13");

                    assertThat(z.get("z").get(2).get("ts").asText()).isEqualTo(Long.toString(x2Ts));
                    assertThat(z.get("z").get(2).get("value").asText()).isEqualTo("12");

                    assertThat(z.get("z").get(3).get("ts").asText()).isEqualTo(Long.toString(x1Ts));
                    assertThat(z.get("z").get(3).get("value").asText()).isEqualTo("11");
                });

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = findJobs(List.of(JobType.CF_REPROCESSING), List.of(testDevice.getUuidId())).stream().findFirst().orElseThrow();
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(cfReprocessingJob.getResult().getSuccessfulCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getEntityId()).isEqualTo(testDevice.getId());
            assertThat(cfReprocessingJob.getEntityName()).isEqualTo(testDevice.getName());
        });
    }

    @Test
    public void testReprocessCalculatedFieldWhenEntityIsProfile() throws Exception {
        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(120);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(45);

        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);
        Asset testAsset = createAsset("Test asset", assetProfile.getId());
        long aTs_1 = currentTime - TimeUnit.SECONDS.toMillis(100);
        long aTs_2 = currentTime - TimeUnit.SECONDS.toMillis(85);
        long aTs_3 = currentTime - TimeUnit.SECONDS.toMillis(60);
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":100}}", aTs_1)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":200}}", aTs_2)));
        pushTelemetry(testAsset.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"b\":300}}", aTs_3)));

        DeviceProfile deviceProfile = doPost("/api/deviceProfile", createDeviceProfile("Test Device Profile"), DeviceProfile.class);

        Device testDevice1 = createDevice("Test device 1", "1234567890111", deviceProfile.getId());
        long d1Ts_1 = currentTime - TimeUnit.SECONDS.toMillis(100);
        long d1Ts_2 = currentTime - TimeUnit.SECONDS.toMillis(90);
        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":10}}", d1Ts_1)));
        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":20}}", d1Ts_2)));


        Device testDevice2 = createDevice("Test device 2", "1234567890222", deviceProfile.getId());
        long d2Ts_1 = currentTime - TimeUnit.SECONDS.toMillis(90);
        long d2Ts_2 = currentTime - TimeUnit.SECONDS.toMillis(55);
        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":1}}", d2Ts_1)));
        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"a\":2}}", d2Ts_2)));

        /*      telemetry flow:
                     startTs                         endTs
                       |_______________________________|
               |  ts   |   1      2     3      4     5 |
               |device1|  10  -> 20 ->     ->     ->   |
               |device2|      ->  1 ->     ->     -> 2 |
               |asset  |  100 ->    -> 200 -> 300 ->   |
                       |_______________________________|
                                         |--- reprocessing time window
               the result for device 1 should be: 110 -> 120 -> 220 -> 320
               the result for device 2 should be: 101 -> 201 -> 301 -> 302
        */

        CalculatedField savedCalculatedField = createCalculatedField(deviceProfile.getId(), testAsset.getId());

        reprocessCalculatedField(savedCalculatedField, startTs, endTs);

        await().alias("reprocess -> perform calculation for device 1").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode ab_1 = getTimeSeries(testDevice1.getId(), startTs, endTs, "result");
                    assertThat(ab_1).isNotNull();

                    assertThat(ab_1.get("result").get(0).get("ts").asText()).isEqualTo(Long.toString(aTs_3));
                    assertThat(ab_1.get("result").get(0).get("value").asText()).isEqualTo("320.0");

                    assertThat(ab_1.get("result").get(1).get("ts").asText()).isEqualTo(Long.toString(aTs_2));
                    assertThat(ab_1.get("result").get(1).get("value").asText()).isEqualTo("220.0");

                    assertThat(ab_1.get("result").get(2).get("ts").asText()).isEqualTo(Long.toString(d1Ts_2));
                    assertThat(ab_1.get("result").get(2).get("value").asText()).isEqualTo("120.0");

                    assertThat(ab_1.get("result").get(3).get("ts").asText()).isEqualTo(Long.toString(aTs_1));
                    assertThat(ab_1.get("result").get(3).get("value").asText()).isEqualTo("110.0");

                    ObjectNode resultLatest = getLatestTelemetry(testDevice1.getId(), "result");
                    assertThat(resultLatest).isNotNull();
                    assertThat(resultLatest.get("result").get(0).get("value").asText()).isEqualTo("320.0");
                });

        await().alias("reprocess -> perform calculation for device 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode ab_2 = getTimeSeries(testDevice2.getId(), startTs, endTs, "result");
                    assertThat(ab_2).isNotNull();

                    assertThat(ab_2.get("result").get(0).get("ts").asText()).isEqualTo(Long.toString(d2Ts_2));
                    assertThat(ab_2.get("result").get(0).get("value").asText()).isEqualTo("302.0");

                    assertThat(ab_2.get("result").get(1).get("ts").asText()).isEqualTo(Long.toString(aTs_3));
                    assertThat(ab_2.get("result").get(1).get("value").asText()).isEqualTo("301.0");

                    assertThat(ab_2.get("result").get(2).get("ts").asText()).isEqualTo(Long.toString(aTs_2));
                    assertThat(ab_2.get("result").get(2).get("value").asText()).isEqualTo("201.0");

                    assertThat(ab_2.get("result").get(3).get("ts").asText()).isEqualTo(Long.toString(d2Ts_1));
                    assertThat(ab_2.get("result").get(3).get("value").asText()).isEqualTo("101.0");

                    ObjectNode resultLatest = getLatestTelemetry(testDevice2.getId(), "result");
                    assertThat(resultLatest).isNotNull();
                    assertThat(resultLatest.get("result").get(0).get("value").asText()).isEqualTo("302.0");
                });

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = findJobs(List.of(JobType.CF_REPROCESSING), List.of(deviceProfile.getUuidId())).stream().findFirst().orElseThrow();
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(cfReprocessingJob.getResult().getSuccessfulCount()).isEqualTo(2);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(2);
            assertThat(cfReprocessingJob.getEntityId()).isEqualTo(deviceProfile.getId());
            assertThat(cfReprocessingJob.getEntityName()).isEqualTo(deviceProfile.getName());
        });
    }

    @Test
    public void testReprocessCalculatedFieldWhenEntityIsProfileAndTsRollingArgUsed() throws Exception {
        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(1200);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(300);


        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);
        Asset testAsset = createAsset("Test asset", assetProfile.getId());

        doPost("/api/plugins/telemetry/ASSET/" + testAsset.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"altitude\":1531}"));

        DeviceProfile deviceProfile = doPost("/api/deviceProfile", createDeviceProfile("Test Device Profile"), DeviceProfile.class);

        Device testDevice1 = createDevice("Test device 1", "1234567890111", deviceProfile.getId());
        long d1Ts_1 = currentTime - TimeUnit.SECONDS.toMillis(1000);
        long d1Ts_2 = currentTime - TimeUnit.SECONDS.toMillis(550);
        long d1Ts_3 = currentTime - TimeUnit.SECONDS.toMillis(500);

        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":66.12}}", d1Ts_1)));
        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":45.31}}", d1Ts_2)));
        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":70.36}}", d1Ts_3)));


        Device testDevice2 = createDevice("Test device 2", "1234567890222", deviceProfile.getId());
        long d2Ts_1 = currentTime - TimeUnit.SECONDS.toMillis(900);
        long d2Ts_2 = currentTime - TimeUnit.SECONDS.toMillis(550);
        long d2Ts_3 = currentTime - TimeUnit.SECONDS.toMillis(400);

        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":55.16}}", d2Ts_1)));
        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":56.57}}", d2Ts_2)));
        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":59.40}}", d2Ts_3)));


        /*      telemetry flow:
                     startTs                                      endTs
                       |____________________________________________|
               |  ts   |  -1000    -900     -550     -500     -400  |
               |device1|  66.12 ->       -> 45.31 -> 70.36 ->       |
               |device2|        -> 55.16 -> 56.57 ->       -> 59.40 |
               |asset  |        ->       ->       ->       ->       | -> 1531
                       |____________________________________________|
                                         |--- reprocessing time window
               the airDensity for device 1 should be: 1.0 -> 1.05 -> 1.02
               the airDensity for device 2 should be: 1.03 -> 1.02 -> 1.02
        */

        CalculatedField savedCalculatedField = createScriptCalculatedField(deviceProfile.getId(), testAsset.getId());

        reprocessCalculatedField(savedCalculatedField, startTs, endTs);

        await().alias("reprocess -> perform calculation for device 1").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode airDensity = getTimeSeries(testDevice1.getId(), startTs, endTs, "airDensity");
                    assertThat(airDensity).isNotNull();

                    assertThat(airDensity.get("airDensity").get(0).get("ts").asText()).isEqualTo(Long.toString(d1Ts_3));
                    assertThat(airDensity.get("airDensity").get(0).get("value").asText()).isEqualTo("1.02");

                    assertThat(airDensity.get("airDensity").get(1).get("ts").asText()).isEqualTo(Long.toString(d1Ts_2));
                    assertThat(airDensity.get("airDensity").get(1).get("value").asText()).isEqualTo("1.05");

                    assertThat(airDensity.get("airDensity").get(2).get("ts").asText()).isEqualTo(Long.toString(d1Ts_1));
                    assertThat(airDensity.get("airDensity").get(2).get("value").asText()).isEqualTo("1.0");

                    ObjectNode airDensityLatest = getLatestTelemetry(testDevice1.getId(), "airDensity");
                    assertThat(airDensityLatest).isNotNull();
                    assertThat(airDensityLatest.get("airDensity").get(0).get("value").asText()).isEqualTo("1.02");
                });

        await().alias("reprocess -> perform calculation for device 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode airDensity = getTimeSeries(testDevice2.getId(), startTs, endTs, "airDensity");
                    assertThat(airDensity).isNotNull();

                    assertThat(airDensity.get("airDensity").get(0).get("ts").asText()).isEqualTo(Long.toString(d2Ts_3));
                    assertThat(airDensity.get("airDensity").get(0).get("value").asText()).isEqualTo("1.02");

                    assertThat(airDensity.get("airDensity").get(1).get("ts").asText()).isEqualTo(Long.toString(d2Ts_2));
                    assertThat(airDensity.get("airDensity").get(1).get("value").asText()).isEqualTo("1.02");

                    assertThat(airDensity.get("airDensity").get(2).get("ts").asText()).isEqualTo(Long.toString(d2Ts_1));
                    assertThat(airDensity.get("airDensity").get(2).get("value").asText()).isEqualTo("1.03");

                    ObjectNode airDensityLatest = getLatestTelemetry(testDevice2.getId(), "airDensity");
                    assertThat(airDensityLatest).isNotNull();
                    assertThat(airDensityLatest.get("airDensity").get(0).get("value").asText()).isEqualTo("1.02");
                });

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = findJobs(List.of(JobType.CF_REPROCESSING), List.of(deviceProfile.getUuidId())).stream().findFirst().orElseThrow();
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(cfReprocessingJob.getResult().getSuccessfulCount()).isEqualTo(2);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(2);
            assertThat(cfReprocessingJob.getEntityId()).isEqualTo(deviceProfile.getId());
            assertThat(cfReprocessingJob.getEntityName()).isEqualTo(deviceProfile.getName());
        });
    }

    @Test
    public void testReprocessCalculatedFieldWhenEntityIsProfileAndTsRollingArgUsed_failed() throws Exception {
        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(1200);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(300);

        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);
        Asset testAsset = createAsset("Test asset", assetProfile.getId());

        doPost("/api/plugins/telemetry/ASSET/" + testAsset.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"altitude\":1531}"));

        DeviceProfile deviceProfile = doPost("/api/deviceProfile", createDeviceProfile("Test Device Profile"), DeviceProfile.class);

        Device testDevice1 = createDevice("Test device 1", "1234567890111", deviceProfile.getId());
        long d1Ts_1 = currentTime - TimeUnit.SECONDS.toMillis(1000);
        long d1Ts_2 = currentTime - TimeUnit.SECONDS.toMillis(550);
        long d1Ts_3 = currentTime - TimeUnit.SECONDS.toMillis(500);

        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":66.12}}", d1Ts_1)));
        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":45.31}}", d1Ts_2)));
        pushTelemetry(testDevice1.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":70.36}}", d1Ts_3)));


        Device testDevice2 = createDevice("Test device 2", "1234567890222", deviceProfile.getId());
        long d2Ts_1 = currentTime - TimeUnit.SECONDS.toMillis(900);
        long d2Ts_2 = currentTime - TimeUnit.SECONDS.toMillis(550);
        long d2Ts_3 = currentTime - TimeUnit.SECONDS.toMillis(400);

        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":55.16}}", d2Ts_1)));
        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":56.57}}", d2Ts_2)));
        pushTelemetry(testDevice2.getId(), JacksonUtil.toJsonNode(String.format("{\"ts\":%s, \"values\":{\"temperatureInF\":59.40}}", d2Ts_3)));

        CalculatedField savedCalculatedField = createScriptCalculatedField(deviceProfile.getId(), testAsset.getId());

        doReturn(Futures.immediateFailedFuture(new RuntimeException("Failed to fetch timeseries data")))
                .when(timeseriesService).findAll(any(), any(), any());

        reprocessCalculatedField(savedCalculatedField, startTs, endTs);

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = getLastReprocessingJob(savedCalculatedField.getId());
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(cfReprocessingJob.getResult().getFailedCount()).isEqualTo(2);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(2);
            List<CfReprocessingTaskResult.CfReprocessingTaskFailure> failures = cfReprocessingJob.getResult().getResults().stream()
                    .map(result -> (CfReprocessingTaskResult) result)
                    .map(CfReprocessingTaskResult::getFailure)
                    .toList();
            assertThat(failures).hasSize(2);
            assertThat(failures).anySatisfy(failure -> {
                assertThat(failure.getEntityInfo().getId()).isEqualTo(testDevice1.getId());
                assertThat(failure.getEntityInfo().getName()).isEqualTo(testDevice1.getName());
                assertThat(failure.getError()).contains("Failed to fetch timeseries data");
            });
            assertThat(failures).anySatisfy(failure -> {
                assertThat(failure.getEntityInfo().getId()).isEqualTo(testDevice2.getId());
                assertThat(failure.getEntityInfo().getName()).isEqualTo(testDevice2.getName());
                assertThat(failure.getError()).contains("Failed to fetch timeseries data");
            });
        });

        // fixing the method and reprocessing the job
        doCallRealMethod().when(timeseriesService).findAll(any(), any(), any());
        Job job = getLastReprocessingJob(savedCalculatedField.getId());
        reprocessJob(job.getId());

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = findJobs(List.of(JobType.CF_REPROCESSING), List.of(deviceProfile.getUuidId())).stream().findFirst().orElseThrow();
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(cfReprocessingJob.getResult().getSuccessfulCount()).isEqualTo(2);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(2);
            assertThat(cfReprocessingJob.getEntityId()).isEqualTo(deviceProfile.getId());
            assertThat(cfReprocessingJob.getEntityName()).isEqualTo(deviceProfile.getName());
        });
        await().alias("reprocess -> perform calculation for device 1").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode airDensity = getTimeSeries(testDevice1.getId(), startTs, endTs, "airDensity");
                    assertThat(airDensity).isNotNull();

                    assertThat(airDensity.get("airDensity").get(0).get("ts").asText()).isEqualTo(Long.toString(d1Ts_3));
                    assertThat(airDensity.get("airDensity").get(0).get("value").asText()).isEqualTo("1.02");

                    assertThat(airDensity.get("airDensity").get(1).get("ts").asText()).isEqualTo(Long.toString(d1Ts_2));
                    assertThat(airDensity.get("airDensity").get(1).get("value").asText()).isEqualTo("1.05");

                    assertThat(airDensity.get("airDensity").get(2).get("ts").asText()).isEqualTo(Long.toString(d1Ts_1));
                    assertThat(airDensity.get("airDensity").get(2).get("value").asText()).isEqualTo("1.0");
                });

        await().alias("reprocess -> perform calculation for device 2").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode airDensity = getTimeSeries(testDevice2.getId(), startTs, endTs, "airDensity");
                    assertThat(airDensity).isNotNull();

                    assertThat(airDensity.get("airDensity").get(0).get("ts").asText()).isEqualTo(Long.toString(d2Ts_3));
                    assertThat(airDensity.get("airDensity").get(0).get("value").asText()).isEqualTo("1.02");

                    assertThat(airDensity.get("airDensity").get(1).get("ts").asText()).isEqualTo(Long.toString(d2Ts_2));
                    assertThat(airDensity.get("airDensity").get(1).get("value").asText()).isEqualTo("1.02");

                    assertThat(airDensity.get("airDensity").get(2).get("ts").asText()).isEqualTo(Long.toString(d2Ts_1));
                    assertThat(airDensity.get("airDensity").get(2).get("value").asText()).isEqualTo("1.03");
                });
    }

    @Test
    public void testReprocessCalculatedFieldWhenConfigIsNotValid() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("A + 10");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument a = new Argument();
        a.setRefEntityKey(new ReferencedEntityKey("a", ArgumentType.ATTRIBUTE, null));
        config.setArguments(Map.of("a", a));
        config.setExpression("a + 10");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("result");
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        var response = doGet("/api/calculatedField/" + savedCalculatedField.getUuidId() + "/reprocess/validate", CfReprocessingValidationResult.class);

        assertThat(response.isValid()).isFalse();
        assertThat(response.message()).contains(CalculatedFieldReprocessingValidator.NO_TELEMETRY_ARGS);
    }

    @Test
    public void testReprocessCalculatedFieldWhenJobIsRunning() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        AssetProfile assetProfile = doPost("/api/assetProfile", createAssetProfile("Test Asset Profile"), AssetProfile.class);
        Asset testAsset = createAsset("Test asset", assetProfile.getId());

        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(120);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(45);

        CalculatedField savedCalculatedField = createCalculatedField(testDevice.getId(), testAsset.getId());
        reprocessCalculatedField(savedCalculatedField, startTs, endTs);

        var response = doGet("/api/calculatedField/" + savedCalculatedField.getUuidId() + "/reprocess/validate", CfReprocessingValidationResult.class);

        assertThat(response.isValid()).isFalse();
        assertThat(response.lastJobStatus().isOneOf(QUEUED, PENDING, RUNNING)).isTrue();
    }

    private CalculatedField createScriptCalculatedField(EntityId entityId, EntityId refEntityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SCRIPT);
        calculatedField.setName("Air density" + RandomStringUtils.randomAlphabetic(5));
        calculatedField.setDebugSettings(DebugSettings.all());

        ScriptCalculatedFieldConfiguration config = new ScriptCalculatedFieldConfiguration();

        Argument argument1 = new Argument();
        argument1.setRefEntityId(refEntityId);
        ReferencedEntityKey refEntityKey1 = new ReferencedEntityKey("altitude", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE);
        argument1.setRefEntityKey(refEntityKey1);
        Argument argument2 = new Argument();
        ReferencedEntityKey refEntityKey2 = new ReferencedEntityKey("temperatureInF", ArgumentType.TS_ROLLING, null);
        argument2.setTimeWindow(300000L);
        argument2.setLimit(5);
        argument2.setRefEntityKey(refEntityKey2);

        config.setArguments(Map.of("altitude", argument1, "temperature", argument2));

        config.setExpression(exampleScript);

        config.setOutput(new TimeSeriesOutput());

        calculatedField.setConfiguration(config);

        return doPost("/api/calculatedField", calculatedField, CalculatedField.class);
    }

    private CalculatedField createCalculatedField(EntityId entityId, EntityId refEntityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("A + B");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument a = new Argument();
        ReferencedEntityKey refEntityKeyA = new ReferencedEntityKey("a", ArgumentType.TS_LATEST, null);
        a.setRefEntityKey(refEntityKeyA);
        Argument b = new Argument();
        b.setRefEntityId(refEntityId);
        ReferencedEntityKey refEntityKeyB = new ReferencedEntityKey("b", ArgumentType.TS_LATEST, null);
        b.setRefEntityKey(refEntityKeyB);
        config.setArguments(Map.of("a", a, "b", b));
        config.setExpression("a + b");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("result");
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        return doPost("/api/calculatedField", calculatedField, CalculatedField.class);
    }

    public void testSimpleCalculatedFieldWhenCtxBecameUninitialized() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("M + 1");
        calculatedField.setDebugSettings(DebugSettings.all());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("m", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        config.setArguments(Map.of("m", argument));
        config.setExpression("m + 1");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("m1");
        output.setDecimalsByDefault(0);
        config.setOutput(output);
        calculatedField.setConfiguration(config);

        calculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"m\":1}"));

        await().alias("create CF -> ctx is initialized -> perform calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode m1 = getLatestTelemetry(testDevice.getId(), "m1");
                    assertThat(m1).isNotNull();
                    assertThat(m1.get("m1").get(0).get("value").asText()).isEqualTo("2");
                });

        config.setExpression("m m");
        calculatedField.setConfiguration(config);
        calculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"m\":2}"));

        await().alias("update CF -> ctx is not initialized -> no calculation performed").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode m1 = getLatestTelemetry(testDevice.getId(), "m1");
                    assertThat(m1).isNotNull();
                    assertThat(m1.get("m1").get(0).get("value").asText()).isEqualTo("2");
                });
    }

    private CalculatedField createCalculatedFieldWhenUseLatestTs(EntityId entityId) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("x + y");
        calculatedField.setDebugSettings(DebugSettings.all());
        calculatedField.setConfigurationVersion(1);

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument x = new Argument();
        ReferencedEntityKey refEntityKeyX = new ReferencedEntityKey("x", ArgumentType.TS_LATEST, null);
        x.setRefEntityKey(refEntityKeyX);
        Argument y = new Argument();
        ReferencedEntityKey refEntityKeyY = new ReferencedEntityKey("y", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE);
        y.setRefEntityKey(refEntityKeyY);
        config.setArguments(Map.of("x", x, "y", y));
        config.setExpression("x + y");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("z");
        output.setDecimalsByDefault(0);
        config.setOutput(output);

        config.setUseLatestTs(true);

        calculatedField.setConfiguration(config);

        return doPost("/api/calculatedField", calculatedField, CalculatedField.class);
    }

    private void pushTelemetry(EntityId entityId, JsonNode telemetry) throws Exception {
        doPost("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/timeseries/" + DataConstants.SERVER_SCOPE, telemetry);
    }

    private ObjectNode getTimeSeries(EntityId entityId, long startTs, long endTs, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys={keys}&startTs={startTs}&endTs={endTs}", ObjectNode.class, String.join(",", keys), startTs, endTs);
    }

    @Test
    public void testGeofencingCalculatedField_reprocess_overTimeWindow() throws Exception {
        // --- Arrange entities and zones ---
        Device device = createDevice("GF Device (reprocess)", "sn-geo-reproc-1");

        // Allowed polygon
        String allowedPolygon = "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]";
        // Restricted polygon
        String restrictedPolygon = "[[50.475000, 30.510000], [50.475000, 30.512000], [50.477000, 30.512000], [50.477000, 30.510000]]";

        Asset allowedZoneAsset = createAsset("Allowed Zone (reproc)", null);
        doPost("/api/plugins/telemetry/ASSET/" + allowedZoneAsset.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + allowedPolygon + "}")).andExpect(status().isOk());

        Asset restrictedZoneAsset = createAsset("Restricted Zone (reproc)", null);
        doPost("/api/plugins/telemetry/ASSET/" + restrictedZoneAsset.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + restrictedPolygon + "}")).andExpect(status().isOk());

        // Relations FROM device -> zones
        EntityRelation relAllowed = new EntityRelation();
        relAllowed.setFrom(device.getId());
        relAllowed.setTo(allowedZoneAsset.getId());
        relAllowed.setType("AllowedZone");
        doPost("/api/relation", relAllowed).andExpect(status().isOk());

        EntityRelation relRestricted = new EntityRelation();
        relRestricted.setFrom(device.getId());
        relRestricted.setTo(restrictedZoneAsset.getId());
        relRestricted.setType("RestrictedZone");
        doPost("/api/relation", relRestricted).andExpect(status().isOk());

        // --- Prepare historical telemetry (two points with explicit timestamps) ---
        long currentTime = System.currentTimeMillis();
        // reprocessing time window(TW)
        long startTs = currentTime - TimeUnit.SECONDS.toMillis(1200);
        long endTs = currentTime - TimeUnit.SECONDS.toMillis(300);

        // Point 1: inside Allowed
        long ts1 = currentTime - TimeUnit.SECONDS.toMillis(900);
        String p1 = """
        {"ts": %d, "values": {"latitude": 50.4730, "longitude": 30.5050}}
        """.formatted(ts1);
        // Point 2: inside Restricted
        long ts2 = currentTime - TimeUnit.SECONDS.toMillis(600);
        String p2 = """
        {"ts": %d, "values": {"latitude": 50.4760, "longitude": 30.5110}}
        """.formatted(ts2);

        pushTelemetry(device.getId(), JacksonUtil.toJsonNode(p1));
        pushTelemetry(device.getId(), JacksonUtil.toJsonNode(p2));

        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("Geofencing CF (reprocess)");
        cf.setDebugSettings(DebugSettings.off());

        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        ZoneGroupConfiguration allowedGroup = new ZoneGroupConfiguration("zone",
                REPORT_TRANSITION_EVENTS_ONLY, false);
        RelationPathQueryDynamicSourceConfiguration allowedDyn = new RelationPathQueryDynamicSourceConfiguration();
        allowedDyn.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, "AllowedZone")));
        allowedGroup.setRefDynamicSourceConfiguration(allowedDyn);

        ZoneGroupConfiguration restrictedGroup = new ZoneGroupConfiguration("zone",
                REPORT_TRANSITION_EVENTS_ONLY, false);
        RelationPathQueryDynamicSourceConfiguration restrictedDyn = new RelationPathQueryDynamicSourceConfiguration();
        restrictedDyn.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, "RestrictedZone")));
        restrictedGroup.setRefDynamicSourceConfiguration(restrictedDyn);

        cfg.setZoneGroups(Map.of("allowedZones", allowedGroup, "restrictedZones", restrictedGroup));

        cfg.setOutput(new TimeSeriesOutput());

        cf.setConfiguration(cfg);

        CalculatedField saved = doPost("/api/calculatedField", cf, CalculatedField.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();

        // --- Trigger CF reprocessing for the TW ---
        // Expectation: final state in the window is at ts2 -> LEFT Allowed, ENTERED Restricted.
        reprocessCalculatedField(saved, startTs, endTs);

        // --- Assert results after reprocessing ---
        await().alias("Geofencing CF reprocess").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getTimeSeries(device.getId(), startTs, endTs, "allowedZonesEvent,restrictedZonesEvent");
                    assertThat(result).isNotNull().hasSize(2);

                    assertThat(result.get("allowedZonesEvent")).hasSize(2);
                    assertThat(result.get("restrictedZonesEvent")).hasSize(1);

                    assertThat(result.get("allowedZonesEvent").get(0).get("value").asText()).isEqualTo("LEFT");
                    assertThat(result.get("allowedZonesEvent").get(0).get("ts").asText()).isEqualTo(Long.toString(ts2));

                    assertThat(result.get("allowedZonesEvent").get(1).get("value").asText()).isEqualTo("ENTERED");
                    assertThat(result.get("allowedZonesEvent").get(1).get("ts").asText()).isEqualTo(Long.toString(ts1));

                    assertThat(result.get("restrictedZonesEvent").get(0).get("value").asText()).isEqualTo("ENTERED");
                    assertThat(result.get("restrictedZonesEvent").get(0).get("ts").asText()).isEqualTo(Long.toString(ts2));
                });

        await().atMost(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Job cfReprocessingJob = findJobs(List.of(JobType.CF_REPROCESSING), List.of(device.getUuidId())).stream().findFirst().orElseThrow();
            assertThat(cfReprocessingJob.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(cfReprocessingJob.getResult().getSuccessfulCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getResult().getTotalCount()).isEqualTo(1);
            assertThat(cfReprocessingJob.getEntityId()).isEqualTo(device.getId());
            assertThat(cfReprocessingJob.getEntityName()).isEqualTo(device.getName());
        });
    }

    @Test
    public void testGeofencingCalculatedField_withZonesCreatedOnDevice() throws Exception {
        // --- Arrange entities ---
        Device device = createDevice("GF Test Device", "sn-geo-2");

        // Allowed zone polygon (square)
        String allowedPolygon = "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]";
        // Restricted zone polygon (square)
        String restrictedPolygon = "[[50.475000, 30.510000], [50.475000, 30.512000], [50.477000, 30.512000], [50.477000, 30.510000]]";

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"allowedZone\":" + allowedPolygon + "}")).andExpect(status().isOk());

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"restrictedZone\":" + restrictedPolygon + "}")).andExpect(status().isOk());

        // Initial device coordinates (inside Allowed, outside Restricted)
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4730,\"longitude\":30.5050}"));

        // --- Build CF: GEOFENCING ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getDeviceProfileId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("Geofencing CF");
        cf.setDebugSettings(DebugSettings.off());

        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST on the device
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone groups: ATTRIBUTE on the device
        ZoneGroupConfiguration allowedZonesGroup = new ZoneGroupConfiguration("allowedZone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        ZoneGroupConfiguration restrictedZonesGroup = new ZoneGroupConfiguration("restrictedZone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);

        cfg.setZoneGroups(Map.of("allowedZones", allowedZonesGroup, "restrictedZones", restrictedZonesGroup));

        // Output to server attributes
        AttributesOutput out = new AttributesOutput();
        out.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(out);

        cf.setConfiguration(cfg);

        doPost("/api/calculatedField", cf, CalculatedField.class);

        // --- Assert initial evaluation (ENTERED / OUTSIDE) ---
        await().alias("initial geofencing evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(),
                            "allowedZonesEvent", "allowedZonesStatus", "restrictedZonesStatus", "restrictedZonesEvent");
                    // --- no restrictedZonesEvent as no transition happened yet
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(3);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "INSIDE")
                            .containsEntry("restrictedZonesStatus", "OUTSIDE");
                });

        // --- delete attributes reported in previous evaluation
        doDelete("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/SERVER_SCOPE?keys=allowedZonesEvent,allowedZonesStatus,restrictedZonesStatus", String.class);

        // --- Update restrictedZone by 'restrictedZone' attribute update
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"restrictedZone\":" + restrictedPolygon + "}")).andExpect(status().isOk());

        // --- Assert no transition ---
        // --- Assert attributes updated with the same values for restrictedZones ---
        // --- Assert attributes updated with the new values for allowedZones ---
        await().alias("evaluation after version bump of geo argument")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(),
                            "allowedZonesEvent", "allowedZonesStatus",
                            "restrictedZonesEvent", "restrictedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(2);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesStatus", "INSIDE")
                            .containsEntry("restrictedZonesStatus", "OUTSIDE");
                });
    }

    @Test
    public void testGeofencingCalculatedField_withoutRelationsCreationAndDynamicRefresh() throws Exception {
        // --- Arrange entities ---
        Device device = createDevice("GF Device", "sn-geo-1");

        // Allowed zone polygon (square)
        String allowedPolygon = "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]";
        // Restricted zone polygon (square)
        String restrictedPolygon = "[[50.475000, 30.510000], [50.475000, 30.512000], [50.477000, 30.512000], [50.477000, 30.510000]]";

        Asset allowedZoneAsset = createAsset("Allowed Zone", null);
        doPost("/api/plugins/telemetry/ASSET/" + allowedZoneAsset.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + allowedPolygon + "}")).andExpect(status().isOk());

        Asset restrictedZoneAsset = createAsset("Restricted Zone", null);
        doPost("/api/plugins/telemetry/ASSET/" + restrictedZoneAsset.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + restrictedPolygon + "}")).andExpect(status().isOk());

        // Relations from device to zones
        EntityRelation deviceToAllowedZoneRelation = new EntityRelation();
        deviceToAllowedZoneRelation.setFrom(device.getId());
        deviceToAllowedZoneRelation.setTo(allowedZoneAsset.getId());
        deviceToAllowedZoneRelation.setType("AllowedZone");

        EntityRelation deviceToRestrictedZoneRelation = new EntityRelation();
        deviceToRestrictedZoneRelation.setFrom(device.getId());
        deviceToRestrictedZoneRelation.setTo(restrictedZoneAsset.getId());
        deviceToRestrictedZoneRelation.setType("RestrictedZone");

        doPost("/api/relation", deviceToAllowedZoneRelation).andExpect(status().isOk());
        doPost("/api/relation", deviceToRestrictedZoneRelation).andExpect(status().isOk());

        // Initial device coordinates (inside Allowed, outside Restricted)
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4730,\"longitude\":30.5050}"));

        // --- Build CF: GEOFENCING ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("Geofencing CF");
        cf.setDebugSettings(DebugSettings.off());

        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();

        // Coordinates: TS_LATEST on the device
        EntityCoordinates entityCoordinates = new EntityCoordinates("latitude", "longitude");
        cfg.setEntityCoordinates(entityCoordinates);

        // Zone groups: ATTRIBUTE on specific assets (one zone per group)
        ZoneGroupConfiguration allowedZonesGroup = new ZoneGroupConfiguration("zone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var allowedZoneDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        allowedZoneDynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, "AllowedZone")));
        allowedZonesGroup.setRefDynamicSourceConfiguration(allowedZoneDynamicSourceConfiguration);

        ZoneGroupConfiguration restrictedZonesGroup = new ZoneGroupConfiguration("zone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var restrictedZoneDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        restrictedZoneDynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, "RestrictedZone")));
        restrictedZonesGroup.setRefDynamicSourceConfiguration(restrictedZoneDynamicSourceConfiguration);

        cfg.setZoneGroups(Map.of("allowedZones", allowedZonesGroup, "restrictedZones", restrictedZonesGroup));

        // Output to server attributes
        AttributesOutput out = new AttributesOutput();
        out.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(out);

        cf.setConfiguration(cfg);

        doPost("/api/calculatedField", cf, CalculatedField.class);

        // --- Assert initial evaluation (ENTERED / OUTSIDE) ---
        await().alias("initial geofencing evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(),
                            "allowedZonesEvent", "allowedZonesStatus", "restrictedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(3);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "INSIDE")
                            .containsEntry("restrictedZonesStatus", "OUTSIDE");
                });

        // --- Move the device into Restricted zone (and outside Allowed) ---
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4760,\"longitude\":30.5110}"));

        // --- Assert transition (LEFT / ENTERED) ---
        await().alias("transition evaluation after movement")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(),
                            "allowedZonesEvent", "allowedZonesStatus",
                            "restrictedZonesEvent", "restrictedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(4);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "LEFT")
                            .containsEntry("restrictedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "OUTSIDE")
                            .containsEntry("restrictedZonesStatus", "INSIDE");
                });
    }

    @Test
    public void testGeofencingCalculatedField_DynamicRefresh_RebindsZoneArguments() throws Exception {
        // --- Update min allowed scheduled update intervals for CFs ---
        loginSysAdmin();
        EntityInfo tenantProfileEntityInfo = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        assertThat(tenantProfileEntityInfo).isNotNull();
        TenantProfile foundTenantProfile = doGet("/api/tenantProfile/" + tenantProfileEntityInfo.getId().getId().toString(), TenantProfile.class);
        assertThat(foundTenantProfile).isNotNull();
        assertThat(foundTenantProfile.getDefaultProfileConfiguration()).isNotNull();
        int minAllowedScheduledUpdateIntervalInSecForCF = TIMEOUT / 10;
        foundTenantProfile.getDefaultProfileConfiguration().setMinAllowedScheduledUpdateIntervalInSecForCF(minAllowedScheduledUpdateIntervalInSecForCF);
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", foundTenantProfile, TenantProfile.class);
        assertThat(savedTenantProfile).isNotNull();
        assertThat(savedTenantProfile.getDefaultProfileConfiguration().getMinAllowedScheduledUpdateIntervalInSecForCF()).isEqualTo(minAllowedScheduledUpdateIntervalInSecForCF);
        loginTenantAdmin();

        // --- Arrange entities ---
        Device device = createDevice("GF Device dyn", "sn-geo-dyn-1");

        // Allowed Zone A: covers initial point (ENTERED)
        String allowedPolygonA = "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]";

        Asset allowedZoneA = createAsset("Allowed Zone A", null);
        doPost("/api/plugins/telemetry/ASSET/" + allowedZoneA.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + allowedPolygonA + "}")).andExpect(status().isOk());

        // Relation from device to Allowed Zone A
        EntityRelation relAllowedA = new EntityRelation();
        relAllowedA.setFrom(device.getId());
        relAllowedA.setTo(allowedZoneA.getId());
        relAllowedA.setType("AllowedZone");
        doPost("/api/relation", relAllowedA).andExpect(status().isOk());

        // Initial device coordinates: INSIDE Zone A
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4730,\"longitude\":30.5050}")).andExpect(status().isOk());

        // --- Build CF: GEOFENCING with dynamic 'allowedZones' and short scheduled refresh ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.GEOFENCING);
        cf.setName("Geofencing CF (dynamic refresh)");
        cf.setDebugSettings(DebugSettings.off());

        GeofencingCalculatedFieldConfiguration cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setEntityCoordinates(new EntityCoordinates(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY));

        var allowedZonesGroup = new ZoneGroupConfiguration("zone", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        var allowedZoneDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        allowedZoneDynamicSourceConfiguration.setLevels(List.of(new RelationPathLevel(EntitySearchDirection.FROM, "AllowedZone")));
        allowedZonesGroup.setRefDynamicSourceConfiguration(allowedZoneDynamicSourceConfiguration);
        cfg.setZoneGroups(Map.of("allowedZones", allowedZonesGroup));

        // Server attributes output
        AttributesOutput out = new AttributesOutput();
        out.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(out);

        // Enable scheduled refresh with a 6-second interval
        cfg.setScheduledUpdateInterval(minAllowedScheduledUpdateIntervalInSecForCF);
        cfg.setScheduledUpdateEnabled(true);

        cf.setConfiguration(cfg);
        CalculatedField savedCalculatedField = doPost("/api/calculatedField", cf, CalculatedField.class);
        assertThat(savedCalculatedField).isNotNull();
        CalculatedFieldConfiguration configuration = savedCalculatedField.getConfiguration();
        assertThat(configuration).isInstanceOf(GeofencingCalculatedFieldConfiguration.class);
        var geofencingConfiguration = (GeofencingCalculatedFieldConfiguration) configuration;
        assertThat(geofencingConfiguration.isScheduledUpdateEnabled()).isTrue();

        // --- Assert initial evaluation (ENTERED) ---
        await().alias("initial geofencing evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(), "allowedZonesEvent", "allowedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(2);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "INSIDE");
                });

        // --- Move device OUTSIDE Zone A (expect LEFT) ---
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4760,\"longitude\":30.5110}")).andExpect(status().isOk());

        await().alias("outside zone A (LEFT)")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(), "allowedZonesEvent", "allowedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(2);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "LEFT")
                            .containsEntry("allowedZonesStatus", "OUTSIDE");
                });

        // --- Create Allowed Zone B covering the CURRENT location ---
        String allowedPolygonB = "[[50.475500, 30.510500], [50.475500, 30.511500], [50.476500, 30.511500], [50.476500, 30.510500]]";

        Asset allowedZoneB = createAsset("Allowed Zone B", null);
        doPost("/api/plugins/telemetry/ASSET/" + allowedZoneB.getUuidId() + "/attributes/" + DataConstants.SERVER_SCOPE,
                JacksonUtil.toJsonNode("{\"zone\":" + allowedPolygonB + "}")).andExpect(status().isOk());

        // Add a new relation
        EntityRelation relAllowedB = new EntityRelation();
        relAllowedB.setFrom(device.getId());
        relAllowedB.setTo(allowedZoneB.getId());
        relAllowedB.setType("AllowedZone");
        doPost("/api/relation", relAllowedB).andExpect(status().isOk());

        awaitForCalculatedFieldEntityMessageProcessorToRegisterCfStateAsReadyToRefreshDynamicArguments(device.getId(), savedCalculatedField.getId(), minAllowedScheduledUpdateIntervalInSecForCF);

        // --- Same coordinates as before, but now we expect ENTERED since a new zone is registered ---
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"latitude\":50.4760,\"longitude\":30.5110}")).andExpect(status().isOk());

        // --- Assert dynamic refresh picks up new relation and flips event back to ENTERED on the next telemetry update ---
        await().alias("dynamic refresh rebinds allowedZones")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs = getServerAttributes(device.getId(), "allowedZonesEvent", "allowedZonesStatus");
                    assertThat(attrs).isNotNull().isNotEmpty().hasSize(2);
                    Map<String, String> m = kv(attrs);
                    assertThat(m).containsEntry("allowedZonesEvent", "ENTERED")
                            .containsEntry("allowedZonesStatus", "INSIDE");
                });
    }

    @Test
    public void testPropagationCalculatedField_withExpression() throws Exception {
        // --- Arrange entities ---
        Device device = createDevice("Propagation Device With Expression", "sn-prop-1");
        Asset asset1 = createAsset("Propagated Asset 1", null);
        Asset asset2 = createAsset("Propagated Asset 2", null);

        // Create relations FROM assets TO device
        EntityRelation rel1 = new EntityRelation(asset1.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        EntityRelation rel2 = new EntityRelation(asset2.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        doPost("/api/relation", rel1).andExpect(status().isOk());
        doPost("/api/relation", rel2).andExpect(status().isOk());

        // Telemetry on device
        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"temperature\":12.5}")).andExpect(status().isOk());

        // --- Build CF: PROPAGATION with expression ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.PROPAGATION);
        cf.setName("Propagation CF (expr)");
        cf.setConfigurationVersion(1);

        PropagationCalculatedFieldConfiguration cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));
        cfg.setApplyExpressionToResolvedArguments(true);

        Argument arg = new Argument();
        arg.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        cfg.setArguments(Map.of("t", arg));

        cfg.setExpression("{\"testResult\": t * 2}");

        AttributesOutput output = new AttributesOutput();
        output.setScope(AttributeScope.SERVER_SCOPE);
        cfg.setOutput(output);

        cf.setConfiguration(cfg);

        doPost("/api/calculatedField", cf, CalculatedField.class);

        // --- Assert propagated calculation (expression applied) ---
        await().alias("propagation expr mode evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs1 = getServerAttributes(asset1.getId(), "testResult");
                    ArrayNode attrs2 = getServerAttributes(asset2.getId(), "testResult");
                    assertThat(attrs1).isNotNull();
                    assertThat(attrs2).isNotNull();
                    assertThat(attrs1.get(0).get("value").asDouble()).isEqualTo(25.0);
                    assertThat(attrs2.get(0).get("value").asDouble()).isEqualTo(25.0);
                });

        String deleteUrl = String.format("/api/v2/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                asset1.getId().getId(), EntityType.ASSET,
                EntityRelation.CONTAINS_TYPE, device.getId().getId(), EntityType.DEVICE
        );
        doDelete(deleteUrl).andExpect(status().isOk());
        doDelete("/api/plugins/telemetry/ASSET/" + asset1.getId() + "/SERVER_SCOPE?keys=testResult").andExpect(status().isOk());

        doPost("/api/plugins/telemetry/DEVICE/" + device.getUuidId() + "/timeseries/unusedScope",
                JacksonUtil.toJsonNode("{\"temperature\":25}")).andExpect(status().isOk());

        // --- Assert propagated calculation (expression applied with new temperature argument and one relation removed) ---
        await().alias("propagation expr mode evaluation after temperature update")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ArrayNode attrs1 = getServerAttributes(asset1.getId(), "testResult");
                    ArrayNode attrs2 = getServerAttributes(asset2.getId(), "testResult");
                    assertThat(attrs1).isNullOrEmpty();
                    assertThat(attrs2).isNotNull();
                    assertThat(attrs2.get(0).get("value").asDouble()).isEqualTo(50);
                });
    }

    @Test
    public void testPropagationCalculatedField_withoutExpression() throws Exception {
        // --- Arrange entities ---
        Device device = createDevice("Propagation Device Without Expression", "sn-prop-2");
        Asset asset1 = createAsset("Propagated Asset 1", null);
        Asset asset2 = createAsset("Propagated Asset 2", null);

        // Create relations FROM assets TO device
        EntityRelation rel1 = new EntityRelation(asset1.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        EntityRelation rel2 = new EntityRelation(asset2.getId(), device.getId(), EntityRelation.CONTAINS_TYPE);
        doPost("/api/relation", rel1).andExpect(status().isOk());
        doPost("/api/relation", rel2).andExpect(status().isOk());

        // Telemetry on device
        long ts = System.currentTimeMillis() - 300000L;
        postTelemetry(device.getId(), String.format("{\"ts\": %s, \"values\": {\"temperature\":12.5}}", ts));

        // --- Build CF: PROPAGATION without expression ---
        CalculatedField cf = new CalculatedField();
        cf.setEntityId(device.getId());
        cf.setType(CalculatedFieldType.PROPAGATION);
        cf.setName("Propagation CF (args-only)");
        cf.setConfigurationVersion(1);

        PropagationCalculatedFieldConfiguration cfg = new PropagationCalculatedFieldConfiguration();
        cfg.setRelation(new RelationPathLevel(EntitySearchDirection.TO, EntityRelation.CONTAINS_TYPE));
        cfg.setApplyExpressionToResolvedArguments(false); // arguments-only mode

        Argument arg = new Argument();
        arg.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        cfg.setArguments(Map.of("temperatureComputed", arg));

        cfg.setOutput(new TimeSeriesOutput());

        cf.setConfiguration(cfg);

        doPost("/api/calculatedField", cf, CalculatedField.class);

        // --- Assert propagated calculation (arguments-only mode) ---
        await().alias("propagation args-only evaluation")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode telemetry1 = getLatestTelemetry(asset1.getId(), "temperatureComputed");
                    ObjectNode telemetry2 = getLatestTelemetry(asset2.getId(), "temperatureComputed");
                    assertThat(telemetry1).isNotNull();
                    assertThat(telemetry2).isNotNull();
                    assertThat(telemetry1.get("temperatureComputed").get(0).get("ts").asText()).isEqualTo(Long.toString(ts));
                    assertThat(telemetry1.get("temperatureComputed").get(0).get("value").asDouble()).isEqualTo(12.5);
                    assertThat(telemetry2.get("temperatureComputed").get(0).get("ts").asText()).isEqualTo(Long.toString(ts));
                    assertThat(telemetry2.get("temperatureComputed").get(0).get("value").asDouble()).isEqualTo(12.5);
                });

        String deleteUrl = String.format("/api/v2/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                asset1.getId().getId(), EntityType.ASSET,
                EntityRelation.CONTAINS_TYPE, device.getId().getId(), EntityType.DEVICE
        );
        doDelete(deleteUrl).andExpect(status().isOk());
        doDelete("/api/plugins/telemetry/ASSET/" + asset1.getId() + "/timeseries/delete?keys=temperatureComputed&deleteAllDataForKeys=true").andExpect(status().isOk());

        // Update telemetry on device
        long newTs = System.currentTimeMillis() - 300000L;
        postTelemetry(device.getId(), String.format("{\"ts\": %s, \"values\": {\"temperature\":25}}", newTs));

        // --- Assert propagated calculation (arguments-only mode after update) ---
        await().alias("propagation args-only evaluation after temperature update")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode telemetry1 = getLatestTelemetry(asset1.getId(), "temperatureComputed");
                    ObjectNode telemetry2 = getLatestTelemetry(asset2.getId(), "temperatureComputed");
                    assertThat(telemetry1).isNotNull();
                    assertThat(telemetry2).isNotNull();
                    assertThat(telemetry1.get("temperatureComputed").get(0).get("value")).isEqualTo(NullNode.instance);
                    assertThat(telemetry2.get("temperatureComputed").get(0).get("ts").asText()).isEqualTo(Long.toString(newTs));
                    assertThat(telemetry2.get("temperatureComputed").get(0).get("value").asDouble()).isEqualTo(25);
                });
    }

    @Test
    public void testCalculatedFieldWhenTheSameTelemetryKeysUsed() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");
        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"a\":5}"));

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("a + b");
        calculatedField.setDebugSettings(DebugSettings.all());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("a", ArgumentType.TS_LATEST, null);
        Argument argumentA = new Argument();
        argumentA.setRefEntityKey(refEntityKey);
        Argument argumentB = new Argument();
        argumentB.setRefEntityKey(refEntityKey);
        config.setArguments(Map.of("a", argumentA, "b", argumentB));
        config.setExpression("a + b");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("c");
        output.setDecimalsByDefault(0);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode c = getLatestTelemetry(testDevice.getId(), "c");
                    assertThat(c).isNotNull();
                    assertThat(c.get("c").get(0).get("value").asText()).isEqualTo("10");
                });

        doPost("/api/plugins/telemetry/DEVICE/" + testDevice.getUuidId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode("{\"a\":10}"));

        await().alias("update telemetry -> recalculate state").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode c = getLatestTelemetry(testDevice.getId(), "c");
                    assertThat(c).isNotNull();
                    assertThat(c.get("c").get(0).get("value").asText()).isEqualTo("20");
                });
    }

    @Test
    public void testSimpleCalculatedFieldWhenSkipRuleEngineOutputProcessing() throws Exception {
        Device testDevice = createDevice("Test device", "1234567890");

        postTelemetry(testDevice.getId(), "{\"temperature\":24.5}");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(testDevice.getId());
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("C to F");
        calculatedField.setDebugSettings(DebugSettings.all());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        config.setArguments(Map.of("T", argument));
        config.setExpression("(T * 9/5) + 32");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("fahrenheitTemp");
        output.setDecimalsByDefault(1);
        output.setStrategy(new TimeSeriesImmediateOutputStrategy(1000L, true, true, true, true));

        config.setOutput(output);

        config.setUseLatestTs(true);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);

        await().alias("create CF -> perform initial calculation").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode fahrenheitTemp = getLatestTelemetry(testDevice.getId(), "fahrenheitTemp");
                    assertThat(fahrenheitTemp).isNotNull();
                    assertThat(fahrenheitTemp.get("fahrenheitTemp").get(0).get("value").asText()).isEqualTo("76.1");
                });
    }

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

    private ArrayNode getServerAttributes(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/attributes/SERVER_SCOPE?keys=" + String.join(",", keys), ArrayNode.class);
    }

    private Asset createAsset(String name, AssetProfileId assetProfileId) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setAssetProfileId(assetProfileId);
        return doPost("/api/asset", asset, Asset.class);
    }

    private Device createDevice(String name, String accessToken, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device.setDeviceProfileId(deviceProfileId);
        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
        deviceData.setConfiguration(new DefaultDeviceConfiguration());
        device.setDeviceData(deviceData);
        return doPost("/api/device?accessToken=" + accessToken, device, Device.class);
    }

    private Job reprocessCalculatedField(CalculatedField savedCalculatedField, long startTs, long endTs) throws Exception {
        return doGet("/api/calculatedField/" + savedCalculatedField.getUuidId() + "/reprocess?startTs={startTs}&endTs={endTs}", Job.class, startTs, endTs);
    }

    private Job getLastReprocessingJob(CalculatedFieldId calculatedFieldId) throws Exception {
        return doGet("/api/calculatedField/" + calculatedFieldId.getId() + "/reprocess/job", Job.class);
    }

    private static Map<String, String> kv(ArrayNode attrs) {
        Map<String, String> m = new HashMap<>();
        for (JsonNode n : attrs) {
            m.put(n.get("key").asText(), n.get("value").asText());
        }
        return m;
    }

}

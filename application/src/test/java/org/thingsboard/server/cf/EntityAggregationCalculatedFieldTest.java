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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggFunction;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggKeyInput;
import org.thingsboard.server.common.data.cf.configuration.aggregation.AggMetric;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.AggInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.CustomInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.HourInterval;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.Watermark;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.controller.AbstractWebTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.cf.CalculatedFieldIntegrationTest.POLL_INTERVAL;

@DaoSqlTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "actors.calculated_fields.check_interval=1"
})
public class EntityAggregationCalculatedFieldTest extends AbstractControllerTest {

    private final String TZ = "Europe/Kyiv";

    private Tenant savedTenant;

    @Before
    public void beforeEach() throws Exception {
        loginSysAdmin();

        updateDefaultTenantProfileConfig(tenantProfileConfig -> {
            tenantProfileConfig.setMinAllowedDeduplicationIntervalInSecForCF(1);
            tenantProfileConfig.setMinAllowedAggregationIntervalInSecForCF(1);
        });

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        assertThat(savedTenant).isNotNull();

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant@thingsboard.org");
        tenantAdmin.setFirstName("John");
        tenantAdmin.setLastName("Doe");

        createUserAndLogin(tenantAdmin, "testPassword");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testCreateCfAndNoTelemetryDuringInterval_checkAggregation() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval(TZ, 0L, 5L);
        long intervalEndTs = customInterval.getCurrentIntervalEndTs();

        CalculatedField totalConsumptionCF = createTotalConsumptionCF(device.getId(), customInterval, null);
        long interval = customInterval.getCurrentIntervalDurationMillis();

        await().alias("create CF and no telemetry during interval -> save metric with default value")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("9999");
                });
    }

    @Test
    public void testCreateCfWithoutWatermark_checkAggregation() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval(TZ, 0L, 5L);
        long currentIntervalStartTs = customInterval.getCurrentIntervalStartTs();
        long currentIntervalEndTs = customInterval.getCurrentIntervalEndTs();

        long tsBeforeInterval = currentIntervalStartTs - 1000;
        long tsInInterval_1 = currentIntervalStartTs + 1000;
        long tsInInterval_2 = currentIntervalStartTs + 500;
        long tsInInterval_3 = currentIntervalStartTs + 200;
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsBeforeInterval));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":100}}", tsInInterval_1));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":180}}", tsInInterval_2));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsInInterval_3));

        long interval = customInterval.getCurrentIntervalDurationMillis();
        CalculatedField totalConsumptionCF = createTotalConsumptionCF(device.getId(), customInterval, null);

        await().alias("create CF -> perform aggregation after interval end")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("400");
                });

        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":500}}", tsInInterval_1));

        await().alias("update telemetry that belongs to previous interval -> no aggregation since watermark is not set ")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("400");
                });
    }

    @Test
    public void testCreateCfWithWatermark_checkAggregationDuringWatermark() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        CustomInterval customInterval = new CustomInterval(TZ, 0L, 5L);
        long currentIntervalStartTs = customInterval.getCurrentIntervalStartTs();
        long currentIntervalEndTs = customInterval.getCurrentIntervalEndTs();

        long tsBeforeInterval = currentIntervalStartTs - 1000L;
        long tsInInterval_1 = currentIntervalStartTs + 1000L;
        long tsInInterval_2 = currentIntervalStartTs + 500L;
        long tsInInterval_3 = currentIntervalStartTs + 200L;
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsBeforeInterval));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":100}}", tsInInterval_1));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":180}}", tsInInterval_2));
        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":120}}", tsInInterval_3));

        long interval = customInterval.getCurrentIntervalDurationMillis();
        Watermark watermark = new Watermark(10);
        CalculatedField totalConsumptionCF = createTotalConsumptionCF(device.getId(), customInterval, watermark);

        await().alias("create CF -> perform aggregation after interval end")
                .atMost(2 * interval, TimeUnit.MILLISECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("400");
                });

        postTelemetry(device.getId(), String.format("{\"ts\": \"%s\", \"values\": {\"energy\":300}}", tsInInterval_1));

        await().alias("update telemetry during watermark -> perform aggregation")
                .atMost(2 * 10, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode result = getLatestTelemetry(device.getId(), "consumption");
                    assertThat(result).isNotNull();
                    assertThat(result.get("consumption").get(0).get("value").asText()).isEqualTo("600");
                });
    }

    @Test
    public void testReprocessCalculatedField() throws Exception {
        Device device = createDevice("Device", "1234567890111");

        LocalDate testDate = LocalDate.of(2025, 11, 11);
        ZonedDateTime dateTime = ZonedDateTime.of(testDate, LocalTime.of(13, 24), ZoneId.of(TZ));
        // reprocessing time window(TW)
        long startTs = dateTime.minusHours(5).toInstant().toEpochMilli(); // 2025-11-11 8:24
        long endTs = dateTime.toInstant().toEpochMilli(); // 2025-11-11 13:24

        // outside the TW
        long interval_1_1 = ts(testDate, 8, 11, 23);
        long interval_1_2 = ts(testDate, 8, 33, 56);
        long interval_1_3 = ts(testDate, 8, 47, 12);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":11}}", interval_1_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":12}}", interval_1_2));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":8}}", interval_1_3));

        // outside the TW (but telemetry will be used for initial processing)
        long interval_2_nextStartTs = ts(testDate, 10, 0, 0);
        long interval_2_1 = ts(testDate, 9, 0, 0);
        long interval_2_2 = ts(testDate, 9, 15, 11);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":13}}", interval_2_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":35}}", interval_2_2));

        // inside the TW
        long interval_3_nextStartTs = ts(testDate, 11, 0, 0);
        long interval_3_1 = ts(testDate, 10, 20, 44);
        long interval_3_2 = ts(testDate, 10, 40, 33);
        long interval_3_3 = ts(testDate, 10, 55, 22);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":3}}", interval_3_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":22}}", interval_3_2));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":22}}", interval_3_3));

        // inside the TW
        long interval_5_nextStartTs = ts(testDate, 13, 0, 0);
        long interval_5_1 = ts(testDate, 12, 11, 46);
        long interval_5_2 = ts(testDate, 12, 26, 11);
        long interval_5_3 = ts(testDate, 12, 59, 31);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":5}}", interval_5_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":51}}", interval_5_2));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":12}}", interval_5_3));

        // inside the TW
        long interval_4_nextStartTs = ts(testDate, 12, 0, 0);

        // outside the TW
        long interval_6_1 = ts(testDate, 13, 17, 32);
        long interval_6_2 = ts(testDate, 13, 38, 31);

        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":22}}", interval_6_1));
        postTelemetry(device.getId(), String.format("{\"ts\":%s, \"values\":{\"energy\":11}}", interval_6_2));

        /*
                                              startTs                        endTs
                                                 |-----------------------------|
                             |         |         |         |         |         |         |
               |  intervals  |   8-9   |  9-10   |  10-11  |  11-12  |  12-13  |  13-14  |
               |  telemetry  | 11 12 8 |  13 35  | 3 22 22 |         | 5 51 12 |  22 11  |
                             |         |         |         |         |         |         |
                                                 |-----------------------------|
                                                                |--- reprocessing time window
               consumption should be: 48 -> 47 -> 9999(default value) -> 68
        */

        CalculatedField savedCalculatedField = createTotalConsumptionCF(device.getId(), new HourInterval(TZ, 0L), null);

        reprocessCalculatedField(savedCalculatedField, startTs, endTs);

        await().alias("reprocess -> perform calculation for time window").atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ObjectNode consumption = getTimeSeries(device.getId(), startTs, endTs, "consumption");
                    assertThat(consumption).isNotNull();

                    assertThat(consumption.get("consumption").get(0).get("ts").asText()).isEqualTo(Long.toString(interval_5_nextStartTs - 1));
                    assertThat(consumption.get("consumption").get(0).get("value").asText()).isEqualTo("68");

                    assertThat(consumption.get("consumption").get(1).get("ts").asText()).isEqualTo(Long.toString(interval_4_nextStartTs - 1));
                    assertThat(consumption.get("consumption").get(1).get("value").asText()).isEqualTo("9999");

                    assertThat(consumption.get("consumption").get(2).get("ts").asText()).isEqualTo(Long.toString(interval_3_nextStartTs - 1));
                    assertThat(consumption.get("consumption").get(2).get("value").asText()).isEqualTo("47");

                    assertThat(consumption.get("consumption").get(3).get("ts").asText()).isEqualTo(Long.toString(interval_2_nextStartTs - 1));
                    assertThat(consumption.get("consumption").get(3).get("value").asText()).isEqualTo("48");
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

    private long ts(LocalDate date, int hour, int minute, int second) {
        return ZonedDateTime.of(date, LocalTime.of(hour, minute, second), ZoneId.of(TZ))
                .toInstant()
                .toEpochMilli();
    }

    private CalculatedField createTotalConsumptionCF(EntityId entityId, AggInterval aggInterval, Watermark watermark) {
        Map<String, Argument> arguments = new HashMap<>();
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("energy", ArgumentType.TS_LATEST, null));
        arguments.put("en", argument);

        Map<String, AggMetric> aggMetrics = new HashMap<>();

        AggMetric consumption = new AggMetric();
        consumption.setFunction(AggFunction.SUM);
        consumption.setInput(new AggKeyInput("en"));
        consumption.setDefaultValue(9999L);
        aggMetrics.put("consumption", consumption);

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setDecimalsByDefault(0);

        return createAggCf("Consumption per minute", entityId,
                aggInterval,
                watermark,
                arguments,
                aggMetrics,
                output);
    }

    private CalculatedField createAggCf(String name,
                                        EntityId entityId,
                                        AggInterval aggInterval,
                                        Watermark watermark,
                                        Map<String, Argument> inputs,
                                        Map<String, AggMetric> metrics,
                                        Output output) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setName(name);
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.ENTITY_AGGREGATION);

        EntityAggregationCalculatedFieldConfiguration configuration = new EntityAggregationCalculatedFieldConfiguration();

        configuration.setArguments(inputs);
        configuration.setMetrics(metrics);
        configuration.setInterval(aggInterval);
        if (watermark != null) {
            configuration.setWatermark(watermark);
        }
        configuration.setOutput(output);

        calculatedField.setConfiguration(configuration);
        calculatedField.setDebugSettings(DebugSettings.all());
        return saveCalculatedField(calculatedField);
    }

    private ObjectNode getLatestTelemetry(EntityId entityId, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys=" + String.join(",", keys), ObjectNode.class);
    }

    private ObjectNode getTimeSeries(EntityId entityId, long startTs, long endTs, String... keys) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + entityId.getEntityType() + "/" + entityId.getId() + "/values/timeseries?keys={keys}&startTs={startTs}&endTs={endTs}", ObjectNode.class, String.join(",", keys), startTs, endTs);
    }

    private Job reprocessCalculatedField(CalculatedField savedCalculatedField, long startTs, long endTs) throws Exception {
        return doGet("/api/calculatedField/" + savedCalculatedField.getUuidId() + "/reprocess?startTs={startTs}&endTs={endTs}", Job.class, startTs, endTs);
    }

}

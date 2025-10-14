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
package org.thingsboard.server.msa.connectivity;

import com.google.gson.JsonObject;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.TestProperties;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

public class JavaRestClientTest extends AbstractContainerTest {

    private RestClient restClient;

    @BeforeClass
    public void beforeClass() throws Exception {
        SSLContext ssl = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();

        var tls = new DefaultClientTlsStrategy(
                ssl,
                HostnameVerificationPolicy.CLIENT,
                NoopHostnameVerifier.INSTANCE
        );

        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tls)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();

        RestTemplate rt = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        restClient = new RestClient(rt, TestProperties.getBaseUrl());
    }

    @BeforeMethod
    public void setUp() throws Exception {
        restClient.login("tenant@thingsboard.org", "tenant");
    }

    @AfterMethod
    public void tearDown() {
    }

    @Test
    public void testGetAlarmsV2() {
        Device device = restClient.saveDevice(defaultDevicePrototype(RandomStringUtils.randomAlphabetic(5)));
        assertThat(device).isNotNull();

        String type = "High temp" + RandomStringUtils.randomAlphabetic(5);
        Alarm alarm = Alarm.builder()
                .originator(device.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type(type)
                .build();
        restClient.saveAlarm(alarm);

        // get /api/v2/alarm
        PageData<AlarmInfo> alarmsV2 = restClient.getAlarmsV2(device.getId(), null, null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(alarmsV2.getData()).hasSize(1);

        PageData<AlarmInfo> activeAlarms = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.ACTIVE), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(activeAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> cleared = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.CLEARED), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(cleared.getData()).hasSize(0);

        PageData<AlarmInfo> activeAndClearedAlarms = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.CLEARED, AlarmSearchStatus.ACTIVE), null, null, null, new TimePageLink(10, 0));
        assertThat(activeAndClearedAlarms.getData()).hasSize(1);

        // get /api/v2/alarms
        PageData<AlarmInfo> allAlarmsV2 = restClient.getAllAlarmsV2(List.of(AlarmSearchStatus.ACTIVE), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(allAlarmsV2.getData()).hasSize(1);

        PageData<AlarmInfo> allClearedAlarmsV2 = restClient.getAllAlarmsV2(List.of(AlarmSearchStatus.CLEARED), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(allClearedAlarmsV2.getData()).hasSize(0);

        // get /api/alarms
        PageData<AlarmInfo> allAlarms = restClient.getAllAlarms(AlarmSearchStatus.ACTIVE, null, new TimePageLink(10, 0), null);
        assertThat(allAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> allClearedAlarms = restClient.getAllAlarms(AlarmSearchStatus.CLEARED, null, new TimePageLink(10, 0), null);
        assertThat(allClearedAlarms.getData()).hasSize(0);

    }

    @Test
    public void testTimeSeriesByReadTsKvQueries() {
        Device device = restClient.saveDevice(defaultDevicePrototype(RandomStringUtils.randomAlphabetic(5)));
        assertThat(device).isNotNull();

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());
        for (int i = 0; i < 3; i++) {
            JsonObject values = new JsonObject();
            values.addProperty("temperature", i + 25);
            testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), JacksonUtil.toJsonNode(createPayload().toString()));
        }

        restClient.saveEntityTelemetry(device.getId(), "ts", JacksonUtil.toJsonNode("{\"temperature\": 25, \"humidity\": 60}"));
        restClient.saveEntityTelemetry(device.getId(), "ts", JacksonUtil.toJsonNode("{\"temperature\": 27, \"humidity\": 59}"));
        restClient.saveEntityTelemetry(device.getId(), "ts", JacksonUtil.toJsonNode("{\"temperature\": 33, \"humidity\": 62}"));

        List<BaseReadTsKvQuery> queries = new ArrayList<>();
        BaseReadTsKvQuery tempQuery = new BaseReadTsKvQuery("temperature", System.currentTimeMillis() - 5000, System.currentTimeMillis(), 5000, 3, Aggregation.AVG);
        BaseReadTsKvQuery humQuery = new BaseReadTsKvQuery("humidity", System.currentTimeMillis() - 5000, System.currentTimeMillis(), 5000, 3, Aggregation.MAX);
        queries.add(tempQuery);
        queries.add(humQuery);
        List<ReadTsKvQueryResult> results = restClient.getTimeseriesByQueries(device.getId(), queries);
        assertThat(results).isNotNull().hasSize(2);

        ReadTsKvQueryResult tempQueryResult = results.get(0);
        assertThat(tempQueryResult.getData()).hasSize(1);
        TsKvEntry tempTsKv = tempQueryResult.getData().get(0);
        assertThat(tempTsKv.getKey()).isEqualTo("temperature");
        assertThat(tempTsKv.getValue()).isEqualTo((25 + 27 + 33) / 3d);

        ReadTsKvQueryResult humQueryResult = results.get(1);
        assertThat(humQueryResult.getData()).hasSize(1);
        TsKvEntry humTsKv = humQueryResult.getData().get(0);
        assertThat(humTsKv.getKey()).isEqualTo("humidity");
        assertThat(humTsKv.getValue()).isEqualTo(62L);
    }
}

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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.ReportJobResult;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class ReportControllerTest extends AbstractControllerTest {

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @After
    public void afterTest() throws Exception {
    }

    @Test
    public void testReportWithEntityTable() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDevicesEntityAlias(devicesAliasId);

        EntityTableComponent tableComponent = new EntityTableComponent();
        tableComponent.setDataSources(List.of(DataSource.builder()
                        .type("entity")
                        .entityAliasId(devicesAliasId)
                        .dataKeys(List.of(
                                new DataKey("createdTime", "entityField", "CREATED TIME"),
                                new DataKey("name", "entityField", "NAME"),
                                new DataKey("type", "entityField", "TYPE"),
                                new DataKey("temperature", "timeseries", "TEMPERATURE"),
                                new DataKey("threshold", "attribute", "THRESHOLD")
                        ))
                        .build()));

        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAlias(entityAlias);
        configuration.setComponent(tableComponent);

        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setName("Device inventory report");
        reportTemplate.setType(ReportTemplateType.REPORT);

        ReportTemplate savedTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        List<Device> devices = new ArrayList<>();
        List<String> expectedReportLines = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            device = doPost("/api/device", device, Device.class);
            devices.add(device);

            long temperature = (long) (Math.random() * 100);
            long threshold = (long) (Math.random() * 100);
            String telemetryPayload = "{\"temperature\":" + temperature + "}";
            String attributePayload = "{\"threshold\":" + threshold + "}";
            doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/" + DataConstants.SHARED_SCOPE, telemetryPayload, String.class, status().isOk());
            doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attributePayload, String.class, status().isOk());
            expectedReportLines.add(device.getCreatedTime() + "," +
                                    device.getName() + "," +
                                    device.getType() + "," +
                                    temperature + "," +
                                    threshold);
        }

        //generate report
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateConfig(configuration);
        String csvReport = doPost("/api/v2/report/test", reportRequest, String.class);

        // Check headers and content
        String[] lines = csvReport.split("\r?\n");
        assertThat(lines[0]).contains("CREATED TIME,NAME,TYPE,TEMPERATURE,THRESHOLD");
        for (int i = 0; i < devices.size(); i++) {
            assertThat(lines[i + 1]).contains(expectedReportLines.get(i));
        }
    }

    @Test
    public void testCreateJobForCsvReport() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDevicesEntityAlias(devicesAliasId);

        EntityTableComponent tableComponent = new EntityTableComponent();
        tableComponent.setDataSources(List.of(DataSource.builder()
                        .type("entity")
                        .entityAliasId(devicesAliasId)
                        .dataKeys(List.of(
                                new DataKey("createdTime", "entityField", "CREATED TIME"),
                                new DataKey("name", "entityField", "NAME"),
                                new DataKey("type", "entityField", "TYPE"),
                                new DataKey("temperature", "timeseries", "TEMPERATURE"),
                                new DataKey("threshold", "attribute", "THRESHOLD")
                        ))
                        .build()));

        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAlias(entityAlias);
        configuration.setComponent(tableComponent);

        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setName("Devices report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        List<Device> devices = new ArrayList<>();
        List<String> expectedReportLines = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            device = doPost("/api/device", device, Device.class);
            devices.add(device);

            long temperature = (long) (Math.random() * 100);
            long threshold = (long) (Math.random() * 100);
            String telemetryPayload = "{\"temperature\":" + temperature + "}";
            String attributePayload = "{\"threshold\":" + threshold + "}";
            doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/" + DataConstants.SHARED_SCOPE, telemetryPayload, String.class, status().isOk());
            doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attributePayload, String.class, status().isOk());
            expectedReportLines.add(device.getCreatedTime() + "," +
                                    device.getName() + "," +
                                    device.getType() + "," +
                                    temperature + "," +
                                    threshold);
        }

        //generate report
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(reportTemplate.getId());
        Job job = doPost("/api/v2/report", reportRequest, Job.class);

        Job completedJob = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> doGet("/api/job/" + job.getId(), Job.class),
                result -> result.getStatus() == JobStatus.COMPLETED);

        ReportJobResult result = (ReportJobResult) completedJob.getResult();
        String csvReport = doGet("/api/blobEntity/" + result.getReportBlobId() + "/download", String.class);

        // Check headers and content
        String[] lines = csvReport.split("\r?\n");
        assertThat(lines[0]).contains("CREATED TIME,NAME,TYPE,TEMPERATURE,THRESHOLD");
        for (int i = 0; i < devices.size(); i++) {
            assertThat(lines[i + 1]).contains(expectedReportLines.get(i));
        }
    }

    private static EntityAlias buildDevicesEntityAlias(String aliasId) {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");
        return new EntityAlias(aliasId, "devices", filter);
    }

}

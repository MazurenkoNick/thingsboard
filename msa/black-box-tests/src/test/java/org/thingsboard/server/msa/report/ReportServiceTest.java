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
package org.thingsboard.server.msa.report;

import com.fasterxml.jackson.databind.JsonNode;
import org.awaitility.Awaitility;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.ReportJobResult;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.TableSortOrder;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.timewindow.AggregationConfiguration;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.common.data.report.configuration.timewindow.QuickTimeInterval;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantAdmin;

public class ReportServiceTest extends AbstractContainerTest {

    private TenantId tenantId;
    private UserId tenantAdminId;

    private ReportTemplate csvReportTemplate;
    private List<Device> devices;

    @BeforeClass
    public void beforeClass() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");

        tenantId = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant")).getId();
        tenantAdminId = testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId, "tenantAdmin@thingsboard.org"), "tenant");

        csvReportTemplate = createCsvReportTemplate();
        devices = createDevices(50);
    }

    @BeforeMethod
    public void beforeMethod() {
        testRestClient.getAndSetUserToken(tenantAdminId);
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        testRestClient.deleteTenant(tenantId);
    }

    @Test
    public void testGenerateTestCsvReport() {
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(csvReportTemplate.getId());
        String csvReport = new String(testRestClient.requestTestReport(reportRequest));

        checkCsvReport(csvReport);
    }

    @Test
    public void testRequestCsvReport() {
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(csvReportTemplate.getId());
        JobId reportJobId = testRestClient.requestReport(reportRequest).getId();
        Job reportJob = Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> testRestClient.getJobById(reportJobId),
                job -> job.getStatus() == JobStatus.COMPLETED);

        ReportJobResult result = (ReportJobResult) reportJob.getResult();
        Report report = result.getReport();
        assertThat(report).isNotNull();

        String csvReport = new String(testRestClient.downloadReport(report.getId()));
        checkCsvReport(csvReport);
    }

    @Test
    public void testEntityTableReportComponentWithTsAggregation() {
        Device device = testRestClient.postDevice("", defaultDevicePrototype("report"));
        for (int i = 0; i < 3; i++) {
            testRestClient.postTelemetry(device.getId(), JacksonUtil.toJsonNode("{\"temperature\":" + (i + 25) +"}"));
        }

        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildSingleDeviceAlias(devicesAliasId, device.getId());

        EntityTableComponent tableComponent = new EntityTableComponent();
        DataKey tempField = new DataKey("temperature", "timeseries", "TEMPERATURE");
        tempField.setAggregationType(Aggregation.AVG);
        tempField.setTimewindow(buildCurrentDateTimeWindow());
        tempField.setDecimals(0);
        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(
                        new DataKey("createdTime", "entityField", "CREATED TIME"),
                        new DataKey("name", "entityField", "NAME"),
                        tempField
                ))
                .build()));

        ReportTemplate csvReportTemplate = createReportTemplate(entityAlias, tableComponent);

        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(csvReportTemplate.getId());
        String csvReport = new String(testRestClient.requestTestReport(reportRequest));

        String[] lines = csvReport.split("\r?\n");
        assertThat(lines[1]).isEqualTo(String.join(",", String.valueOf(device.getCreatedTime()),
                device.getName(), String.valueOf(26)));
    }

    @Test
    public void testTimeseriesReportComponent() {
        Device device = testRestClient.postDevice("", defaultDevicePrototype("report"));
        long ts = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            testRestClient.postTelemetry(device.getId(), JacksonUtil.toJsonNode("{\"ts\": " + (ts - i) + ", \"values\":{\"temperature\":" + (i + 25) +"}}"));
        }

        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildSingleDeviceAlias(devicesAliasId, device.getId());

        TimeseriesTableComponent tableComponent = new TimeseriesTableComponent();
        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(new DataKey("temperature", "timeseries", "TEMPERATURE")))
                .build()));
        tableComponent.setTimewindow(buildCurrentDateTimeWindow());
        tableComponent.setShowTimestamp(true);
        tableComponent.setTimestampLabel("Timestamp");
        tableComponent.setTimestampPattern("milliseconds");

        ReportTemplate csvReportTemplate = createReportTemplate(entityAlias, tableComponent);

        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(csvReportTemplate.getId());
        String csvReport = new String(testRestClient.requestTestReport(reportRequest));

        String[] lines = csvReport.split("\r?\n");
        assertThat(lines[0]).isEqualTo(String.join(",", "Timestamp", "TEMPERATURE"));
        for (int i = 1; i <= 3; i++) {
            assertThat(lines[i]).isEqualTo(String.join(",",  String.valueOf(ts - i + 1), String.valueOf( 25 + i - 1)));
        }
    }

    private ReportTemplate createReportTemplate(EntityAlias entityAlias, ReportComponent tableComponent) {
        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAliases(List.of(entityAlias));
        configuration.setComponents(List.of(tableComponent));

        ReportTemplate csvReportTemplate = new ReportTemplate();
        csvReportTemplate.setConfiguration(configuration);
        csvReportTemplate.setName("Device inventory report");
        csvReportTemplate.setType(ReportTemplateType.REPORT);
        csvReportTemplate.setFormat(TbReportFormat.CSV);
        csvReportTemplate = testRestClient.postReportTemplate(csvReportTemplate);
        return csvReportTemplate;
    }

    private TimeWindowConfiguration buildCurrentDateTimeWindow() {
        TimeWindowConfiguration timewindow = new TimeWindowConfiguration();
        History history = new History();
        history.setHistoryType(2);
        history.setQuickInterval(QuickTimeInterval.CURRENT_DAY);
        history.setInterval(Interval.of(1000));
        timewindow.setHistory(history);
        timewindow.setTimezone(TimeZone.getDefault().getID());
        AggregationConfiguration aggregation = new AggregationConfiguration();
        aggregation.setType(Aggregation.NONE);
        aggregation.setLimit(25000);
        timewindow.setAggregation(aggregation);
        return timewindow;
    }

    private List<Device> createDevices(int count) {
        List<Device> devices = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            JsonNode telemetry = JacksonUtil.newObjectNode()
                    .put("temperature", (long) (Math.random() * 100));
            JsonNode attributes = JacksonUtil.newObjectNode()
                    .put("threshold", (long) (Math.random() * 100));
            device.setAdditionalInfoField("telemetry", telemetry);
            device.setAdditionalInfoField("attributes", attributes);

            device = testRestClient.postDevice(device.getName(), device);
            devices.add(device);

            testRestClient.postTelemetry(device.getName(), telemetry);
            testRestClient.postAttribute(device.getName(), attributes);
        }
        return devices;
    }

    private void checkCsvReport(String csvReport) {
        String[] lines = csvReport.split("\r?\n");
        assertThat(lines[0]).contains("CREATED TIME,NAME,TYPE,TEMPERATURE,THRESHOLD");
        for (int i = 0; i < devices.size(); i++) {
            String line = lines[i + 1];
            Device device = devices.get(i);
            assertThat(line).isEqualTo(String.join(",",
                    String.valueOf(device.getCreatedTime()),
                    device.getName(),
                    device.getType(),
                    device.getAdditionalInfo().get("telemetry").get("temperature").asText(),
                    device.getAdditionalInfo().get("attributes").get("threshold").asText()));
        }
    }

    private ReportTemplate createCsvReportTemplate() {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDevicesEntityAlias(devicesAliasId);

        EntityTableComponent tableComponent = new EntityTableComponent();
        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(
                        new DataKey("createdTime", "entityField", "CREATED TIME"),
                        new DataKey("name", "entityField", "NAME"),
                        new DataKey("type", "entityField", "TYPE"),
                        new DataKey("temperature", "timeseries", "TEMPERATURE"),
                        new DataKey("threshold", "attribute", "THRESHOLD")
                ))
                .build()));
        tableComponent.setTableSortOrder(new TableSortOrder("CREATED TIME", TableSortOrder.Direction.ASC));

        return createReportTemplate(entityAlias, tableComponent);
    }

    private static EntityAlias buildDevicesEntityAlias(String aliasId) {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");
        return new EntityAlias(aliasId, "devices", filter);
    }

    private static EntityAlias buildSingleDeviceAlias(String aliasId, DeviceId deviceId) {
        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(AliasEntityId.fromEntityId(deviceId));
        return new EntityAlias(aliasId, "device", filter);
    }

}

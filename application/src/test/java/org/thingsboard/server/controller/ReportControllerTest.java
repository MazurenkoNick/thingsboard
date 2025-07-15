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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.job.ReportJobResult;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.AffectedUserFilter;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.AlarmFilterConfig;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.DataSourceType;
import org.thingsboard.server.common.data.report.configuration.EntityAlias;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.common.data.report.configuration.style.Heading;
import org.thingsboard.server.common.data.report.configuration.timewindow.AggregationConfiguration;
import org.thingsboard.server.common.data.report.configuration.timewindow.History;
import org.thingsboard.server.common.data.report.configuration.timewindow.QuickTimeInterval;
import org.thingsboard.server.common.data.report.configuration.timewindow.TimeWindowConfiguration;
import org.thingsboard.server.dao.notification.DefaultNotifications;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class ReportControllerTest extends AbstractControllerTest {

    @Autowired
    private DefaultNotifications defaultNotifications;

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
        List<DataKey> dataKeys = List.of(
                new DataKey("createdTime", "entityField", "CREATED TIME"),
                new DataKey("name", "entityField", "NAME"),
                new DataKey("type", "entityField", "TYPE"),
                new DataKey("temperature", "timeseries", "TEMPERATURE"),
                new DataKey("threshold", "attribute", "THRESHOLD")
        );
        tableComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(dataKeys)
                .build()));
        tableComponent.setShowTableHeading(true);
        String tableHeadingText = "This is my table";
        Heading tableHeading = new Heading();
        tableHeading.setText(tableHeadingText);
        tableComponent.setTableHeading(tableHeading);

        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAliases(List.of(entityAlias));
        configuration.setComponents(List.of(tableComponent));

        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setName("Device inventory report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setFormat(TbReportFormat.CSV);

        ReportTemplate savedTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        List<Device> devices = new ArrayList<>();
        List<String> expectedReportLines = new ArrayList<>();
        expectedReportLines.add(tableHeadingText);
        expectedReportLines.add(dataKeys.stream().map(DataKey::getLabel).collect(Collectors.joining(",")));

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

        // Check content
        List<String> actualLines = new ArrayList<>(Arrays.asList(csvReport.split("\r?\n")));
        assertThat(actualLines).containsExactlyInAnyOrderElementsOf(expectedReportLines);
    }

    @Test
    public void testReportWithAlarmTableComponent() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDevicesEntityAlias(devicesAliasId);

        AlarmTableComponent alarmTableComponent = new AlarmTableComponent();
        AlarmFilterConfig alarmFilterConfig = new AlarmFilterConfig();
        alarmFilterConfig.setSeverityList(List.of(AlarmSeverity.WARNING));
        List<DataKey> dataKeys = List.of(
                new DataKey("createdTime", "alarm", "ALARM CREATED TIME"),
                new DataKey("name", "entityField", "ORIGINATOR"),
                new DataKey("originator", "alarm", "ORIGINATOR2"),
                new DataKey("type", "alarm", "ALARM TYPE"),
                new DataKey("status", "alarm", "ALARM STATUS")
        );
        alarmTableComponent.setAlarmSource(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .alarmFilterConfig(alarmFilterConfig)
                .dataKeys(dataKeys)
                .build());
        TimeWindowConfiguration timewindow = buildCurrentDatTimeWindow();
        alarmTableComponent.setTimewindow(timewindow);

        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAliases(List.of(entityAlias));
        configuration.setComponents(List.of(alarmTableComponent));

        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setName("Device inventory report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setFormat(TbReportFormat.CSV);

        ReportTemplate savedTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Device device = new Device();
            device.setCustomerId(customerId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        List<String> expectedReportLines = new ArrayList<>();
        expectedReportLines.add(dataKeys.stream().map(DataKey::getLabel).collect(Collectors.joining(",")));

        for (int i = 0; i < devices.size(); i++) {
            Alarm alarm = new Alarm();
            Device device = devices.get(i);
            alarm.setOriginator(device.getId());
            alarm.setType("alarm" + i);
            alarm.setSeverity(AlarmSeverity.WARNING);
            Alarm createdAlarm = doPost("/api/alarm", alarm, Alarm.class);
            Thread.sleep(1);

            expectedReportLines.add(createdAlarm.getCreatedTime() + "," +
                    device.getName() + "," +
                    device.getName() + "," +
                    alarm.getType() + "," +
                    alarm.getStatus());
        }

        //generate report
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateConfig(configuration);
        String csvReport = doPost("/api/v2/report/test", reportRequest, String.class);

        // Check content
        List<String> actualLines = new ArrayList<>(Arrays.asList(csvReport.split("\r?\n")));
        assertThat(actualLines).containsExactlyInAnyOrderElementsOf(expectedReportLines);
    }

    @Test
    public void testReportWithTimeSeriesTable() throws Exception {
        String devicesAliasId = StringUtils.randomAlphabetic(10);
        EntityAlias entityAlias = buildDevicesEntityAlias(devicesAliasId);

        TimeseriesTableComponent tsComponent = new TimeseriesTableComponent();
        tsComponent.setShowTimestamp(true);
        tsComponent.setTimestampLabel("Timestamp");
        tsComponent.setTimestampPattern("milliseconds");
        tsComponent.setDataSources(List.of(DataSource.builder()
                .type(DataSourceType.ENTITY)
                .entityAliasId(devicesAliasId)
                .dataKeys(List.of(
                     new DataKey("temperature", "timeseries", "TEMPERATURE")
                ))
                .latestDataKeys(List.of(
                        new DataKey("name", "entityField", "NAME"),
                        new DataKey("active", "attribute", "ACTIVE")
                ))
                .build()));
        TimeWindowConfiguration timewindow = buildCurrentDatTimeWindow();
        tsComponent.setTimewindow(timewindow);

        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAliases(List.of(entityAlias));
        configuration.setComponents(List.of(tsComponent));

        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setName("Device inventory report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setFormat(TbReportFormat.CSV);

        ReportTemplate savedTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        List<Device> devices = new ArrayList<>();
        List<String> expectedReportLines = new ArrayList<>();
        int telemetryCount = 3;

        for (int i = 0; i < 18; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            device = doPost("/api/device", device, Device.class);
            devices.add(device);

            // headers
            expectedReportLines.add("Timestamp,TEMPERATURE,NAME,ACTIVE");

            for (int j = 0; j < telemetryCount; j++) {
                long temperature = (long) (Math.random() * 100);
                long threshold = (long) (Math.random() * 100);
                String attributePayload = "{\"threshold\":" + threshold + "}";
                long ts = System.currentTimeMillis() - 300000L * j;

                doPost("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/" + DataConstants.SERVER_SCOPE, JacksonUtil.toJsonNode(String.format("{\"ts\": %s, \"values\": {\"temperature\":%s}}", ts, temperature)));
                doPost("/api/plugins/telemetry/" + device.getId() + "/" + DataConstants.SHARED_SCOPE, attributePayload, String.class, status().isOk());
                expectedReportLines.add(ts + "," +
                                temperature + "," +
                                device.getName() + "," +
                                "false");
            }
        }

        //generate report
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateConfig(configuration);
        String csvReport = doPost("/api/v2/report/test", reportRequest, String.class);

        List<String> actualLines = new ArrayList<>(Arrays.asList(csvReport.split("\r?\n")));
        assertThat(actualLines).isEqualTo(expectedReportLines);
    }

    private static TimeWindowConfiguration buildCurrentDatTimeWindow() {
        TimeWindowConfiguration timewindow = new TimeWindowConfiguration();
        History history = new History();
        history.setHistoryType(2);
        history.setQuickInterval(QuickTimeInterval.CURRENT_DAY);
        history.setInterval(1000);
        timewindow.setHistory(history);
        timewindow.setTimezone(TimeZone.getDefault().getID());
        AggregationConfiguration aggregation = new AggregationConfiguration();
        aggregation.setType(Aggregation.NONE);
        aggregation.setLimit(25000);
        timewindow.setAggregation(aggregation);
        return timewindow;
    }

    @Test
    public void testCreateJobForCsvReport() throws Exception {
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

        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setEntityAliases(List.of(entityAlias));
        configuration.setComponents(List.of(tableComponent));
        configuration.setNamePattern("test.csv");

        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setName("Devices report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setFormat(TbReportFormat.CSV);
        reportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);
        ReportTemplateId reportTemplateId = reportTemplate.getId();

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

        NotificationTarget recipient = createNotificationTarget(new AffectedUserFilter());
        NotificationTemplate notificationTemplate = saveNotificationTemplate(DefaultNotifications.reportGenerated.toTemplate());

        //generate report
        ReportRequest reportRequest = new ReportRequest();
        reportRequest.setReportTemplateId(reportTemplateId);
        reportRequest.setRecipientId(recipient.getId());
        reportRequest.setNotificationTemplateId(notificationTemplate.getId());
        doPost("/api/v2/report/request", reportRequest, Job.class);

        List<Job> jobs = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                        findJobs(List.of(JobType.REPORT), List.of(reportTemplateId.getId())),
                result -> !result.isEmpty() && result.get(0).getStatus() == JobStatus.COMPLETED);
        Job job = jobs.get(0);
        assertThat(job.getResult().getCompletedCount()).isEqualTo(1);
        assertThat(job.getResult().getTotalCount()).isEqualTo(1);
        assertThat(job.getEntityId()).isEqualTo(reportTemplateId);
        assertThat(job.getEntityName()).isEqualTo(reportTemplate.getName());

        ReportJobResult result = (ReportJobResult) job.getResult();
        String csvReport = doGet("/api/v2/report/" + result.getReport().getId() + "/download", String.class);

        // Check headers and content
        String[] lines = csvReport.split("\r?\n");
        assertThat(lines[0]).contains("CREATED TIME,NAME,TYPE,TEMPERATURE,THRESHOLD");
        for (int i = 0; i < devices.size(); i++) {
            assertThat(lines[i + 1]).contains(expectedReportLines.get(i));
        }

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            Notification reportNotification = getMyNotifications(true, 10).stream()
                    .filter(notification -> notification.getType() == NotificationType.REPORT_GENERATED)
                    .findFirst().orElse(null);
            assertThat(reportNotification).isNotNull();
            assertThat(reportNotification.getSubject()).isEqualTo("Report generated");
            assertThat(reportNotification.getText()).isEqualTo("CSV report 'test.csv' is ready");
        });
    }

    @Test
    public void testGetReportInfos() throws Exception {
        loginTenantAdmin();
        ReportTemplate csvTemplate = buildReportTemplate(TbReportFormat.CSV);
        csvTemplate = doPost("/api/reportTemplate", csvTemplate, ReportTemplate.class);

        ReportTemplate pdfTemplate = buildReportTemplate(TbReportFormat.PDF);
        pdfTemplate = doPost("/api/reportTemplate", pdfTemplate, ReportTemplate.class);

        NotificationTarget recipient = createNotificationTarget(new AffectedUserFilter());
        NotificationTemplate notificationTemplate = saveNotificationTemplate(DefaultNotifications.reportGenerated.toTemplate());
        for (int i = 0; i < 5; i++) {
            ReportRequest csvRequest = new ReportRequest();
            csvRequest.setReportTemplateId(csvTemplate.getId());
            csvRequest.setRecipientId(recipient.getId());
            csvRequest.setNotificationTemplateId(notificationTemplate.getId());
            doPost("/api/v2/report/request", csvRequest, Job.class);

            ReportRequest pdfRequest = new ReportRequest();
            pdfRequest.setReportTemplateId(pdfTemplate.getId());
            pdfRequest.setRecipientId(recipient.getId());
            pdfRequest.setNotificationTemplateId(notificationTemplate.getId());
            doPost("/api/v2/report/request", pdfRequest, Job.class);
        }

        PageData<ReportInfo> reportInfos = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                        doGetTypedWithPageLink("/api/v2/reportInfos?", new TypeReference<PageData<ReportInfo>>() {
                        }, new PageLink(30)),
                result -> result.getData().size() == 10);
        for (ReportInfo info : reportInfos.getData()) {
            assertThat(info.getUserName()).isEqualTo(TENANT_ADMIN_EMAIL);
        }

        // filter by reportTemplateId
        PageData<ReportInfo> csvReportsByTemplateId = doGetTypedWithPageLink("/api/v2/reportInfos?reportTemplateId=" + csvTemplate.getId().getId() + "&", new TypeReference<>() {
        }, new PageLink(30));

        assertThat(csvReportsByTemplateId.getData()).hasSize(5);
        for (ReportInfo info : csvReportsByTemplateId.getData()) {
            assertThat(info.getTemplateInfo()).isEqualTo(new EntityInfo(csvTemplate.getId(), csvTemplate.getName()));
            assertThat(info.getFormat()).isEqualTo(TbReportFormat.CSV);
        }

        // filter by userId
        PageData<ReportInfo> csvReportsByUserId = doGetTypedWithPageLink("/api/v2/reportInfos?userId=" + customerAdminUserId.getId() + "&", new TypeReference<>() {
        }, new PageLink(30));

        assertThat(csvReportsByUserId.getData()).isEmpty();
    }

    private ReportTemplate buildReportTemplate(TbReportFormat format) {
        ReportTemplate template = new ReportTemplate();
        template.setName(StringUtils.randomAlphabetic(10));
        template.setType(ReportTemplateType.REPORT);
        template.setFormat(format);
        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setComponents(new ArrayList<>());
        template.setConfiguration(configuration);
        return template;
    }

    private static EntityAlias buildDevicesEntityAlias(String aliasId) {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");
        return new EntityAlias(aliasId, "devices", filter);
    }

}

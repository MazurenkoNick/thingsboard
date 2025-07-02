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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.common.data.scheduler.MonthlyRepeat;
import org.thingsboard.server.common.data.scheduler.ScheduledReportInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerRepeat;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class SchedulerEventControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testSaveSchedulerEvent() throws Exception {
        SchedulerEvent schedulerEvent = createSchedulerEvent();
        SchedulerEvent savedSchedulerEvent = doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);
        Assert.assertNotNull(savedSchedulerEvent);
        Assert.assertNotNull(savedSchedulerEvent.getId());
        Assert.assertTrue(savedSchedulerEvent.getCreatedTime() > 0);
        Assert.assertEquals(schedulerEvent.getName(), savedSchedulerEvent.getName());
        savedSchedulerEvent.setName("New Scheduler Event");
        doPost("/api/schedulerEvent", savedSchedulerEvent, SchedulerEvent.class);
        SchedulerEvent foundSchedulerEvent = doGet("/api/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString(), SchedulerEvent.class);
        Assert.assertEquals(savedSchedulerEvent.getName(), foundSchedulerEvent.getName());
        Assert.assertTrue(savedSchedulerEvent.isEnabled());
    }

    @Test
    public void testFindSchedulerEventById() throws Exception {
        SchedulerEvent schedulerEvent = createSchedulerEvent();
        SchedulerEvent savedSchedulerEvent = doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);
        SchedulerEvent foundSchedulerEvent = doGet("/api/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString(), SchedulerEvent.class);
        Assert.assertNotNull(foundSchedulerEvent);
        Assert.assertEquals(savedSchedulerEvent, foundSchedulerEvent);
    }

    @Test
    public void testDeleteSchedulerEvent() throws Exception {
        SchedulerEvent schedulerEvent = createSchedulerEvent();
        SchedulerEvent savedSchedulerEvent = doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);

        doDelete("/api/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindEdgeSchedulerEventInfosByTenantIdAndName() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        List<SchedulerEventId> edgeSchedulerEvents = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            SchedulerEvent schedulerEvent = createSchedulerEvent();
            schedulerEvent.setName("Scheduler Event " + i);
            SchedulerEvent savedSchedulerEvent = doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);
            doPost("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/schedulerEvent/" + savedSchedulerEvent.getId().getId().toString(), SchedulerEvent.class);
            edgeSchedulerEvents.add(savedSchedulerEvent.getId());
        }

        List<SchedulerEventId> loadedEdgeSchedulerEvents = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<SchedulerEventInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/schedulerEvents?",
                    new TypeReference<>() {}, pageLink);
            loadedEdgeSchedulerEvents.addAll(pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList()));
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertTrue(edgeSchedulerEvents.size() == loadedEdgeSchedulerEvents.size() &&
                edgeSchedulerEvents.containsAll(loadedEdgeSchedulerEvents));

        for (SchedulerEventId schedulerEventId : loadedEdgeSchedulerEvents) {
            doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                    + "/schedulerEvent/" + schedulerEventId.getId().toString(), SchedulerEventInfo.class);
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/schedulerEvents?",
                new TypeReference<>() {}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getTotalElements());
    }

    @Test
    public void testScheduleReportGeneration() throws Exception {
        loginTenantAdmin();
        ReportTemplate tenantTemplate = buildTemplate("CSV report template", ReportTemplateType.REPORT, TbReportFormat.CSV);
        tenantTemplate = doPost("/api/reportTemplate", tenantTemplate, ReportTemplate.class);

        SchedulerEvent schedulerEvent = createReportSchedulerEvent(tenantTemplate.getId(), tenantAdminUserId);
        doPost("/api/schedulerEvent", schedulerEvent, SchedulerEvent.class);

        loginCustomerAdminUser();
        ReportTemplate customerTemplate = buildTemplate("CSV report template", ReportTemplateType.REPORT, TbReportFormat.CSV);
        customerTemplate = doPost("/api/reportTemplate", customerTemplate, ReportTemplate.class);

        SchedulerEvent schedulerEvent2 = createReportSchedulerEvent(customerTemplate.getId(), customerAdminUserId);
        doPost("/api/schedulerEvent", schedulerEvent2, SchedulerEvent.class);

        // wait for reports
        loginTenantAdmin();
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() ->
                        doGetTypedWithPageLink("/api/v2/reportInfos?",  new TypeReference<PageData<ReportInfo>>() {
                        }, new PageLink(30)),
                result -> result.getData().size() == 2);

        // check report infos
        PageData<ScheduledReportInfo> scheduledReportInfos = doGetTypedWithPageLink("/api/scheduledReports?includeCustomers=true&", new TypeReference<PageData<ScheduledReportInfo>>() {
        }, new PageLink(30));
        assertThat(scheduledReportInfos.getData()).hasSize(2);

        // filter by report template id
        PageData<ScheduledReportInfo> tenantReportInfos = doGetTypedWithPageLink("/api/scheduledReports?includeCustomers=true&reportTemplateId=" + tenantTemplate.getId().getId() + "&", new TypeReference<PageData<ScheduledReportInfo>>() {
        }, new PageLink(30));
        assertThat(tenantReportInfos.getData()).hasSize(1);
    }

    private ReportTemplate buildTemplate(String templateName, ReportTemplateType report, TbReportFormat format) {
        ReportTemplate template = new ReportTemplate();
        template.setName(templateName);
        template.setType(report);
        template.setFormat(format);
        CsvReportTemplateConfig configuration = new CsvReportTemplateConfig();
        configuration.setComponents(new ArrayList<>());
        template.setConfiguration(configuration);
        return template;
    }

    private SchedulerEvent createReportSchedulerEvent(ReportTemplateId templateId, UserId userId) {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName("Report Scheduler Event");
        schedulerEvent.setType("generateReport");
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", System.currentTimeMillis() + 3000);
        schedule.put("timezone", "UTC");
        MonthlyRepeat schedulerRepeat = new MonthlyRepeat();
        schedule.set("repeat", JacksonUtil.valueToTree(schedulerRepeat));
        schedulerEvent.setSchedule(schedule);
        ReportConfig reportConfig = new ReportConfig();
        reportConfig.setReportTemplateId(templateId);
        reportConfig.setTimezone("Europe/Kiev");
        reportConfig.setUserId(userId);
        schedulerEvent.setConfiguration(JacksonUtil.valueToTree(reportConfig));
        return schedulerEvent;
    }

    private SchedulerEvent createSchedulerEvent() {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setName("Scheduler Event");
        schedulerEvent.setType("Custom Type");
        ObjectNode schedule = JacksonUtil.newObjectNode();
        schedule.put("startTime", System.currentTimeMillis());
        schedule.put("timezone", "UTC");
        SchedulerRepeat schedulerRepeat = new MonthlyRepeat();
        schedule.set("repeat", JacksonUtil.valueToTree(schedulerRepeat));
        schedulerEvent.setSchedule(schedule);
        return schedulerEvent;
    }
}

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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.scheduler.MonthlyRepeat;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class ReportTemplateServiceTest extends AbstractServiceTest {

    @Autowired
    ReportTemplateService reportTemplateService;
    @Autowired
    RelationService relationService;
    @Autowired
    SchedulerEventService schedulerEventService;

    private final IdComparator<ReportTemplateInfo> idComparator = new IdComparator<>();

    @Test
    public void testSaveReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setDescription("My report");
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);

        Assert.assertNotNull(savedReportTemplate);
        Assert.assertNotNull(savedReportTemplate.getId());
        Assert.assertTrue(savedReportTemplate.getCreatedTime() > 0);
        Assert.assertEquals(reportTemplate.getTenantId(), savedReportTemplate.getTenantId());
        Assert.assertNotNull(savedReportTemplate.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedReportTemplate.getCustomerId().getId());
        Assert.assertEquals(reportTemplate.getName(), savedReportTemplate.getName());

        savedReportTemplate.setName("My new report");

        reportTemplateService.saveReportTemplate(savedReportTemplate);
        ReportTemplate foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertEquals(foundReportTemplate.getName(), savedReportTemplate.getName());

        reportTemplateService.deleteReportTemplate(tenantId, savedReportTemplate.getId());
    }

    @Test
    public void testSaveReportTemplateWithEmptyName() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithEmptyFormat() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithEmptyType() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithNameContains0x00_thenDataValidationException() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());
        reportTemplate.setName("F0929906\000\000\000\000\000\000\000\000\000");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithEmptyTenant() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithInvalidTenant() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        reportTemplate.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testFindReportTemplateById() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);
        ReportTemplate foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertNotNull(foundReportTemplate);
        Assert.assertEquals(savedReportTemplate, foundReportTemplate);
        reportTemplateService.deleteReportTemplate(tenantId, savedReportTemplate.getId());
    }

    @Test
    public void testDeleteReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);
        EntityRelation relation = new EntityRelation(tenantId, savedReportTemplate.getId(), EntityRelation.CONTAINS_TYPE);
        relationService.saveRelation(tenantId, relation);

        ReportTemplate foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertNotNull(foundReportTemplate);
        reportTemplateService.deleteReportTemplate(tenantId, savedReportTemplate.getId());
        foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertNull(foundReportTemplate);
        Assert.assertTrue(relationService.findByTo(tenantId, savedReportTemplate.getId(), RelationTypeGroup.COMMON).isEmpty());
    }

    @Test
    public void testDeleteReportTemplateUsedInScheduler() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setFormat(TbReportFormat.PDF);
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);

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
        reportConfig.setReportTemplateId(savedReportTemplate.getId());
        reportConfig.setTimezone("Europe/Kiev");
        reportConfig.setUserId(new UserId(Uuids.random()));
        schedulerEvent.setConfiguration(JacksonUtil.valueToTree(reportConfig));
        schedulerEvent.setTenantId(tenantId);
        schedulerEventService.saveSchedulerEvent(schedulerEvent);

        Assertions.assertThrows(DataValidationException.class, () -> {
            reportTemplateService.deleteReportTemplate(tenantId, savedReportTemplate.getId());
        });
    }

    @Test
    public void testFindReportTemplatesByTenantId() {
        List<ReportTemplateInfo> reportTemplates = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setTenantId(tenantId);
            reportTemplate.setName("ReportTemplate" + i);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            reportTemplates.add(new ReportTemplateInfo(reportTemplateService.saveReportTemplate(reportTemplate)));
        }

        List<ReportTemplateInfo> loadedReportTemplates = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
            loadedReportTemplates.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        reportTemplates.sort(idComparator);
        loadedReportTemplates.sort(idComparator);

        Assert.assertEquals(reportTemplates, loadedReportTemplates);

        reportTemplateService.deleteReportTemplatesByTenantId(tenantId);

        pageLink = new PageLink(4);
        pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindReportTemplatesByTenantIdAndName() {
        String title1 = "Report title 1";
        List<ReportTemplateInfo> reportTemplatesTitle1 = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setTenantId(tenantId);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            reportTemplatesTitle1.add(new ReportTemplateInfo(reportTemplateService.saveReportTemplate(reportTemplate)));
        }
        String title2 = "Report title 2";
        List<ReportTemplateInfo> reportTemplatesTitle2 = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setTenantId(tenantId);
            reportTemplate.setFormat(TbReportFormat.PDF);
            reportTemplate.setType(ReportTemplateType.REPORT);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setConfiguration(PdfReportTemplateConfig.builder().components(Collections.emptyList()).build());
            reportTemplatesTitle2.add(new ReportTemplateInfo(reportTemplateService.saveReportTemplate(reportTemplate)));
        }

        List<ReportTemplateInfo> loadedReportTemplatesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3, 0, title1);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
            loadedReportTemplatesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        reportTemplatesTitle1.sort(idComparator);
        loadedReportTemplatesTitle1.sort(idComparator);

        Assert.assertEquals(reportTemplatesTitle1, loadedReportTemplatesTitle1);

        List<ReportTemplateInfo> loadedReportTemplatesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
            loadedReportTemplatesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        reportTemplatesTitle2.sort(idComparator);
        loadedReportTemplatesTitle2.sort(idComparator);

        Assert.assertEquals(reportTemplatesTitle2, loadedReportTemplatesTitle2);

        for (ReportTemplateInfo reportTemplate : reportTemplatesTitle1) {
            reportTemplateService.deleteReportTemplate(tenantId, reportTemplate.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (ReportTemplateInfo reportTemplate : reportTemplatesTitle2) {
            reportTemplateService.deleteReportTemplate(tenantId, reportTemplate.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = reportTemplateService.findReportTemplates(tenantId, ReportTemplateQuery.builder().pageLink(pageLink).build());
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

}

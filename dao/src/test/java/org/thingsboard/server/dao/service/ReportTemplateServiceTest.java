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
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfiguration;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class ReportTemplateServiceTest extends AbstractServiceTest {

    @Autowired
    ReportTemplateService reportTemplateService;
    @Autowired
    SchedulerEventService schedulerEventService;
    @Autowired
    RelationService relationService;

    private final IdComparator<ReportTemplateInfo> idComparator = new IdComparator<>();

    @Test
    public void testSaveReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setDescription("My report");
        reportTemplate.setConfiguration(new ReportTemplateConfiguration());
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
        reportTemplate.setConfiguration(new ReportTemplateConfiguration());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithNameContains0x00_thenDataValidationException() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setConfiguration(new ReportTemplateConfiguration());
        reportTemplate.setName("F0929906\000\000\000\000\000\000\000\000\000");
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithEmptyTenant() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setConfiguration(new ReportTemplateConfiguration());
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testSaveReportTemplateWithInvalidTenant() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setConfiguration(new ReportTemplateConfiguration());
        reportTemplate.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> reportTemplateService.saveReportTemplate(reportTemplate));
    }

    @Test
    public void testFindReportTemplateById() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setConfiguration(new ReportTemplateConfiguration());
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
        reportTemplate.setConfiguration(new ReportTemplateConfiguration());
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
    public void testFindReportTemplatesByTenantId() {
        List<ReportTemplateInfo> reportTemplates = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setTenantId(tenantId);
            reportTemplate.setName("ReportTemplate" + i);
            reportTemplate.setConfiguration(new ReportTemplateConfiguration());
            reportTemplates.add(new ReportTemplateInfo(reportTemplateService.saveReportTemplate(reportTemplate)));
        }

        List<ReportTemplateInfo> loadedReportTemplates = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = reportTemplateService.findReportTemplatesByTenantId(tenantId, pageLink);
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
        pageData = reportTemplateService.findReportTemplatesByTenantId(tenantId, pageLink);
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
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setConfiguration(new ReportTemplateConfiguration());
            reportTemplatesTitle1.add(new ReportTemplateInfo(reportTemplateService.saveReportTemplate(reportTemplate)));
        }
        String title2 = "Report title 2";
        List<ReportTemplateInfo> reportTemplatesTitle2 = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setConfiguration(new ReportTemplateConfiguration());
            reportTemplatesTitle2.add(new ReportTemplateInfo(reportTemplateService.saveReportTemplate(reportTemplate)));
        }

        List<ReportTemplateInfo> loadedReportTemplatesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3, 0, title1);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = reportTemplateService.findReportTemplatesByTenantId(tenantId, pageLink);
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
            pageData = reportTemplateService.findReportTemplatesByTenantId(tenantId, pageLink);
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
        pageData = reportTemplateService.findReportTemplatesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (ReportTemplateInfo reportTemplate : reportTemplatesTitle2) {
            reportTemplateService.deleteReportTemplate(tenantId, reportTemplate.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = reportTemplateService.findReportTemplatesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }


    @Test
    public void testDeleteReportTemplateWithSchedulerEvent() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setConfiguration(new ReportTemplateConfiguration());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);

        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setTenantId(tenantId);
        schedulerEvent.setName("Schedule for my report");
        schedulerEvent.setType("Report");
        schedulerEvent.setConfiguration(JacksonUtil.newObjectNode());
        schedulerEvent.setSchedule(JacksonUtil.newObjectNode());

        SchedulerEvent savedSchedulerEvent = schedulerEventService.saveSchedulerEvent(schedulerEvent);
        savedReportTemplate.setSchedulerEventId(savedSchedulerEvent.getId());
        reportTemplateService.saveReportTemplate(savedReportTemplate);

        SchedulerEvent foundSchedulerEvent = schedulerEventService.findSchedulerEventById(tenantId, savedSchedulerEvent.getId());
        Assert.assertNotNull(foundSchedulerEvent);

        reportTemplateService.deleteReportTemplate(tenantId, savedReportTemplate.getId());
        ReportTemplate foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertNull(foundReportTemplate);

        foundSchedulerEvent = schedulerEventService.findSchedulerEventById(tenantId, savedSchedulerEvent.getId());
        Assert.assertNull(foundSchedulerEvent);
    }

    @Test
    public void testDeleteSchedulerEventForReportTemplate() {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setTenantId(tenantId);
        reportTemplate.setName("My report");
        reportTemplate.setConfiguration(new ReportTemplateConfiguration());
        ReportTemplate savedReportTemplate = reportTemplateService.saveReportTemplate(reportTemplate);

        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setTenantId(tenantId);
        schedulerEvent.setName("Schedule for my report");
        schedulerEvent.setType("Report");
        schedulerEvent.setConfiguration(JacksonUtil.newObjectNode());
        schedulerEvent.setSchedule(JacksonUtil.newObjectNode());

        SchedulerEvent savedSchedulerEvent = schedulerEventService.saveSchedulerEvent(schedulerEvent);
        savedReportTemplate.setSchedulerEventId(savedSchedulerEvent.getId());
        savedReportTemplate = reportTemplateService.saveReportTemplate(savedReportTemplate);
        Assert.assertNotNull(savedReportTemplate.getSchedulerEventId());

        schedulerEventService.deleteSchedulerEvent(tenantId, savedSchedulerEvent.getId());

        ReportTemplate foundReportTemplate = reportTemplateService.findReportTemplateById(tenantId, savedReportTemplate.getId());
        Assert.assertNull(foundReportTemplate.getSchedulerEventId());
    }

}

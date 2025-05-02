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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class ReportTemplateControllerTest extends AbstractControllerTest {

    private final IdComparator<ReportTemplateInfo> idComparator = new IdComparator<>();

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
    public void testSaveReportTemplate() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setDescription("My report");
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());

        Mockito.reset(tbClusterService, auditLogService);

        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        testNotifyEntityEntityGroupNullAllOneTime(savedReportTemplate, savedReportTemplate.getId(), savedReportTemplate.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED);

        Assert.assertNotNull(savedReportTemplate);
        Assert.assertNotNull(savedReportTemplate.getId());
        Assert.assertTrue(savedReportTemplate.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedReportTemplate.getTenantId());
        Assert.assertNotNull(savedReportTemplate.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedReportTemplate.getCustomerId().getId());
        Assert.assertEquals(reportTemplate.getName(), savedReportTemplate.getName());
        Assert.assertEquals(reportTemplate.getDescription(), savedReportTemplate.getDescription());
        Assert.assertEquals(reportTemplate.getConfiguration(), savedReportTemplate.getConfiguration());

        Mockito.reset(tbClusterService, auditLogService);

        savedReportTemplate.setName("My new report");
        doPost("/api/reportTemplate", savedReportTemplate, ReportTemplate.class);

        testNotifyEntityEntityGroupNullAllOneTime(savedReportTemplate, savedReportTemplate.getId(), savedReportTemplate.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);

        ReportTemplate foundReportTemplate = doGet("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString(), ReportTemplate.class);
        Assert.assertEquals(foundReportTemplate.getName(), savedReportTemplate.getName());
    }

    @Test
    public void testSaveReportTemplateWithViolationOfLengthValidation() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName(StringUtils.randomAlphabetic(300));
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("name");
        doPost("/api/reportTemplate", reportTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(reportTemplate, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        reportTemplate.setName("Normal name");
        reportTemplate.setDescription(StringUtils.randomAlphabetic(2000));
        msgError = "description length must be equal or less than 1024";
        doPost("/api/reportTemplate", reportTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(reportTemplate, savedTenant.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testUpdateReportTemplateFromDifferentTenant() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());
        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "REPORT_TEMPLATE '" + savedReportTemplate.getName() + "'!";
        doPost("/api/reportTemplate", savedReportTemplate)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionWrite + msgError)));

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermissionDelete + msgError)));

        testNotifyEntityNever(savedReportTemplate.getId(), savedReportTemplate);

        deleteDifferentTenant();
    }

    @Test
    public void testFindReportTemplateById() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());
        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);
        ReportTemplate foundReportTemplate = doGet("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString(), ReportTemplate.class);
        Assert.assertNotNull(foundReportTemplate);
        Assert.assertEquals(savedReportTemplate, foundReportTemplate);
    }

    @Test
    public void testDeleteReportTemplate() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());
        ReportTemplate savedReportTemplate = doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/reportTemplate/" + savedReportTemplate.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityEntityGroupNullAllOneTime(savedReportTemplate, savedReportTemplate.getId(), savedReportTemplate.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, savedReportTemplate.getId().getId().toString());

        String reportTemplateIdStr = savedReportTemplate.getId().getId().toString();
        doGet("/api/reportTemplate/" + reportTemplateIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Report template", reportTemplateIdStr))));
    }

    @Test
    public void testSaveReportTemplateWithEmptyName() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setType(ReportTemplateType.REPORT);
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Report template name " + msgErrorShouldBeSpecified;
        doPost("/api/reportTemplate", reportTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(reportTemplate, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testSaveReportTemplateWithEmptyType() throws Exception {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setName("My report");
        reportTemplate.setConfiguration(new PdfReportTemplateConfig());

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Report template type " + msgErrorShouldBeSpecified;
        doPost("/api/reportTemplate", reportTemplate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(reportTemplate, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testFindReportTemplates() throws Exception {
        List<ReportTemplateInfo> reportTemplates = new ArrayList<>();
        int cntEntity = 178;

        Mockito.reset(tbClusterService, auditLogService);

        for (int i = 0; i < cntEntity; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            reportTemplate.setName("ReportTemplate" + i);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(new PdfReportTemplateConfig());
            reportTemplates.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }
        List<ReportTemplateInfo> loadedReportTemplates = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedReportTemplates.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new ReportTemplate(), new ReportTemplate(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        reportTemplates.sort(idComparator);
        loadedReportTemplates.sort(idComparator);

        Assert.assertEquals(reportTemplates, loadedReportTemplates);
    }

    @Test
    public void testFindReportTemplatesByName() throws Exception {
        String title1 = "Report template title 1";
        List<ReportTemplateInfo> reportTemplatesTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(new PdfReportTemplateConfig());
            reportTemplatesTitle1.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }
        String title2 = "Report template title 2";
        List<ReportTemplateInfo> reportTemplatesTitle2 = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            ReportTemplate reportTemplate = new ReportTemplate();
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            reportTemplate.setName(name);
            reportTemplate.setType(ReportTemplateType.REPORT);
            reportTemplate.setConfiguration(new PdfReportTemplateConfig());
            reportTemplatesTitle2.add(new ReportTemplateInfo(doPost("/api/reportTemplate", reportTemplate, ReportTemplate.class)));
        }

        List<ReportTemplateInfo> loadedReportTemplatesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<ReportTemplateInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                    new TypeReference<>() {
                    }, pageLink);
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
            pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedReportTemplatesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        reportTemplatesTitle2.sort(idComparator);
        loadedReportTemplatesTitle2.sort(idComparator);

        Assert.assertEquals(reportTemplatesTitle2, loadedReportTemplatesTitle2);

        for (ReportTemplateInfo reportTemplate : loadedReportTemplatesTitle1) {
            doDelete("/api/reportTemplate/" + reportTemplate.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (ReportTemplateInfo reportTemplate : loadedReportTemplatesTitle2) {
            doDelete("/api/reportTemplate/" + reportTemplate.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/reportTemplateInfos/all?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

}

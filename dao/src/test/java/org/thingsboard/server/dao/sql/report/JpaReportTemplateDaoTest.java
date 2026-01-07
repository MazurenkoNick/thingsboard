/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.report;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.PdfReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.report.ReportTemplateDao;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JpaReportTemplateDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private ReportTemplateDao reportTemplateDao;


    @Test
    public void testSaveReportTemplateName0x00_thenSomeDatabaseException() {
        assertThatThrownBy(() ->
                saveReportTemplate(UUID.randomUUID(), Uuids.timeBased(), Uuids.timeBased(), "F0929906\000\000\000\000\000\000\000\000\000"));
    }

    private ReportTemplate saveReportTemplate(UUID id, UUID tenantId, UUID customerId, String name) {
        return saveReportTemplate(id, tenantId, customerId, name, new PdfReportTemplateConfig(), null);
    }

    private ReportTemplate saveReportTemplate(UUID id, UUID tenantId, UUID customerId, String name, ReportTemplateConfig configuration, String description) {
        ReportTemplate reportTemplate = new ReportTemplate();
        reportTemplate.setId(new ReportTemplateId(id));
        reportTemplate.setTenantId(TenantId.fromUUID(tenantId));
        reportTemplate.setCustomerId(new CustomerId(customerId));
        reportTemplate.setName(name);
        reportTemplate.setFormat(configuration.getFormat());
        reportTemplate.setConfiguration(configuration);
        reportTemplate.setDescription(description);
        return reportTemplateDao.save(TenantId.fromUUID(tenantId), reportTemplate);
    }
}

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
package org.thingsboard.server.service.license;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.license.client.TbLicenseStatisticsService;
import org.thingsboard.license.shared.TbStatistics;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.dao.mobile.QrCodeSettingsDao;
import org.thingsboard.server.dao.rule.RuleNodeDao;
import org.thingsboard.server.service.install.ProjectInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "anonymous-usage-reporting", value = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DefaultTbLicenseStatisticsService implements TbLicenseStatisticsService {

    private static final List<EntityType> COUNTED_TYPES = List.of(EntityType.TENANT, EntityType.CUSTOMER, EntityType.DEVICE,
            EntityType.ASSET, EntityType.RULE_CHAIN, EntityType.DASHBOARD);

    private final EntityDaoRegistry entityDaoRegistry;
    private final IntegrationDao integrationDao;
    private final RuleNodeDao ruleNodeDao;
    private final ConverterDao converterDao;
    private final QrCodeSettingsDao qrCodeSettingsDao;
    private final ProjectInfo projectInfo;

    @Value("#{('${database.ts.type}' == 'cassandra') or ('${database.ts_latest.type}' == 'cassandra')}")
    private boolean dbHybrid;

    @Override
    public TbStatistics getCurrentStatistics() {
        TbStatistics statistics = new TbStatistics();

        Map<String, Long> entitiesCounts = new HashMap<>();
        for (EntityType entityType : COUNTED_TYPES) {
            long count = entityDaoRegistry.getDao(entityType).count();
            entitiesCounts.put(entityType.name(), count);
        }
        statistics.setEntitiesCounts(entitiesCounts);

        statistics.setRuleNodeTypes(ruleNodeDao.countRuleNodesPerType());
        statistics.setIntegrationsCountsPerType(integrationDao.countIntegrationsPerType());

        statistics.setGenericConverters(converterDao.countGenericConverters());
        statistics.setTypedConverters(converterDao.countTypedConverters());
        statistics.setJsConvertersCount(converterDao.contByJsScriptLang());
        statistics.setTbelConvertersCount(converterDao.contByTbelScriptLang());

        statistics.setQrCodeUsage(qrCodeSettingsDao.count());

        statistics.setTbVersion(projectInfo.getProjectVersion());
        statistics.setDbHybrid(dbHybrid);
        return statistics;
    }
}

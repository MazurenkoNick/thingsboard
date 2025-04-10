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
import org.thingsboard.license.shared.TbInstanceStatistics;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "TB_ANONYMOUS_USAGE_REPORTING", havingValue = "true", matchIfMissing = true)
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
    private boolean cassandra;

    @Value("#{('${database.ts.type}' == 'timescale') or ('${database.ts_latest.type}' == 'timescale')}")
    private boolean timescale;

    @Override
    public TbInstanceStatistics getCurrentStatistics() {
        TbInstanceStatistics statistics = new TbInstanceStatistics();

        Map<String, Long> entitiesCounts = new HashMap<>();
        for (EntityType entityType : COUNTED_TYPES) {
            entitiesCounts.put(entityType.name(), getSafely(() -> entityDaoRegistry.getDao(entityType).count()));
        }
        statistics.setEntitiesCounts(entitiesCounts);

        statistics.setRuleNodeTypes(getSafely(() ->
                        ruleNodeDao.countRuleNodesPerType().entrySet().stream()
                                .collect(Collectors.toMap(
                                        entry -> prepareRuleNodeType(entry.getKey()),
                                        Map.Entry::getValue
                                ))
                , null));

        statistics.setIntegrationsCountsPerType(getSafely(integrationDao::countIntegrationsPerType, null));

        statistics.setGenericConverters(getSafely(converterDao::countGenericConverters));
        statistics.setTypedConverters(getSafely(converterDao::countTypedConverters));
        statistics.setDedicatedConverters(getSafely(converterDao::countDedicatedConverters));
        statistics.setJsConvertersCount(getSafely(converterDao::countByJsScriptLang));
        statistics.setTbelConvertersCount(getSafely(converterDao::countByTbelScriptLang));

        statistics.setQrCodeUsage(getSafely(qrCodeSettingsDao::count));

        statistics.setTbVersion(projectInfo.getProjectVersion());
        statistics.setCassandra(cassandra);
        statistics.setTimescale(timescale);
        return statistics;
    }

    private Long getSafely(Supplier<Long> task) {
        return getSafely(task, -1L);
    }

    private <T> T getSafely(Supplier<T> task, T defaultValue) {
        try {
            return task.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String prepareRuleNodeType(String type) {
        if (!type.startsWith("org.thingsboard")) {
            return "custom." + type.substring(type.lastIndexOf('.') + 1);
        }
        return type;
    }
}

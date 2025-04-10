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

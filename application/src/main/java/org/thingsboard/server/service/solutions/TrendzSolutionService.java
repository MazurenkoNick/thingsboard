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
package org.thingsboard.server.service.solutions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.TrendzApiClient;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.solutions.data.CreatedEntityInfo;
import org.thingsboard.server.service.solutions.data.DashboardLinkInfo;
import org.thingsboard.server.service.solutions.data.SolutionInstallContext;
import org.thingsboard.server.service.solutions.trendz.TrendzEntityPreprocessor;
import org.thingsboard.server.service.solutions.trendz.TrendzEntityPreprocessorManager;
import org.thingsboard.server.service.solutions.trendz.data.TrendzEntityType;
import org.thingsboard.server.service.solutions.trendz.data.TrendzPreprocessConfig;
import org.thingsboard.server.service.solutions.trendz.data.TrendzPreprocessResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class TrendzSolutionService {

    private final JwtTokenFactory jwtTokenFactory;
    private final DashboardService dashboardService;
    private final TrendzSettingsService trendzSettingsService;
    private final TrendzEntityPreprocessorManager trendzEntityPreprocessorManager;

    @Autowired
    public TrendzSolutionService(
            JwtTokenFactory jwtTokenFactory,
            DashboardService dashboardService,
            TrendzSettingsService trendzSettingsService,
            TrendzEntityPreprocessorManager trendzEntityPreprocessorManager
    ) {
        this.jwtTokenFactory = jwtTokenFactory;
        this.dashboardService = dashboardService;
        this.trendzSettingsService = trendzSettingsService;
        this.trendzEntityPreprocessorManager = trendzEntityPreprocessorManager;
    }


    public Map<TrendzEntityType, Set<UUID>> provisionTrendzSolution(Path solutionDir, SolutionInstallContext ctx) {
        if (!trendzSolutionExists(ctx.getSolutionId(), solutionDir)) {
            log.debug("Trendz solution is not found, skipping");
            return Collections.emptyMap();
        }
        log.debug("Start installation of Trendz part of the solution template");


        Set<UUID> customerIdSet = ctx.getCreatedEntities().entrySet().stream()
                .filter(entry -> entry.getValue().getType().equals("Customer"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (customerIdSet.size() > 1) {
            throw new RuntimeException("Trendz solution template contains more than one customer");
        }

        UUID tenantId = ctx.getTenantId().getId();
        UUID customerId = customerIdSet.isEmpty() ? EntityId.NULL_UUID : customerIdSet.iterator().next();
        UUID userId = ctx.getUser().getUuidId();

        AccessJwtToken accessJwtToken = this.jwtTokenFactory.createAccessJwtToken((SecurityUser) ctx.getUser());
        String jwtToken = accessJwtToken.getToken();

        Map<TrendzEntityType, Set<UUID>> trendzEntityIds = Collections.emptyMap();
        try {
            TrendzSettings trendzSettings = this.trendzSettingsService.findTrendzSettings(TenantId.fromUUID(tenantId));

            boolean validSettings = validateTrendzSettings(trendzSettings);
            if (!validSettings) {
                return Collections.emptyMap();
            }
            String trendzUrl = extractUrl(trendzSettings.getBaseUrl());
            TrendzApiClient client = new TrendzApiClient(trendzUrl, TrendzApiClient.TokenType.JWT, jwtToken);

            validateInstall(client);

            Map<String, Object> importData = loadMigrationData(ctx.getSolutionId(), solutionDir);
            TrendzPreprocessConfig config = createPreprocessConfig(importData, ctx.getCreatedEntities(), tenantId, customerId, userId);
            TrendzPreprocessResult preprocessResult = preprocess(config);

            refreshIdsInDashboard(ctx.getTenantId(), ctx.getDashboardLinks(), config.getOldToNewIdMap());

            ObjectNode preparedMigrationData = preprocessResult.getPreparedMigrationData();
            trendzEntityIds = preprocessResult.getTrendzEntityIds();

            UUID importExecutionId = client.sendImportMigrationData(preparedMigrationData);
            client.awaitTaskExecution(importExecutionId);

        } catch (Exception e) {
            deleteTrendzSolution(solutionDir, ctx.getUser(), ctx.getTenantId(), ctx.getSolutionId(), trendzEntityIds);
            throw new RuntimeException(e);
        }
        log.debug("Installation of Trendz part of the solution template is finished");
        return trendzEntityIds;
    }

    public void runTrendzSolutionDataGeneration(SolutionInstallContext ctx, Map<TrendzEntityType, Set<UUID>> trendzEntityIds) {
        if (trendzEntityIds.isEmpty()) {
            return;
        }

        UUID tenantId = ctx.getTenantId().getId();

        AccessJwtToken accessJwtToken = this.jwtTokenFactory.createAccessJwtToken((SecurityUser) ctx.getUser());
        String jwtToken = accessJwtToken.getToken();

        try {
            TrendzSettings trendzSettings = this.trendzSettingsService.findTrendzSettings(TenantId.fromUUID(tenantId));
            String trendzUrl = extractUrl(trendzSettings.getBaseUrl());

            TrendzApiClient client = new TrendzApiClient(trendzUrl, TrendzApiClient.TokenType.JWT, jwtToken);

            Set<UUID> calculationFieldIdSet = trendzEntityIds.get(TrendzEntityType.CALCULATION_FIELD);
            Set<UUID> taskSequenceIdSet = trendzEntityIds.get(TrendzEntityType.TASK_SEQUENCE);

            // processing
            for (UUID id : taskSequenceIdSet) {
                UUID taskId = client.loadTaskIdByReference("TASK_SEQUENCE_EXECUTION", id.toString());
                UUID executionId = client.runTask(taskId);
                client.awaitTaskExecution(executionId);
            }

            // post activation for calculations
            for (UUID id : calculationFieldIdSet) {
                ObjectNode body = makeEnableCalculationFieldRequest(id, true);
                client.sendEnableCalculationFieldRequest(body);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteTrendzSolution(Path solutionDir, User user, TenantId tenant, String solutionId, Map<TrendzEntityType, Set<UUID>> trendzEntityIds) {
        if (!trendzSolutionExists(solutionId, solutionDir)) {
            log.debug("Trendz solution is not found, skipping");
            return;
        }
        log.debug("Start removing of Trendz part of the solution template");
        UUID tenantId = tenant.getId();

        try {
            TrendzSettings trendzSettings = this.trendzSettingsService.findTrendzSettings(TenantId.fromUUID(tenantId));
            boolean validSettings = validateTrendzSettings(trendzSettings);
            if (!validSettings) {
                return;
            }
            String trendzUrl = extractUrl(trendzSettings.getBaseUrl());

            AccessJwtToken accessJwtToken = this.jwtTokenFactory.createAccessJwtToken((SecurityUser) user);
            String jwtToken = accessJwtToken.getToken();

            TrendzApiClient client = new TrendzApiClient(trendzUrl, TrendzApiClient.TokenType.JWT, jwtToken);

            validateDelete(client);

            for (TrendzEntityType type : TrendzEntityType.values()) {
                String deletePath = type.getDeletePath();

                Set<UUID> IdSet = trendzEntityIds.getOrDefault(type, Collections.emptySet());
                for (UUID id : IdSet) {
                    client.sendDeleteEntity(deletePath, id);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.debug("Removing Trendz part of the solution template is finished");
    }


    private boolean trendzSolutionExists(String solutionId, Path solutionDir) {
        Path filePath = resolve(solutionDir, solutionId, "entities", "trendz_migration_data.json");
        return Files.exists(filePath);
    }


    private void validateInstall(TrendzApiClient client) {
        boolean available = client.isTrendzServiceReachable();
        if (!available) {
            throw new RuntimeException("Trendz service is not available.");
        }

        boolean validSubscription = client.sendTrendzSubscriptionValid();
        if (!validSubscription) {
            throw new RuntimeException("Trendz license subscription is not active.");
        }

        boolean keyIsSet = client.sendCheckSigningKey();
        if (!keyIsSet) {
            throw new RuntimeException("JWT signing key is not set in Trendz service. Please check your Trendz settings and try again.");
        }
    }

    private void validateDelete(TrendzApiClient client) {
        boolean available = client.isTrendzServiceReachable();
        if (!available) {
            throw new RuntimeException("Trendz service is not available.");
        }
    }


    private Map<String, Object> loadMigrationData(String solutionId, Path solutionDir) {
        Map<String, Object> importData = loadEntityIfFileExists(solutionDir, solutionId, "trendz_migration_data.json", Map.class);
        assert importData != null;
        return importData;
    }


    private TrendzPreprocessConfig createPreprocessConfig(Map<String, Object> importData, Map<UUID, CreatedEntityInfo> createdEntities, UUID tenantId, UUID customerId, UUID userId) {
        Map<UUID, UUID> oldToNewIdMap = new HashMap<>();
        oldToNewIdMap.put(EntityId.NULL_UUID, EntityId.NULL_UUID);

        ZonedDateTime endDate = ZonedDateTime.now();
        ZonedDateTime startDate = endDate.minusDays(62).truncatedTo(ChronoUnit.DAYS);

        Map<String, String> nameToIdMap = createdEntities.entrySet().stream()
                .map(entry -> Map.entry(entry.getValue().getName(), entry.getKey().toString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return TrendzPreprocessConfig.builder()
                .importData(importData)
                .oldToNewIdMap(oldToNewIdMap)
                .nameToIdMap(nameToIdMap)
                .tenantId(tenantId)
                .customerId(customerId)
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private TrendzPreprocessResult preprocess(TrendzPreprocessConfig config) {
        for (TrendzEntityType type : TrendzEntityType.values()) {
            TrendzEntityPreprocessor preprocessor = this.trendzEntityPreprocessorManager.getPreprocessor(type);
            preprocessor.preprocess(config);
        }

        Map<String, Object> importData = config.getImportData();
        ObjectNode preparedMigrationData = (ObjectNode) JacksonUtil.valueToTree(importData);
        Map<TrendzEntityType, Set<UUID>> trendzEntityIds = new HashMap<>();
        for (TrendzEntityType type : TrendzEntityType.values()) {
            Set<UUID> idSet = extractIdSet(type.getCollectionName(), preparedMigrationData);
            trendzEntityIds.put(type, idSet);
        }
        return new TrendzPreprocessResult(preparedMigrationData, trendzEntityIds);
    }

    private Set<UUID> extractIdSet(String entityField, ObjectNode preparedMigrationData) {
        ArrayNode entityArrayNode = (ArrayNode) preparedMigrationData.get(entityField);
        return StreamSupport.stream(entityArrayNode.spliterator(), false)
                .map(entity -> entity.get("id").asText())
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }


    private ObjectNode makeEnableCalculationFieldRequest(UUID calculationFieldId, boolean enabled) {
        ObjectNode requestNode = JacksonUtil.newObjectNode();
        requestNode.put("calculationFieldId", calculationFieldId.toString());
        requestNode.put("enabled", enabled);
        requestNode.put("tzName", "UTC");
        requestNode.put("refreshTimeUnit", "HOUR");
        requestNode.put("refreshTimeUnitCount", 1);

        ObjectNode reprocessDatePickerConfig = JacksonUtil.newObjectNode();
        requestNode.set("reprocessDatePickerConfig", reprocessDatePickerConfig);
        reprocessDatePickerConfig.put("startTs", 0);
        reprocessDatePickerConfig.put("endTs", 0);

        ArrayNode itemSetNode = JacksonUtil.newArrayNode();
        requestNode.set("itemSet", itemSetNode);

        return requestNode;
    }

    private void refreshIdsInDashboard(TenantId tenantId, List<DashboardLinkInfo> dashboardLinkInfos, Map<UUID, UUID> oldToNewIdMap) {
        for (DashboardLinkInfo dashboardLinkInfo : dashboardLinkInfos) {
            Dashboard dashboard = this.dashboardService.findDashboardById(tenantId, dashboardLinkInfo.getDashboardId());
            JsonNode configurationJson = replaceIds(oldToNewIdMap, dashboard.getConfiguration());
            dashboard.setConfiguration(configurationJson);
            this.dashboardService.saveDashboard(dashboard);
        }
    }

    private JsonNode replaceIds(Map<UUID, UUID> oldToNewIdMap, JsonNode dashboardJson) {
        String jsonStr = JacksonUtil.toString(dashboardJson);
        assert jsonStr != null;
        for (var e : oldToNewIdMap.entrySet()) {
            jsonStr = jsonStr.replace(e.getKey().toString(), e.getValue().toString());
        }
        return JacksonUtil.toJsonNode(jsonStr);
    }


    private <T> T loadEntityIfFileExists(Path solutionDir, String solutionId, String fileName, Class<T> clazz) {
        Path filePath = resolve(solutionDir, solutionId, "entities", fileName);
        if (Files.exists(filePath)) {
            return JacksonUtil.readValue(filePath.toFile(), clazz);
        } else {
            return null;
        }
    }

    private Path resolve(Path solutionDir, String subdir, String... subdirs) {
        return solutionDir.resolve(Paths.get(subdir, subdirs));
    }

    private String extractUrl(String baseUrl) {
        try {
            URL url = new URL(baseUrl);
            return url.getProtocol() + "://" + url.getAuthority();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Trendz URL: " + baseUrl, e);
        }
    }


    public boolean validateTrendzSettings(TrendzSettings settings) {
        if (!settings.isEnabled()) {
            log.debug("Trendz is not enabled");
            return false;
        }
        if (settings.getBaseUrl() == null) {
            log.debug("Trendz base url is not configured");
            return false;
        }
        try {
            URL url = new URL(settings.getBaseUrl()); // catching MalformedURLException
        } catch (Exception e) {
            log.debug("URL is not valid: {}", settings.getBaseUrl());
            return false;
        }
        return true;
    }
}

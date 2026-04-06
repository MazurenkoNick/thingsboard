/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.entitiy.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.BulkOperationPreview;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.BulkOperationResult;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkippedApp;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultAgentBulkOperationService implements AgentBulkOperationService {

    private static final int BATCH_UPDATE_SIZE = 50;
    private static final Set<AgentAppEventActionType> ALLOWED_BULK_ACTIONS = Set.of(
            AgentAppEventActionType.UPDATE,
            AgentAppEventActionType.DELETE,
            AgentAppEventActionType.RESTART,
            AgentAppEventActionType.ROLLBACK,
            AgentAppEventActionType.UPGRADE
    );

    private final TbAgentApplicationService tbAgentApplicationService; // todo replace with event orchestrator
    private final AgentAppProfileService profileService;
    private final AgentApplicationDao applicationDao;
    private final AgentAppEventService agentAppEventService;
    private final AgentBulkActionService agentBulkActionService;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;

    @Value("${queue.agent.bulk-ops-pack-processing-timeout:600000}")
    private long stuckActionThresholdMs;

    @Scheduled(initialDelayString = "${queue.agent.bulk-ops-stuck-check-interval-ms:600000}",
            fixedDelayString = "${queue.agent.bulk-ops-stuck-check-interval-ms:600000}")
    public void failStuckBulkActions() {
        if (!partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
            return;
        }
        long threshold = System.currentTimeMillis() - stuckActionThresholdMs;
        List<AgentBulkAction> stuckActions = agentBulkActionService.findByStatusIn(
                List.of(AgentBulkActionStatus.IN_PROGRESS));
        for (AgentBulkAction action : stuckActions) {
            Long processingStartedTime = action.getProcessingStartedTime();
            if (processingStartedTime != null && processingStartedTime < threshold) {
                log.warn("Failing stuck bulk action {} (processing started at {}, threshold {})", action.getId(), processingStartedTime, threshold);
                action.setStatus(AgentBulkActionStatus.FAILED);
                action.setErrorMsg("Stuck in IN_PROGRESS state, failed by cleanup job");
                agentBulkActionService.save(action.getTenantId(), action);
            }
        }
    }

    @Override
    public AgentBulkAction enqueueBulkOperation(TenantId tenantId, AgentGroupId groupId, AgentAppProfileId profileId, BulkOperationRequest request) {
        AgentAppEventActionType actionType = request.getActionType();
        if (actionType == null || !ALLOWED_BULK_ACTIONS.contains(actionType)) {
            throw new DataValidationException("Action type '" + actionType + "' is not allowed for bulk operations");
        }
        AgentBulkAction bulkAction = saveBulkAction(tenantId, groupId, profileId, actionType);
        clusterService.pushMsgToAgentBulkOps(bulkAction, request);

        return bulkAction;
    }

    @Override
    public BulkOperationPreview preview(TenantId tenantId, AgentGroupId groupId, AgentAppProfileId profileId, BulkOperationRequest request) {
        AgentAppEventActionType actionType = request.getActionType();
        if (actionType == null || !ALLOWED_BULK_ACTIONS.contains(actionType)) {
            throw new DataValidationException("Action type '" + actionType + "' is not allowed for bulk operations");
        }
        AgentAppProfile profile = profileService.findProfileById(tenantId, profileId);

        BulkOperationResult result = new BulkOperationResult();
        List<AgentApplication> eligibleApps = filterEligibleApps(groupId, result, profile, actionType, true);

        BulkOperationPreview preview = new BulkOperationPreview();
        preview.setTotal(result.getTotal().get());
        preview.setEligible(eligibleApps.size());
        preview.setSkipped(new ArrayList<>(result.getSkipped()));
        return preview;
    }

    @Override
    public void processBulkOperation(AgentBulkOperationMsg msg) {
        TenantId tenantId = TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        AgentBulkActionId bulkActionId = new AgentBulkActionId(new UUID(msg.getBulkActionIdMSB(), msg.getBulkActionIdLSB()));
        try {
            AgentGroupId groupId = new AgentGroupId(new UUID(msg.getGroupIdMSB(), msg.getGroupIdLSB()));
            AgentAppProfileId profileId = new AgentAppProfileId(new UUID(msg.getProfileIdMSB(), msg.getProfileIdLSB()));
            AgentAppEventActionType actionType = AgentAppEventActionType.valueOf(msg.getActionType());
            boolean force = msg.getForce();

            ByteString stepInputsBytes = msg.getStepInputs();
            Map<UUID, AgentAppStepState> stepInputs = null;
            if (!stepInputsBytes.isEmpty()) {
                stepInputs = convertByteStringToStepInputs(stepInputsBytes);
            }

            AgentBulkAction bulkAction = agentBulkActionService.findById(tenantId, bulkActionId);
            if (bulkAction == null) {
                log.warn("Bulk action {} not found, skipping", bulkActionId);
                return;
            }

            bulkAction.setStatus(AgentBulkActionStatus.IN_PROGRESS);
            bulkAction.setProcessingStartedTime(System.currentTimeMillis());
            agentBulkActionService.save(tenantId, bulkAction);

            AgentAppProfile profile = profileService.findProfileById(tenantId, profileId);

            filterAppsAndExecuteBulkOperations(tenantId, groupId, bulkAction, profile, actionType, force, stepInputs);
        } catch (Exception e) {
            log.error("Bulk operation {} failed unexpectedly", bulkActionId, e);
            try {
                AgentBulkAction bulkAction = agentBulkActionService.findById(tenantId, bulkActionId);
                if (bulkAction != null) {
                    bulkAction.setStatus(AgentBulkActionStatus.FAILED);
                    bulkAction.setErrorMsg("Unexpected error: " + e.getMessage());
                    agentBulkActionService.save(tenantId, bulkAction);
                }
            } catch (Exception saveError) {
                log.error("Failed to save FAILED status for bulk action {}", bulkActionId, saveError);
            }
        }
    }

    private void filterAppsAndExecuteBulkOperations(TenantId tenantId, AgentGroupId groupId, AgentBulkAction bulkAction,
                                                    AgentAppProfile profile, AgentAppEventActionType actionType,
                                                    boolean force, Map<UUID, AgentAppStepState> stepInputs) {
        BulkOperationResult result = new BulkOperationResult();
        List<AgentApplication> eligibleApps;
        try {
            eligibleApps = filterEligibleApps(groupId, result, profile, actionType, force);
        } catch (Exception e) {
            log.error("Bulk operation {} failed during filtering for bulkAction {}", actionType, bulkAction.getId(), e);
            bulkAction.setStatus(AgentBulkActionStatus.FAILED);
            bulkAction.setErrorMsg(e.getMessage());
            agentBulkActionService.save(tenantId, bulkAction);
            return;
        }

        bulkAction.setTotal(result.getTotal().get());

        if (eligibleApps.isEmpty()) {
            bulkAction.setStatus(AgentBulkActionStatus.COMPLETED);
            bulkAction.setSkipCounts(buildSkipCounts(result));
            agentBulkActionService.save(tenantId, bulkAction);
            return;
        }

        agentBulkActionService.save(tenantId, bulkAction);

        execBulkOperationForEach(tenantId, bulkAction, actionType, stepInputs, eligibleApps, result);
    }

    private void execBulkOperationForEach(TenantId tenantId, AgentBulkAction bulkAction,
                                          AgentAppEventActionType actionType, Map<UUID, AgentAppStepState> stepInputs,
                                          List<AgentApplication> eligibleApps, BulkOperationResult result) {
        final UUID bulkActionId = bulkAction.getId().getId();
        int processed = 0;
        for (var app : eligibleApps) {
            try {
                execBulkOperation(tenantId, app, actionType, bulkActionId, stepInputs);
                result.incrementSubmitted();
            } catch (Exception e) {
                result.getSkipped().add(getSkippedOnFailure(e, app, actionType));
            }
            processed++;
            if (processed % BATCH_UPDATE_SIZE == 0 || processed == eligibleApps.size()) {
                bulkAction.setSubmitted(result.getSubmitted().get());
                bulkAction.setSkipCounts(buildSkipCounts(result));
                if (processed == eligibleApps.size()) {
                    bulkAction.setStatus(AgentBulkActionStatus.COMPLETED);
                }
                agentBulkActionService.save(tenantId, bulkAction);
            }
        }
    }

    private void execBulkOperation(TenantId tenantId, AgentApplication app,
                                   AgentAppEventActionType actionType,
                                   UUID bulkActionId, Map<UUID, AgentAppStepState> stepInputs) throws Exception {
        AgentAppEventRequest eventRequest = new AgentAppEventRequest();
        eventRequest.setActionType(actionType);
        eventRequest.setApplication(app);
        eventRequest.setStepInputs(stepInputs);
        eventRequest.setBulkActionId(bulkActionId);
        tbAgentApplicationService.execActionEvent(tenantId, app.getId(), eventRequest, true);
    }

    private Map<UUID, AgentAppStepState> convertByteStringToStepInputs(ByteString stepInputsBytes) {
        return JacksonUtil.fromString(
                stepInputsBytes.toString(StandardCharsets.UTF_8),
                new TypeReference<>() {
                });
    }

    private Map<SkipReason, Integer> buildSkipCounts(BulkOperationResult result) {
        Map<SkipReason, Integer> counts = new EnumMap<>(SkipReason.class);
        for (SkippedApp skipped : result.getSkipped()) {
            counts.merge(skipped.getReason(), 1, Integer::sum);
        }
        return counts;
    }

    private SkippedApp getSkippedOnFailure(Throwable throwable, AgentApplication app, AgentAppEventActionType actionType) {
        log.warn("Failed to execute bulk {} for app {}: {}", actionType, app.getId(), throwable.getMessage());
        SkipReason reason = SkipReason.ERROR;
        if (throwable instanceof ThingsboardException tbe
                && tbe.getErrorCode().equals(ThingsboardErrorCode.TOO_MANY_REQUESTS)) {
            reason = SkipReason.ACTIVE_EVENT;
        }
        return new SkippedApp(app.getId().toString(), app.getName(), reason, throwable.getMessage());
    }

    private List<AgentApplication> filterEligibleApps(AgentGroupId groupId, BulkOperationResult result, AgentAppProfile profile,
                                                     AgentAppEventActionType actionType, boolean force) {
        PageDataIterable<AgentApplication> it = new PageDataIterable<>(
                link -> applicationDao.findByApplicationProfileIdAndAgentGroupId(profile.getId().getId(), groupId.getId(), link),
                100);

        List<AgentApplication> eligibleApps = new ArrayList<>();
        for (var app : it) {
            result.incrementTotal();
            Optional<SkipReason> skipReason = shouldSkipOperation(profile, actionType, app);

            if (skipReason.isPresent()) {
                if (!force) {
                    throw new DataValidationException(
                            "Bulk operation blocked: app " + app.getId() + " has blocker: " + skipReason.get() +
                                    ". Use force=true to skip problematic apps.");
                }
                result.getSkipped().add(new SkippedApp(app.getId().toString(), app.getName(), skipReason.get()));
            } else {
                eligibleApps.add(app);
            }
        }
        return eligibleApps;
    }

    private Optional<SkipReason> shouldSkipOperation(AgentAppProfile profile, AgentAppEventActionType actionType, AgentApplication app) {
        SkipReason skipReason = null;

        if (actionType == AgentAppEventActionType.UPGRADE && !app.getTemplateId().equals(profile.getTemplateId())) {
            skipReason = SkipReason.VERSION_MISMATCH;
        } else if (agentAppEventService.hasActiveEventForApplication(app.getId())) {
            skipReason = SkipReason.ACTIVE_EVENT;
        }

        return Optional.ofNullable(skipReason);
    }

    private AgentBulkAction saveBulkAction(TenantId tenantId, AgentGroupId groupId, AgentAppProfileId profileId,
                                           AgentAppEventActionType actionType) {
        AgentBulkAction bulkAction = new AgentBulkAction();
        bulkAction.setTenantId(tenantId);
        bulkAction.setGroupId(groupId.getId());
        bulkAction.setProfileId(profileId.getId());
        bulkAction.setActionType(actionType);
        bulkAction.setStatus(AgentBulkActionStatus.QUEUED);
        return agentBulkActionService.save(tenantId, bulkAction);
    }

}

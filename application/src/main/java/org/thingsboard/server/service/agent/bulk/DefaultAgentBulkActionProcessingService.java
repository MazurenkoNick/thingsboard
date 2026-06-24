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
package org.thingsboard.server.service.agent.bulk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventDeliveryState;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.agent.BulkOperationPreview;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.BulkOperationResult;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkippedApp;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentProfileId;
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
import org.thingsboard.server.service.entitiy.agent.TbAgentApplicationService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
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
public class DefaultAgentBulkActionProcessingService implements AgentBulkActionProcessingService {

    private static final int BATCH_UPDATE_SIZE = 50;
    private static final int PREVIEW_SAMPLE_PER_REASON = 20;
    private static final Set<AgentAppEventActionType> ALLOWED_BULK_ACTIONS = Set.of(
            AgentAppEventActionType.UPDATE,
            AgentAppEventActionType.DELETE,
            AgentAppEventActionType.RESTART,
            AgentAppEventActionType.ROLLBACK,
            AgentAppEventActionType.UPGRADE
    );

    private final TbAgentApplicationService tbAgentApplicationService;
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
        PageDataIterable<AgentBulkAction> stuckActions = new PageDataIterable<>(
                link -> agentBulkActionService.findStuckBulkActions(threshold, link), 100);
        for (AgentBulkAction action : stuckActions) {
            AgentBulkActionStatus originalStatus = action.getStatus();
            log.warn("Failing stuck bulk action {} in status {} (threshold {})",
                    action.getId(), originalStatus, threshold);
            action.setStatus(AgentBulkActionStatus.START_FAILED);
            action.setErrorMsg("Stuck in " + originalStatus + " state, failed by cleanup job");
            agentBulkActionService.save(action.getTenantId(), action);
        }
    }

    @Override
    public AgentBulkAction enqueueBulkOperation(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId, BulkOperationRequest request) {
        AgentAppEventActionType actionType = request.getActionType();
        if (actionType == null || !ALLOWED_BULK_ACTIONS.contains(actionType)) {
            throw new DataValidationException("Action type '" + actionType + "' is not allowed for bulk operations");
        }
        AgentBulkAction bulkAction = saveBulkAction(tenantId, agentProfileId, applicationProfileId, actionType);
        clusterService.pushMsgToAgentBulkOps(bulkAction, request);

        return bulkAction;
    }

    @Override
    public BulkOperationPreview preview(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId, BulkOperationRequest request) {
        AgentAppEventActionType actionType = request.getActionType();
        if (actionType == null || !ALLOWED_BULK_ACTIONS.contains(actionType)) {
            throw new DataValidationException("Action type '" + actionType + "' is not allowed for bulk operations");
        }
        AgentAppProfileInfo profile = profileService.findProfileInfoById(tenantId, applicationProfileId);

        BulkOperationResult result = new BulkOperationResult();
        List<AgentApplicationInfo> eligibleApps = filterEligibleApps(tenantId, agentProfileId, result, profile, actionType);

        BulkOperationPreview preview = new BulkOperationPreview();
        preview.setTotal(result.getTotal());
        preview.setEligible(eligibleApps.size());
        preview.setSkippedCountsByReason(buildSkipCounts(result));
        preview.setSkippedSample(sampleSkippedPerReason(result.getSkipped()));
        return preview;
    }

    @Override
    public void processBulkOperation(AgentBulkOperationMsg msg) {
        TenantId tenantId = TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        AgentBulkActionId bulkActionId = new AgentBulkActionId(new UUID(msg.getBulkActionIdMSB(), msg.getBulkActionIdLSB()));
        try {
            AgentProfileId agentProfileId = new AgentProfileId(new UUID(msg.getAgentProfileIdMSB(), msg.getAgentProfileIdLSB()));
            AgentAppProfileId applicationProfileId = new AgentAppProfileId(new UUID(msg.getApplicationProfileIdMSB(), msg.getApplicationProfileIdLSB()));
            AgentAppEventActionType actionType = AgentAppEventActionType.valueOf(msg.getActionType());

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

            AgentAppProfileInfo profile = profileService.findProfileInfoById(tenantId, applicationProfileId);

            filterAppsAndExecuteBulkOperations(tenantId, agentProfileId, bulkAction, profile, actionType, stepInputs);
        } catch (Exception e) {
            log.error("Bulk operation {} failed unexpectedly", bulkActionId, e);
            try {
                AgentBulkAction bulkAction = agentBulkActionService.findById(tenantId, bulkActionId);
                if (bulkAction != null) {
                    bulkAction.setStatus(AgentBulkActionStatus.START_FAILED);
                    bulkAction.setErrorMsg("Unexpected error: " + e.getMessage());
                    agentBulkActionService.save(tenantId, bulkAction);
                }
            } catch (Exception saveError) {
                log.error("Failed to save FAILED status for bulk action {}", bulkActionId, saveError);
            }
        }
    }

    private void filterAppsAndExecuteBulkOperations(TenantId tenantId, AgentProfileId agentProfileId, AgentBulkAction bulkAction,
                                                    AgentAppProfileInfo profile, AgentAppEventActionType actionType,
                                                    Map<UUID, AgentAppStepState> stepInputs) {
        BulkOperationResult result = new BulkOperationResult();
        List<AgentApplicationInfo> eligibleApps;
        try {
            eligibleApps = filterEligibleApps(tenantId, agentProfileId, result, profile, actionType);
        } catch (Exception e) {
            log.error("Bulk operation {} failed during filtering for bulkAction {}", actionType, bulkAction.getId(), e);
            bulkAction.setStatus(AgentBulkActionStatus.START_FAILED);
            bulkAction.setErrorMsg(e.getMessage());
            agentBulkActionService.save(tenantId, bulkAction);
            return;
        }

        bulkAction.setTotal(result.getTotal());

        if (eligibleApps.isEmpty()) {
            bulkAction.setStatus(AgentBulkActionStatus.START_FAILED);
            bulkAction.setErrorMsg("Couldn't find any eligible applications for execution");
            bulkAction.setSkipCounts(buildSkipCounts(result));
            agentBulkActionService.save(tenantId, bulkAction);
            return;
        }
        agentBulkActionService.save(tenantId, bulkAction);
        saveErrorMsgsForSkipped(tenantId, bulkAction, actionType, stepInputs, result);
        execBulkOperationForEach(tenantId, bulkAction, actionType, stepInputs, result, eligibleApps);
    }

    private void saveErrorMsgsForSkipped(TenantId tenantId, AgentBulkAction bulkAction,
                                         AgentAppEventActionType actionType, Map<UUID, AgentAppStepState> stepInputs,
                                         BulkOperationResult result) {
        result.getSkipped().forEach(s -> {
            final UUID bulkActionId = bulkAction.getId().getId();

            // save synthetic error event for users to understand the status of the execution
            AgentAppEvent errEvent = new AgentAppEvent();
            errEvent.setTenantId(tenantId);
            errEvent.setApplicationId(s.getApplicationId());
            errEvent.setAgentId(s.getAgentId());
            errEvent.setApplicationName(s.getApplicationName());
            errEvent.setActionType(actionType);
            errEvent.setDeliveryState(AgentAppEventDeliveryState.DELIVERY_FAIL);
            errEvent.setStatus(AgentAppEventStatus.START_FAILED);
            errEvent.setErrorMessage(s.getReason() + " " + s.getMsg());
            errEvent.setUpdatedTime(System.currentTimeMillis());
            errEvent.setStepStates(stepInputs);
            errEvent.setBulkActionId(bulkActionId);

            agentAppEventService.save(tenantId, errEvent, false);
        });
    }

    private void execBulkOperationForEach(TenantId tenantId, AgentBulkAction bulkAction,
                                          AgentAppEventActionType actionType, Map<UUID, AgentAppStepState> stepInputs,
                                          BulkOperationResult result, List<AgentApplicationInfo> eligibleApps) {
        final UUID bulkActionId = bulkAction.getId().getId();
        int processed = 0;
        for (var app : eligibleApps) {
            try {
                AgentAppEvent event = execAppActionEvent(tenantId, app, actionType, bulkActionId, stepInputs);
                if (event != null) {
                    result.incrementSubmitted();
                }
            } catch (Exception e) {
                result.getSkipped().add(getSkippedOnFailure(e, app, actionType));
            }
            processed++;
            if (processed % BATCH_UPDATE_SIZE == 0 || processed == eligibleApps.size()) {
                bulkAction.setSubmitted(result.getSubmitted());
                bulkAction.setSkipCounts(buildSkipCounts(result));
                if (processed == eligibleApps.size()) {
                    bulkAction.setStatus(AgentBulkActionStatus.STARTED);
                }
                agentBulkActionService.save(tenantId, bulkAction);
            }
        }
    }

    private AgentAppEvent execAppActionEvent(TenantId tenantId, AgentApplication app,
                                             AgentAppEventActionType actionType,
                                             UUID bulkActionId, Map<UUID, AgentAppStepState> stepInputs) throws Exception {
        AgentAppEventRequest eventRequest = new AgentAppEventRequest();
        eventRequest.setActionType(actionType);
        eventRequest.setApplication(app);
        eventRequest.setStepInputs(stepInputs);
        eventRequest.setBulkActionId(bulkActionId);
        // Run the active-event check (under the per-app row lock in execActionEvent) so two concurrent
        // bulk actions on overlapping apps can't both create an active event for the same application.
        return tbAgentApplicationService.execActionEvent(tenantId, app.getId(), eventRequest);
    }

    private Map<UUID, AgentAppStepState> convertByteStringToStepInputs(ByteString stepInputsBytes) {
        String json = stepInputsBytes.toString(StandardCharsets.UTF_8);
        if (json.isBlank() || "null".equals(json)) {
            return null;
        }
        return JacksonUtil.fromString(json, new TypeReference<>() {});
    }

    private Map<SkipReason, Integer> buildSkipCounts(BulkOperationResult result) {
        Map<SkipReason, Integer> counts = new EnumMap<>(SkipReason.class);
        for (SkippedApp skipped : result.getSkipped()) {
            counts.merge(skipped.getReason(), 1, Integer::sum);
        }
        return counts;
    }

    private List<SkippedApp> sampleSkippedPerReason(Collection<SkippedApp> skipped) {
        Map<SkipReason, Integer> perReason = new EnumMap<>(SkipReason.class);
        List<SkippedApp> sample = new ArrayList<>();
        for (SkippedApp s : skipped) {
            int taken = perReason.getOrDefault(s.getReason(), 0);
            if (taken < DefaultAgentBulkActionProcessingService.PREVIEW_SAMPLE_PER_REASON) {
                sample.add(s);
                perReason.put(s.getReason(), taken + 1);
            }
        }
        return sample;
    }

    private SkippedApp getSkippedOnFailure(Throwable throwable, AgentApplicationInfo app, AgentAppEventActionType actionType) {
        log.warn("Failed to execute bulk {} for app {}: {}", actionType, app.getId(), throwable.getMessage());
        return toSkippedApp(app, resolveSkipReason(throwable), throwable.getMessage());
    }

    private SkipReason resolveSkipReason(Throwable throwable) {
        if (throwable instanceof TbRateLimitsException) {
            return SkipReason.RATE_LIMIT_EXCEEDED;
        }
        if (throwable instanceof ThingsboardException
                && TbAgentApplicationService.EVENT_IN_PROGRESS_ERROR_MSG.equals(throwable.getMessage())) {
            return SkipReason.ACTIVE_EVENT;
        }
        return SkipReason.ERROR;
    }

    private List<AgentApplicationInfo> filterEligibleApps(TenantId tenantId, AgentProfileId agentProfileId, BulkOperationResult result,
                                                          AgentAppProfileInfo profile, AgentAppEventActionType actionType) {
        PageDataIterable<AgentApplicationInfo> it = new PageDataIterable<>(
                link -> applicationDao.findByApplicationProfileIdAndAgentProfileId(tenantId, profile.getId().getId(), agentProfileId.getId(), link),
                100);

        List<AgentApplicationInfo> eligibleApps = new ArrayList<>();
        for (var app : it) {
            result.incrementTotal();
            Optional<SkipReason> skipReason = shouldSkipOperation(profile, actionType, app);

            if (skipReason.isPresent()) {
                result.getSkipped().add(toSkippedApp(app, skipReason.get(), null));
            } else {
                eligibleApps.add(app);
            }
        }
        return eligibleApps;
    }

    private SkippedApp toSkippedApp(AgentApplicationInfo app, SkipReason reason, String msg) {
        return new SkippedApp(
                app.getAgentId(),
                app.getAgentName(),
                app.getId(),
                app.getName(),
                reason,
                msg
        );
    }

    private Optional<SkipReason> shouldSkipOperation(AgentAppProfileInfo profile, AgentAppEventActionType actionType, AgentApplicationInfo app) {
        SkipReason skipReason = null;

        if (templateVersionEqualityRequired(actionType) && !app.getTemplateId().equals(profile.getTemplateId())) {
            skipReason = SkipReason.VERSION_MISMATCH;
        } else if (agentAppEventService.hasActiveOrPendingEventForApplication(app.getId())) {
            skipReason = SkipReason.ACTIVE_EVENT;
        } else if (isValidForUpgrade(profile, actionType, app)) {
            skipReason = SkipReason.VERSION_MISMATCH;
        }

        return Optional.ofNullable(skipReason);
    }

    private boolean isValidForUpgrade(AgentAppProfileInfo profile, AgentAppEventActionType actionType, AgentApplicationInfo app) {
        return actionType == AgentAppEventActionType.UPGRADE && !profile.getTemplateCurrentVersion().equals(app.getNextVersion());
    }

    private boolean templateVersionEqualityRequired(AgentAppEventActionType actionType) {
        return actionType != AgentAppEventActionType.UPGRADE && actionType != AgentAppEventActionType.RESTART;
    }

    private AgentBulkAction saveBulkAction(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId,
                                           AgentAppEventActionType actionType) {
        AgentBulkAction bulkAction = new AgentBulkAction();
        bulkAction.setTenantId(tenantId);
        bulkAction.setAgentProfileId(agentProfileId.getId());
        bulkAction.setApplicationProfileId(applicationProfileId.getId());
        bulkAction.setActionType(actionType);
        bulkAction.setStatus(AgentBulkActionStatus.QUEUED);
        return agentBulkActionService.save(tenantId, bulkAction);
    }

}

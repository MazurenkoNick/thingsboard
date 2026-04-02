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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.BulkOperationPreview;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.BulkOperationResult;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkipReason;
import org.thingsboard.server.common.data.agent.BulkOperationResult.SkippedApp;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultAgentBulkOperationService implements AgentBulkOperationService {

    private static final Set<AgentAppEventActionType> ALLOWED_BULK_ACTIONS = Set.of(
            AgentAppEventActionType.UPDATE,
            AgentAppEventActionType.DELETE,
            AgentAppEventActionType.RESTART,
            AgentAppEventActionType.ROLLBACK,
            AgentAppEventActionType.UPGRADE
    );

    private final AgentAppProfileService profileService;
    private final AgentApplicationDao applicationDao;
    private final AgentAppEventService agentAppEventService;
    private final TbAgentApplicationService tbAgentApplicationService;
    private final AgentBulkActionService agentBulkActionService;

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        executor = ThingsBoardExecutors.newLimitedTasksExecutor(
                Runtime.getRuntime().availableProcessors(), 10_000, "bulk-agent-ops");
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public AgentBulkAction bulkOperation(TenantId tenantId, AgentGroupId groupId, AgentAppProfileId profileId,
                                         BulkOperationRequest request, boolean force, User user) {
        AgentAppEventActionType actionType = request.getActionType();
        if (actionType == null || !ALLOWED_BULK_ACTIONS.contains(actionType)) {
            throw new DataValidationException("Action type '" + actionType + "' is not allowed for bulk operations");
        }
        AgentAppProfile profile = profileService.findProfileById(tenantId, profileId);

        BulkOperationResult result = new BulkOperationResult();
        List<AgentApplication> eligibleApps = filterEligibleApps(groupId, result, profile, actionType, force);

        AgentBulkAction bulkAction = saveBulkAction(tenantId, groupId, profileId, actionType);

        if (eligibleApps.isEmpty()) {
            bulkAction.setTotal(result.getTotal().get());
            bulkAction.setSkipCounts(buildSkipCounts(result));
            return agentBulkActionService.save(tenantId, bulkAction);
        }

        final UUID bulkActionId = bulkAction.getId().getId();
        CountDownLatch latch = new CountDownLatch(eligibleApps.size());
        SecurityContext securityContext = SecurityContextHolder.getContext();

        for (var app : eligibleApps) {
            DonAsynchron.submit(() -> {
                SecurityContextHolder.setContext(securityContext);
                return execBulkOperation(tenantId, request, app, actionType, user, bulkActionId);
            }, success -> {
                result.incrementSubmitted();
                latch.countDown();
            }, throwable -> {
                SkippedApp skipped = getSkippedOnFailure(throwable, app, actionType);
                result.getSkipped().add(skipped);
                latch.countDown();
            }, executor);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bulk operation interrupted", e);
        }

        bulkAction.setTotal(result.getTotal().get());
        bulkAction.setSubmitted(result.getSubmitted().get());
        bulkAction.setSkipCounts(buildSkipCounts(result));
        return agentBulkActionService.save(tenantId, bulkAction);
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

    private Map<SkipReason, Integer> buildSkipCounts(BulkOperationResult result) {
        Map<SkipReason, Integer> counts = new EnumMap<>(SkipReason.class);
        for (SkippedApp skipped : result.getSkipped()) {
            counts.merge(skipped.getReason(), 1, Integer::sum);
        }
        return counts;
    }

    private AgentBulkAction saveBulkAction(TenantId tenantId, AgentGroupId groupId, AgentAppProfileId profileId, AgentAppEventActionType actionType) {
        AgentBulkAction bulkAction = new AgentBulkAction();

        bulkAction.setTenantId(tenantId);
        bulkAction.setGroupId(groupId.getId());
        bulkAction.setProfileId(profileId.getId());
        bulkAction.setActionType(actionType);
        return agentBulkActionService.save(tenantId, bulkAction);
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

    private AgentApplication execBulkOperation(TenantId tenantId, BulkOperationRequest request, AgentApplication app,
                                               AgentAppEventActionType actionType, User user, UUID bulkActionId) throws Exception {
        AgentAppEventRequest eventRequest = new AgentAppEventRequest();
        eventRequest.setActionType(actionType);
        eventRequest.setApplication(app);
        eventRequest.setStepInputs(request.getStepInputs());
        eventRequest.setBulkActionId(bulkActionId);
        boolean skipActiveEventCheck = true;
        tbAgentApplicationService.execActionEvent(tenantId, app.getId(), eventRequest, user, skipActiveEventCheck);
        return app;
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

    private Optional<SkipReason> shouldSkipOperation(AgentAppProfile profile, AgentAppEventActionType actionType, AgentApplication app) {
        SkipReason skipReason = null;

        if (actionType == AgentAppEventActionType.UPGRADE && !app.getTemplateId().equals(profile.getTemplateId())) {
            skipReason = SkipReason.VERSION_MISMATCH;
        } else if (agentAppEventService.hasActiveEventForApplication(app.getId())) {
            skipReason = SkipReason.ACTIVE_EVENT;
        }

        return Optional.ofNullable(skipReason);
    }
}

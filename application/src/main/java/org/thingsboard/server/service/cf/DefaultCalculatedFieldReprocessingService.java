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
package org.thingsboard.server.service.cf;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest.Strategy;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval.AggInterval;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.CfReprocessingTask;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.AggIntervalEntry;
import org.thingsboard.server.service.cf.ctx.state.aggregation.single.EntityAggregationCalculatedFieldState;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createDefaultKvEntry;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.createStateByType;
import static org.thingsboard.server.utils.CalculatedFieldArgumentUtils.transformSingleValueArgument;

@TbRuleEngineComponent
@Service
@Slf4j
public class DefaultCalculatedFieldReprocessingService extends AbstractCalculatedFieldProcessingService implements CalculatedFieldReprocessingService {

    private static final Set<EntityType> supportedReprocessingEntities = EnumSet.of(
            EntityType.DEVICE, EntityType.ASSET
    );

    @Value("${actors.calculated_fields.calculation_timeout:5}")
    private long cfCalculationResultTimeout;

    @Value("${queue.calculated_fields.telemetry_fetch_pack_size:2000}")
    private int telemetryFetchPackSize;

    private final ActorSystemContext systemContext;

    public DefaultCalculatedFieldReprocessingService(AttributesService attributesService,
                                                     TimeseriesService timeseriesService,
                                                     ApiLimitService apiLimitService,
                                                     RelationService relationService,
                                                     OwnersCacheService ownersService,
                                                     TbClusterService clusterService,
                                                     ActorSystemContext systemContext,
                                                     TelemetrySubscriptionService tsSubService) {
        super(attributesService, timeseriesService, tsSubService, apiLimitService, relationService, ownersService, clusterService);
        this.systemContext = systemContext;
    }

    @Override
    protected String getExecutorNamePrefix() {
        return "calculated-field-reprocessing-callback";
    }

    @Override
    public void reprocess(CfReprocessingTask task) throws Exception {
        TenantId tenantId = task.getTenantId();
        EntityId entityId = task.getEntityId();
        log.debug("[{}] Received reprocessing request: {}", tenantId, task);
        if (!supportedReprocessingEntities.contains(entityId.getEntityType())) {
            throw new IllegalArgumentException("EntityType '" + entityId.getEntityType() + "' is not supported for reprocessing");
        }
        CalculatedField calculatedField = task.getCalculatedField();
        if (calculatedField.getType() == CalculatedFieldType.ALARM) {
            throw new IllegalArgumentException("Reprocessing not applicable for this type");
        }
        if (OutputType.ATTRIBUTES.equals(calculatedField.getConfiguration().getOutput().getType())) {
            throw new IllegalArgumentException("'ATTRIBUTES' output type is not supported for reprocessing");
        }

        long startTs = task.getStartTs();
        long endTs = task.getEndTs();

        CalculatedFieldCtx cfCtx = new CalculatedFieldCtx(calculatedField, systemContext);
        cfCtx.setUseLatestTs(false);
        cfCtx.init();
        CalculatedFieldState state = initState(tenantId, entityId, cfCtx, startTs);
        CFReprocessingCtx ctx = buildCtx(tenantId, entityId, cfCtx, state);

        try (ctx) {
            ctx.checkStateSize();
            ctx.processInitialState(startTs);
            ctx.prepareCtx(startTs, endTs);
            ctx.processData(startTs, endTs);
            ctx.awaitResults();
        }
    }

    private Future<Void> processStateIfReady(CFReprocessingCtx ctx, long ts) throws Exception {
        CalculatedFieldState state = ctx.getState();
        boolean initialized = ctx.getCfCtx().isInitialized();
        if (initialized && state.isReady()) {
            log.trace("[{}][{}] Performing calculation for CF {}", ctx.getTenantId(), ctx.getEntityId(), ctx.getCfId());
            CalculatedFieldResult calculationResult = ctx.performCalculation(state).get(cfCalculationResultTimeout, TimeUnit.SECONDS);
            ctx.checkStateSize();
            if (!calculationResult.isEmpty()) {
                ctx.setLatestResult(new TbPair<>(ts, calculationResult));
                return saveResult(ctx, calculationResult, ts, Strategy.TIME_SERIES_ONLY);
            }
        } else {
            if (log.isTraceEnabled()) {
                if (!initialized) {
                    log.trace("[{}][{}] Calculated field state is not initialized! {}", ctx.getTenantId(), ctx.getEntityId(), ctx.getCfId());
                }
                if (!state.isReady()) {
                    log.trace("[{}][{}] Calculated field state is not ready! {}, {}", ctx.getTenantId(), ctx.getEntityId(), ctx.getCfId(), state.getReadinessStatus().errorMsg());
                }
            }
            ctx.checkStateSize();
        }
        return Futures.immediateVoidFuture();
    }

    private Future<Void> processArgumentValuesUpdate(CFReprocessingCtx ctx, Map<String, ArgumentEntry> newArgValues, long ts) throws Exception {
        if (newArgValues.isEmpty()) {
            log.info("[{}] No argument values to process for CF.", ctx.getCfId());
        }
        if (!ctx.getState().update(newArgValues, ctx.getCfCtx()).isEmpty()) {
            return processStateIfReady(ctx, ts);
        } else {
            return Futures.immediateVoidFuture();
        }
    }

    private CalculatedFieldState initState(TenantId tenantId, EntityId entityId, CalculatedFieldCtx ctx, long startTs) throws InterruptedException {
        CalculatedFieldState state = createStateByType(ctx, entityId);
        state.setCtx(ctx, null);
        state.init(false);
        if (CalculatedFieldType.ENTITY_AGGREGATION.equals(ctx.getCfType())) {
            return state;
        }

        Map<String, ArgumentEntry> arguments;
        try {
            arguments = fetchArguments(ctx, entityId, startTs).get(); // will be interrupted on task processing timeout
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException(cause.getMessage(), cause);
        }
        state.update(arguments, ctx);
        log.debug("[{}][{}] Initialized state for CF {}", tenantId, entityId, ctx.getCfId());
        return state;
    }

    @Override
    protected ListenableFuture<ArgumentEntry> fetchTsLatest(TenantId tenantId, EntityId entityId, Argument argument, long startTs) {
        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), 0, startTs, 0, 1, Aggregation.NONE);
        log.trace("[{}][{}] Fetching timeseries for latest for query {}", tenantId, entityId, query);
        ListenableFuture<List<TsKvEntry>> tsKvListFuture = timeseriesService.findAll(tenantId, entityId, List.of(query));

        return Futures.transform(tsKvListFuture, tsKvList -> {
            log.debug("[{}][{}] Fetched timeseries for latest for query {}: {}", tenantId, entityId, query, tsKvList);
            TsKvEntry tsKvEntry;
            if (tsKvList.isEmpty() || tsKvList.get(0) == null || tsKvList.get(0).getValue() == null) {
                tsKvEntry = new BasicTsKvEntry(startTs, createDefaultKvEntry(argument), SingleValueArgumentEntry.DEFAULT_VERSION);
            } else {
                tsKvEntry = tsKvList.get(0);
            }
            return transformSingleValueArgument(Optional.of(tsKvEntry));
        }, calculatedFieldCallbackExecutor);
    }

    private List<TsKvEntry> fetchTelemetryBatch(TenantId tenantId, EntityId entityId, Argument argument, long startTs, long endTs, int limit) throws InterruptedException {
        EntityId sourceEntityId = resolveEntityId(tenantId, entityId, argument);
        ReadTsKvQuery query = new BaseReadTsKvQuery(argument.getRefEntityKey().getKey(), startTs, endTs, 0, limit, Aggregation.NONE, "ASC");
        log.trace("[{}][{}] Fetching telemetry batch for query {}", tenantId, entityId, query);
        List<TsKvEntry> result;// will be interrupted on task processing timeout
        try {
            result = timeseriesService.findAll(tenantId, sourceEntityId, List.of(query)).get();
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch telemetry for " + sourceEntityId + " for key " + argument.getRefEntityKey().getKey() + ": " + e.getCause().getMessage(), e.getCause());
        }
        log.debug("[{}][{}] Fetched {} timeseries for query {}", tenantId, entityId, result.size(), query);
        return result;
    }

    private Future<Void> saveResult(CFReprocessingCtx ctx, CalculatedFieldResult calculatedFieldResult, long ts, Strategy strategy) {
        JsonElement result = JsonParser.parseString(Objects.requireNonNull(calculatedFieldResult.stringValue()));
        log.trace("[{}][{}] Saving CF result: {}", ctx.getTenantId(), ctx.getEntityId(), result);
        SettableFuture<Void> future = SettableFuture.create();
        saveTimeSeries(ctx.getTenantId(), ctx.getEntityId(), result, ts, strategy, TbCallback.wrap(future));
        return future;
    }

    private CFReprocessingCtx buildCtx(TenantId tenantId, EntityId entityId,
                                       CalculatedFieldCtx cfCtx, CalculatedFieldState state) {
        if (CalculatedFieldType.RELATED_ENTITIES_AGGREGATION.equals(cfCtx.getCfType())) {
            return new RelatedEntitiesCfReprocessingCtx(tenantId, entityId, cfCtx, state);
        }
        if (CalculatedFieldType.ENTITY_AGGREGATION.equals(cfCtx.getCfType())) {
            return new EntityAggCfReprocessingCtx(tenantId, entityId, cfCtx, state);
        }
        return new SimpleCfReprocessingCtx(tenantId, entityId, cfCtx, state);
    }

    public interface CFReprocessingCtx extends AutoCloseable {

        TenantId getTenantId();

        EntityId getEntityId();

        CalculatedFieldCtx getCfCtx();

        CalculatedFieldId getCfId();

        CalculatedFieldState getState();

        void setLatestResult(TbPair<Long, CalculatedFieldResult> latestResult);

        void checkStateSize();

        void processInitialState(long startTs) throws Exception;

        void awaitResults() throws InterruptedException;

        void prepareCtx(long startTs, long endTs) throws Exception;

        void processData(long startTs, long endTs) throws Exception;

        Future<CalculatedFieldResult> performCalculation(CalculatedFieldState state) throws Exception;

        void close();

    }

    @Getter
    public abstract class AbstractCfReprocessingCtx implements CFReprocessingCtx, AutoCloseable {

        protected final TenantId tenantId;
        protected final EntityId entityId;
        protected final CalculatedFieldCtx cfCtx;
        protected final CalculatedFieldState state;
        protected final CalculatedFieldId cfId;
        protected final CalculatedFieldEntityCtxId ctxId;

        @Setter
        protected TbPair<Long, CalculatedFieldResult> latestResult;
        protected final List<Future<Void>> resultFutures = new ArrayList<>();

        public AbstractCfReprocessingCtx(TenantId tenantId, EntityId entityId, CalculatedFieldCtx cfCtx, CalculatedFieldState state) {
            this.tenantId = tenantId;
            this.entityId = entityId;
            this.cfCtx = cfCtx;
            this.state = state;
            this.cfId = cfCtx.getCfId();
            this.ctxId = new CalculatedFieldEntityCtxId(tenantId, cfId, entityId);
        }

        public void processInitialState(long startTs) throws Exception {
            processStateIfReady(this, startTs).get();
        }

        public void checkStateSize() {
            state.checkStateSize(ctxId, cfCtx.getMaxStateSize());
            if (!state.isSizeOk()) {
                throw new RuntimeException(cfCtx.getSizeExceedsLimitMessage());
            }
        }

        public void addResult(Future<Void> resultFuture, int awaitPeriod) throws InterruptedException {
            resultFutures.add(resultFuture);
            if (resultFutures.size() % awaitPeriod == 0) {
                awaitResults();
            }
        }

        public void awaitResults() throws InterruptedException {
            for (Future<Void> resultFuture : resultFutures) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                try {
                    resultFuture.get();
                } catch (ExecutionException e) { // in case of single failure - cancelling everything
                    throw new RuntimeException("Failed to save calculated field results: " + e.getCause().getMessage(), e.getCause());
                }
            }
            log.debug("[{}][{}] Saved {} CF results", tenantId, entityId, resultFutures.size());
            resultFutures.clear();
        }

        @Override
        public void close() {
            log.debug("[{}][{}] Closing CF reprocessing context", tenantId, entityId);
            resultFutures.forEach(future -> future.cancel(true));
            cfCtx.close();
        }

    }

    @Getter
    public class EntityAggCfReprocessingCtx extends AbstractCfReprocessingCtx {

        private final LinkedList<AggIntervalEntry> intervals = new LinkedList<>();
        private AggIntervalEntry intervalCursor;

        public EntityAggCfReprocessingCtx(TenantId tenantId, EntityId entityId, CalculatedFieldCtx cfCtx, CalculatedFieldState state) {
            super(tenantId, entityId, cfCtx, state);
        }

        public void processInitialState(long startTs) {}

        @Override
        public void prepareCtx(long startTs, long endTs) {
            if (!(cfCtx.getCalculatedField().getConfiguration() instanceof EntityAggregationCalculatedFieldConfiguration config)) {
                return;
            }
            AggInterval interval = config.getInterval();
            interval.getIntervalsBetween(startTs, endTs).forEach(intervalEntry -> {
                intervals.addLast(new AggIntervalEntry(intervalEntry.getFirst(), intervalEntry.getSecond()));
            });
            intervalCursor = intervals.peekFirst();
        }

        @Override
        public void processData(long startTs, long endTs) throws Exception {
            while (true) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                AggIntervalEntry interval = intervals.pollFirst();
                intervalCursor = interval;

                if (intervalCursor == null) {
                    var latestResult = getLatestResult();
                    if (latestResult != null) {
                        Future<Void> result = saveResult(this, latestResult.getSecond(), latestResult.getFirst(), Strategy.LATEST_AND_WS);
                        addResult(result, telemetryFetchPackSize);
                    }
                    break;
                }

                Future<Void> result = processStateIfReady(this, interval.getEndTs());
                addResult(result, telemetryFetchPackSize);
            }
        }

        @Override
        public Future<CalculatedFieldResult> performCalculation(CalculatedFieldState state) throws Exception {
            var aggState = (EntityAggregationCalculatedFieldState) state;
            return aggState.performAggregationDuringInterval(this);
        }

        @Override
        public void close() {
            super.close();
            intervals.clear();
        }

    }

    @Getter
    public abstract class ArgBufferCfReprocessingCtx extends AbstractCfReprocessingCtx {

        public ArgBufferCfReprocessingCtx(TenantId tenantId, EntityId entityId, CalculatedFieldCtx cfCtx, CalculatedFieldState state) {
            super(tenantId, entityId, cfCtx, state);
        }

        @Override
        public void processData(long startTs, long endTs) throws Exception {
            while (true) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                OptionalLong minTs = findNextTimestamp();

                if (minTs.isEmpty()) {
                    var latestResult = getLatestResult();
                    if (latestResult != null) {
                        Future<Void> result = saveResult(this, latestResult.getSecond(), latestResult.getFirst(), Strategy.LATEST_AND_WS);
                        addResult(result, telemetryFetchPackSize);
                    }
                    return;
                }

                Map<String, ArgumentEntry> updatedArgs = getUpdatedArgs(minTs.getAsLong(), startTs, endTs);
                Future<Void> result = processArgumentValuesUpdate(this, updatedArgs, minTs.getAsLong());
                addResult(result, telemetryFetchPackSize);
            }
        }

        public Map<String, ArgumentEntry> getUpdatedArgs(long minTs, long startTs, long endTs) throws InterruptedException {
            Map<String, ArgumentEntry> updatedArgs = new HashMap<>();

            for (String argName : getArgNames()) {
                ArgumentEntry argumentEntry = processArgBuffer(argName, minTs, startTs, endTs);
                if (argumentEntry != null) {
                    updatedArgs.put(argName, argumentEntry);
                }
            }

            return updatedArgs;
        }

        @Override
        public Future<CalculatedFieldResult> performCalculation(CalculatedFieldState state) throws Exception {
            return state.performCalculation(Collections.emptyMap(), cfCtx);
        }

        protected abstract Set<String> getArgNames();

        protected abstract ArgumentEntry processArgBuffer(String argName, long minTs, long startTs, long endTs) throws InterruptedException;

        protected ArgumentEntry processArgEntityBuffer(String argName, EntityId sourceId, LinkedList<TsKvEntry> buffer, long minTs, long startTs, long endTs) throws InterruptedException {
            if (buffer != null && !buffer.isEmpty() && buffer.getFirst().getTs() == minTs) {
                TsKvEntry kvEntry = buffer.removeFirst();
                ArgumentEntry argumentEntry = ArgumentEntry.createSingleValueArgument(sourceId, new SingleValueArgumentEntry(kvEntry));

                if (buffer.isEmpty()) {
                    refillArgEntityBuffer(argName, sourceId, startTs, endTs);
                }

                return argumentEntry;
            }
            return null;
        }

        protected void refillArgEntityBuffer(String argName, EntityId sourceId, long startTs, long endTs) throws InterruptedException {
            Argument arg = cfCtx.getArguments().get(argName);
            long cursorTs = Optional.ofNullable(getArgCursor(argName, sourceId)).orElse(startTs);
            LinkedList<TsKvEntry> nextBatch = fetchTelemetryBatch(tenantId, entityId, arg, cursorTs, endTs, telemetryFetchPackSize).stream()
                    .filter(tsKv -> tsKv.getTs() > cursorTs)
                    .collect(Collectors.toCollection(LinkedList::new));
            if (!nextBatch.isEmpty()) {
                putNewBatch(argName, sourceId, nextBatch);
            }
        }

        protected abstract Long getArgCursor(String argName, EntityId sourceId);

        protected abstract void putNewBatch(String argName, EntityId sourceId, LinkedList<TsKvEntry> nextBatch);

        protected abstract OptionalLong findNextTimestamp();

    }

    @Getter
    public class RelatedEntitiesCfReprocessingCtx extends ArgBufferCfReprocessingCtx {

        private final Map<String, Map<EntityId, LinkedList<TsKvEntry>>> entityTelemetryBuffers = new HashMap<>();
        private final Map<String, Map<EntityId, Long>> entityCursors = new HashMap<>();

        public RelatedEntitiesCfReprocessingCtx(TenantId tenantId, EntityId entityId, CalculatedFieldCtx cfCtx, CalculatedFieldState state) {
            super(tenantId, entityId, cfCtx, state);
        }

        @Override
        public void prepareCtx(long startTs, long endTs) throws Exception {
            Map<String, Argument> arguments = cfCtx.getArguments();
            for (Entry<String, Argument> e : arguments.entrySet()) {
                String argName = e.getKey();
                Argument arg = e.getValue();
                if (!ArgumentType.TS_LATEST.equals(arg.getRefEntityKey().getType())) {
                    continue;
                }
                if (cfCtx.getCalculatedField().getConfiguration() instanceof RelatedEntitiesAggregationCalculatedFieldConfiguration configuration) {
                    List<EntityId> relatedEntities = resolveRelatedEntities(tenantId, entityId, configuration.getRelation()).get();
                    if (relatedEntities == null) {
                        continue;
                    }
                    for (EntityId relatedEntity : relatedEntities) {
                        LinkedList<TsKvEntry> batch = new LinkedList<>(fetchTelemetryBatch(tenantId, relatedEntity, arg, startTs, endTs, telemetryFetchPackSize));
                        if (!batch.isEmpty()) {
                            entityTelemetryBuffers.computeIfAbsent(argName, name -> new HashMap<>()).put(relatedEntity, batch);
                            entityCursors.computeIfAbsent(argName, name -> new HashMap<>()).put(relatedEntity, batch.getLast().getTs());
                        }
                    }
                }
            }
        }

        public OptionalLong findNextTimestamp() {
            return entityTelemetryBuffers.values().stream()
                    .map(Map::values)
                    .flatMap(Collection::stream)
                    .filter(buffer -> !buffer.isEmpty())
                    .mapToLong(buffer -> buffer.getFirst().getTs())
                    .min();
        }

        @Override
        protected Set<String> getArgNames() {
            return entityTelemetryBuffers.keySet();
        }

        @Override
        protected Long getArgCursor(String argName, EntityId sourceId) {
            if (entityCursors.containsKey(argName)) {
                return entityCursors.get(argName).get(sourceId);
            }
            return null;
        }

        @Override
        protected void putNewBatch(String argName, EntityId sourceId, LinkedList<TsKvEntry> nextBatch) {
            entityTelemetryBuffers.get(argName).put(sourceId, nextBatch);
            entityCursors.get(argName).put(sourceId, nextBatch.getLast().getTs());
        }

        protected ArgumentEntry processArgBuffer(String argName, long minTs, long startTs, long endTs) throws InterruptedException {
            Map<EntityId, LinkedList<TsKvEntry>> entityBuffers = entityTelemetryBuffers.get(argName);

            Map<EntityId, ArgumentEntry> entityArguments = new HashMap<>();
            for (Map.Entry<EntityId, LinkedList<TsKvEntry>> entityBuffer : entityBuffers.entrySet()) {
                EntityId sourceId = entityBuffer.getKey();
                LinkedList<TsKvEntry> buffer = entityBuffer.getValue();

                ArgumentEntry argumentEntry = processArgEntityBuffer(argName, sourceId, buffer, minTs, startTs, endTs);
                if (argumentEntry != null) {
                    entityArguments.put(sourceId, argumentEntry);
                }
            }

            if (entityArguments.isEmpty()) {
                return null;
            }
            return ArgumentEntry.createAggArgument(entityArguments);
        }

        @Override
        public void close() {
            super.close();
            entityTelemetryBuffers.clear();
            entityCursors.clear();
        }

    }

    @Getter
    public class SimpleCfReprocessingCtx extends ArgBufferCfReprocessingCtx {

        private final Map<String, LinkedList<TsKvEntry>> telemetryBuffers = new HashMap<>();
        private final Map<String, Long> cursors = new HashMap<>();

        public SimpleCfReprocessingCtx(TenantId tenantId, EntityId entityId, CalculatedFieldCtx cfCtx, CalculatedFieldState state) {
            super(tenantId, entityId, cfCtx, state);
        }

        @Override
        public void prepareCtx(long startTs, long endTs) throws Exception {
            Map<String, Argument> arguments = cfCtx.getArguments();
            for (Entry<String, Argument> e : arguments.entrySet()) {
                String argName = e.getKey();
                Argument arg = e.getValue();
                if (ArgumentType.ATTRIBUTE.equals(arg.getRefEntityKey().getType())) {
                    continue;
                }
                LinkedList<TsKvEntry> batch = new LinkedList<>(fetchTelemetryBatch(tenantId, entityId, arg, startTs, endTs, telemetryFetchPackSize));
                if (!batch.isEmpty()) {
                    telemetryBuffers.put(argName, batch);
                    cursors.put(argName, batch.getLast().getTs());
                }
            }
        }

        public OptionalLong findNextTimestamp() {
            return telemetryBuffers.values().stream()
                    .filter(buffer -> !buffer.isEmpty())
                    .mapToLong(buffer -> buffer.get(0).getTs())
                    .min();
        }

        protected Set<String> getArgNames() {
            return telemetryBuffers.keySet();
        }

        @Override
        protected Long getArgCursor(String argName, EntityId sourceId) {
            return cursors.get(argName);
        }

        @Override
        protected void putNewBatch(String argName, EntityId sourceId, LinkedList<TsKvEntry> nextBatch) {
            telemetryBuffers.put(argName, nextBatch);
            cursors.put(argName, nextBatch.getLast().getTs());
        }

        protected ArgumentEntry processArgBuffer(String argName, long minTs, long startTs, long endTs) throws InterruptedException {
            LinkedList<TsKvEntry> buffer = telemetryBuffers.get(argName);
            return processArgEntityBuffer(argName, entityId, buffer, minTs, startTs, endTs);
        }

        @Override
        public void close() {
            super.close();
            telemetryBuffers.clear();
            cursors.clear();
        }

    }

}

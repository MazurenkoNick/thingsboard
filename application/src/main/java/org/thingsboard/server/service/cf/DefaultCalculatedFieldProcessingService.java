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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.calculatedField.CalculatedFieldTelemetryMsg;
import org.thingsboard.server.actors.calculatedField.MultipleTbCallback;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldLinkedTelemetryMsgProto.Builder;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldNotificationMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.security.permission.OwnersCacheService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.utils.CalculatedFieldUtils.toProto;

@TbRuleEngineComponent
@Service
@Slf4j
public class DefaultCalculatedFieldProcessingService extends AbstractCalculatedFieldProcessingService implements CalculatedFieldProcessingService {

    private final TbClusterService clusterService;
    private final PartitionService partitionService;

    public DefaultCalculatedFieldProcessingService(AttributesService attributesService,
                                                   TimeseriesService timeseriesService,
                                                   ApiLimitService apiLimitService,
                                                   RelationService relationService,
                                                   OwnersCacheService ownersCacheService,
                                                   TbClusterService clusterService,
                                                   PartitionService partitionService) {
        super(attributesService, timeseriesService, apiLimitService, relationService, ownersCacheService);
        this.clusterService = clusterService;
        this.partitionService = partitionService;
    }

    @Override
    protected String getExecutorNamePrefix() {
        return "calculated-field-callback";
    }

    @Override
    public Map<String, ArgumentEntry> fetchDynamicArgsFromDb(CalculatedFieldCtx ctx, EntityId entityId) {
        // only scheduledSupported CF instances supports dynamic arguments scheduled updates
        if (!ctx.getCalculatedField().getType().equals(CalculatedFieldType.GEOFENCING)) {
            return Map.of();
        }
        return resolveArgumentFutures(fetchGeofencingCalculatedFieldArguments(ctx, entityId, true, System.currentTimeMillis()));
    }

    @Override
    public Map<String, ArgumentEntry> fetchArgsFromDb(TenantId tenantId, EntityId entityId, Map<String, Argument> arguments) {
        Map<String, ListenableFuture<ArgumentEntry>> argFutures = new HashMap<>();
        for (var entry : arguments.entrySet()) {
            if (entry.getValue().hasRelationQuerySource()) {
                continue;
            }
            var argEntityId = resolveEntityId(tenantId, entityId, entry.getValue());
            var argValueFuture = fetchArgumentValue(tenantId, argEntityId, entry.getValue(), System.currentTimeMillis());
            argFutures.put(entry.getKey(), argValueFuture);
        }
        return resolveArgumentFutures(argFutures);
    }

    @Override
    public void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, CalculatedFieldResult calculatedFieldResult, List<CalculatedFieldId> cfIds, TbCallback callback) {
        try {
            OutputType type = calculatedFieldResult.getType();
            TbMsgType msgType = OutputType.ATTRIBUTES.equals(type) ? TbMsgType.POST_ATTRIBUTES_REQUEST : TbMsgType.POST_TELEMETRY_REQUEST;
            TbMsgMetaData md = OutputType.ATTRIBUTES.equals(type) ? new TbMsgMetaData(Map.of(SCOPE, calculatedFieldResult.getScope().name())) : TbMsgMetaData.EMPTY;
            TbMsg msg = TbMsg.newMsg().type(msgType).originator(entityId).previousCalculatedFieldIds(cfIds).metaData(md).data(calculatedFieldResult.toStringOrElseNull()).build();
            clusterService.pushMsgToRuleEngine(tenantId, entityId, msg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    callback.onSuccess();
                    log.trace("[{}][{}] Pushed message to rule engine: {} ", tenantId, entityId, msg);
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onFailure(t);
                }
            });
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push message to rule engine. CalculatedFieldResult: {}", tenantId, entityId, calculatedFieldResult, e);
            callback.onFailure(e);
        }
    }

    @Override
    public void pushMsgToLinks(CalculatedFieldTelemetryMsg msg, List<CalculatedFieldEntityCtxId> linkedCalculatedFields, TbCallback callback) {
        Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> unicasts = new HashMap<>();
        List<CalculatedFieldEntityCtxId> broadcasts = new ArrayList<>();
        for (CalculatedFieldEntityCtxId link : linkedCalculatedFields) {
            var linkEntityId = link.entityId();
            var linkEntityType = linkEntityId.getEntityType();
            // Let's assume number of entities in profile is N, and number of partitions is P. If N > P, we save by broadcasting to all partitions. Usually N >> P.
            boolean broadcast = EntityType.DEVICE_PROFILE.equals(linkEntityType) || EntityType.ASSET_PROFILE.equals(linkEntityType);
            if (broadcast) {
                broadcasts.add(link);
            } else {
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, link.tenantId(), link.entityId());
                unicasts.computeIfAbsent(tpi, k -> new ArrayList<>()).add(link);
            }
        }
        MultipleTbCallback linkCallback = new MultipleTbCallback(2, callback);
        if (!broadcasts.isEmpty()) {
            broadcast(broadcasts, msg, linkCallback);
        } else {
            linkCallback.onSuccess();
        }
        if (!unicasts.isEmpty()) {
            unicast(unicasts, msg, linkCallback);
        } else {
            linkCallback.onSuccess();
        }
    }

    private void unicast(Map<TopicPartitionInfo, List<CalculatedFieldEntityCtxId>> unicasts, CalculatedFieldTelemetryMsg msg, MultipleTbCallback mainCallback) {
        TbQueueCallback callback = new TbCallbackWrapper(new MultipleTbCallback(unicasts.size(), mainCallback));
        unicasts.forEach((topicPartitionInfo, ctxIds) -> {
            CalculatedFieldLinkedTelemetryMsgProto linkedTelemetryMsgProto = buildLinkedTelemetryMsgProto(msg.getProto(), ctxIds);
            clusterService.pushMsgToCalculatedFields(topicPartitionInfo, UUID.randomUUID(),
                    ToCalculatedFieldMsg.newBuilder().setLinkedTelemetryMsg(linkedTelemetryMsgProto).build(), callback);
        });
    }

    private void broadcast(List<CalculatedFieldEntityCtxId> broadcasts, CalculatedFieldTelemetryMsg msg, MultipleTbCallback mainCallback) {
        TbQueueCallback callback = new TbCallbackWrapper(mainCallback);
        CalculatedFieldLinkedTelemetryMsgProto linkedTelemetryMsgProto = buildLinkedTelemetryMsgProto(msg.getProto(), broadcasts);
        clusterService.broadcastToCalculatedFields(ToCalculatedFieldNotificationMsg.newBuilder().setLinkedTelemetryMsg(linkedTelemetryMsgProto).build(), callback);
    }

    private CalculatedFieldLinkedTelemetryMsgProto buildLinkedTelemetryMsgProto(CalculatedFieldTelemetryMsgProto telemetryProto, List<CalculatedFieldEntityCtxId> links) {
        Builder builder = CalculatedFieldLinkedTelemetryMsgProto.newBuilder();
        builder.setMsg(telemetryProto);
        for (CalculatedFieldEntityCtxId link : links) {
            builder.addLinks(toProto(link));
        }
        return builder.build();
    }

    private static class TbCallbackWrapper implements TbQueueCallback {
        private final TbCallback callback;

        public TbCallbackWrapper(TbCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            callback.onSuccess();
        }

        @Override
        public void onFailure(Throwable t) {
            callback.onFailure(t);
        }
    }

}

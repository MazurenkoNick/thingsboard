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
package org.thingsboard.server.service.edge.rpc.processor.ai;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.AiModelUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class AiModelEdgeProcessor extends BaseAiModelProcessor implements AiModelProcessor {

    @Override
    public ListenableFuture<Void> processAiModelMsgFromEdge(TenantId tenantId, Edge edge, AiModelUpdateMsg aiModelUpdateMsg) {
        AiModelId aiModelId = new AiModelId(new UUID(aiModelUpdateMsg.getIdMSB(), aiModelUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (aiModelUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    processAiModel(tenantId, aiModelId, aiModelUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    deleteAiModel(tenantId, edge, aiModelId);
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(aiModelUpdateMsg.getMsgType());
            };
        } catch (DataValidationException e) {
            return Futures.immediateFailedFuture(e);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        AiModelId aiModelId = new AiModelId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                Optional<AiModel> aiModel = edgeCtx.getAiModelService().findAiModelById(edgeEvent.getTenantId(), aiModelId);
                if (aiModel.isPresent()) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    AiModelUpdateMsg aiModelUpdateMsg = EdgeMsgConstructorUtils.constructAiModelUpdatedMsg(msgType, aiModel.get());
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAiModelUpdateMsg(aiModelUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                AiModelUpdateMsg aiModelUpdateMsg = EdgeMsgConstructorUtils.constructAiModelDeleteMsg(aiModelId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAiModelUpdateMsg(aiModelUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    private void processAiModel(TenantId tenantId, AiModelId aiModelId, AiModelUpdateMsg aiModelUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateAiModel(tenantId, aiModelId, aiModelUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            Optional<AiModel> aiModel = edgeCtx.getAiModelService().findAiModelById(tenantId, aiModelId);
            aiModel.ifPresent(model -> pushEntityEventToRuleEngine(tenantId, edge, model, TbMsgType.ENTITY_CREATED));
        }
        Boolean aiModelNameUpdated = resultPair.getSecond();
        if (aiModelNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.AI_MODEL, EdgeEventActionType.UPDATED, aiModelId, null);
        }
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.AI_MODEL;
    }
}

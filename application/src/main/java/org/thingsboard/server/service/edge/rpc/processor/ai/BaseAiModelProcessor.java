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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.AiModelUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.Optional;

@Slf4j
public abstract class BaseAiModelProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<AiModel> aiModelValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateAiModel(TenantId tenantId, AiModelId aiModelId, AiModelUpdateMsg aiModelUpdateMsg) {
        boolean isCreated = false;
        boolean isNameUpdated = false;
        try {
            AiModel aiModel = JacksonUtil.fromString(aiModelUpdateMsg.getEntity(), AiModel.class, true);
            if (aiModel == null) {
                throw new RuntimeException("[{" + tenantId + "}] aiModelUpdateMsg {" + aiModelUpdateMsg + " } cannot be converted to aiModel");
            }

            Optional<AiModel> aiModelById = edgeCtx.getAiModelService().findAiModelById(tenantId, aiModelId);
            if (aiModelById.isEmpty()) {
                aiModel.setCreatedTime(Uuids.unixTimestamp(aiModelId.getId()));
                isCreated = true;
                aiModel.setId(null);
            } else {
                aiModel.setId(aiModelId);
            }

            String aiModelName = aiModel.getName();
            Optional<AiModel> aiModelByName = edgeCtx.getAiModelService().findAiModelByTenantIdAndName(aiModel.getTenantId(), aiModelName);
            if (aiModelByName.isPresent() && !aiModelByName.get().getId().equals(aiModelId)) {
                aiModelName = aiModelName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] aiModel with name {} already exists. Renaming aiModel name to {}",
                        tenantId, aiModel.getName(), aiModelByName.get().getName());
                isNameUpdated = true;
            }
            aiModel.setName(aiModelName);

            aiModelValidator.validate(aiModel, AiModel::getTenantId);

            if (isCreated) {
                aiModel.setId(aiModelId);
            }

            edgeCtx.getAiModelService().save(aiModel, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process aiModel update msg [{}]", tenantId, aiModelUpdateMsg, e);
            throw e;
        }
        return Pair.of(isCreated, isNameUpdated);
    }

    protected void deleteAiModel(TenantId tenantId, Edge edge, AiModelId aiModelId) {
        Optional<AiModel> aiModel = edgeCtx.getAiModelService().findAiModelById(tenantId, aiModelId);
        if (aiModel.isPresent()) {
            edgeCtx.getAiModelService().deleteByTenantIdAndId(tenantId, aiModelId);
            pushEntityEventToRuleEngine(tenantId, edge, aiModel.get(), TbMsgType.ENTITY_DELETED);
        }
    }

}

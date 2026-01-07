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
package org.thingsboard.server.service.entitiy.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import static java.util.Objects.requireNonNullElseGet;

@Service
@TbCoreComponent
@RequiredArgsConstructor
class DefaultTbAiModelService extends AbstractTbEntityService implements TbAiModelService {

    private final AiModelService aiModelService;

    @Override
    public AiModel save(AiModel model, User user) {
        var actionType = model.getId() == null ? ActionType.ADDED : ActionType.UPDATED;

        var tenantId = user.getTenantId();
        model.setTenantId(tenantId);

        AiModel savedModel;
        try {
            savedModel = aiModelService.save(model);
            autoCommit(user, savedModel.getId());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, requireNonNullElseGet(model.getId(), () -> emptyId(EntityType.AI_MODEL)), model, actionType, user, e);
            throw e;
        }

        logEntityActionService.logEntityAction(tenantId, savedModel.getId(), savedModel, actionType, user);

        return savedModel;
    }

    @Override
    public boolean delete(AiModel model, User user) {
        var actionType = ActionType.DELETED;

        var tenantId = user.getTenantId();
        var modelId = model.getId();

        boolean deleted;
        try {
            deleted = aiModelService.deleteByTenantIdAndId(tenantId, modelId);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, modelId, model, actionType, user, e, modelId.toString());
            throw e;
        }

        if (deleted) {
            logEntityActionService.logEntityAction(tenantId, modelId, model, actionType, user, modelId.toString());
        }

        return deleted;
    }

}

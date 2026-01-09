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
package org.thingsboard.server.dao.service.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.ai.AiModelDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class AiModelDataValidator extends DataValidator<AiModel> {

    private final TenantService tenantService;
    private final AiModelDao aiModelDao;

    @Override
    protected AiModel validateUpdate(TenantId tenantId, AiModel model) {
        Optional<AiModel> existing = aiModelDao.findByTenantIdAndId(tenantId, model.getId());
        if (existing.isEmpty()) {
            throw new DataValidationException("Cannot update non-existent AI model!");
        }
        return existing.get();
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AiModel model) {
        // ID validation
        if (model.getId() != null) {
            if (model.getUuidId() == null) {
                throw new DataValidationException("AI model UUID should be specified!");
            }
            if (model.getId().isNullUid()) {
                throw new DataValidationException("AI model UUID must not be the reserved null value!");
            }
        }

        // tenant ID validation
        if (model.getTenantId() == null || model.getTenantId().getId() == null) {
            throw new DataValidationException("AI model should be assigned to tenant!");
        }
        if (model.getTenantId().isSysTenantId()) {
            throw new DataValidationException("AI model cannot be assigned to the system tenant!");
        }
        if (!tenantService.tenantExists(tenantId)) {
            throw new DataValidationException("AI model reference a non-existent tenant!");
        }
    }

}

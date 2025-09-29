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
package org.thingsboard.server.dao.service.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.dao.pat.ApiKeyDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.exception.DataValidationException;

@Component
@RequiredArgsConstructor
public class ApiKeyDataValidator extends DataValidator<ApiKey> {

    private final ApiKeyDao apiKeyDao;
    private final TenantDao tenantDao;
    private final UserDao userDao;

    @Override
    protected void validateDataImpl(TenantId tenantId, ApiKey apiKey) {
        if (apiKey.getId() != null) {
            if (apiKey.getUuidId() == null) {
                throw new org.thingsboard.server.exception.DataValidationException("Api Key UUID should be specified!");
            }
            if (apiKey.getId().isNullUid()) {
                throw new DataValidationException("API key UUID must not be the reserved null value!");
            }
        }

        if (apiKey.getTenantId() == null || apiKey.getTenantId().getId() == null) {
            throw new DataValidationException("API key should be assigned to tenant!");
        }
        if (!TenantId.SYS_TENANT_ID.equals(apiKey.getTenantId()) && tenantDao.findById(apiKey.getTenantId(), apiKey.getTenantId().getId()) == null) {
            throw new DataValidationException("API key reference a non-existent tenant!");
        }

        if (apiKey.getUserId() == null || apiKey.getUserId().getId() == null) {
            throw new DataValidationException("API key should be assigned to user!");
        }
        if (userDao.findById(apiKey.getTenantId(), apiKey.getUserId().getId()) == null) {
            throw new DataValidationException("API key reference a non-existent user!");
        }
    }

    @Override
    protected ApiKey validateUpdate(TenantId tenantId, ApiKey apiKey) {
        ApiKey old = apiKeyDao.findById(tenantId, apiKey.getUuidId());
        if (old == null) {
            throw new DataValidationException("Cannot update non-existent API key!");
        }
        if (!old.getUserId().equals(apiKey.getUserId())) {
            throw new DataValidationException("Cannot update api key user id!");
        }
        if (old.getExpirationTime() != apiKey.getExpirationTime()) {
            throw new DataValidationException("Cannot update api key expiration time!");
        }
        return old;
    }

}

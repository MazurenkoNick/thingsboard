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
package org.thingsboard.server.dao.sql.pat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ApiKeyEntity;
import org.thingsboard.server.dao.pat.ApiKeyDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Set;
import java.util.UUID;

@Slf4j
@SqlDao
@Component
public class JpaApiKeyDao extends JpaAbstractDao<ApiKeyEntity, ApiKey> implements ApiKeyDao {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Override
    public ApiKey findByValue(String value) {
        return DaoUtil.getData(apiKeyRepository.findByValue(value));
    }

    @Override
    public ApiKey findInternalByDescription(TenantId tenantId, String description) {
        return DaoUtil.getData(apiKeyRepository.findFirstByTenantIdAndDescriptionAndInternal(tenantId.getId(), description, true));
    }

    @Override
    public Set<String> deleteByTenantId(TenantId tenantId) {
        return apiKeyRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public Set<String> deleteByUserId(TenantId tenantId, UserId userId) {
        return apiKeyRepository.deleteByUserId(tenantId.getId(), userId.getId());
    }

    @Override
    public int deleteAllByExpirationTimeBefore(long ts) {
        return apiKeyRepository.deleteAllByExpirationTimeBefore(ts);
    }

    @Override
    protected Class<ApiKeyEntity> getEntityClass() {
        return ApiKeyEntity.class;
    }

    @Override
    protected JpaRepository<ApiKeyEntity, UUID> getRepository() {
        return apiKeyRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.API_KEY;
    }

}

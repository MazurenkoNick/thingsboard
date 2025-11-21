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
package org.thingsboard.server.dao.pat;

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.validator.ApiKeyDataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.user.UserServiceImpl.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.user.UserServiceImpl.INCORRECT_USER_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl extends AbstractCachedEntityService<ApiKeyCacheKey, ApiKey, ApiKeyEvictEvent> implements ApiKeyService {

    private static final String INCORRECT_API_KEY_ID = "Incorrect ApiKeyId ";
    private static final int MAX_API_KEY_VALUE_LENGTH = 255;

    private final ApiKeyDao apiKeyDao;
    private final ApiKeyInfoDao apiKeyInfoDao;
    @Lazy
    private final ApiKeyDataValidator apiKeyValidator;

    @Value("${security.api_key.value_prefix:}")
    private String prefix;

    @Value("${security.api_key.value_bytes_size:64}")
    private int valueBytesSize;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(ApiKeyEvictEvent event) {
        cache.evict(ApiKeyCacheKey.of(event.value()));
    }

    @Override
    public ApiKey saveApiKey(TenantId tenantId, ApiKeyInfo apiKeyInfo) {
        log.trace("Executing saveApiKey [{}]", apiKeyInfo);
        try {
            var apiKey = new ApiKey(apiKeyInfo);
            var old = apiKeyValidator.validate(apiKey, ApiKeyInfo::getTenantId);
            if (old == null) {
                String value = generateApiKeySecret();
                apiKey.setValue(value);
            } else {
                apiKey.setValue(old.getValue());
            }

            if (!TenantId.SYS_TENANT_ID.equals(apiKey.getTenantId()) || !apiKey.isInternal()) {
                apiKey.setInternal(false);
                apiKey.setPermissions(null);
            }

            var savedApiKey = apiKeyDao.save(tenantId, apiKey);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedApiKey.getId()).entity(savedApiKey).created(apiKey.getId() == null).build());
            if (old != null && old.isEnabled() != apiKey.isEnabled()) {
                publishEvictEvent(new ApiKeyEvictEvent(apiKey.getValue()));
            }
            return savedApiKey;
        } catch (Exception e) {
            checkConstraintViolation(e, "api_key_value_unq_key", "API Key with such value already exists!");
            throw e;
        }
    }

    @Override
    public ApiKey rotateInternalApiKey(TenantId tenantId, ApiKeyInfo apiKeyInfo) {
        log.trace("Executing rotateInternalApiKey [{}]", apiKeyInfo);
        var apiKey = new ApiKey(apiKeyInfo);
        var old = apiKeyValidator.validate(apiKey, ApiKey::getTenantId);
        if (!old.isInternal()) {
            throw new IllegalArgumentException("Can't rotate non-internal API Key!");
        }
        String value = generateApiKeySecret();
        apiKey.setValue(value);
        try {
            var rotatedApiKey = apiKeyDao.save(tenantId, apiKey);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(rotatedApiKey.getId()).entity(rotatedApiKey).created(apiKey.getId() == null).build());
            publishEvictEvent(new ApiKeyEvictEvent(apiKey.getValue()));
            return rotatedApiKey;
        } catch (Exception e) {
            checkConstraintViolation(e, "api_key_value_unq_key", "API Key with such value already exists!");
            throw e;
        }
    }

    @Override
    public ApiKey findApiKeyById(TenantId tenantId, ApiKeyId apiKeyId) {
        log.trace("Executing findApiKeyById [{}] [{}]", tenantId, apiKeyId);
        validateId(apiKeyId, id -> INCORRECT_API_KEY_ID + id);
        return apiKeyDao.findById(tenantId, apiKeyId.getId());
    }

    @Override
    public PageData<ApiKeyInfo> findApiKeysByUserId(TenantId tenantId, UserId userId, PageLink pageLink) {
        log.trace("Executing findApiKeysByUserId [{}][{}]", tenantId, userId);
        validateId(userId, id -> INCORRECT_USER_ID + id);
        return apiKeyInfoDao.findByUserId(tenantId, userId, pageLink);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findApiKeyById(tenantId, new ApiKeyId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(apiKeyDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public void deleteApiKey(TenantId tenantId, ApiKey apiKey, boolean force) {
        UUID apiKeyId = apiKey.getUuidId();
        validateId(apiKeyId, id -> INCORRECT_API_KEY_ID + id);
        if (apiKey.isInternal() && !force) {
            throw new DataValidationException("Cannot delete internal API Key!");
        }
        apiKeyDao.removeById(tenantId, apiKeyId);
        publishEvictEvent(new ApiKeyEvictEvent(apiKey.getValue()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteApiKeysByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Set<String> values = apiKeyDao.deleteByTenantId(tenantId);
        values.forEach(value -> publishEvictEvent(new ApiKeyEvictEvent(value)));
    }

    @Override
    public void deleteByUserId(TenantId tenantId, UserId userId) {
        log.trace("Executing deleteApiKeysByUserId, tenantId [{}]", tenantId);
        validateId(userId, id -> INCORRECT_USER_ID + id);
        Set<String> values = apiKeyDao.deleteByUserId(tenantId, userId);
        values.forEach(value -> publishEvictEvent(new ApiKeyEvictEvent(value)));
    }

    @Override
    public ApiKey findApiKeyByValue(String value) {
        log.trace("Executing findApiKeyByValue [{}]", value);
        var cacheKey = ApiKeyCacheKey.of(value);
        return cache.getAndPutInTransaction(cacheKey, () -> apiKeyDao.findByValue(value), true);
    }

    private String generateApiKeySecret() {
        return prefix + StringUtils.generateSafeToken(Math.min(valueBytesSize, MAX_API_KEY_VALUE_LENGTH));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.API_KEY;
    }

}

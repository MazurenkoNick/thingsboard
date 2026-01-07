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
package org.thingsboard.server.dao.sql.ai;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ai.AiModel;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.ai.AiModelDao;
import org.thingsboard.server.dao.model.sql.AiModelEntity;
import org.thingsboard.server.dao.sql.HasSecretsEntityDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

@SqlDao
@Component
@RequiredArgsConstructor
class JpaAiModelDao extends JpaAbstractDao<AiModelEntity, AiModel> implements AiModelDao, HasSecretsEntityDao {

    private final AiModelRepository aiModelRepository;

    @Override
    public Optional<AiModel> findByTenantIdAndId(TenantId tenantId, AiModelId modelId) {
        return aiModelRepository.findByTenantIdAndId(tenantId.getId(), modelId.getId()).map(DaoUtil::getData);
    }

    @Override
    public AiModel findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(aiModelRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public AiModel findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(aiModelRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<AiModel> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AiModel> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(aiModelRepository.findByTenantId(
                tenantId, StringUtils.defaultIfEmpty(pageLink.getTextSearch(), null), toPageRequest(pageLink))
        );
    }

    @Override
    public PageData<AiModelId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(aiModelRepository.findIdsByTenantId(tenantId, toPageRequest(pageLink)).map(AiModelId::new));
    }

    private static PageRequest toPageRequest(PageLink pageLink) {
        Sort sort;
        SortOrder sortOrder = pageLink.getSortOrder();
        if (sortOrder == null) {
            sort = Sort.by(Sort.Direction.ASC, "id");
        } else {
            sort = JpaSort.unsafe(
                    Sort.Direction.fromString(sortOrder.getDirection().name()),
                    AiModelEntity.COLUMN_MAP.getOrDefault(sortOrder.getProperty(), sortOrder.getProperty())
            ).and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), sort);
    }

    @Override
    public AiModelId getExternalIdByInternal(AiModelId internalId) {
        return aiModelRepository.getExternalIdById(internalId.getId()).map(AiModelId::new).orElse(null);
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return aiModelRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public boolean deleteById(TenantId tenantId, AiModelId modelId) {
        return aiModelRepository.deleteByIdIn(Set.of(modelId.getId())) > 0;
    }

    @Override
    public Set<AiModelId> deleteByTenantId(TenantId tenantId) {
        return aiModelRepository.deleteByTenantId(tenantId.getId()).stream()
                .map(AiModelId::new)
                .collect(toSet());
    }

    @Override
    public boolean deleteByTenantIdAndId(TenantId tenantId, AiModelId modelId) {
        return aiModelRepository.deleteByTenantIdAndIdIn(tenantId.getId(), Set.of(modelId.getId())) > 0;
    }

    @Override
    public List<EntityInfo> findByTenantIdAndSecretPlaceholder(TenantId tenantId, String placeholder) {
        return aiModelRepository.findByTenantIdAndSecretPlaceholder(tenantId.getId(), placeholder);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL;
    }

    @Override
    protected Class<AiModelEntity> getEntityClass() {
        return AiModelEntity.class;
    }

    @Override
    protected JpaRepository<AiModelEntity, UUID> getRepository() {
        return aiModelRepository;
    }

}

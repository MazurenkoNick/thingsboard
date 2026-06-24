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
package org.thingsboard.server.dao.agent;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.agent.AgentAppTemplateCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentAppTemplateCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("AgentAppTemplateDaoService")
@Slf4j
public class BaseAgentAppTemplateService extends AbstractCachedEntityService<AgentAppTemplateCacheKey, AgentAppTemplate, AgentAppTemplateCacheEvictEvent> implements AgentAppTemplateService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_AGENT_APP_TEMPLATE_ID = "Incorrect agentAppTemplateId ";

    @Autowired
    private AgentAppTemplateDao agentAppTemplateDao;

    @Autowired
    private DataValidator<AgentAppTemplate> agentAppTemplateValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentAppTemplateCacheEvictEvent event) {
        cache.evict(new AgentAppTemplateCacheKey(event.getTemplateId()));
    }

    @Override
    @Transactional
    public AgentAppTemplate save(TenantId tenantId, AgentAppTemplate template) {
        log.trace("Executing saveAgentAppTemplate [{}]", template);
        AgentAppTemplate old = agentAppTemplateValidator.validate(template, t -> tenantId);
        AgentAppTemplate saved = agentAppTemplateDao.save(tenantId, template);
        publishEvictEvent(new AgentAppTemplateCacheEvictEvent(saved.getId().getId()));
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(saved.getId())
                .entity(saved)
                .oldEntity(old)
                .created(old == null)
                .build());
        return saved;
    }

    @Override
    public AgentAppTemplate findById(TenantId tenantId, AgentAppTemplateId templateId) {
        log.trace("Executing findAgentAppTemplateById [{}]", templateId);
        validateId(templateId, id -> INCORRECT_AGENT_APP_TEMPLATE_ID + id);
        return cache.getAndPutInTransaction(new AgentAppTemplateCacheKey(templateId.getId()),
                () -> agentAppTemplateDao.findById(tenantId, templateId.getId()), false);
    }

    @Override
    public AgentAppTemplate findLatestByAppTypeAndConfigType(AgentApplicationType appType, AgentAppConfigType configType) {
        log.trace("Executing findAgentAppTemplate appType [{}], configType [{}]",
                appType, configType);
        return agentAppTemplateDao.findLatestByAppTypeAndConfigType(TenantId.SYS_TENANT_ID, appType, configType);
    }

    @Override
    public AgentAppTemplate findByAppTypeAndConfigTypeAndVersion(AgentApplicationType appType, AgentAppConfigType configType, String currentVersion) {
        log.trace("Executing findAgentAppTemplate appType [{}], configType [{}], currentVersion [{}]",
                appType, configType, currentVersion);
        return agentAppTemplateDao.findByAppTypeAndConfigTypeAndVersion(TenantId.SYS_TENANT_ID, appType, configType, currentVersion);
    }

    @Override
    public List<AgentAppTemplate> findByAppTypeAndConfigType(AgentApplicationType appType, AgentAppConfigType configType) {
        log.trace("Executing findByAppTypeAndConfigType appType [{}], configType [{}]", appType, configType);
        return agentAppTemplateDao.findByAppTypeAndConfigType(TenantId.SYS_TENANT_ID, appType, configType);
    }

    @Override
    public List<AgentAppTemplate> findAll(TenantId tenantId) {
        log.trace("Executing findAll, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return agentAppTemplateDao.findAll(tenantId);
    }

    @Override
    @Transactional
    public void delete(TenantId tenantId, AgentAppTemplateId templateId) {
        log.trace("Executing deleteAgentAppTemplate [{}]", templateId);
        validateId(templateId, id -> INCORRECT_AGENT_APP_TEMPLATE_ID + id);
        AgentAppTemplate template = agentAppTemplateDao.findById(tenantId, templateId.getId());
        if (template != null) {
            agentAppTemplateDao.removeById(tenantId, templateId.getId());
            publishEvictEvent(new AgentAppTemplateCacheEvictEvent(templateId.getId()));
            eventPublisher.publishEvent(DeleteEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(templateId)
                    .entity(template)
                    .build());
        }
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new AgentAppTemplateId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentAppTemplate> future = agentAppTemplateDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future).transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_TEMPLATE;
    }
}

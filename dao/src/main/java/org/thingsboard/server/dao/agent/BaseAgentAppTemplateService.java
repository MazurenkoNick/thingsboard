/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("AgentAppTemplateDaoService")
@Slf4j
public class BaseAgentAppTemplateService extends AbstractEntityService implements AgentAppTemplateService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_AGENT_APP_TEMPLATE_ID = "Incorrect agentAppTemplateId ";

    @Autowired
    private AgentAppTemplateDao agentAppTemplateDao;

    @Autowired
    private DataValidator<AgentAppTemplate> agentAppTemplateValidator;

    @Override
    @Transactional
    public AgentAppTemplate save(TenantId tenantId, AgentAppTemplate template) {
        log.trace("Executing saveAgentAppTemplate [{}]", template);
        AgentAppTemplate old = agentAppTemplateValidator.validate(template, t -> tenantId);
        AgentAppTemplate saved = agentAppTemplateDao.save(tenantId, template);
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(saved.getId())
                .entity(saved)
                .oldEntity(old)
                .created(template.getId() == null)
                .build());
        return saved;
    }

    @Override
    public AgentAppTemplate findById(TenantId tenantId, AgentAppTemplateId templateId) {
        log.trace("Executing findAgentAppTemplateById [{}]", templateId);
        validateId(templateId, id -> INCORRECT_AGENT_APP_TEMPLATE_ID + id);
        return agentAppTemplateDao.findById(tenantId, templateId.getId());
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
            eventPublisher.publishEvent(DeleteEntityEvent.builder()
                    .tenantId(tenantId)
                    .entityId(templateId)
                    .entity(template)
                    .build());
        }
    }
}

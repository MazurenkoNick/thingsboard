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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppEventDao;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Objects;

@Component
@AllArgsConstructor
public class AgentApplicationDataValidator extends DataValidator<AgentApplication> {

    private final AgentService agentService;
    private final AgentApplicationDao agentApplicationDao;
    private final AgentAppTemplateDao agentAppTemplateDao;
    private final AgentAppEventDao agentAppEventDao;
    private final AgentAppProfileService agentAppProfileService;

    @Override
    protected AgentApplication validateUpdate(TenantId tenantId, AgentApplication agentApplication) {
        AgentApplication old = agentApplicationDao.findById(tenantId, agentApplication.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent application!");
        }
        if (old.isPendingDeletion() && agentApplication.isPendingDeletion()) {
            throw new DataValidationException("Application is already pending for removal");
        }
        if (!agentApplication.isPendingDeletion() &&
                agentAppEventDao.hasActiveEventForApplication(agentApplication.getId().getId())) {
            throw new DataValidationException("Cannot update application while an event is being processed");
        }
        if (agentApplication.getDesiredTemplateId() != null) {
            validateUpgradeVersion(old, agentApplication);
        } else {
            validateConfigNotChangedWhenTemplateStale(tenantId, old, agentApplication);
            validateProfileManagedConfigChange(old, agentApplication);
        }
        return old;
    }

    private void validateProfileManagedConfigChange(AgentApplication old, AgentApplication application) {
        if (application.getApplicationProfileId() == null) {
            return;
        }
        if (isApplicationProfileChangedOrAdded(application, old)) {
            return;
        }
        boolean profileRefetched = !Objects.equals(application.getProfileConfigVersion(), old.getProfileConfigVersion());
        if (profileRefetched) {
            return;
        }
        if (!AgentAppConfig.equalsIgnoringCreds(application.getAppType(), application.getConfig(), old.getConfig())) {
            throw new DataValidationException(
                    "Direct config update is not allowed for profile-managed applications (only credential fields can be modified).");
        }
    }

    private boolean isApplicationProfileChangedOrAdded(AgentApplication application, AgentApplication old) {
        return application.getApplicationProfileId() != null
                && !application.getApplicationProfileId().equals(old.getApplicationProfileId());
    }
    // Block plain updates that also change the config when the app's profile was upgraded, app wasn't yet.
    // Upgrades (desiredTemplateId != null) are the correct path to resolve that drift, so we only enforce this on non-upgrade updates.
    // Template ids uniquely identify (appType, version), so UUID inequality is sufficient to detect a version drift.
    private void validateConfigNotChangedWhenTemplateStale(TenantId tenantId, AgentApplication old, AgentApplication agentApplication) {
        if (old.getApplicationProfileId() == null) {
            return;
        }
        AgentAppProfile profile = agentAppProfileService.findProfileById(tenantId, old.getApplicationProfileId());
        if (profile == null || profile.getTemplateId() == null || old.getTemplateId() == null) {
            return;
        }
        if (Objects.equals(old.getTemplateId(), profile.getTemplateId())) {
            return;
        }
        if (!AgentAppConfig.equalsIgnoringCreds(old.getAppType(), old.getConfig(), agentApplication.getConfig())) {
            throw new DataValidationException(
                    "Cannot update configuration: the application's template is out of sync with its profile. Run an upgrade first.");
        }
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentApplication agentApplication) {
        if (agentApplication.getAgentId() == null) {
            throw new DataValidationException("Agent application should be assigned to agent!");
        }
        if (agentApplication.getAppType() == null) {
            throw new DataValidationException("Agent application type must not be null!");
        }
        validateTemplate(agentApplication);
        Agent agent = agentService.findAgentById(tenantId, agentApplication.getAgentId());
        if (agent == null) {
            throw new DataValidationException("Agent application is referencing non-existent agent!");
        }
        if (!agent.getTenantId().equals(tenantId)) {
            throw new DataValidationException("Agent application cannot be assigned to agent from different tenant!");
        }
        if (agentApplication.getName() != null && agentApplication.getName().length() > 255) {
            throw new DataValidationException("Agent application name length must be equal or shorter than 255!");
        }
        if (agentApplication.getApplicationProfileId() == null && agentApplication.getConfig() == null) {
            throw new DataValidationException("Agent application config must not be null!");
        }
        if (agentApplication.getRelatedEntityId() != null) {
            if (agentApplication.getAppType() == AgentApplicationType.GENERIC) {
                throw new DataValidationException("Generic agent application can't have related entity id");
            }
            if (!agentApplication.getRelatedEntityId().getEntityType().equals(agentApplication.getAppType().getRelatedEntityType())) {
                throw new DataValidationException("Entity id with this type can't be assigned to this application");
            }
        }
        if (agentApplication.getConfig() != null) {
            agentApplication.getConfig().validate();
        }
    }

    private void validateUpgradeVersion(AgentApplication old, AgentApplication agentApplication) {
        AgentAppTemplate currentTemplate = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, old.getTemplateId().getId());
        if (currentTemplate == null || currentTemplate.getNextVersion() == null) {
            throw new DataValidationException("No next version available for upgrade");
        }
        AgentAppTemplate desiredTemplate = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, agentApplication.getDesiredTemplateId().getId());
        if (desiredTemplate == null) {
            throw new DataValidationException("Desired template not found: " + agentApplication.getDesiredTemplateId());
        }
        if (!currentTemplate.getNextVersion().equals(desiredTemplate.getCurrentVersion())) {
            throw new DataValidationException("Desired template version " + desiredTemplate.getCurrentVersion()
                    + " does not match the next available version " + currentTemplate.getNextVersion());
        }
    }

    private void validateTemplate(AgentApplication agentApplication) {
        if (agentApplication.getTemplateId() == null) {
            throw new DataValidationException("Agent application should be assigned to template!");
        }
        if (agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, agentApplication.getTemplateId().getId()) == null) {
            throw new DataValidationException("Agent application is referencing non-existent template!");
        }
    }
}

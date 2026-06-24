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

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppArgumentReferenceValidator;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppTemplateDao;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Objects;

@Component
@AllArgsConstructor
public class AgentApplicationDataValidator extends DataValidator<AgentApplication> {

    private final AgentService agentService;
    private final AgentApplicationDao agentApplicationDao;
    private final AgentAppTemplateDao agentAppTemplateDao;
    private final AgentAppProfileService agentAppProfileService;
    @Lazy
    private final EntityServiceRegistry entityServiceRegistry;

    @Override
    protected AgentApplication validateUpdate(TenantId tenantId, AgentApplication application) {
        AgentApplicationInfo old = agentApplicationDao.findInfoById(tenantId, application.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent application!");
        }
        if (old.isPendingDeletion() && application.isPendingDeletion()) {
            throw new DataValidationException("Application is already pending for removal");
        }
        if (!Objects.equals(old.getOrigin(), application.getOrigin())) {
            throw new DataValidationException("Application origin does not match");
        }
        if (old.getAppType() != application.getAppType()) {
            throw new DataValidationException("Application type does not match");
        }
        if (!Objects.equals(old.getProjectName(), application.getProjectName())) {
            throw new DataValidationException("Application project name does not match");
        }
        if (application.getDesiredTemplateId() == null && !Objects.equals(old.getTemplateId(), application.getTemplateId())) {
            throw new DataValidationException("Cannot update template template id without specifying desired templateId");
        }

        boolean isUpgrade = application.getDesiredTemplateId() != null;
        AgentAppProfileId newProfileId = application.getApplicationProfileId();

        if (isUpgrade) {
            validateUpgradeChain(old, application);
            if (newProfileId != null) {
                AgentAppProfile profile = loadProfile(tenantId, newProfileId);
                requireDesiredTemplateMatchesProfile(application, profile);
                requireConfigMatchesProfile(application, profile);
            }
            return old;
        }

        if (newProfileId == null) {
            return old;
        }
        boolean sameProfile = newProfileId.equals(old.getApplicationProfileId());
        if (sameProfile && AgentAppConfig.equalsIgnoringCreds(application.getAppType(), application.getConfig(), old.getConfig())) {
            return old;
        }
        AgentAppProfile profile = loadProfile(tenantId, newProfileId);
        requireProfileTemplateMatchesCurrent(tenantId, old, profile);
        requireConfigMatchesProfile(application, profile);
        return old;
    }

    private AgentAppProfile loadProfile(TenantId tenantId, AgentAppProfileId profileId) {
        AgentAppProfile profile = agentAppProfileService.findProfileById(tenantId, profileId);
        if (profile == null) {
            throw new DataValidationException("Application profile not found");
        }
        return profile;
    }

    private void requireConfigMatchesProfile(AgentApplication application, AgentAppProfile profile) {
        if (!AgentAppConfig.equalsIgnoringCreds(application.getAppType(), application.getConfig(), profile.getConfig())) {
            throw new DataValidationException(
                    "Application config does not match the profile's config (only credential fields can differ)");
        }
    }

    private void requireDesiredTemplateMatchesProfile(AgentApplication application, AgentAppProfile profile) {
        if (!Objects.equals(application.getDesiredTemplateId(), profile.getTemplateId())) {
            throw new DataValidationException("Desired template does not match the profile's template");
        }
    }

    private void requireProfileTemplateMatchesCurrent(TenantId tenantId, AgentApplicationInfo old, AgentAppProfile profile) {
        AgentAppTemplate profileTemplate = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, profile.getTemplateId().getId());
        if (profileTemplate == null
                || !Objects.equals(profileTemplate.getCurrentVersion(), old.getCurrentVersion())) {
            throw new DataValidationException("Profile's template version does not match the application's current version");
        }
    }

    private void validateUpgradeChain(AgentApplication old, AgentApplication application) {
        AgentAppTemplate currentTemplate = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, old.getTemplateId().getId());
        if (currentTemplate == null || currentTemplate.getNextVersion() == null) {
            throw new DataValidationException("No next version available for upgrade!");
        }
        AgentAppTemplate desiredTemplate = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, application.getDesiredTemplateId().getId());
        if (desiredTemplate == null) {
            throw new DataValidationException("Desired template not found");
        }
        if (!currentTemplate.getNextVersion().equals(desiredTemplate.getCurrentVersion())) {
            throw new DataValidationException("Desired template version does not match the next available version");
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
        if (agentApplication.getOrigin() == null) {
            throw new DataValidationException("Agent application origin must not be null!");
        }
        if (agentApplication.getProjectName() == null) {
            throw new DataValidationException("Agent application project name must not be null!");
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
        if (agentApplication.getConfig() != null) {
            agentApplication.getConfig().validate();
            agentApplication.getConfig().validateForProfile(agentApplication.getAppType());
            AgentAppArgumentReferenceValidator.validate(tenantId, agentApplication.getConfig(), entityServiceRegistry);
        }
    }

    private void validateTemplate(AgentApplication application) {
        if (application.getTemplateId() == null) {
            throw new DataValidationException("Agent application should be assigned to template!");
        }
        AgentAppTemplate template = agentAppTemplateDao.findById(TenantId.SYS_TENANT_ID, application.getTemplateId().getId());
        String appTypeDefaultVersion = application.getAppType().getDefaultVersion();
        if (template == null) {
            throw new DataValidationException("Agent application is referencing non-existent template!");
        }
        if (appTypeDefaultVersion != null && !appTypeDefaultVersion.equals(template.getCurrentVersion())) {
            throw new DataValidationException("Template version does not match the application's current version");
        }
    }
}

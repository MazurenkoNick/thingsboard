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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.TbAgentAppProfileService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.AGENT_APP_TEMPLATE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentAppProfileController extends BaseController {

    private static final String TEMPLATE_ID = "agentAppTemplateId";
    private static final String PROFILE_ID = "profileId";

    private final TbAgentAppProfileService tbProfileService;
    private final SystemSecurityService systemSecurityService;

    @ApiOperation(value = "Get Agent Application Profile (getAgentAppProfileById)",
            notes = "Fetch the Agent Application Profile by Id." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profile/{profileId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppProfile getAgentAppProfileById(
            @Parameter(description = "Profile Id") @PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter(PROFILE_ID, strProfileId);
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        return checkAgentAppProfileId(profileId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent Application Profile Info (getAgentAppProfileInfoById)",
            notes = "Fetch the Agent Application Profile Info by Id. Readable by any tenant admin without "
                    + "AGENT_APP_PROFILE permission, so agent/application screens can resolve the linked profile."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profile/info/{profileId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppProfileInfo getAgentAppProfileInfoById(
            @Parameter(description = "Profile Id") @PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter(PROFILE_ID, strProfileId);
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        AgentAppProfileInfo profileInfo = checkNotNull(agentAppProfileService.findProfileInfoById(getTenantId(), profileId));
        if (!getTenantId().equals(profileInfo.getTenantId())) {
            throw permissionDenied();
        }
        return profileInfo;
    }

    @ApiOperation(value = "Create or Update Agent Application Profile (saveAgentAppProfile)",
            notes = "Creates or updates an agent application profile." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profile", method = RequestMethod.POST)
    @ResponseBody
    public AgentAppProfile saveAgentAppProfile(
            @RequestBody AgentAppProfile profile) throws Exception {
        profile.setTenantId(getTenantId());
        checkEntity(profile.getId(), profile, Resource.AGENT_APP_PROFILE);
        return tbProfileService.save(profile, getCurrentUser());
    }

    @ApiOperation(value = "Delete Agent Application Profile (deleteAgentAppProfile)",
            notes = "Deletes the agent application profile. Cannot delete if referenced by applications." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profile/{profileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAgentAppProfile(
            @Parameter(description = "Profile Id") @PathVariable(PROFILE_ID) String strProfileId) throws Exception {
        checkParameter(PROFILE_ID, strProfileId);
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        AgentAppProfile profile = checkAgentAppProfileId(profileId, Operation.DELETE);
        tbProfileService.delete(profile, getCurrentUser());
    }

    @ApiOperation(value = "Merge template into application profile for preview (mergeAgentAppProfileForPreview)",
            notes = "Merges the specified template into an agent application profile for preview purposes. " +
                    "The compose type determines which compose configuration variant from the template is used. " +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/profiles/merge/{agentAppTemplateId}/preview")
    @ResponseBody
    public AgentAppProfile mergeAgentAppProfileForPreview(
            @Parameter(description = AGENT_APP_TEMPLATE_ID_PARAM_DESCRIPTION)
            @PathVariable(TEMPLATE_ID) String strTemplateId,
            @Parameter(description = "The compose type to select from the template (e.g. 'monolith', 'microservices')")
            @RequestParam(required = false) String composeType,
            @Parameter(description = "The event action this merge previews (e.g. UPGRADE). Drives action-specific merge rules.")
            @RequestParam(required = false) AgentAppEventActionType actionType,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Agent application profile to merge template with")
            @RequestBody AgentAppProfile appProfile,
            HttpServletRequest request) throws ThingsboardException {
        checkParameter(TEMPLATE_ID, strTemplateId);
        TenantId tenantId = getCurrentUser().getTenantId();
        AgentAppTemplateId templateId = new AgentAppTemplateId(toUUID(strTemplateId));
        AgentAppTemplate template = checkAgentAppTemplateId(templateId, Operation.READ);

        appProfile.setTenantId(tenantId);
        String baseUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), request);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder()
                .template(template)
                .selectedComposeType(composeType)
                .setHostValues(true)
                .actionType(actionType)
                .baseUrl(baseUrl)
                .build();
        return tbProfileService.mergeForPreview(tenantId, appProfile, ctx);
    }

    @ApiOperation(value = "Get Agent Application Profiles by app type (getAgentAppProfilesByAppType)",
            notes = "Returns a list of agent application profiles filtered by application type."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/profiles/{appType}", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentAppProfileInfo> getAgentAppProfilesByAppType(
            @PathVariable @Parameter(description = "Application type, e.g. 'EDGE', 'GATEWAY', 'GENERIC'") AgentApplicationType appType) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(agentAppProfileService.findProfileInfosByTenantIdAndAppType(tenantId, appType));
    }

    @ApiOperation(value = "Get Tenant Agent Application Profiles (getTenantAgentAppProfiles)",
            notes = "Returns a page of agent application profiles owned by tenant." + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agent/app/profiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentAppProfile> getTenantAgentAppProfiles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = "Optional search text") @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_APP_PROFILE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentAppProfileService.findProfilesByTenantId(tenantId, pageLink));
    }
}

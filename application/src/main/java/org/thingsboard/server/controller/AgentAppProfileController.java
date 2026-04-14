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
package org.thingsboard.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;

import java.util.List;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.TbAgentAppProfileService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentAppProfileController extends BaseController {

    private static final String TEMPLATE_ID = "agentAppTemplateId";
    private static final String TEMPLATE_ID_PARAM_DESCRIPTION = "A string value representing the agent app template id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String PROFILE_ID = "profileId";

    private final TbAgentAppProfileService tbProfileService;

    @ApiOperation(value = "Get Agent Application Profile (getAgentAppProfileById)",
            notes = "Fetch the Agent Application Profile by Id." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/app/profile/{profileId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppProfile getAgentAppProfileById(
            @Parameter(description = "Profile Id") @PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter(PROFILE_ID, strProfileId);
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        return checkAgentAppProfileId(profileId, Operation.READ);
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

    @ApiOperation(value = "Merge template into application profile for preview (mergeForPreview)",
            notes = "Merges the specified template into an agent application profile for preview purposes. " +
                    "The compose type determines which compose configuration variant from the template is used. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/agent/app/profiles/merge/{agentAppTemplateId}/preview")
    @ResponseBody
    public AgentAppProfile mergeForPreview(
            @Parameter(description = TEMPLATE_ID_PARAM_DESCRIPTION)
            @PathVariable(TEMPLATE_ID) String strTemplateId,
            @Parameter(description = "The compose type to select from the template (e.g. 'monolith', 'microservices')")
            @RequestParam String composeType,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Agent application profile to merge template with")
            @RequestBody AgentAppProfile appProfile) throws ThingsboardException {
        checkParameter(TEMPLATE_ID, strTemplateId);
        TenantId tenantId = getCurrentUser().getTenantId();
        AgentAppTemplateId templateId = new AgentAppTemplateId(toUUID(strTemplateId));
        AgentAppTemplate template = checkAgentAppTemplateId(templateId, Operation.READ);

        appProfile.setTenantId(tenantId);
        return tbProfileService.mergeForPreview(tenantId, appProfile, template, composeType);
    }

    @ApiOperation(value = "Get Agent Application Profiles by app type (getAgentAppProfilesByAppType)",
            notes = "Returns a list of agent application profiles filtered by application type."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/app/profiles/{appType}", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentAppProfile> getAgentAppProfilesByAppType(
            @PathVariable @Parameter(description = "Application type, e.g. 'EDGE', 'GATEWAY', 'GENERIC'") AgentApplicationType appType) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(agentAppProfileService.findProfilesByTenantIdAndAppType(tenantId, appType));
    }

    @ApiOperation(value = "Get Tenant Agent Application Profiles (getTenantAgentAppProfiles)",
            notes = "Returns a page of agent application profiles owned by tenant." + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
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
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentAppProfileService.findProfilesByTenantId(tenantId, pageLink));
    }
}

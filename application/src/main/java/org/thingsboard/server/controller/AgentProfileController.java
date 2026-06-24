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
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentInstructions;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;
import org.thingsboard.server.common.data.agent.BulkOperationPreview;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.install.AgentInstallInstructionsService;
import org.thingsboard.server.service.entitiy.agent.TbAgentProfileService;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;

import java.util.ArrayList;
import java.util.List;

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
public class AgentProfileController extends BaseController {

    private static final String AGENT_PROFILE_ID = "agentProfileId";
    private static final String APPLICATION_PROFILE_ID = "applicationProfileId";

    private final TbAgentProfileService tbAgentProfileService;
    private final AgentBulkActionService agentBulkActionService;
    private final AgentInstallInstructionsService agentInstallInstructionsService;

    @ApiOperation(value = "Get Agent Profile (getAgentProfileById)",
            notes = "Fetch the Agent Profile object based on the provided Agent Profile Id." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/{agentProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentProfile getAgentProfileById(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        return checkAgentProfileId(agentProfileId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent Profile Info (getAgentProfileInfoById)",
            notes = "Fetch the Agent Profile Info object based on the provided Agent Profile Id. "
                    + "Agent Profile Info is a lightweight object that contains only id and name of the profile." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/info/{agentProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentProfileInfo getAgentProfileInfoById(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentProfileInfo agentProfileInfo = checkNotNull(agentProfileService.findAgentProfileInfoById(getTenantId(), agentProfileId));
        if (!getTenantId().equals(agentProfileInfo.getTenantId())) {
            throw permissionDenied();
        }
        return agentProfileInfo;
    }

    @ApiOperation(value = "Get Agent Provision Instructions (getAgentProvisionInstructions)",
            notes = "Returns the auto-provision docker command for the specified agent profile with the server address and gRPC port resolved server-side."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/instructions/provision/{agentProfileId}/{method}", method = RequestMethod.GET)
    @ResponseBody
    public AgentInstructions getAgentProvisionInstructions(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId,
            @Parameter(description = "Installation method ('docker')", schema = @Schema(allowableValues = {"docker"}))
            @PathVariable("method") String method,
            HttpServletRequest request) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentProfile profile = checkAgentProfileId(agentProfileId, Operation.READ);
        return checkNotNull(agentInstallInstructionsService.getProvisionInstructions(profile, method, request));
    }

    @ApiOperation(value = "Create or Update Agent Profile (saveAgentProfile)",
            notes = "Creates or updates the Agent Profile. When an id is not present in the request, a new Agent Profile is created, "
                    + "otherwise the existing one is updated. Optionally assigns the provided agent application profiles to this agent profile in the same request."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/profile")
    @ResponseBody
    public AgentProfile saveAgentProfile(@RequestBody AgentProfile agentProfile,
                                         @Parameter(description = "A list of agent application profile ids to assign to this agent profile")
                                         @RequestParam(required = false) String[] appProfileIds) throws Exception {
        agentProfile.setTenantId(getTenantId());
        checkEntity(agentProfile.getId(), agentProfile, Resource.AGENT_PROFILE);
        List<AgentAppProfileId> appProfileIdList = null;
        if (appProfileIds != null && appProfileIds.length > 0) {
            appProfileIdList = new ArrayList<>(appProfileIds.length);
            for (String rawId : appProfileIds) {
                AgentAppProfileId appProfileId = new AgentAppProfileId(toUUID(rawId));
                checkAgentAppProfileId(appProfileId, Operation.READ);
                appProfileIdList.add(appProfileId);
            }
        }
        return tbAgentProfileService.save(agentProfile, appProfileIdList, getCurrentUser());
    }

    @ApiOperation(value = "Delete Agent Profile (deleteAgentProfile)",
            notes = "Deletes the Agent Profile. Referencing a non-existing Agent Profile Id will cause an error. "
                    + "The default Agent Profile can not be deleted." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/{agentProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAgentProfile(@Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId) throws Exception {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentProfile agentProfile = checkAgentProfileId(agentProfileId, Operation.DELETE);
        tbAgentProfileService.delete(agentProfile, getCurrentUser());
    }

    @ApiOperation(value = "Get Default Agent Profile (getDefaultAgentProfileInfo)",
            notes = "Fetch the Agent Profile Info object that is marked as default within the current tenant scope." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/info/default", method = RequestMethod.GET)
    @ResponseBody
    public AgentProfileInfo getDefaultAgentProfileInfo() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_PROFILE, Operation.READ);
        return checkNotNull(agentProfileService.findDefaultAgentProfileInfo(getTenantId()));
    }

    @ApiOperation(value = "Make Agent Profile Default (setDefaultAgentProfile)",
            notes = "Marks agent profile as default within a tenant scope." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/{agentProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    public AgentProfile setDefaultAgentProfile(@Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentProfile agentProfile = checkAgentProfileId(agentProfileId, Operation.WRITE);
        AgentProfile previousDefaultAgentProfile = agentProfileService.findDefaultAgentProfile(getTenantId());
        return tbAgentProfileService.setDefaultAgentProfile(agentProfile, previousDefaultAgentProfile, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Agent Profiles (getTenantAgentProfiles)",
            notes = "Returns a page of Agent Profile objects owned by the current tenant. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agent/profiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentProfile> getTenantAgentProfiles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_PROFILE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentProfileService.findAgentProfilesByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Agent Profile Infos (getTenantAgentProfileInfos)",
            notes = "Returns a page of Agent Profile Info objects owned by the current tenant. "
                    + "Agent Profile Info is a lightweight object that contains only id and name of the profile. "
                    + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agent/profileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentProfileInfo> getTenantAgentProfileInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_PROFILE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentProfileService.findAgentProfileInfosByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Agent Profile App Profile Infos (getAgentProfileAppProfileInfos)",
            notes = "Returns app profiles assigned to the given agent profile, enriched with template version, " +
                    "the count of AgentApplications using each app profile within this agent profile " +
                    "and the additional info of the assignment relation." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/{agentProfileId}/appProfilesInfo", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentAppProfileRelationInfo> getAgentProfileAppProfileInfos(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        checkAgentProfileId(agentProfileId, Operation.READ);
        return agentAppProfileService.findProfileRelationInfosByAgentProfileId(getTenantId(), agentProfileId);
    }

    @ApiOperation(value = "Assign App Profile to Agent Profile (assignAppProfileToAgentProfile)",
            notes = "Assigns the given agent application profile to the agent profile, making it available to applications managed by that agent profile."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/{agentProfileId}/appProfile/{applicationProfileId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void assignAppProfileToAgentProfile(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId,
            @Parameter(description = "Agent Application Profile Id") @PathVariable(APPLICATION_PROFILE_ID) String strApplicationProfileId) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        checkParameter(APPLICATION_PROFILE_ID, strApplicationProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentAppProfileId applicationProfileId = new AgentAppProfileId(toUUID(strApplicationProfileId));
        checkAgentProfileId(agentProfileId, Operation.WRITE);
        AgentAppProfileInfo applicationProfileInfo = checkNotNull(
                agentAppProfileService.findProfileInfoById(getTenantId(), applicationProfileId));
        if (!getTenantId().equals(applicationProfileInfo.getTenantId())) {
            throw permissionDenied();
        }
        agentProfileService.assignAppProfileToAgentProfile(getTenantId(), agentProfileId, applicationProfileId);
    }

    @ApiOperation(value = "Assign Multiple App Profiles to Agent Profile (assignAppProfilesToAgentProfile)",
            notes = "Assigns the given set of agent application profiles to the agent profile in a single request, replacing the previous assignment set."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/profile/{agentProfileId}/appProfiles/assign")
    @ResponseStatus(value = HttpStatus.OK)
    public void assignAppProfilesToAgentProfile(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId,
            @Parameter(description = "A list of agent application profile ids to assign to this agent profile")
            @RequestParam String[] appProfileIds) throws Exception {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentProfile agentProfile = checkAgentProfileId(agentProfileId, Operation.WRITE);
        List<AgentAppProfileId> appProfileIdList = new ArrayList<>(appProfileIds.length);
        for (String rawId : appProfileIds) {
            AgentAppProfileId appProfileId = new AgentAppProfileId(toUUID(rawId));
            checkAgentAppProfileId(appProfileId, Operation.READ);
            appProfileIdList.add(appProfileId);
        }
        tbAgentProfileService.assignAppProfiles(agentProfile, appProfileIdList, getCurrentUser());
    }

    @ApiOperation(value = "Unassign App Profile from Agent Profile (unassignAppProfileFromAgentProfile)",
            notes = "Removes the assignment of the given agent application profile from the agent profile." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/{agentProfileId}/appProfile/{applicationProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void unassignAppProfileFromAgentProfile(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId,
            @Parameter(description = "Agent Application Profile Id") @PathVariable(APPLICATION_PROFILE_ID) String strApplicationProfileId) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        checkParameter(APPLICATION_PROFILE_ID, strApplicationProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentAppProfileId applicationProfileId = new AgentAppProfileId(toUUID(strApplicationProfileId));
        checkAgentProfileId(agentProfileId, Operation.WRITE);
        agentProfileService.unassignAppProfileFromAgentProfile(getTenantId(), agentProfileId, applicationProfileId);
    }

    @ApiOperation(value = "Set App Profile Relates On Auto-Discovery (setAppProfileRelatesOnAutoDiscovery)",
            notes = "Enables or disables automatic assignment of the app profile to new auto-discovered applications " +
                    "with a matching template. Only one app profile per agent profile and template pair may relate on auto-discovery."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/profile/{agentProfileId}/appProfile/{applicationProfileId}/autoDiscovery")
    @ResponseStatus(value = HttpStatus.OK)
    public void setAppProfileRelatesOnAutoDiscovery(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId,
            @Parameter(description = "Agent Application Profile Id") @PathVariable(APPLICATION_PROFILE_ID) String strApplicationProfileId,
            @Parameter(description = "Whether the app profile should be auto-assigned to new auto-discovered applications with a matching template")
            @RequestParam boolean relate) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        checkParameter(APPLICATION_PROFILE_ID, strApplicationProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentAppProfileId applicationProfileId = new AgentAppProfileId(toUUID(strApplicationProfileId));
        AgentProfile agentProfile = checkAgentProfileId(agentProfileId, Operation.WRITE);
        checkAgentAppProfileId(applicationProfileId, Operation.READ);
        tbAgentProfileService.setAppProfileRelatesOnAutoDiscovery(agentProfile, applicationProfileId, relate, getCurrentUser());
    }

    @ApiOperation(value = "Preview Bulk Operation (previewBulkOperation)",
            notes = "Previews the effect of a bulk operation over the applications of the given agent profile and application profile "
                    + "without enqueuing it, returning the set of affected applications and the resulting actions." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/profile/{agentProfileId}/appProfile/{applicationProfileId}/bulk/preview")
    @ResponseBody
    public BulkOperationPreview previewBulkOperation(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId,
            @Parameter(description = "Agent Application Profile Id") @PathVariable(APPLICATION_PROFILE_ID) String strApplicationProfileId,
            @RequestBody BulkOperationRequest request) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        checkParameter(APPLICATION_PROFILE_ID, strApplicationProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentAppProfileId applicationProfileId = new AgentAppProfileId(toUUID(strApplicationProfileId));
        checkAgentProfileId(agentProfileId, Operation.WRITE);
        checkAgentAppProfileId(applicationProfileId, Operation.READ);
        return agentBulkActionProcessingService.preview(getTenantId(), agentProfileId, applicationProfileId, request);
    }

    @ApiOperation(value = "Get Bulk Actions for Agent Profile (getAgentProfileBulkActions)",
            notes = "Returns a page of bulk actions previously submitted for the given agent profile. " + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/{agentProfileId}/bulk", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentBulkAction> getAgentProfileBulkActions(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        checkAgentProfileId(agentProfileId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, null, sortProperty, sortOrder);
        return agentBulkActionService.findByAgentProfileId(getTenantId(), agentProfileId, pageLink);
    }

    @ApiOperation(value = "Get Bulk Actions for Agent Profile and Application Profile (getAgentProfileAppProfileBulkActions)",
            notes = "Returns a page of bulk actions previously submitted for the given agent profile and application profile pair. "
                    + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/profile/{agentProfileId}/appProfile/{applicationProfileId}/bulk",
            params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentBulkAction> getAgentProfileAppProfileBulkActions(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId,
            @Parameter(description = "Agent Application Profile Id") @PathVariable(APPLICATION_PROFILE_ID) String strApplicationProfileId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        checkParameter(APPLICATION_PROFILE_ID, strApplicationProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentAppProfileId applicationProfileId = new AgentAppProfileId(toUUID(strApplicationProfileId));
        checkAgentProfileId(agentProfileId, Operation.READ);
        checkAgentAppProfileId(applicationProfileId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, null, sortProperty, sortOrder);
        return agentBulkActionService.findByAgentProfileIdAndApplicationProfileId(
                getTenantId(), agentProfileId, applicationProfileId, pageLink);
    }

    @ApiOperation(value = "Bulk Operation (bulkOperation)",
            notes = "Enqueues a bulk operation over the applications of the given agent profile and application profile and "
                    + "returns the created bulk action to track its progress." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/profile/{agentProfileId}/appProfile/{applicationProfileId}/bulk")
    @ResponseBody
    public AgentBulkAction bulkOperation(
            @Parameter(description = "Agent Profile Id") @PathVariable(AGENT_PROFILE_ID) String strAgentProfileId,
            @Parameter(description = "Agent Application Profile Id") @PathVariable(APPLICATION_PROFILE_ID) String strApplicationProfileId,
            @RequestBody BulkOperationRequest request) throws ThingsboardException {
        checkParameter(AGENT_PROFILE_ID, strAgentProfileId);
        checkParameter(APPLICATION_PROFILE_ID, strApplicationProfileId);
        AgentProfileId agentProfileId = new AgentProfileId(toUUID(strAgentProfileId));
        AgentAppProfileId applicationProfileId = new AgentAppProfileId(toUUID(strApplicationProfileId));
        checkAgentProfileId(agentProfileId, Operation.WRITE);
        checkAgentAppProfileId(applicationProfileId, Operation.READ);
        return agentBulkActionProcessingService.enqueueBulkOperation(getTenantId(), agentProfileId, applicationProfileId, request);
    }
}

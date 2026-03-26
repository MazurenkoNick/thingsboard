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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.agent.AgentGroup;
import org.thingsboard.server.common.data.agent.AgentGroupInfo;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.agent.BulkOperationResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentGroupId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.TbAgentGroupService;
import org.thingsboard.server.service.entitiy.agent.AgentBulkOperationService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentGroupController extends BaseController {

    private static final String GROUP_ID = "groupId";
    private static final String PROFILE_ID = "profileId";

    private final TbAgentGroupService tbGroupService;

    @ApiOperation(value = "Get Agent Group (getAgentGroupById)")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/group/{groupId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentGroup getAgentGroupById(
            @PathVariable(GROUP_ID) String strGroupId) throws ThingsboardException {
        checkParameter(GROUP_ID, strGroupId);
        AgentGroupId groupId = new AgentGroupId(toUUID(strGroupId));
        return checkAgentGroupId(groupId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent Group Info (getAgentGroupInfoById)")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/group/info/{groupId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentGroupInfo getAgentGroupInfoById(
            @PathVariable(GROUP_ID) String strGroupId) throws ThingsboardException {
        checkParameter(GROUP_ID, strGroupId);
        AgentGroupId groupId = new AgentGroupId(toUUID(strGroupId));
        return checkNotNull(agentGroupService.findGroupInfoById(getTenantId(), groupId));
    }

    @ApiOperation(value = "Create or Update Agent Group (saveAgentGroup)")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/agent/group")
    @ResponseBody
    public AgentGroup saveAgentGroup(@RequestBody AgentGroup group) throws Exception {
        group.setTenantId(getTenantId());
        checkEntity(group.getId(), group, Resource.AGENT_GROUP);
        return tbGroupService.save(group, getCurrentUser());
    }

    @ApiOperation(value = "Delete Agent Group (deleteAgentGroup)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/group/{groupId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAgentGroup(@PathVariable(GROUP_ID) String strGroupId) throws Exception {
        checkParameter(GROUP_ID, strGroupId);
        AgentGroupId groupId = new AgentGroupId(toUUID(strGroupId));
        AgentGroup group = checkAgentGroupId(groupId, Operation.DELETE);
        tbGroupService.delete(group, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Agent Groups (getTenantAgentGroups)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agent/groups", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentGroup> getTenantAgentGroups(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentGroupService.findGroupsByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Agent Group Infos (getTenantAgentGroupInfos)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agent/groupInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentGroupInfo> getTenantAgentGroupInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentGroupService.findGroupInfosByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Customer Agent Groups (getCustomerAgentGroups)")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/agent/groups", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentGroup> getCustomerAgentGroups(
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentGroupService.findGroupsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
    }

    @ApiOperation(value = "Assign Agent Group to Customer (assignAgentGroupToCustomer)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/agent/group/{groupId}", method = RequestMethod.POST)
    @ResponseBody
    public AgentGroup assignAgentGroupToCustomer(
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @PathVariable(GROUP_ID) String strGroupId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(GROUP_ID, strGroupId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        AgentGroupId groupId = new AgentGroupId(toUUID(strGroupId));
        checkAgentGroupId(groupId, Operation.ASSIGN_TO_CUSTOMER);
        return tbGroupService.assignGroupToCustomer(getTenantId(), groupId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign Agent Group from Customer (unassignAgentGroupFromCustomer)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/agent/group/{groupId}", method = RequestMethod.DELETE)
    @ResponseBody
    public AgentGroup unassignAgentGroupFromCustomer(
            @PathVariable(GROUP_ID) String strGroupId) throws ThingsboardException {
        checkParameter(GROUP_ID, strGroupId);
        AgentGroupId groupId = new AgentGroupId(toUUID(strGroupId));
        AgentGroup group = checkAgentGroupId(groupId, Operation.UNASSIGN_FROM_CUSTOMER);
        if (group.getCustomerId() == null || group.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            throw new ThingsboardException("Agent group isn't assigned to any customer!",
                    org.thingsboard.server.common.data.exception.ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        Customer customer = checkCustomerId(group.getCustomerId(), Operation.READ);
        return tbGroupService.unassignGroupFromCustomer(getTenantId(), groupId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Get Group Profile Relations (getGroupProfileRelations)")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/group/{groupId}/profiles", method = RequestMethod.GET)
    @ResponseBody
    public List<EntityRelation> getGroupProfileRelations(
            @PathVariable(GROUP_ID) String strGroupId) throws ThingsboardException {
        checkParameter(GROUP_ID, strGroupId);
        AgentGroupId groupId = new AgentGroupId(toUUID(strGroupId));
        checkAgentGroupId(groupId, Operation.READ);
        return agentGroupService.findProfileRelations(getTenantId(), groupId);
    }

    @ApiOperation(value = "Assign Profile to Group (assignProfileToGroup)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/group/{groupId}/profile/{profileId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void assignProfileToGroup(
            @PathVariable(GROUP_ID) String strGroupId,
            @PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter(GROUP_ID, strGroupId);
        checkParameter(PROFILE_ID, strProfileId);
        AgentGroupId groupId = new AgentGroupId(toUUID(strGroupId));
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        checkAgentGroupId(groupId, Operation.WRITE);
        checkAgentAppProfileId(profileId, Operation.READ);
        agentGroupService.assignProfileToGroup(getTenantId(), groupId, profileId);
    }

    @ApiOperation(value = "Unassign Profile from Group (unassignProfileFromGroup)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/group/{groupId}/profile/{profileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void unassignProfileFromGroup(
            @PathVariable(GROUP_ID) String strGroupId,
            @PathVariable(PROFILE_ID) String strProfileId) throws ThingsboardException {
        checkParameter(GROUP_ID, strGroupId);
        checkParameter(PROFILE_ID, strProfileId);
        AgentGroupId groupId = new AgentGroupId(toUUID(strGroupId));
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        checkAgentGroupId(groupId, Operation.WRITE);
        agentGroupService.unassignProfileFromGroup(getTenantId(), groupId, profileId);
    }

    @ApiOperation(value = "Bulk Operation (bulkOperation)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/group/{groupId}/profile/{profileId}/bulk")
    @ResponseBody
    public BulkOperationResult bulkOperation(
            @PathVariable(GROUP_ID) String strGroupId,
            @PathVariable(PROFILE_ID) String strProfileId,
            @RequestBody BulkOperationRequest request,
            @RequestParam(defaultValue = "false") boolean force) throws ThingsboardException {
        checkParameter(GROUP_ID, strGroupId);
        checkParameter(PROFILE_ID, strProfileId);
        AgentGroupId groupId = new AgentGroupId(toUUID(strGroupId));
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        checkAgentGroupId(groupId, Operation.WRITE);
        checkAgentAppProfileId(profileId, Operation.READ);
        return agentBulkOperationService.bulkOperation(getTenantId(), groupId, profileId, request, force, getCurrentUser());
    }
}

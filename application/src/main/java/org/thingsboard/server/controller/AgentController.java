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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.TbAgentService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentController extends BaseController {

    private static final String AGENT_ID = "agentId";
    private static final String AGENT_ID_PARAM_DESCRIPTION = "A string value representing the agent id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    private final TbAgentService tbAgentService;
    private final AgentAppEventService agentAppEventService;

    @ApiOperation(value = "Get Agent (getAgentById)",
            notes = "Fetch the Agent object based on the provided Agent Id. " +
                    "If the user has the authority of 'TENANT_ADMIN', the server checks that the agent is owned by the same tenant. " +
                    "If the user has the authority of 'CUSTOMER_USER', the server checks that the agent is assigned to the same customer."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/{agentId}", method = RequestMethod.GET)
    @ResponseBody
    public Agent getAgentById(@Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
                              @PathVariable(AGENT_ID) String strAgentId) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        return checkAgentId(agentId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent App Events by Agent Id (getAgentAppEventsByAgentId)",
            notes = "Returns a page of agent application events for all applications belonging to the specified agent. "
                    + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/{agentId}/events", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentAppEvent> getAgentAppEventsByAgentId(
            @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_ID) String strAgentId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value reserved for future event filtering")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "updatedTime"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        checkAgentId(agentId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return agentAppEventService.findByAgentId(tenantId, agentId, pageLink);
    }

    @ApiOperation(value = "Get Agent Info (getAgentInfoById)",
            notes = "Fetch the Agent Info object based on the provided Agent Id. " +
                    "Agent Info extends the Agent object and adds customer title and 'is public' flag."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/info/{agentId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentInfo getAgentInfoById(@Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
                                      @PathVariable(AGENT_ID) String strAgentId) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        return checkNotNull(agentService.findAgentInfoById(getTenantId(), agentId));
    }

    @ApiOperation(value = "Create Or Update Agent (saveAgent)",
            notes = "Creates or Updates the Agent. When creating agent, platform generates Agent Id as " + UUID_WIKI_LINK +
                    "The newly created Agent id will be present in the response. " +
                    "Specify existing Agent id to update the agent. " +
                    "Referencing non-existing Agent Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Agent entity. "
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/agent")
    @ResponseBody
    public Agent saveAgent(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the agent.") @RequestBody Agent agent) throws Exception {
        agent.setTenantId(getTenantId());
        checkEntity(agent.getId(), agent, Resource.AGENT);
        return tbAgentService.save(agent, getCurrentUser());
    }

    @ApiOperation(value = "Delete agent (deleteAgent)",
            notes = "Deletes the agent and all the relations (from and to the agent). Referencing non-existing agent Id will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/{agentId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAgent(@Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
                            @PathVariable(AGENT_ID) String strAgentId) throws Exception {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        Agent agent = checkAgentId(agentId, Operation.DELETE);
        tbAgentService.delete(agent, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Agents (getTenantAgents)",
            notes = "Returns a page of agents owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agents", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Agent> getTenantAgents(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing agent name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentService.findAgentsByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Agent Infos (getTenantAgentInfos)",
            notes = "Returns a page of agent info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/agentInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentInfo> getTenantAgentInfos(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing agent name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentService.findAgentInfosByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Customer Agents (getCustomerAgents)",
            notes = "Returns a page of agent objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/agents", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Agent> getCustomerAgents(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing agent name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentService.findAgentsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
    }

    @ApiOperation(value = "Get Customer Agent Infos (getCustomerAgentInfos)",
            notes = "Returns a page of agent info objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/agentInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AgentInfo> getCustomerAgentInfos(
            @Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing agent name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentService.findAgentInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
    }

    @ApiOperation(value = "Assign agent to customer (assignAgentToCustomer)",
            notes = "Creates assignment of the agent to customer. Customer will be able to query agent afterwards." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/agent/{agentId}", method = RequestMethod.POST)
    @ResponseBody
    public Agent assignAgentToCustomer(@Parameter(description = CUSTOMER_ID_PARAM_DESCRIPTION)
                                       @PathVariable(CUSTOMER_ID) String strCustomerId,
                                       @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
                                       @PathVariable(AGENT_ID) String strAgentId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(AGENT_ID, strAgentId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        checkAgentId(agentId, Operation.ASSIGN_TO_CUSTOMER);
        return tbAgentService.assignAgentToCustomer(getTenantId(), agentId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign agent from customer (unassignAgentFromCustomer)",
            notes = "Clears assignment of the agent to customer. Customer will not be able to query agent afterwards." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/agent/{agentId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Agent unassignAgentFromCustomer(@Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
                                           @PathVariable(AGENT_ID) String strAgentId) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        Agent agent = checkAgentId(agentId, Operation.UNASSIGN_FROM_CUSTOMER);
        if (agent.getCustomerId() == null || agent.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            throw new ThingsboardException("Agent isn't assigned to any customer!",
                    org.thingsboard.server.common.data.exception.ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        Customer customer = checkCustomerId(agent.getCustomerId(), Operation.READ);
        return tbAgentService.unassignAgentToCustomer(getTenantId(), agentId, customer, getCurrentUser());
    }
}

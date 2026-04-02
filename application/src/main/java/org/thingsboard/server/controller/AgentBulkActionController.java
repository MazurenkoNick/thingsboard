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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentBulkActionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentBulkActionController extends BaseController {

    private static final String BULK_ACTION_ID = "bulkActionId";

    private final AgentBulkActionService agentBulkActionService;
    private final AgentAppEventService agentAppEventService;

    @ApiOperation(value = "Get Agent Bulk Action (getAgentBulkAction)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/agent/bulk/{bulkActionId}")
    @ResponseBody
    public AgentBulkAction getAgentBulkAction(
            @PathVariable(BULK_ACTION_ID) String strBulkActionId) throws ThingsboardException {
        checkParameter(BULK_ACTION_ID, strBulkActionId);
        AgentBulkActionId bulkActionId = new AgentBulkActionId(toUUID(strBulkActionId));
        AgentBulkAction bulkAction = checkNotNull(agentBulkActionService.findById(getTenantId(), bulkActionId));
        if (!getTenantId().equals(bulkAction.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return bulkAction;
    }

    @ApiOperation(value = "Get Agent Bulk Action Events (getAgentBulkActionEvents)")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/agent/bulk/{bulkActionId}/events", params = {"pageSize", "page"})
    @ResponseBody
    public PageData<AgentAppEvent> getAgentBulkActionEvents(
            @PathVariable(BULK_ACTION_ID) String strBulkActionId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true) @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true) @RequestParam int page,
            @RequestParam(required = false) AgentAppEventStatus status,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION) @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION) @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(BULK_ACTION_ID, strBulkActionId);
        AgentBulkActionId bulkActionId = new AgentBulkActionId(toUUID(strBulkActionId));
        AgentBulkAction bulkAction = checkNotNull(agentBulkActionService.findById(getTenantId(), bulkActionId));
        if (!getTenantId().equals(bulkAction.getTenantId())) {
            throw new ThingsboardException("You don't have permission to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED);
        }
        PageLink pageLink = createPageLink(pageSize, page, null, sortProperty, sortOrder);
        return agentAppEventService.findByBulkActionId(bulkActionId, status, pageLink);
    }
}

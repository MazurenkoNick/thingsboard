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
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.TbAgentApplicationService;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentApplicationController extends BaseController {

    private static final String TEMPLATE_ID = "agentAppTemplateId";
    private static final String TEMPLATE_ID_PARAM_DESCRIPTION = "A string value representing the agent app template id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String AGENT_APP_ID = "agentApplicationId";
    private static final String AGENT_APP_ID_PARAM_DESCRIPTION = "A string value representing the agent application id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String AGENT_ID = "agentId";
    private static final String AGENT_ID_PARAM_DESCRIPTION = "A string value representing the agent id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    private final TbAgentApplicationService tbAgentApplicationService;

    @ApiOperation(value = "Get Agent Application (getAgentApplicationById)",
            notes = "Fetch the Agent Application object based on the provided Agent Application Id."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/app/{agentApplicationId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentApplication getAgentApplicationById(@Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
                                                    @PathVariable(AGENT_APP_ID) String strAgentAppId) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        return checkAgentAppId(agentApplicationId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent Applications by Agent Id (getAgentApplicationsByAgentId)",
            notes = "Returns a list of agent applications that belong to the specified agent."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/{agentId}/apps", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentApplication> getAgentApplicationsByAgentId(@Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
                                                                @PathVariable(AGENT_ID) String strAgentId) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        checkAgentId(agentId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(agentAppService.findAllByAgentId(tenantId, agentId));
    }

    @ApiOperation(value = "Create Or Update Agent Application (saveAgentApplication)",
            notes = "Creates or Updates the Agent Application."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/agent/app")
    @ResponseBody
    public AgentApplication saveAgentApplication(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the agent application.")
            @RequestBody AgentApplication agentApplication) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        agentApplication.setTenantId(tenantId);
        if (agentApplication.getId() != null) {
            checkAgentAppId(agentApplication.getId(), Operation.WRITE);
        } else {
            checkEntity(null, agentApplication, org.thingsboard.server.service.security.permission.Resource.AGENT_APPLICATION);
        }
        return checkNotNull(agentAppService.save(tenantId, agentApplication));
    }

    @ApiOperation(value = "Delete Agent Application (deleteAgentApplication)",
            notes = "Deletes the agent application. Referencing non-existing agent application Id will cause an error."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/app/{agentApplicationId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAgentApplication(@Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
                                       @PathVariable(AGENT_APP_ID) String strAgentAppId) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.DELETE);
        TenantId tenantId = getCurrentUser().getTenantId();
        agentAppService.delete(tenantId, agentApplicationId);
    }

    @ApiOperation(value = "Merge template into application for preview (mergeForPreview)",
            notes = "Merges the specified template into an agent application for preview purposes. " +
                    "If no application is provided in the body, a new one is created from the template. " +
                    "The compose type determines which compose configuration variant from the template is used."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/agent/app/merge/{agentAppTemplateId}/preview")
    @ResponseBody
    public AgentApplication mergeForPreview(
            @Parameter(description = TEMPLATE_ID_PARAM_DESCRIPTION)
            @PathVariable(TEMPLATE_ID) String strTemplateId,
            @Parameter(description = "The compose type to select from the template (e.g. 'monolith', 'microservices')")
            @RequestParam String composeType,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional agent application to merge with. If null, a new application is created from the template.")
            @RequestBody(required = false) AgentApplication application) throws ThingsboardException {
        checkParameter(TEMPLATE_ID, strTemplateId);
        TenantId tenantId = getCurrentUser().getTenantId();
        AgentAppTemplateId templateId = new AgentAppTemplateId(toUUID(strTemplateId));
        AgentAppTemplate template = checkAgentAppTemplateId(templateId, Operation.READ);

        if (template.getAppType() == AgentApplicationType.GENERIC) {
            throw new ThingsboardException("Can't merge agent application with the template of the generic type", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (application == null) {
            application = AgentApplication.fromTemplate(template);
            application.setTenantId(tenantId);
        }
        return tbAgentApplicationService.mergeForPreview(tenantId, application, template, composeType);
    }
}

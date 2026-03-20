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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentAppTemplateController extends BaseController {

    private static final String TEMPLATE_ID = "templateId";
    private static final String TEMPLATE_ID_PARAM_DESCRIPTION = "A string value representing the agent app template id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String APP_TYPE_PARAM_DESCRIPTION = "A string value representing the agent configuration type, e.g. 'EDGE', 'GATEWAY'";
    private static final String CONFIG_TYPE_PARAM_DESCRIPTION = "A string value representing the agent configuration type, e.g. 'DOCKER_COMPOSE'";


    @ApiOperation(value = "Get Agent App Template (getAgentAppTemplateById)",
            notes = "Fetch the Agent App Template object based on the provided Template Id."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/app/template/{templateId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppTemplate getAgentAppTemplateById(@Parameter(description = TEMPLATE_ID_PARAM_DESCRIPTION)
                                                    @PathVariable(TEMPLATE_ID) String strTemplateId) throws ThingsboardException {
        checkParameter(TEMPLATE_ID, strTemplateId);
        AgentAppTemplateId templateId = new AgentAppTemplateId(toUUID(strTemplateId));
        return checkAgentAppTemplateId(templateId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent App Template (getLatestAgentAppTemplateByType)",
            notes = "Fetch the latest Agent App Template object based on its creation date"
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/app/template/{appType}/{configType}/latest", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppTemplate getLatestAgentAppTemplateByAppTypeAndConfigType(
            @PathVariable @Parameter(description = APP_TYPE_PARAM_DESCRIPTION) AgentApplicationType appType,
            @PathVariable @Parameter(description = CONFIG_TYPE_PARAM_DESCRIPTION) AgentAppConfigType configType) throws ThingsboardException {
        return checkNotNull(agentAppTemplateService.findLatestByAppTypeAndConfigType(appType, configType));
    }

    @ApiOperation(value = "Get Agent App Template by current version (getAgentAppTemplateByCurrentVersion)",
            notes = "Fetch the Agent App Template object based on the provided app type, config type, and current version."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/app/template/{appType}/{configType}/{currentVersion}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppTemplate getAgentAppTemplateByCurrentVersion(
            @PathVariable @Parameter(description = APP_TYPE_PARAM_DESCRIPTION) AgentApplicationType appType,
            @PathVariable @Parameter(description = CONFIG_TYPE_PARAM_DESCRIPTION) AgentAppConfigType configType,
            @PathVariable @Parameter(description = "A string value representing the current version of the template, e.g. '1.0.0'") String currentVersion) throws ThingsboardException {
        return checkNotNull(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(appType, configType, currentVersion));
    }

    @ApiOperation(value = "Get all Agent App Templates (getAgentAppTemplates)",
            notes = "Returns a list of all agent app templates available for the current tenant."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/agent/app/templates", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentAppTemplate> getAgentAppTemplates() throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(agentAppTemplateService.findAll(tenantId));
    }

}

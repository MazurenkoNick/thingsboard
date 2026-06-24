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
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;

import java.util.List;

import static org.thingsboard.server.controller.ControllerConstants.AGENT_APP_TEMPLATE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentAppTemplateController extends BaseController {

    private static final String TEMPLATE_ID = "agentAppTemplateId";
    private static final String APP_TYPE_PARAM_DESCRIPTION = "A string value representing the agent application type, e.g. 'EDGE', 'GATEWAY'";
    private static final String CONFIG_TYPE_PARAM_DESCRIPTION = "A string value representing the agent configuration type, e.g. 'DOCKER_COMPOSE'";


    @ApiOperation(value = "Get Agent App Template (getAgentAppTemplateById)",
            notes = "Fetch the Agent App Template object based on the provided Template Id."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/template/{agentAppTemplateId}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppTemplate getAgentAppTemplateById(@Parameter(description = AGENT_APP_TEMPLATE_ID_PARAM_DESCRIPTION)
                                                    @PathVariable(TEMPLATE_ID) String strTemplateId) throws ThingsboardException {
        checkParameter(TEMPLATE_ID, strTemplateId);
        AgentAppTemplateId templateId = new AgentAppTemplateId(toUUID(strTemplateId));
        return checkAgentAppTemplateId(templateId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent App Template (getLatestAgentAppTemplateByType)",
            notes = "Fetch the latest Agent App Template object based on its creation date"
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/template/{appType}/{configType}/latest", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppTemplate getLatestAgentAppTemplateByAppTypeAndConfigType(
            @PathVariable @Parameter(description = APP_TYPE_PARAM_DESCRIPTION) AgentApplicationType appType,
            @PathVariable @Parameter(description = CONFIG_TYPE_PARAM_DESCRIPTION) AgentAppConfigType configType) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_APP_TEMPLATE, Operation.READ);
        return checkNotNull(agentAppTemplateService.findLatestByAppTypeAndConfigType(appType, configType));
    }

    @ApiOperation(value = "Get Agent App Template by current version (getAgentAppTemplateByCurrentVersion)",
            notes = "Fetch the Agent App Template object based on the provided app type, config type, and current version."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/template/{appType}/{configType}/{currentVersion}", method = RequestMethod.GET)
    @ResponseBody
    public AgentAppTemplate getAgentAppTemplateByCurrentVersion(
            @PathVariable @Parameter(description = APP_TYPE_PARAM_DESCRIPTION) AgentApplicationType appType,
            @PathVariable @Parameter(description = CONFIG_TYPE_PARAM_DESCRIPTION) AgentAppConfigType configType,
            @PathVariable @Parameter(description = "A string value representing the current version of the template, e.g. '1.0.0'") String currentVersion) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_APP_TEMPLATE, Operation.READ);
        return checkNotNull(agentAppTemplateService.findByAppTypeAndConfigTypeAndVersion(appType, configType, currentVersion));
    }

    @ApiOperation(value = "Get Agent App Templates by type (getAgentAppTemplatesByAppType)",
            notes = "Returns a list of agent app templates filtered by application type and config type."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/templates/{appType}/{configType}", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentAppTemplate> getAgentAppTemplatesByAppType(
            @PathVariable @Parameter(description = APP_TYPE_PARAM_DESCRIPTION) AgentApplicationType appType,
            @PathVariable @Parameter(description = CONFIG_TYPE_PARAM_DESCRIPTION) AgentAppConfigType configType) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_APP_TEMPLATE, Operation.READ);
        return checkNotNull(agentAppTemplateService.findByAppTypeAndConfigType(appType, configType));
    }

    @ApiOperation(value = "Get all Agent App Templates (getAgentAppTemplates)",
            notes = "Returns a list of all agent app templates available for the current tenant."
                    + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/agent/app/templates", method = RequestMethod.GET)
    @ResponseBody
    public List<AgentAppTemplate> getAgentAppTemplates() throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.AGENT_APP_TEMPLATE, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(agentAppTemplateService.findAll(tenantId));
    }

}

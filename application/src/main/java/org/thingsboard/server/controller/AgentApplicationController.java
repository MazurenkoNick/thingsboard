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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventFilter;
import org.thingsboard.server.common.data.agent.AgentAppEventStatus;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.agent.AgentAppEventService;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.TbAgentApplicationService;
import org.thingsboard.server.service.security.permission.Operation;

import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
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
    private static final String AGENT_APP_EVENT_ID = "agentAppEventId";
    private static final String AGENT_APP_EVENT_ID_PARAM_DESCRIPTION = "A string value representing the agent app event id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    private static final String AGENT_ID = "agentId";
    private static final String AGENT_ID_PARAM_DESCRIPTION = "A string value representing the agent id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    private final TbAgentApplicationService tbAgentApplicationService;
    private final AgentAppEventService agentAppEventService;
    private final AgentAppUnitService agentAppUnitService;

    @ApiOperation(value = "Get Agent Application (getAgentApplicationById)",
            notes = "Fetch the Agent Application object based on the provided Agent Application Id."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/agent/app/{agentApplicationId}")
    @ResponseBody
    public AgentApplication getAgentApplicationById(@Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
                                                    @PathVariable(AGENT_APP_ID) String strAgentAppId) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        return checkAgentAppId(agentApplicationId, Operation.READ);
    }

    @ApiOperation(value = "Get Agent Applications by Agent Id (getAgentApplicationsByAgentId)",
            notes = "Returns a page of agent applications that belong to the specified agent. "
                    + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/agent/{agentId}/apps", params = {"pageSize", "page"})
    @ResponseBody
    public PageData<AgentApplication> getAgentApplicationsByAgentId(
            @Parameter(description = AGENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_ID) String strAgentId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value representing application name")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_ID, strAgentId);
        AgentId agentId = new AgentId(toUUID(strAgentId));
        checkAgentId(agentId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(agentAppService.findByAgentId(tenantId, agentId, pageLink));
    }

    @ApiOperation(value = "Get Agent Application by Related Entity (getAgentApplicationByRelatedEntity)",
            notes = "Returns the agent application linked to the specified entity. "
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/agent/apps/{entityType}/{entityId}")
    @ResponseBody
    public AgentApplication getAgentApplicationByRelatedEntity(
            @Parameter(description = "Entity type", required = true)
            @PathVariable String entityType,
            @Parameter(description = "Entity id", required = true)
            @PathVariable String entityId) throws ThingsboardException {
        EntityId relatedEntityId = EntityIdFactory.getByTypeAndId(entityType, entityId);
        checkEntityId(relatedEntityId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(agentAppService.findByRelatedEntity(tenantId, relatedEntityId));
    }

    @ApiOperation(value = "Update Agent Application (saveAgentApplication)",
            notes = "Updates the Agent Application."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PutMapping("/agent/app")
    @ResponseBody
    public AgentApplication updateAgentApplication(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the agent application.")
            @RequestBody AgentApplication agentApplication) throws Exception {
        agentApplication.setTenantId(getTenantId());
        checkEntityId(agentApplication.getId(), Operation.WRITE);
        return tbAgentApplicationService.update(agentApplication, getCurrentUser());
    }

    @ApiOperation(value = "Install Agent Application (installAgentApp)",
            notes = "Creates a new agent application and an INSTALL event in a single operation. " +
                    "The request body must include the application object."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/agent/app/event")
    @ResponseBody
    public AgentApplication installAgentApp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the install event request with the application.")
            @RequestBody AgentAppEventRequest request) throws Exception {
        if (request.getApplication() == null) {
            throw new ThingsboardException("Install request must include an application", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        TenantId tenantId = getCurrentUser().getTenantId();
        return tbAgentApplicationService.install(tenantId, request, getCurrentUser());
    }

    @ApiOperation(value = "Execute Agent Application Event (createAgentAppEvent)",
            notes = "Creates an event for the specified agent application. The action type determines the operation " +
                    "(UPDATE, DELETE, RESTART, UPGRADE, ROLLBACK). Step inputs can be provided for actions that require them."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/agent/app/{agentApplicationId}/event")
    @ResponseStatus(value = HttpStatus.OK)
    public void createAgentAppEvent(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON value representing the event request.")
            @RequestBody AgentAppEventRequest request) throws Exception {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentAppEventActionType actionType = request.getActionType();
        if (actionType == null) {
            throw new ThingsboardException("Action type must not be null", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (actionType == AgentAppEventActionType.INSTALL) {
            throw new ThingsboardException("Use the install endpoint for INSTALL events", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.WRITE);
        TenantId tenantId = getCurrentUser().getTenantId();
        tbAgentApplicationService.execActionEvent(tenantId, agentApplicationId, request);
    }

    @ApiOperation(value = "Cancel Agent Application Event (cancelAgentAppEvent)",
            notes = "Force-cancels an in-flight or pending agent application event, marking it as ERROR. "
                    + "Cannot cancel events that are already in a terminal state (FINISHED or ERROR)."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/agent/app/{agentApplicationId}/event/{agentAppEventId}/cancel")
    @ResponseStatus(value = HttpStatus.OK)
    public void cancelAgentAppEvent(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @Parameter(description = AGENT_APP_EVENT_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_EVENT_ID) String strAgentAppEventId) throws Exception {
        checkParameter(AGENT_APP_EVENT_ID, strAgentAppEventId);
        AgentAppEventId agentAppEventId = new AgentAppEventId(toUUID(strAgentAppEventId));
        TenantId tenantId = getCurrentUser().getTenantId();
        tbAgentApplicationService.cancelEvent(tenantId, agentAppEventId);
    }

    @ApiOperation(value = "Get Agent Application Units (getAgentAppUnits)",
            notes = "Returns a page of units (containers, volumes, networks) for the specified agent application. "
                    + "Live per-unit data such as `image` and `state` lives in server-scope attributes on each unit "
                    + "and should be fetched separately via the standard telemetry/attributes API."
                    + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/agent/app/{agentApplicationId}/units", params = {"pageSize", "page"})
    @ResponseBody
    public PageData<AgentAppUnit> getAgentAppUnits(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Case-insensitive substring match against the unit identifier")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = "Optional unit type filter (CONTAINER, VOLUME, NETWORK)")
            @RequestParam(required = false) AgentAppUnitType type,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "identifier", "type"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        AgentAppUnitFilter filter = AgentAppUnitFilter.builder()
                .tenantId(tenantId)
                .applicationId(agentApplicationId)
                .type(type)
                .build();
        return agentAppUnitService.findByFilter(filter, pageLink);
    }

    @ApiOperation(value = "Get Agent Application Events (getAgentAppEvents)",
            notes = "Returns a page of events for the specified agent application. "
                    + PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/agent/app/{agentApplicationId}/events", params = {"pageSize", "page"})
    @ResponseBody
    public PageData<AgentAppEvent> getAgentAppEvents(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "Optional String value reserved for future event filtering")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = "Optional action type filter")
            @RequestParam(required = false) AgentAppEventActionType actionType,
            @Parameter(description = "Optional status filter")
            @RequestParam(required = false) AgentAppEventStatus status,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "updatedTime"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId agentApplicationId = new AgentApplicationId(toUUID(strAgentAppId));
        checkAgentAppId(agentApplicationId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        AgentAppEventFilter filter = AgentAppEventFilter.builder()
                .tenantId(tenantId)
                .applicationId(agentApplicationId)
                .actionType(actionType)
                .status(status)
                .build();
        return agentAppEventService.findByFilter(filter, pageLink);
    }

    @ApiOperation(value = "Merge template into application for preview (mergeForPreview)",
            notes = "Merges the specified template into an agent application for preview purposes. " +
                    "If no application is provided in the body, a new one is created from the template. " +
                    "The compose type determines which compose configuration variant from the template is used. " +
                    "Optionally provide relatedEntityId to auto-fill entity credentials (Edge routing key/secret or Gateway access token) into the compose."
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

        if (application == null) {
            application = AgentApplication.fromTemplate(template);
        }
        application.setTenantId(tenantId);
        return tbAgentApplicationService.mergeForPreview(tenantId, application, template, composeType);
    }

    @ApiOperation(value = "Detach Application from Profile (detachFromProfile)",
            notes = "Detaches the application from its profile. Resolves effective config (profile + credentials) " +
                    "and writes it into the app's config. The app becomes standalone and editable."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/{agentApplicationId}/detach")
    @ResponseBody
    public AgentApplication detachFromProfile(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId) throws Exception {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        AgentApplicationId appId = new AgentApplicationId(toUUID(strAgentAppId));
        AgentApplication app = checkAgentAppId(appId, Operation.WRITE);
        TenantId tenantId = getCurrentUser().getTenantId();

        if (app.getApplicationProfileId() == null) {
            throw new ThingsboardException("Application is not attached to any profile", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        AgentAppProfile profile = agentAppProfileService.findProfileById(tenantId, app.getApplicationProfileId());
        if (profile != null && profile.getConfig() != null) {
            app.setConfig(profile.getConfig());
        }
        app.setApplicationProfileId(null);
        return tbAgentApplicationService.update(app, getCurrentUser());
    }

    @ApiOperation(value = "Attach Application to Profile (attachToProfile)",
            notes = "Re-attaches the application to a profile. Only succeeds if the app's templateId matches the profile's templateId. " +
                    "After attachment, the app's config is cleared (profile is the source)."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/agent/app/{agentApplicationId}/attach/{profileId}")
    @ResponseBody
    public AgentApplication attachToProfile(
            @Parameter(description = AGENT_APP_ID_PARAM_DESCRIPTION)
            @PathVariable(AGENT_APP_ID) String strAgentAppId,
            @PathVariable("profileId") String strProfileId) throws Exception {
        checkParameter(AGENT_APP_ID, strAgentAppId);
        checkParameter("profileId", strProfileId);

        AgentApplicationId appId = new AgentApplicationId(toUUID(strAgentAppId));
        AgentApplication app = checkAgentAppId(appId, Operation.WRITE);
        AgentAppProfileId profileId = new AgentAppProfileId(toUUID(strProfileId));
        AgentAppProfile profile = checkAgentAppProfileId(profileId, Operation.READ);

        if (!app.getTemplateId().equals(profile.getTemplateId())) {
            throw new ThingsboardException("Cannot attach: application templateId does not match profile templateId",
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }

        app.setApplicationProfileId(profileId);
        app.setConfig(null);
        return tbAgentApplicationService.update(app, getCurrentUser());
    }
}

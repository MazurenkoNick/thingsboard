/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.edqs.EdqsState;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.AvailableEntityKeys;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilter;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.query.EntityQueryService;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_DATA_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_COUNT_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_DATA_QUERY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class EntityQueryController extends BaseController {

    private final EntityQueryService entityQueryService;
    private final EdqsService edqsService;

    private static final int MAX_PAGE_SIZE = 100;

    @ApiOperation(value = "Count Entities by Query", notes = ENTITY_COUNT_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/entitiesQuery/count")
    public long countEntitiesByQuery(
            @Parameter(description = "A JSON value representing the entity count query. See API call notes above for more details.")
            @RequestBody EntityCountQuery query) throws ThingsboardException {
        checkNotNull(query);
        resolveQuery(query);
        return entityQueryService.countEntitiesByQuery(getCurrentUser(), query);
    }

    @ApiOperation(value = "Find Entity Data by Query", notes = ENTITY_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/entitiesQuery/find")
    public PageData<EntityData> findEntityDataByQuery(
            @Parameter(description = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        resolveQuery(query);
        return entityQueryService.findEntityDataByQuery(getCurrentUser(), query);
    }

    @ApiOperation(value = "Find Alarms by Query", notes = ALARM_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/alarmsQuery/find")
    public PageData<AlarmData> findAlarmDataByQuery(
            @Parameter(description = "A JSON value representing the alarm data query. See API call notes above for more details.")
            @RequestBody AlarmDataQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getPageLink());
        accessControlService.checkPermission(getCurrentUser(), Resource.ALARM, Operation.READ);
        UserId assigneeId = query.getPageLink().getAssigneeId();
        if (assigneeId != null) {
            checkUserId(assigneeId, Operation.READ);
        }
        resolveQuery(query);
        return entityQueryService.findAlarmDataByQuery(getCurrentUser(), query);
    }

    @ApiOperation(value = "Count Alarms by Query (countAlarmsByQuery)", notes = "Returns the number of alarms that match the query definition.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/alarmsQuery/count")
    public long countAlarmsByQuery(@Parameter(description = "A JSON value representing the alarm count query.")
                                   @RequestBody AlarmCountQuery query) throws ThingsboardException {
        checkNotNull(query);
        UserId assigneeId = query.getAssigneeId();
        if (assigneeId != null) {
            checkUserId(assigneeId, Operation.READ);
        }
        resolveQuery(query);
        return entityQueryService.countAlarmsByQuery(getCurrentUser(), query);
    }

    @ApiOperation(
            value = "Find Available Entity Keys by Query",
            notes = """
                    Returns unique time series and/or attribute key names from entities matching the query.\n
                    Executes the Entity Data Query to find up to 100 entities, then fetches and aggregates all distinct key names.\n
                    Primarily used for UI features like autocomplete suggestions.""" + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/entitiesQuery/find/keys")
    public DeferredResult<AvailableEntityKeys> findAvailableEntityKeysByQuery(
            @Parameter(description = "Entity data query to find entities. Page size is capped at 100.")
            @RequestBody EntityDataQuery query,

            // fixme: combination of timeseries = false and attributes = false is allowed, but always results in empty response, therefore does not make any sense
            //        such combinations should NOT be allowed, but changing this will break clients

            @Parameter(description = """
                    When true, includes unique time series key names in the response.
                    When false, the 'timeseries' list will be empty.""")
            @RequestParam("timeseries") boolean includeTimeseries,

            @Parameter(description = """
                    When true, includes unique attribute key names in the response.
                    When false, the 'attribute' list will be empty. Use 'scope' parameter to filter by attribute scope.""")
            @RequestParam("attributes") boolean includeAttributes,

            @Parameter(description = """
                    Filters attribute keys by scope. Only applies when 'attributes' is true.
                    If not specified, returns attribute keys from all scopes.""",
                    schema = @Schema(allowableValues = {"SERVER_SCOPE", "SHARED_SCOPE", "CLIENT_SCOPE"}))
            @RequestParam(value = "scope", required = false) AttributeScope scope
    ) throws ThingsboardException {
        resolveQuery(query);
        EntityDataPageLink pageLink = query.getPageLink();
        if (pageLink.getPageSize() > MAX_PAGE_SIZE) {
            pageLink.setPageSize(MAX_PAGE_SIZE);
        }
        return wrapFuture(entityQueryService.getKeysByQuery(getCurrentUser(), getTenantId(), query, includeTimeseries, includeAttributes, scope));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping("/edqs/system/request")
    public void processSystemEdqsRequest(@RequestBody ToCoreEdqsRequest request) {
        edqsService.processSystemRequest(request);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @GetMapping("/edqs/state")
    public EdqsState getEdqsState() {
        return edqsService.getState();
    }

    private void resolveQuery(EntityCountQuery query) throws ThingsboardException {
        if (query.getEntityFilter() != null) {
            var user = getCurrentUser();
            EntityFilter.resolveEntityFilter(query.getEntityFilter(), getTenantId(), user.getId(), user.getOwnerId());
        }
    }

}

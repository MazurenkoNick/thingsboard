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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.template.TemplateMergeRequest;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AgentAppTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.agent.TbAgentApplicationService;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.UUID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentApplicationController extends BaseController {

    private final TbAgentApplicationService tbAgentApplicationService;

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping("/agent/app/merge/{agentAppTemplateId}/preview")
    @ResponseBody
    public AgentApplication mergeForPreview(
            @PathVariable UUID agentAppTemplateId,
            @Valid @RequestBody TemplateMergeRequest req) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();

        AgentAppTemplateId templateId = new AgentAppTemplateId(agentAppTemplateId);
        AgentAppTemplate agentAppTemplate = checkEntityId(templateId, agentAppTemplateService::findById, Operation.READ);

        if (agentAppTemplate.getAppType() == AgentApplicationType.GENERIC) {
            throw new ThingsboardException("Can't merge agent application with the template of the generic type", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return tbAgentApplicationService.mergeForPreview(tenantId, req.getAgentApplication(), agentAppTemplate, req);
    }
}

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
package org.thingsboard.server.service.agent.action;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.dao.agent.config.ProfileConfigResolver;

@Component
@TbCoreComponent
@RequiredArgsConstructor
public class UpdateActionHandler implements AgentAppActionHandler {

    private final ProfileConfigResolver profileConfigResolver;

    @Override
    public AgentAppEventActionType getActionType() {
        return AgentAppEventActionType.UPDATE;
    }

    @Override
    public void handle(AgentApplication application, AgentAppEventRequest request, AgentAppActionContext ctx) {
        AgentApplication incoming = request.getApplication();
        if (application.getApplicationProfileId() != null) {
            // Profile-managed: by default re-resolve compose from the profile so any profile/template drift is picked up.
            // When the caller asks to skip the refetch (credentials-only update), keep the existing compose and
            // apply just the incoming creds via setConfig.
            if (request.isSkipProfileRefetch()) {
                if (incoming != null && incoming.getConfig() != null) {
                    application.setConfig(incoming.getConfig());
                }
            } else {
                profileConfigResolver.resolve(ctx.getTenantId(), application);
            }
            if (incoming != null && incoming.getName() != null && !incoming.getName().isBlank()) {
                application.setName(incoming.getName());
            }
            return;
        }
        if (incoming != null) {
            if (incoming.getName() != null && !incoming.getName().isBlank()) {
                application.setName(incoming.getName());
            }
            if (incoming.getConfig() != null) {
                application.setConfig(incoming.getConfig());
            }
        }
    }
}

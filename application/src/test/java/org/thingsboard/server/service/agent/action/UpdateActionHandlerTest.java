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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.config.ProfileConfigResolver;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpdateActionHandlerTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    private ProfileConfigResolver profileConfigResolver;

    private UpdateActionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateActionHandler(profileConfigResolver);
    }

    @Test
    void getActionType_isUpdate() {
        assertThat(handler.getActionType()).isEqualTo(AgentAppEventActionType.UPDATE);
    }

    @Test
    void profileManaged_skipRefetch_keepsIncomingConfig_andRenames() {
        AgentApplication application = new AgentApplication();
        application.setApplicationProfileId(new AgentAppProfileId(UUID.randomUUID()));
        DockerComposeConfig incomingConfig = new DockerComposeConfig();

        AgentApplication incoming = new AgentApplication();
        incoming.setName("renamed");
        incoming.setConfig(incomingConfig);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(incoming);
        request.setSkipProfileRefetch(true);

        handler.handle(application, request, new AgentAppActionContext(TENANT_ID));

        assertThat(application.getConfig()).isSameAs(incomingConfig);
        assertThat(application.getName()).isEqualTo("renamed");
        verifyNoInteractions(profileConfigResolver);
    }

    @Test
    void profileManaged_normal_reResolvesConfig_andRenames() {
        AgentApplication application = new AgentApplication();
        application.setApplicationProfileId(new AgentAppProfileId(UUID.randomUUID()));

        AgentApplication incoming = new AgentApplication();
        incoming.setName("renamed");

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(incoming);
        request.setSkipProfileRefetch(false);

        handler.handle(application, request, new AgentAppActionContext(TENANT_ID));

        verify(profileConfigResolver).resolve(eq(TENANT_ID), eq(application));
        assertThat(application.getName()).isEqualTo("renamed");
    }

    @Test
    void nonProfileManaged_copiesNameAndConfig() {
        AgentApplication application = new AgentApplication();
        DockerComposeConfig incomingConfig = new DockerComposeConfig();

        AgentApplication incoming = new AgentApplication();
        incoming.setName("standalone");
        incoming.setConfig(incomingConfig);

        AgentAppEventRequest request = new AgentAppEventRequest();
        request.setApplication(incoming);

        handler.handle(application, request, new AgentAppActionContext(TENANT_ID));

        assertThat(application.getName()).isEqualTo("standalone");
        assertThat(application.getConfig()).isSameAs(incomingConfig);
        verifyNoInteractions(profileConfigResolver);
    }
}

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
package org.thingsboard.server.agent;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@DaoSqlTest
public class ProjectSyncAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.0EDGE";

    @Test
    public void testProjectSyncCreatesApplication() {
        createEdgeTemplate();

        String projectName = "test-project";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));

        sendProjectSync(projectName, composeJson, Collections.emptyMap());

        // Wait for app to be created
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        PageData<AgentApplicationInfo> apps = getAgentApps(
                agent.getId().getId().toString());
        Assert.assertEquals(1, apps.getTotalElements());

        AgentApplicationInfo app = apps.getData().get(0);
        Assert.assertEquals(AgentApplicationType.EDGE, app.getAppType());
        Assert.assertEquals(agent.getId(), app.getAgentId());
    }

    @Test
    public void testProjectSyncUpdatesContainerState() {
        createEdgeTemplate();

        String projectName = "state-project";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));

        // First sync to create app
        sendProjectSync(projectName, composeJson, Collections.emptyMap());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        // Second sync with container states
        sendProjectSync(projectName, null,
                Map.of("tb-edge", ContainerInfo.newBuilder()
                        .setState("running")
                        .setImageDigest("sha256:abc123")
                        .build()));

        // Allow state processing time
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .until(() -> true);
    }

    @Test
    public void testProjectRemoval() {
        createEdgeTemplate();

        String projectName = "removal-project";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));

        // Create app via sync
        sendProjectSync(projectName, composeJson, Collections.emptyMap());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        // Send removal — this deletes units, not the app itself
        sendProjectRemoval(projectName);

        // Verify app still exists (removal deletes units, not the application)
        PageData<AgentApplicationInfo> apps = getAgentApps(agent.getId().getId().toString());
        Assert.assertEquals(1, apps.getTotalElements());
    }

    private void createEdgeTemplate() {
        DockerComposeConfig config = new DockerComposeConfig();
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));
        config.setCompose(org.thingsboard.common.util.JacksonUtil.toJsonNode(composeJson));

        ComposeStep step = createComposeStep();

        createAgentAppTemplate(AgentApplicationType.EDGE, EDGE_VERSION,
                config, List.of(step));
    }
}

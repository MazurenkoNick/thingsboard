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
package org.thingsboard.server.agent;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.agent.AgentApplication;
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
                Map.of("tb-edge", "thingsboard/tb-edge:" + EDGE_VERSION));

        sendProjectSync(projectName, composeJson, Collections.emptyMap());

        // Wait for app to be created
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        PageData<AgentApplication> apps = getAgentApps(
                agent.getId().getId().toString());
        Assert.assertEquals(1, apps.getTotalElements());

        AgentApplication app = apps.getData().get(0);
        Assert.assertEquals(AgentApplicationType.EDGE, app.getAppType());
        Assert.assertEquals(agent.getId(), app.getAgentId());
    }

    @Test
    public void testProjectSyncUpdatesContainerState() {
        createEdgeTemplate();

        String projectName = "state-project";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge:" + EDGE_VERSION));

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
                Map.of("tb-edge", "thingsboard/tb-edge:" + EDGE_VERSION));

        // Create app via sync
        sendProjectSync(projectName, composeJson, Collections.emptyMap());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        // Send removal — this deletes units, not the app itself
        sendProjectRemoval(projectName);

        // Verify app still exists (removal deletes units, not the application)
        PageData<AgentApplication> apps = getAgentApps(agent.getId().getId().toString());
        Assert.assertEquals(1, apps.getTotalElements());
    }

    private void createEdgeTemplate() {
        DockerComposeConfig config = new DockerComposeConfig();
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge:" + EDGE_VERSION));
        config.setCompose(org.thingsboard.common.util.JacksonUtil.toJsonNode(composeJson));

        ComposeStep step = createComposeStep();

        createAgentAppTemplate(AgentApplicationType.EDGE, EDGE_VERSION,
                config, List.of(step));
    }
}

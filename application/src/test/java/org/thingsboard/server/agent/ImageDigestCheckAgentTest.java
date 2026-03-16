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
import org.junit.Test;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@DaoSqlTest
public class ImageDigestCheckAgentTest extends AbstractAgentTest {

    private static final String EDGE_VERSION = "4.3.1EDGE";
    private static final String TEMPLATE_DIGEST = "sha256:abc123template";
    private static final String DIFFERENT_DIGEST = "sha256:xyz789different";

    @Test
    public void testImageDigestMismatchSetsPullRequired() {
        createEdgeTemplateWithDigest(TEMPLATE_DIGEST);

        String projectName = "digest-mismatch";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge:" + EDGE_VERSION));

        // Send sync with both compose and container states with mismatching digest
        sendProjectSync(projectName, composeJson,
                Map.of("tb-edge", ContainerInfo.newBuilder()
                        .setState("running")
                        .setImageDigest(DIFFERENT_DIGEST)
                        .build()));

        // Wait for app to be created
        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        PageData<AgentApplication> apps = getAgentApps(agent.getId().getId().toString());
        AgentApplication app = apps.getData().get(0);

        // Verify pullRequired=true
        verifyAttribute(app.getId(), "pullRequired", true);
    }

    @Test
    public void testImageDigestMatchClearsPullRequired() {
        createEdgeTemplateWithDigest(TEMPLATE_DIGEST);

        String projectName = "digest-match";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge:" + EDGE_VERSION));

        // Send sync with both compose and container states with matching digest
        sendProjectSync(projectName, composeJson,
                Map.of("tb-edge", ContainerInfo.newBuilder()
                        .setState("running")
                        .setImageDigest(TEMPLATE_DIGEST)
                        .build()));

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        PageData<AgentApplication> apps = getAgentApps(agent.getId().getId().toString());
        AgentApplication app = apps.getData().get(0);

        // Verify pullRequired=false
        verifyAttribute(app.getId(), "pullRequired", false);
    }

    @Test
    public void testImageDigestCheckFallbackToAppConfig() {
        createEdgeTemplateWithDigest(TEMPLATE_DIGEST);

        String projectName = "digest-fallback";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge:" + EDGE_VERSION));

        // First sync: create app with composeJson (no container states)
        sendProjectSync(projectName, composeJson, Map.of());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        PageData<AgentApplication> apps = getAgentApps(agent.getId().getId().toString());
        AgentApplication app = apps.getData().get(0);

        // Second sync: container states only (no composeJson) — forces fallback to app.getConfig().getCompose()
        sendProjectSync(projectName, null,
                Map.of("tb-edge", ContainerInfo.newBuilder()
                        .setState("running")
                        .setImageDigest(DIFFERENT_DIGEST)
                        .build()));

        // Verify pullRequired=true via the fallback path
        verifyAttribute(app.getId(), "pullRequired", true);
    }

    private AgentAppTemplate createEdgeTemplateWithDigest(String imageDigest) {
        DockerComposeConfig config = new DockerComposeConfig();
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge:" + EDGE_VERSION));
        config.setCompose(org.thingsboard.common.util.JacksonUtil.toJsonNode(composeJson));
        config.setImageDigest(imageDigest);

        ComposeStep step = createComposeStep();

        return createAgentAppTemplate(AgentApplicationType.EDGE, EDGE_VERSION,
                config, List.of(step));
    }
}

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
import org.junit.Test;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
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
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));

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

        PageData<AgentApplicationInfo> apps = getAgentApps(agent.getId().getId().toString());
        AgentApplicationInfo app = apps.getData().get(0);

        // Verify pullRequired=true
        verifyAttribute(app.getId(), "pullRequired", true);
    }

    @Test
    public void testImageDigestMatchClearsPullRequired() {
        createEdgeTemplateWithDigest(TEMPLATE_DIGEST);

        String projectName = "digest-match";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));

        // Send sync with both compose and container states with matching digest
        sendProjectSync(projectName, composeJson,
                Map.of("tb-edge", ContainerInfo.newBuilder()
                        .setState("running")
                        .setImageDigest(TEMPLATE_DIGEST)
                        .build()));

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        PageData<AgentApplicationInfo> apps = getAgentApps(agent.getId().getId().toString());
        AgentApplicationInfo app = apps.getData().get(0);

        // Verify pullRequired=false
        verifyAttribute(app.getId(), "pullRequired", false);
    }

    @Test
    public void testImageDigestCheckFallbackToAppConfig() {
        createEdgeTemplateWithDigest(TEMPLATE_DIGEST);

        String projectName = "digest-fallback";
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));

        // First sync: create app with composeJson (no container states)
        sendProjectSync(projectName, composeJson, Map.of());

        Awaitility.await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getAgentApps(agent.getId().getId().toString()).getTotalElements() > 0);

        PageData<AgentApplicationInfo> apps = getAgentApps(agent.getId().getId().toString());
        AgentApplicationInfo app = apps.getData().get(0);

        // Second sync: container states only (no composeJson) — forces fallback to app.getConfig().getCompose()
        sendProjectSync(projectName, null,
                Map.of("tb-edge", ContainerInfo.newBuilder()
                        .setState("running")
                        .setImageDigest(DIFFERENT_DIGEST)
                        .build()));

        // Verify pullRequired=true via the fallback path
        verifyAttribute(app.getId(), "pullRequired", true);
    }

    private AgentAppTemplate createEdgeTemplateWithDigest(String digest) {
        DockerComposeConfig config = new DockerComposeConfig();
        String composeJson = constructComposeJson(
                Map.of("tb-edge", "thingsboard/tb-edge-pe:" + EDGE_VERSION));
        config.setCompose(org.thingsboard.common.util.JacksonUtil.toJsonNode(composeJson));

        ComposeStep step = createComposeStep();

        return createAgentAppTemplate(AgentApplicationType.EDGE, EDGE_VERSION,
                config, List.of(step), digest);
    }
}

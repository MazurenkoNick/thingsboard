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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppTemplateService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;

@DaoSqlTest
public class AgentAppProfileServiceTest extends AbstractServiceTest {

    @Autowired
    AgentAppProfileService profileService;
    @Autowired
    AgentAppTemplateService templateService;

    @Test
    public void testSaveAndFind() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createEdgeProfile("Test Profile", template);

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        AgentAppProfile found = profileService.findProfileById(tenantId, saved.getId());
        Assert.assertNotNull(found);
        Assert.assertEquals(saved.getId(), found.getId());
        Assert.assertEquals("Test Profile", found.getName());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveEdgeProfile_withValidCredentialKeys() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createEdgeProfile("Valid Edge Profile", template);

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveEdgeProfile_missingCredentialKeys_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName("Bad Edge Profile");
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setTemplateId(template.getId());

        // Config with edge service but missing credential env vars
        DockerComposeConfig config = new DockerComposeConfig();
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", JacksonUtil.newObjectNode());
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-edge", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        config.setCompose(compose);
        profile.setConfig(config);

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testSaveGatewayProfile_accessToken_valid() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW AccessToken", template, "accessToken",
                env -> env.put("TB_GW_ACCESS_TOKEN", "placeholder"));

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveGatewayProfile_accessToken_missingToken_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW Bad AccessToken", template, "accessToken",
                env -> {});

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testSaveGatewayProfile_usernamePassword_valid() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW UserPass", template, "usernamePassword",
                env -> {
                    env.put("TB_GW_CLIENT_ID", "placeholder");
                    env.put("TB_GW_USERNAME", "placeholder");
                    env.put("TB_GW_PASSWORD", "placeholder");
                });

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveGatewayProfile_usernamePassword_missingKeys_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW Bad UserPass", template, "usernamePassword",
                env -> env.put("TB_GW_CLIENT_ID", "placeholder"));

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testSaveGatewayProfile_unsupportedSecurityType_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createGatewayProfile("GW Bad Type", template, "x509",
                env -> {});

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testSaveGenericProfile_noCredentialValidation() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName("Generic Profile");
        profile.setAppType(AgentApplicationType.GENERIC);
        profile.setTemplateId(template.getId());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(JacksonUtil.newObjectNode().put("version", "3"));
        profile.setConfig(config);

        AgentAppProfile saved = profileService.saveProfile(profile);
        Assert.assertNotNull(saved.getId());

        profileService.deleteProfile(tenantId, saved.getId());
    }

    @Test
    public void testSaveProfile_nullConfig_throws() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName("No Config");
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setTemplateId(template.getId());

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(profile));
    }

    @Test
    public void testUpdateProfile_validationStillRuns() {
        AgentAppTemplate template = createTemplate();
        AgentAppProfile profile = createEdgeProfile("Update Test", template);
        AgentAppProfile saved = profileService.saveProfile(profile);

        // Now update with invalid config (missing keys)
        DockerComposeConfig badConfig = new DockerComposeConfig();
        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", JacksonUtil.newObjectNode());
        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-edge", service);
        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        badConfig.setCompose(compose);
        saved.setConfig(badConfig);

        Assertions.assertThrows(DataValidationException.class, () ->
                profileService.saveProfile(saved));

        profileService.deleteProfile(tenantId, saved.getId());
    }

    // ==================== Helpers ====================

    private AgentAppTemplate createTemplate() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.GENERIC);
        template.setCurrentVersion("1.0.0");
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("start");
        template.setStartSteps(List.of(step));
        return templateService.save(TenantId.SYS_TENANT_ID, template);
    }

    private AgentAppProfile createEdgeProfile(String name, AgentAppTemplate template) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setTemplateId(template.getId());

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createEdgeCompose());
        profile.setConfig(config);
        return profile;
    }

    private ObjectNode createEdgeCompose() {
        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("CLOUD_ROUTING_KEY", "placeholder");
        env.put("CLOUD_ROUTING_SECRET", "placeholder");
        env.put("CLOUD_RPC_HOST", "localhost");

        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);

        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-edge", service);

        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);
        return compose;
    }

    private AgentAppProfile createGatewayProfile(String name, AgentAppTemplate template,
                                                   String securityType, java.util.function.Consumer<ObjectNode> envCustomizer) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setAppType(AgentApplicationType.GATEWAY);
        profile.setTemplateId(template.getId());

        ObjectNode env = JacksonUtil.newObjectNode();
        env.put("TB_GW_SECURITY_TYPE", securityType);
        envCustomizer.accept(env);

        ObjectNode service = JacksonUtil.newObjectNode();
        service.put("image", "thingsboard/tb-gateway:3.8.0");
        service.set("environment", env);

        ObjectNode services = JacksonUtil.newObjectNode();
        services.set("tb-gateway", service);

        ObjectNode compose = JacksonUtil.newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        profile.setConfig(config);
        return profile;
    }
}

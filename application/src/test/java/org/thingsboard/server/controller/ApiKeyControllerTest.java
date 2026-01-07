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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class ApiKeyControllerTest extends AbstractControllerTest {

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveApiKey() throws Exception {
        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("New API key description", true);

        doPost("/api/apiKey", apiKeyInfo, ApiKey.class);

        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(1, pageData.getData().size());

        ApiKeyInfo savedApiKey = pageData.getData().get(0);
        Assert.assertNotNull(savedApiKey);
        Assert.assertEquals(apiKeyInfo.getDescription(), savedApiKey.getDescription());
        Assert.assertEquals(apiKeyInfo.isEnabled(), savedApiKey.isEnabled());
        Assert.assertEquals(tenantId, savedApiKey.getTenantId());
        Assert.assertEquals(tenantAdminUser.getId(), savedApiKey.getUserId());

        doDelete("/api/apiKey/" + savedApiKey.getId()).andExpect(status().isOk());
    }

    @Test
    public void testCreateInternalApiKey_thenFailed() throws Exception {
        loginSysAdmin();

        Map<Resource, Set<Operation>> permissions = new HashMap<>();
        permissions.put(Resource.DEVICE, Set.of(Operation.READ, Operation.WRITE));
        permissions.put(Resource.DASHBOARD, Set.of(Operation.READ));
        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.SYS_ADMIN, permissions);
        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);

        ApiKeyInfo apiKeyInfo = constructApiKeyInfo(authorityPermissionsInfo);
        doPost("/api/apiKey", apiKeyInfo)
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSaveInternalApiKeyAsTenantAdmin_shouldFail() throws Exception {
        Map<Resource, Set<Operation>> permissions = new HashMap<>();
        permissions.put(Resource.DEVICE, Set.of(Operation.READ));
        Map<Authority, Map<Resource, Set<Operation>>> operationsByResource = new HashMap<>();
        operationsByResource.put(Authority.TENANT_ADMIN, permissions);
        AuthorityPermissionsInfo authorityPermissionsInfo = new AuthorityPermissionsInfo();
        authorityPermissionsInfo.setOperationsByResource(operationsByResource);

        ApiKeyInfo apiKeyInfo = constructApiKeyInfo(authorityPermissionsInfo);
        doPost("/api/apiKey", apiKeyInfo)
                .andExpect(status().isForbidden());
    }

    @Test
    public void tesFindUserApiKeys() throws Exception {
        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertTrue(pageData.getData().isEmpty());

        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("Test API key description", true);
        int expectedSize = 10;
        for (int i = 0; i < expectedSize; i++) {
            doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        }

        PageData<ApiKeyInfo> pageData2 = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(expectedSize, pageData2.getData().size());

        pageData2.getData().forEach(apiKey -> {
            try {
                doDelete("/api/apiKey/" + apiKey.getId()).andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testUpdateApiKeyDescription() throws Exception {
        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("Test API key description", true);
        doPost("/api/apiKey", apiKeyInfo, ApiKey.class);

        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(1, pageData.getData().size());

        ApiKeyInfo savedApiKey = pageData.getData().get(0);

        String newDescription = "Updated API Key Description";

        ApiKeyInfo updatedApiKeyInfo = doPut("/api/apiKey/" + savedApiKey.getId().getId() + "/description", newDescription, ApiKeyInfo.class);
        Assert.assertNotNull(updatedApiKeyInfo);
        Assert.assertEquals(newDescription, updatedApiKeyInfo.getDescription());

        doDelete("/api/apiKey/" + savedApiKey.getId()).andExpect(status().isOk());
    }

    @Test
    public void testEnableApiKey() throws Exception {
        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("Test API key description", true);
        doPost("/api/apiKey", apiKeyInfo, ApiKey.class);

        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(1, pageData.getData().size());

        ApiKeyInfo savedApiKey = pageData.getData().get(0);

        ApiKeyInfo disabledApiKeyInfo = doPut("/api/apiKey/" + savedApiKey.getId().getId() + "/enabled/false", Boolean.FALSE, ApiKeyInfo.class);
        Assert.assertNotNull(disabledApiKeyInfo);
        Assert.assertFalse(disabledApiKeyInfo.isEnabled());

        ApiKeyInfo enabledApiKeyInfo = doPut("/api/apiKey/" + savedApiKey.getId().getId() + "/enabled/true", Boolean.TRUE, ApiKeyInfo.class);
        Assert.assertNotNull(enabledApiKeyInfo);
        Assert.assertTrue(enabledApiKeyInfo.isEnabled());

        doDelete("/api/apiKey/" + savedApiKey.getId()).andExpect(status().isOk());
    }

    @Test
    public void testDeleteApiKey() throws Exception {
        doDelete("/api/apiKey/" + UUID.randomUUID()).andExpect(status().isNotFound());

        ApiKeyInfo apiKeyInfo = constructApiKeyInfo("Test API key description", false);
        doPost("/api/apiKey", apiKeyInfo, ApiKey.class);

        PageData<ApiKeyInfo> pageData = doGetTypedWithPageLink("/api/apiKeys/" + tenantAdminUserId + "?", new TypeReference<>() {}, new PageLink(10, 0));
        Assert.assertEquals(1, pageData.getData().size());
        ApiKeyInfo savedApiKey = pageData.getData().get(0);

        doDelete("/api/apiKey/" + savedApiKey.getId().getId()).andExpect(status().isOk());
    }

    private ApiKeyInfo constructApiKeyInfo(String description, boolean enabled) {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription(description);
        apiKeyInfo.setEnabled(enabled);
        apiKeyInfo.setUserId(tenantAdminUserId);
        return apiKeyInfo;
    }

    private ApiKeyInfo constructApiKeyInfo(AuthorityPermissionsInfo info) {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("API key description for internal API key");
        apiKeyInfo.setUserId(currentUserId);
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setPermissions(info);
        apiKeyInfo.setInternal(true);
        return apiKeyInfo;
    }

}

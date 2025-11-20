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
package org.thingsboard.server.service.security.auth.pat;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class ApiKeyAuthenticationProviderTest extends AbstractControllerTest {

    ApiKey savedApiKey;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();

        ApiKeyInfo apiKeyInfo = constructApiKeyInfo();
        savedApiKey = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(savedApiKey.getValue());
    }

    @After
    public void cleanUp() throws Exception {
        resetApiKey();
        doDelete("/api/apiKey/" + savedApiKey.getId()).andExpect(status().isOk());
    }

    @Test
    public void testSaveEdgeWithApiKey() throws Exception {
        Edge edge = constructEdge("My edge", "default");

        Mockito.reset(tbClusterService, auditLogService);

        Edge savedEdge = doPostWithApiKey("/api/edge", edge, Edge.class);

        Assert.assertNotNull(savedEdge);
        Assert.assertNotNull(savedEdge.getId());
        Assert.assertTrue(savedEdge.getCreatedTime() > 0);
        Assert.assertEquals(tenantId, savedEdge.getTenantId());
        Assert.assertNotNull(savedEdge.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedEdge.getCustomerId().getId());
        Assert.assertEquals(edge.getName(), savedEdge.getName());

        testNotifyEdgeStateChangeEventManyTimeMsgToEdgeServiceNever(savedEdge, savedEdge.getId(), savedEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.ADDED, 2);

        savedEdge.setName("My new edge");
        doPostWithApiKey("/api/edge", savedEdge, Edge.class);

        Edge foundEdge = doGetWithApiKey("/api/edge/" + savedEdge.getId().getId().toString(), Edge.class);
        Assert.assertEquals(foundEdge.getName(), savedEdge.getName());

        testNotifyEdgeStateChangeEventManyTimeMsgToEdgeServiceNever(foundEdge, foundEdge.getId(), foundEdge.getId(),
                tenantId, tenantAdminUser.getCustomerId(), tenantAdminUser.getId(), tenantAdminUser.getEmail(),
                ActionType.UPDATED, 1);

        doDeleteWithApiKey("/api/edge/" + savedEdge.getId().getId().toString())
                .andExpect(status().isOk());
    }

//    @Test
//    public void testSaveDomainWithApiKey_whenSysAdminWithoutDomainWritePermission_thenForbidden() throws Exception {
//        loginSysAdmin();
//
//        // Create API Key with only READ permission for SECRET
//        Map<Resource, Set<Operation>> permissions = new HashMap<>();
//        permissions.put(Resource.DOMAIN, Set.of(Operation.READ));
//
//        ApiKeyInfo apiKeyInfo = constructSystemApiKeyInfoWithPermissions(currentUserId, permissions);
//        String token = doPost("/api/apiKey", apiKeyInfo, ApiKey.class).getValue();
//        setApiKey(token);
//
//        Domain domain = constructDomain(TenantId.SYS_TENANT_ID, "localhost", true, true);
//
//        doPostWithApiKey("/api/domain", domain)
//                .andExpect(status().isForbidden());
//
//        doDelete("/api/apiKey/" + apiKeyInfo.getId().getId().toString(), token).andExpect(status().isOk());
//
//        loginTenantAdmin();
//    }
//
//    @Test
//    public void testSaveDomainWithApiKey_whenSysAdminWithDomainWritePermission_thenOk() throws Exception {
//        loginSysAdmin();
//
//        // Create API Key with ALL permission for SECRET
//        Map<Resource, Set<Operation>> permissions = new HashMap<>();
//        permissions.put(Resource.DOMAIN, Set.of(Operation.ALL));
//
//        ApiKeyInfo apiKeyInfo = constructSystemApiKeyInfoWithPermissions(currentUserId, permissions);
//        String token = doPost("/api/apiKey", apiKeyInfo, ApiKey.class).getValue();
//        setApiKey(token);
//
//        Domain domain = constructDomain(TenantId.SYS_TENANT_ID, "localhost", true, true);
//
//        doPostWithApiKey("/api/domain", domain)
//                .andExpect(status().isOk());
//        doDeleteWithApiKey("/api/domain/" + domain.getId().getId().toString(), token)
//                .andExpect(status().isOk());
//        doDelete("/api/apiKey/" + apiKeyInfo.getId().getId().toString(), token)
//                .andExpect(status().isOk());
//
//        loginTenantAdmin();
//    }

    @Test
    public void testUnauthorizedWhenKeyDisabled() throws Exception {
        ApiKeyInfo disabledApiKeyInfo = doPut("/api/apiKey/" + savedApiKey.getId().getId() + "/enabled/false", Boolean.FALSE, ApiKeyInfo.class);
        Assert.assertFalse(disabledApiKeyInfo.isEnabled());
        doGetWithApiKey("/api/admin/featuresInfo").andExpect(status().isUnauthorized());
    }

    @Test
    public void testUnauthorizedWhenKeyExpired() throws Exception {
        ApiKeyInfo apiKeyInfo = constructApiKeyInfo();
        apiKeyInfo.setExpirationTime(System.currentTimeMillis() - 1000);
        ApiKey savedApiKeyWithBad = doPost("/api/apiKey", apiKeyInfo, ApiKey.class);
        setApiKey(savedApiKeyWithBad.getValue());
        doPost("/api/apiKey", savedApiKey, ApiKeyInfo.class);
        doGetWithApiKey("/api/admin/featuresInfo").andExpect(status().isUnauthorized());
    }

    private ApiKeyInfo constructApiKeyInfo() {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
        apiKeyInfo.setDescription("New API key description");
        apiKeyInfo.setEnabled(true);
        apiKeyInfo.setUserId(tenantAdminUserId);
        return apiKeyInfo;
    }

//    private ApiKeyInfo constructSystemApiKeyInfoWithPermissions(UserId userId, Map<Resource, Set<Operation>> permissions) {
//        ApiKeyInfo apiKeyInfo = new ApiKeyInfo();
//        apiKeyInfo.setTenantId(TenantId.SYS_TENANT_ID);
//        apiKeyInfo.setUserId(userId);
//        apiKeyInfo.setDescription("Test API Key");
//        apiKeyInfo.setEnabled(true);
//        apiKeyInfo.setPermissions(permissions);
//        return apiKeyInfo;
//    }
//
//    private Domain constructDomain(TenantId tenantId, String domainName, boolean oauth2Enabled, boolean edgeEnabled) {
//        Domain domain = new Domain();
//        domain.setTenantId(tenantId);
//        domain.setName(domainName);
//        domain.setOauth2Enabled(oauth2Enabled);
//        domain.setPropagateToEdge(edgeEnabled);
//        return domain;
//    }

}

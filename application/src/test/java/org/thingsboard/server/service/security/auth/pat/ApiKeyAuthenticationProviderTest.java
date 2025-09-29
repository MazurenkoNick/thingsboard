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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.RawApiKeyToken;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyAuthenticationProviderTest {

    private static final String TEST_API_KEY = "test_api_key";
    private static final String USER_EMAIL = "tenant@thingsboard.org";

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private UserService userService;

    @Mock
    private UserPermissionsService userPermissionsService;

    private ApiKeyAuthenticationProvider provider;
    private TenantId tenantId;
    private UserId userId;
    private User user;
    private UserCredentials userCredentials;
    private ApiKey apiKey;

    @Before
    public void setUp() {
        provider = new ApiKeyAuthenticationProvider(apiKeyService, userService, userPermissionsService);
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        userId = new UserId(UUID.randomUUID());

        user = new User();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setEmail(USER_EMAIL);
        user.setAuthority(Authority.TENANT_ADMIN);

        userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);

        apiKey = new ApiKey();
        apiKey.setId(new ApiKeyId(UUID.randomUUID()));
        apiKey.setTenantId(tenantId);
        apiKey.setUserId(userId);
        apiKey.setHash(TEST_API_KEY);
        apiKey.setEnabled(true);
        apiKey.setExpirationTime(0);
    }

    @Test
    public void testSuccessfulAuthentication() {
        when(apiKeyService.findApiKeyByHash(TEST_API_KEY)).thenReturn(apiKey);
        when(userService.findUserById(tenantId, userId)).thenReturn(user);
        when(userService.findUserCredentialsByUserId(tenantId, userId)).thenReturn(userCredentials);

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(new RawApiKeyToken(TEST_API_KEY));

        Authentication authentication = provider.authenticate(token);

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertTrue(authentication instanceof ApiKeyAuthenticationToken);
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        assertEquals(userId, securityUser.getId());
        assertEquals(tenantId, securityUser.getTenantId());
        assertEquals(USER_EMAIL, securityUser.getEmail());
        assertEquals(Authority.TENANT_ADMIN, securityUser.getAuthority());
    }

    @Test(expected = BadCredentialsException.class)
    public void testEmptyApiKey() {
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(new RawApiKeyToken(""));

        provider.authenticate(token);
    }

    @Test(expected = UsernameNotFoundException.class)
    public void testNonExistentApiKey() {
        when(apiKeyService.findApiKeyByHash(TEST_API_KEY)).thenReturn(null);
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(new RawApiKeyToken(TEST_API_KEY));

        provider.authenticate(token);
    }

    @Test(expected = DisabledException.class)
    public void testDisabledApiKey() {
        apiKey.setEnabled(false);
        when(apiKeyService.findApiKeyByHash(TEST_API_KEY)).thenReturn(apiKey);
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(new RawApiKeyToken(TEST_API_KEY));

        provider.authenticate(token);
    }

    @Test(expected = BadCredentialsException.class)
    public void testExpiredApiKey() {
        apiKey.setExpirationTime(System.currentTimeMillis() - 10000); // Expired 10 seconds ago
        when(apiKeyService.findApiKeyByHash(TEST_API_KEY)).thenReturn(apiKey);
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(new RawApiKeyToken(TEST_API_KEY));

        provider.authenticate(token);
    }

    @Test(expected = UsernameNotFoundException.class)
    public void testNonExistentUser() {
        when(apiKeyService.findApiKeyByHash(TEST_API_KEY)).thenReturn(apiKey);
        when(userService.findUserById(tenantId, userId)).thenReturn(null);
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(new RawApiKeyToken(TEST_API_KEY));

        provider.authenticate(token);
    }

    @Test(expected = UsernameNotFoundException.class)
    public void testNonExistentUserCredentials() {
        when(apiKeyService.findApiKeyByHash(TEST_API_KEY)).thenReturn(apiKey);
        when(userService.findUserById(tenantId, userId)).thenReturn(user);
        when(userService.findUserCredentialsByUserId(tenantId, userId)).thenReturn(null);
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(new RawApiKeyToken(TEST_API_KEY));

        provider.authenticate(token);
    }

    @Test(expected = DisabledException.class)
    public void testDisabledUser() {
        userCredentials.setEnabled(false);
        when(apiKeyService.findApiKeyByHash(TEST_API_KEY)).thenReturn(apiKey);
        when(userService.findUserById(tenantId, userId)).thenReturn(user);
        when(userService.findUserCredentialsByUserId(tenantId, userId)).thenReturn(userCredentials);
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(new RawApiKeyToken(TEST_API_KEY));

        provider.authenticate(token);
    }

    @Test(expected = InsufficientAuthenticationException.class)
    public void testUserWithoutAuthority() {
        user.setAuthority(null);
        when(apiKeyService.findApiKeyByHash(TEST_API_KEY)).thenReturn(apiKey);
        when(userService.findUserById(tenantId, userId)).thenReturn(user);
        when(userService.findUserCredentialsByUserId(tenantId, userId)).thenReturn(userCredentials);
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(new RawApiKeyToken(TEST_API_KEY));

        provider.authenticate(token);
    }

    @Test
    public void testSupports() {
        assertTrue(provider.supports(ApiKeyAuthenticationToken.class));
    }

}

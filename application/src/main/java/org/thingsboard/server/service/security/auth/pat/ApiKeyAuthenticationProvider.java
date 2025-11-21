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

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKey;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.pat.ApiKeyService;
import org.thingsboard.server.service.security.auth.AbstractAuthenticationProvider;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.ApiKeyAuthRequest;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

import java.util.Map;
import java.util.Set;

@Component
public class ApiKeyAuthenticationProvider extends AbstractAuthenticationProvider {

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationProvider(ApiKeyService apiKeyService, UserAuthDetailsCache userAuthDetailsCache,
                                        UserPermissionsService userPermissionsService, CustomerService customerService) {
        super(customerService, userAuthDetailsCache, userPermissionsService);
        this.apiKeyService = apiKeyService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthRequest apiKeyAuthRequest = (ApiKeyAuthRequest) authentication.getCredentials();
        SecurityUser securityUser = authenticate(apiKeyAuthRequest.apiKey(), apiKeyAuthRequest.userId(), apiKeyAuthRequest.customerId());
        return new ApiKeyAuthenticationToken(securityUser);
    }

    private SecurityUser authenticate(String key, UserId userIdInternal, CustomerId customerIdInternal) {
        if (StringUtils.isEmpty(key)) {
            throw new BadCredentialsException("Empty API key");
        }
        ApiKey apiKey = apiKeyService.findApiKeyByValue(key);
        if (apiKey == null) {
            throw new BadCredentialsException("User not found for the provided API key");
        }
        if (!apiKey.isEnabled()) {
            throw new DisabledException("API key auth is not active");
        }
        if (apiKey.getExpirationTime() != 0 && apiKey.getExpirationTime() < System.currentTimeMillis()) {
            throw new CredentialsExpiredException("API key is expired");
        }

        ResolvedUser resolvedUser = resolveUser(apiKey, userIdInternal, customerIdInternal);

        SecurityUser securityUser;

        if (resolvedUser.isPublicCustomer()) {
            securityUser = super.authenticateByPublicId(resolvedUser.customerId().toString(), "Internal API key", null);
        } else {
            securityUser = authenticateByUserId(resolvedUser.tenantId(), resolvedUser.userId());
        }

        if (apiKey.isInternal() && apiKey.getPermissions() != null) {
            Map<Resource, Set<Operation>> permissions = apiKey.getPermissions().getPermissionsForAuthority(securityUser.getAuthority());
            if (permissions != null) {
                securityUser.setUserPermissions(new MergedUserPermissions(permissions, securityUser.getUserPermissions().getGroupPermissions()));
            }
        }

        return securityUser;
    }

    private ResolvedUser resolveUser(ApiKey apiKey, UserId userIdInternal, CustomerId customerIdInternal) {
        if (apiKey.isInternal()) {
            if (userIdInternal != null && !userIdInternal.isNullUid()) {
                return new ResolvedUser(TenantId.SYS_TENANT_ID, userIdInternal, null, false);
            } else if (customerIdInternal != null && !customerIdInternal.isNullUid()) {
                return new ResolvedUser(TenantId.SYS_TENANT_ID, null, customerIdInternal, true);
            }
        }

        // Use API key's user (for regular keys or internal without additional headers)
        return new ResolvedUser(apiKey.getTenantId(), apiKey.getUserId(), null, false);
    }

    private record ResolvedUser(TenantId tenantId, UserId userId, CustomerId customerId, boolean isPublicCustomer) {}

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

}

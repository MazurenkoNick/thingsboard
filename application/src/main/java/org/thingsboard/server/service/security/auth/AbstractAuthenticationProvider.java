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
package org.thingsboard.server.service.security.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.user.cache.UserAuthDetailsCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAuthenticationProvider implements AuthenticationProvider {

    private final CustomerService customerService;
    private final UserAuthDetailsCache userAuthDetailsCache;
    private final UserPermissionsService userPermissionsService;

    protected SecurityUser authenticateByPublicId(String publicId, String authContextName, UserPrincipal userPrincipal) {
        TenantId systemId = TenantId.SYS_TENANT_ID;
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException(authContextName + " is not valid");
        }
        Customer publicCustomer = customerService.findCustomerById(systemId, customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found");
        }

        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException(authContextName + " is not valid");
        }

        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        UserPrincipal principal = userPrincipal == null ? new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, publicId) : userPrincipal;

        MergedUserPermissions userPermissions;
        try {
            userPermissions = userPermissionsService.getMergedPermissions(user, true);
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to get user permissions", e);
        }

        return new SecurityUser(user, true, principal, userPermissions);
    }

    protected SecurityUser authenticateByUserId(TenantId tenantId, UserId userId, AuthorityPermissionsInfo permissionsInfo, boolean internalApiKeyAuth) {
        UserAuthDetails userAuthDetails = userAuthDetailsCache.getUserAuthDetails(tenantId, userId);
        if (userAuthDetails == null) {
            throw new UsernameNotFoundException("User with credentials not found");
        }
        if (!userAuthDetails.credentialsEnabled() && !internalApiKeyAuth) {
            throw new DisabledException("User is not active");
        }

        User user = userAuthDetails.user();
        if (user.getAuthority() == null) {
            throw new InsufficientAuthenticationException("User has no authority assigned");
        }

        UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());

        MergedUserPermissions userPermissions;
        if (permissionsInfo != null) {
            Map<Resource, Set<Operation>> permissions = permissionsInfo.getPermissionsForAuthority(user.getAuthority());
            if (permissions != null && !permissions.isEmpty()) {
                userPermissions = new MergedUserPermissions(permissions, new HashMap<>());
                return new SecurityUser(user, true, userPrincipal, userPermissions);
            }
        }

        try {
            userPermissions = userPermissionsService.getMergedPermissions(user, false);
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to get user permissions", e);
        }

        return new SecurityUser(user, true, userPrincipal, userPermissions);
    }

}

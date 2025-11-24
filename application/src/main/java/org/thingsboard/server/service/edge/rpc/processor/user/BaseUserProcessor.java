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
package org.thingsboard.server.service.edge.rpc.processor.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.UUID;

@Slf4j
public abstract class BaseUserProcessor extends BaseEdgeProcessor {

    @Autowired
    private TbClusterService tbClusterService;

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private DataValidator<User> userValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateUser(TenantId tenantId, UserId userId, UserUpdateMsg userUpdateMsg) {
        boolean isCreated = false;
        boolean userEmailUpdated = false;

        try {
            User user = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
            if (user == null) {
                throw new IllegalArgumentException(String.format("[%s] Failed to parse User from UserUpdateMsg: %s", tenantId, userUpdateMsg));
            }

            User userById = edgeCtx.getUserService().findUserById(tenantId, userId);
            if (userById == null) {
                isCreated = true;
                user.setId(null);
            } else {
                changeOwnerIfRequired(tenantId, user.getCustomerId(), userById.getId());
                user.setId(userId);
            }

            String userEmail = user.getEmail();
            User existing = edgeCtx.getUserService().findUserByTenantIdAndEmail(tenantId, user.getEmail());

            if (existing != null && !existing.getId().equals(user.getId())) {
                String[] splitEmail = userEmail.split("@");
                userEmail = splitEmail[0] + "_" + StringUtils.randomAlphanumeric(15) + "@" + splitEmail[1];
                log.warn("[{}] User with email {} already exists. Renaming User email to {}",
                        tenantId, user.getEmail(), userEmail);
                userEmailUpdated = true;
            }
            user.setEmail(userEmail);
            setCustomerId(tenantId, isCreated ? null : userById.getCustomerId(), user, userUpdateMsg);

            userValidator.validate(user, User::getTenantId);

            if (isCreated) {
                user.setId(userId);
            }

            User savedUser = edgeCtx.getUserService().saveUser(tenantId, user, false);

            if (isCreated) {
                edgeCtx.getEntityGroupService().addEntityToEntityGroupAll(savedUser.getTenantId(), savedUser.getOwnerId(), savedUser.getId());
            }

            safeAddToEntityGroup(tenantId, userUpdateMsg, userId);
            tbClusterService.onUserUpdated(savedUser, isCreated ? null : user);
            userPermissionsService.onUserUpdatedOrRemoved(savedUser);
        } catch (Exception e) {
            log.error("[{}] Failed to process user update msg [{}]", tenantId, userUpdateMsg, e);
            throw new RuntimeException(e);
        }

        return Pair.of(isCreated, userEmailUpdated);
    }

    private void safeAddToEntityGroup(TenantId tenantId, UserUpdateMsg userUpdateMsg, UserId userId) {
        if (userUpdateMsg.hasEntityGroupIdMSB() && userUpdateMsg.hasEntityGroupIdLSB()) {
            UUID entityGroupUUID = safeGetUUID(userUpdateMsg.getEntityGroupIdMSB(),
                    userUpdateMsg.getEntityGroupIdLSB());
            safeAddEntityToGroup(tenantId, new EntityGroupId(entityGroupUUID), userId);
        }
    }

    protected User deleteUser(TenantId tenantId, UserId userId) throws ThingsboardException {
        User userById = edgeCtx.getUserService().findUserById(tenantId, userId);
        if (userById == null) {
            log.trace("[{}] User with id {} does not exist", tenantId, userId);
            return null;
        }
        edgeCtx.getUserService().deleteUser(tenantId, userById);
        userPermissionsService.onUserUpdatedOrRemoved(userById);
        return userById;
    }

    protected void updateUserCredentials(TenantId tenantId, UserCredentialsUpdateMsg updateMsg) {
        UserCredentials userCredentialsFromUpdateMsg = JacksonUtil.fromString(updateMsg.getEntity(), UserCredentials.class, true);
        if (userCredentialsFromUpdateMsg == null) {
            throw new IllegalArgumentException(String.format("[%s] Failed to parse UserCredentials from updateMsg: %s", tenantId, updateMsg));
        }

        User user = edgeCtx.getUserService().findUserById(tenantId, userCredentialsFromUpdateMsg.getUserId());

        if (user == null) {
            log.warn("[{}] Can't find user by id [{}] skipping credentials update. UserCredentialsUpdateMsg [{}]",
                    tenantId, userCredentialsFromUpdateMsg.getUserId(), updateMsg);
            return;
        }

        log.debug("[{}] Updating user credentials for user [{}]. New credentials Id [{}], enabled [{}]",
                tenantId, user.getName(), userCredentialsFromUpdateMsg.getId(), userCredentialsFromUpdateMsg.isEnabled());

        try {
            UserCredentials existing = edgeCtx.getUserService().findUserCredentialsByUserId(tenantId, user.getId());
            boolean created = existing == null;

            UserCredentials updated = created ? new UserCredentials() : existing;
            updated.setId(userCredentialsFromUpdateMsg.getId());
            updated.setUserId(user.getId());
            updated.setEnabled(userCredentialsFromUpdateMsg.isEnabled());
            updated.setActivateToken(userCredentialsFromUpdateMsg.getActivateToken());
            updated.setAdditionalInfo(userCredentialsFromUpdateMsg.getAdditionalInfo());
            updated.setPassword(userCredentialsFromUpdateMsg.getPassword());
            updated.setResetToken(userCredentialsFromUpdateMsg.getResetToken());

            if (created) {
                edgeCtx.getUserService().saveUserCredentials(tenantId, updated, false);
            } else {
                edgeCtx.getUserService().replaceUserCredentials(tenantId, updated, existing.getId(), false);
            }
        } catch (Exception e) {
            log.error("[{}] Can't update user credentials for user [{}], userCredentialsUpdateMsg [{}]",
                    tenantId, user.getName(), updateMsg, e);
            throw new RuntimeException(e);
        }

    }

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, User user, UserUpdateMsg userUpdateMsg);

}

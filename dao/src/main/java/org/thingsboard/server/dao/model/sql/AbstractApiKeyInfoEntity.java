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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.common.data.permission.AuthorityPermissionsInfo;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_DESCRIPTION_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_ENABLED_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_EXPIRATION_TIME_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_INTERNAL_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_PERMISSIONS_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_TENANT_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_USER_ID_COLUMN_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractApiKeyInfoEntity<T extends ApiKeyInfo> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = API_KEY_TENANT_ID_COLUMN_NAME)
    private UUID tenantId;

    @Column(name = API_KEY_USER_ID_COLUMN_NAME)
    private UUID userId;

    @Column(name = API_KEY_EXPIRATION_TIME_COLUMN_NAME)
    private long expirationTime;

    @Column(name = API_KEY_ENABLED_COLUMN_NAME)
    private boolean enabled;

    @Column(name = API_KEY_DESCRIPTION_COLUMN_NAME)
    private String description;

    @Column(name = API_KEY_INTERNAL_COLUMN_NAME)
    private boolean internal;

    @Type(JsonBinaryType.class)
    @Column(name = API_KEY_PERMISSIONS_COLUMN_NAME, columnDefinition = "json")
    private JsonNode permissions;

    public AbstractApiKeyInfoEntity() {
        super();
    }

    public AbstractApiKeyInfoEntity(ApiKeyInfo apiKeyInfo) {
        super(apiKeyInfo);
        this.tenantId = apiKeyInfo.getTenantId().getId();
        this.userId = apiKeyInfo.getUserId().getId();
        this.expirationTime = apiKeyInfo.getExpirationTime();
        this.description = apiKeyInfo.getDescription();
        this.enabled = apiKeyInfo.isEnabled();
        this.internal = apiKeyInfo.isInternal();
        this.permissions = toJson(apiKeyInfo.getPermissions());
    }

    protected ApiKeyInfo toApiKeyInfo() {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo(new ApiKeyId(getUuid()));
        apiKeyInfo.setCreatedTime(createdTime);
        apiKeyInfo.setTenantId(TenantId.fromUUID(tenantId));
        apiKeyInfo.setUserId(new UserId(userId));
        apiKeyInfo.setEnabled(enabled);
        apiKeyInfo.setExpirationTime(expirationTime);
        apiKeyInfo.setDescription(description);
        apiKeyInfo.setInternal(internal);
        apiKeyInfo.setPermissions(fromJson(permissions, AuthorityPermissionsInfo.class));
        return apiKeyInfo;
    }

}

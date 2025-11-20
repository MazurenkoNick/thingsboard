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
package org.thingsboard.server.common.data.pat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiKeyInfo extends BaseData<ApiKeyId> implements TenantEntity {

    @Serial
    private static final long serialVersionUID = -2313196723950490263L;

    @Schema(description = "JSON object with Tenant Id. Tenant Id of the api key cannot be changed.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @Schema(description = "JSON object with User Id. User Id of the api key cannot be changed.")
    private UserId userId;

    @Schema(description = "Expiration time of the api key.")
    private long expirationTime;

    @NoXss
    @NotBlank
    @Length(fieldName = "description")
    @Schema(description = "Api Key description.", example = "Api Key description")
    private String description;

    @Schema(description = "Enabled/disabled api key.", example = "true")
    private boolean enabled;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Indicates if the api key is expired based on current time. Returns false if expirationTime is 0 (no expiry).",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY)
    public boolean isExpired() {
        if (expirationTime == 0) {
            return false;
        }
        return System.currentTimeMillis() > expirationTime;
    }

    @Schema(description = "JSON object with the Api Key Id. " +
            "Specify this field to update the Api Key. " +
            "Referencing non-existing Api Key Id will cause error. " +
            "Omit this field to create new Api Key.")
    @Override
    public ApiKeyId getId() {
        return super.getId();
    }

    public ApiKeyInfo() {
        super();
    }

    public ApiKeyInfo(ApiKeyId id) {
        super(id);
    }

    public ApiKeyInfo(ApiKeyInfo apiKeyInfo) {
        super(apiKeyInfo);
        this.tenantId = apiKeyInfo.getTenantId();
        this.userId = apiKeyInfo.getUserId();
        this.expirationTime = apiKeyInfo.getExpirationTime();
        this.enabled = apiKeyInfo.isEnabled();
        this.description = apiKeyInfo.getDescription();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.API_KEY;
    }

}

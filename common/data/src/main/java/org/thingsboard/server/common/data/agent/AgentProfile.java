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
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class AgentProfile extends BaseData<AgentProfileId> implements HasId<AgentProfileId>, HasTenantId, HasVersion, HasName, TenantEntity {

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_PROFILE;
    }

    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "description")
    private String description;
    @NoXss
    @Length(fieldName = "provisionKey")
    private String provisionKey;
    @NoXss
    @Length(fieldName = "provisionSecret")
    private String provisionSecret;
    private AgentProvisionType provisionType;
    private boolean isDefault;
    @Getter
    private Long version;

    public AgentProfile() {
        super();
    }

    public AgentProfile(AgentProfileId id) {
        super(id);
    }

    public AgentProfile(AgentProfile profile) {
        super(profile);
        this.tenantId = profile.getTenantId();
        this.name = profile.getName();
        this.description = profile.getDescription();
        this.provisionKey = profile.getProvisionKey();
        this.provisionSecret = profile.getProvisionSecret();
        this.provisionType = profile.getProvisionType();
        this.isDefault = profile.isDefault();
        this.version = profile.getVersion();
    }

    @Schema(description = "JSON object with the Agent Profile Id.")
    @Override
    public AgentProfileId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the profile creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Unique profile name within tenant")
    @Override
    public String getName() {
        return name;
    }

    @Schema(description = "Profile description")
    public String getDescription() {
        return description;
    }

    @Schema(description = "Provision key for future auto-provisioning")
    public String getProvisionKey() {
        return provisionKey;
    }

    @Schema(description = "Provision secret for future auto-provisioning")
    public String getProvisionSecret() {
        return provisionSecret;
    }

    @Schema(description = "Provisioning strategy. DISABLED by default.")
    public AgentProvisionType getProvisionType() {
        return provisionType;
    }

    @Schema(description = "Used to mark the default profile that will be assigned to agents when no profile is specified.")
    public boolean isDefault() {
        return isDefault;
    }
}

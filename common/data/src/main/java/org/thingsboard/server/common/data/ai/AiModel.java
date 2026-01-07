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
package org.thingsboard.server.common.data.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoNullChar;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AiModel extends BaseData<AiModelId> implements TenantEntity, HasVersion, ExportableEntity<AiModelId> {

    @Serial
    private static final long serialVersionUID = 9017108678716011604L;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "JSON object representing the ID of the tenant associated with this AI model",
            example = "e3c4b7d2-5678-4a9b-0c1d-2e3f4a5b6c7d"
    )
    private TenantId tenantId;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description = "Version of the AI model record; increments automatically whenever the record is changed",
            example = "7",
            defaultValue = "1"
    )
    private Long version;

    @NotBlank
    @NoNullChar
    @Length(min = 1, max = 255)
    @NoXss
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Display name for this AI model configuration; not the technical model identifier",
            example = "Fast and cost-efficient model"
    )
    private String name;

    @NotNull
    @Valid
    @Schema(
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            accessMode = Schema.AccessMode.READ_WRITE,
            description = "Configuration of the AI model"
    )
    private AiModelConfig configuration;

    private AiModelId externalId;

    public AiModel() {}

    public AiModel(AiModelId id) {
        super(id);
    }

    public AiModel(AiModel model) {
        super(model.getId());
        createdTime = model.getCreatedTime();
        tenantId = model.getTenantId();
        version = model.getVersion();
        name = model.getName();
        configuration = model.getConfiguration();
        externalId = model.getExternalId() == null ? null : new AiModelId(model.getExternalId().getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AI_MODEL;
    }

}

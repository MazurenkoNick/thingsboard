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
package org.thingsboard.server.common.data.cf.configuration.geofencing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.lang.Nullable;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CfArgumentDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZoneGroupConfiguration {

    @Nullable
    private EntityId refEntityId;
    private CfArgumentDynamicSourceConfiguration refDynamicSourceConfiguration;

    @NotBlank
    private final String perimeterKeyName;

    @NotNull
    private final GeofencingReportStrategy reportStrategy;
    private final boolean createRelationsWithMatchedZones;

    private String relationType;
    private EntitySearchDirection direction;

    public void validate(String name) {
        if (EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY.equals(name) || EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY.equals(name)) {
            throw new IllegalArgumentException("Name '" + name + "' is reserved and cannot be used for zone group!");
        }
        if (refDynamicSourceConfiguration != null) {
            refDynamicSourceConfiguration.validate();
        }
        if (!createRelationsWithMatchedZones) {
            return;
        }
        if (StringUtils.isBlank(relationType)) {
            throw new IllegalArgumentException("Relation type must be specified for '" + name + "' zone group!");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Relation direction must be specified for '" + name + "' zone group!");
        }
    }

    public boolean hasRelationQuerySource() {
        return toArgument().hasRelationQuerySource();
    }

    public boolean hasCurrentOwnerSource() {
        return toArgument().hasOwnerSource();
    }

    @JsonIgnore
    public boolean isCfEntitySource(EntityId cfEntityId) {
        if (refEntityId == null && refDynamicSourceConfiguration == null) {
            return true;
        }
        return refEntityId != null && refEntityId.equals(cfEntityId);
    }

    @JsonIgnore
    public boolean isLinkedCfEntitySource(EntityId cfEntityId) {
        return refEntityId != null && !refEntityId.equals(cfEntityId);
    }

    public Argument toArgument() {
        var argument = new Argument();
        argument.setRefEntityId(refEntityId);
        argument.setRefDynamicSourceConfiguration(refDynamicSourceConfiguration);
        argument.setRefEntityKey(new ReferencedEntityKey(perimeterKeyName, ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        return argument;
    }

}

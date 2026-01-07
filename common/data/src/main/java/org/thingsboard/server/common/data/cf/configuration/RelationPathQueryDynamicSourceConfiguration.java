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
package org.thingsboard.server.common.data.cf.configuration;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationPathQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.List;

@Data
public class RelationPathQueryDynamicSourceConfiguration implements CfArgumentDynamicSourceConfiguration {

    private List<RelationPathLevel> levels;

    @Override
    public CFArgumentDynamicSourceType getType() {
        return CFArgumentDynamicSourceType.RELATION_PATH_QUERY;
    }

    @Override
    public void validate() {
        if (CollectionsUtil.isEmpty(levels)) {
            throw new IllegalArgumentException("At least one relation level must be specified!");
        }
        levels.forEach(RelationPathLevel::validate);
    }

    public List<EntityId> resolveEntityIds(List<EntityRelation> relations) {
        EntitySearchDirection lastLevelDirection = getLastLevel().direction();
        return switch (lastLevelDirection) {
            case FROM -> relations.stream().map(EntityRelation::getTo).toList();
            case TO -> relations.stream().map(EntityRelation::getFrom).toList();
        };
    }

    public void validateMaxRelationLevel(String argumentName, int maxAllowedRelationLevel) {
        if (levels.size() > maxAllowedRelationLevel) {
            throw new IllegalArgumentException("Max relation level is greater than configured " +
                                               "maximum allowed relation level in tenant profile: " + maxAllowedRelationLevel + " for argument: " + argumentName);
        }
    }

    public EntityRelationPathQuery toRelationPathQuery(EntityId entityId) {
        return new EntityRelationPathQuery(entityId, levels);
    }

    private RelationPathLevel getLastLevel() {
        return levels.get(levels.size() - 1);
    }

}

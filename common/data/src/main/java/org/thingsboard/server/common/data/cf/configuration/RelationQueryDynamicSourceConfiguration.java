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
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

import java.util.Collections;
import java.util.List;

@Data
public class RelationQueryDynamicSourceConfiguration implements CfArgumentDynamicSourceConfiguration {

    private int maxLevel;
    private boolean fetchLastLevelOnly;
    private EntitySearchDirection direction;
    private String relationType;

    @Override
    public CFArgumentDynamicSourceType getType() {
        return CFArgumentDynamicSourceType.RELATION_QUERY;
    }

    @Override
    public void validate() {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("Relation query dynamic source configuration max relation level can't be less than 1!");
        }
        if (maxLevel > 2) {
            throw new IllegalArgumentException("Relation query dynamic source configuration max relation level can't be greater than 2!");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Relation query dynamic source configuration direction must be specified!");
        }
        if (StringUtils.isBlank(relationType)) {
            throw new IllegalArgumentException("Relation query dynamic source configuration relation type must be specified!");
        }
    }

    @JsonIgnore
    public boolean isSimpleRelation() {
        return maxLevel == 1;
    }

    public EntityRelationsQuery toEntityRelationsQuery(EntityId rootEntityId) {
        if (isSimpleRelation()) {
            throw new IllegalArgumentException("Entity relations query can't be created for a simple relation!");
        }
        var entityRelationsQuery = new EntityRelationsQuery();
        entityRelationsQuery.setParameters(new RelationsSearchParameters(rootEntityId, direction, maxLevel, fetchLastLevelOnly));
        entityRelationsQuery.setFilters(Collections.singletonList(new RelationEntityTypeFilter(relationType, Collections.emptyList())));
        return entityRelationsQuery;
    }

    public List<EntityId> resolveEntityIds(List<EntityRelation> relations) {
        return switch (direction) {
            case FROM -> relations.stream().map(EntityRelation::getTo).toList();
            case TO -> relations.stream().map(EntityRelation::getFrom).toList();
        };
    }

}

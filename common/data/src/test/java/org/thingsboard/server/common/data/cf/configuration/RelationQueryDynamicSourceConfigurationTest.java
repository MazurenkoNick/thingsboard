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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RelationQueryDynamicSourceConfigurationTest {

    @Mock
    EntityId rootEntityId;

    @Mock
    EntityRelation rel1;
    @Mock
    EntityRelation rel2;

    @Test
    void typeShouldBeRelationQuery() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        assertThat(cfg.getType()).isEqualTo(CFArgumentDynamicSourceType.RELATION_QUERY);
    }

    @Test
    void validateShouldThrowWhenMaxLevelLessThanOne() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(0);
        cfg.setDirection(EntitySearchDirection.FROM);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation query dynamic source configuration max relation level can't be less than 1!");
    }

    @Test
    void validateShouldThrowWhenMaxLevelGreaterThanTwo() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(3);
        cfg.setDirection(EntitySearchDirection.FROM);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation query dynamic source configuration max relation level can't be greater than 2!");
    }

    @Test
    void validateShouldThrowWhenDirectionIsNull() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(1);
        cfg.setDirection(null);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation query dynamic source configuration direction must be specified!");
    }

    @ParameterizedTest
    @ValueSource(strings = {" "})
    @NullAndEmptySource
    void validateShouldThrowWhenRelationTypeIsNull(String relationType) {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(1);
        cfg.setDirection(EntitySearchDirection.TO);
        cfg.setRelationType(relationType);

        assertThatThrownBy(cfg::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation query dynamic source configuration relation type must be specified!");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void isSimpleRelationTrueWhenLevelIsOneAndEntityTypesEmptyOrNull(List<EntityType> entityTypes) {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(1);
        assertThat(cfg.isSimpleRelation()).isTrue();
    }

    @Test
    void isSimpleRelationFalseWhenMaxLevelNotOne() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(2);
        assertThat(cfg.isSimpleRelation()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void toEntityRelationsQueryShouldThrowForSimpleRelation(List<EntityType> entityTypes) {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(1);
        cfg.setFetchLastLevelOnly(false);
        cfg.setDirection(EntitySearchDirection.FROM);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        assertThatThrownBy(() -> cfg.toEntityRelationsQuery(rootEntityId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Entity relations query can't be created for a simple relation!");
    }

    @Test
    void toEntityRelationsQueryShouldBuildQueryForNonSimpleRelation() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(2);
        cfg.setFetchLastLevelOnly(true);
        cfg.setDirection(EntitySearchDirection.TO);
        cfg.setRelationType(EntityRelation.MANAGES_TYPE);

        var query = cfg.toEntityRelationsQuery(rootEntityId);

        assertThat(query).isNotNull();
        RelationsSearchParameters params = query.getParameters();
        assertThat(params).isNotNull();
        assertThat(params.getRootId()).isEqualTo(rootEntityId.getId());
        assertThat(params.getDirection()).isEqualTo(EntitySearchDirection.TO);
        assertThat(params.getMaxLevel()).isEqualTo(2);
        assertThat(params.isFetchLastLevelOnly()).isTrue();

        assertThat(query.getFilters()).hasSize(1);
        assertThat(query.getFilters().get(0)).isInstanceOf(RelationEntityTypeFilter.class);
        RelationEntityTypeFilter filter = query.getFilters().get(0);
        assertThat(filter.getRelationType()).isEqualTo(EntityRelation.MANAGES_TYPE);
    }

    @Test
    void resolveEntityIdsFromDirectionFROMReturnsToIds() {
        when(rel1.getTo()).thenReturn(mock(EntityId.class));
        when(rel2.getTo()).thenReturn(mock(EntityId.class));

        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setDirection(EntitySearchDirection.FROM);

        var out = cfg.resolveEntityIds(List.of(rel1, rel2));

        assertThat(out).containsExactly(rel1.getTo(), rel2.getTo());
    }

    @Test
    void resolveEntityIdsFromDirectionTOReturnsFromIds() {
        when(rel1.getFrom()).thenReturn(mock(EntityId.class));
        when(rel2.getFrom()).thenReturn(mock(EntityId.class));

        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setDirection(EntitySearchDirection.TO);

        var out = cfg.resolveEntityIds(List.of(rel1, rel2));

        assertThat(out).containsExactly(rel1.getFrom(), rel2.getFrom());
    }

    @Test
    void validateShouldPassForValidConfig() {
        var cfg = new RelationQueryDynamicSourceConfiguration();
        cfg.setMaxLevel(2);
        cfg.setFetchLastLevelOnly(false);
        cfg.setDirection(EntitySearchDirection.FROM);
        cfg.setRelationType(EntityRelation.CONTAINS_TYPE);

        assertThatCode(cfg::validate).doesNotThrowAnyException();
    }

}

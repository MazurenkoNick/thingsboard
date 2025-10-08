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
package org.thingsboard.server.common.data.cf.configuration.geofencing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CurrentOwnerDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS;

public class ZoneGroupConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = {EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY, EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY})
    void validateShouldThrowWhenUsedReservedEntityCoordinateNames(String name) {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        assertThatThrownBy(() -> zoneGroupConfiguration.validate(name))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name '" + name + "' is reserved and cannot be used for zone group!");
    }

    @ParameterizedTest
    @ValueSource(strings = "  ")
    @NullAndEmptySource
    void validateShouldThrowWhenRelationCreationEnabledAndRelationTypeIsNullEmptyOrBlank(String relationType) {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, true);
        zoneGroupConfiguration.setRelationType(relationType);
        assertThatThrownBy(() -> zoneGroupConfiguration.validate("allowedZonesGroup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation type must be specified for 'allowedZonesGroup' zone group!");
    }

    @Test
    void validateShouldThrowWhenRelationCreationEnabledAndDirectionIsNull() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, true);
        zoneGroupConfiguration.setRelationType(EntityRelation.CONTAINS_TYPE);
        zoneGroupConfiguration.setDirection(null);
        assertThatThrownBy(() -> zoneGroupConfiguration.validate("allowedZonesGroup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation direction must be specified for 'allowedZonesGroup' zone group!");
    }

    @Test
    void validateShouldDoesNotThrowAnyExceptionWhenRelationCreationDisabledAndConfigValid() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        assertThatCode(() -> zoneGroupConfiguration.validate("allowedZonesGroup")).doesNotThrowAnyException();
    }

    @Test
    void validateShouldDoesNotThrowAnyExceptionWhenRelationCreationEnabledAndConfigValid() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, true);
        zoneGroupConfiguration.setRelationType(EntityRelation.CONTAINS_TYPE);
        zoneGroupConfiguration.setDirection(EntitySearchDirection.TO);
        assertThatCode(() -> zoneGroupConfiguration.validate("allowedZonesGroup")).doesNotThrowAnyException();
    }

    @Test
    void whenHasRelationQuerySourceCalled_shouldReturnTrueIfRelationQuerySourceConfigurationIsNotNull() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(new RelationPathQueryDynamicSourceConfiguration());
        assertThat(zoneGroupConfiguration.hasRelationQuerySource()).isTrue();
    }

    @Test
    void whenHasRelationQuerySourceCalled_shouldReturnFalseIfRelationQuerySourceConfigurationIsNull() {
        var zoneGroupConfiguration = mock(ZoneGroupConfiguration.class);
        assertThat(zoneGroupConfiguration.getRefDynamicSourceConfiguration()).isNull();
        assertThat(zoneGroupConfiguration.hasRelationQuerySource()).isFalse();
    }

    @Test
    void whenHasRelationQuerySourceCalled_shouldReturnFalseIfCurrentOwnerSourceConfigured() {
        var zoneGroupConfiguration = mock(ZoneGroupConfiguration.class);
        zoneGroupConfiguration.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());
        assertThat(zoneGroupConfiguration.hasRelationQuerySource()).isFalse();
    }

    @Test
    void validateToArgumentsMethodCallWithoutRefEntityId() {
        var zoneGroupConfiguration = new ZoneGroupConfiguration("perimeter", REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false);
        Argument zoneGroupArgument = zoneGroupConfiguration.toArgument();
        assertThat(zoneGroupArgument).isNotNull();
        assertThat(zoneGroupArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey("perimeter", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
    }

}

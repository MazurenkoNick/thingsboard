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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;

@ExtendWith(MockitoExtension.class)
public class GeofencingCalculatedFieldConfigurationTest {

    @Test
    void typeShouldBeGeofencing() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        assertThat(cfg.getType()).isEqualTo(CalculatedFieldType.GEOFENCING);
    }

    @Test
    void validateShouldCallValidateOnZoneGroups() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        EntityCoordinates entityCoordinatesMock = mock(EntityCoordinates.class);
        cfg.setEntityCoordinates(entityCoordinatesMock);
        var zoneGroupConfiguration = mock(ZoneGroupConfiguration.class);
        cfg.setZoneGroups(Map.of("someGroupName", zoneGroupConfiguration));

        cfg.validate();
        verify(zoneGroupConfiguration).validate("someGroupName");
    }

    @Test
    void validateShouldCallValidateOnZoneGroupsWithoutAnyExceptions() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        EntityCoordinates entityCoordinatesMock = mock(EntityCoordinates.class);
        cfg.setEntityCoordinates(entityCoordinatesMock);
        var zoneGroupConfigurationA = mock(ZoneGroupConfiguration.class);
        var zoneGroupConfigurationB = mock(ZoneGroupConfiguration.class);

        String zoneGroupAName = "zoneGroupA";
        String zoneGroupBName = "zoneGroupB";

        cfg.setZoneGroups(Map.of("zoneGroupA", zoneGroupConfigurationA, "zoneGroupB", zoneGroupConfigurationB));

        assertThatCode(cfg::validate).doesNotThrowAnyException();

        verify(zoneGroupConfigurationA).validate(zoneGroupAName);
        verify(zoneGroupConfigurationB).validate(zoneGroupBName);
    }

    @Test
    void testGetArgumentsOverride() {
        var cfg = new GeofencingCalculatedFieldConfiguration();
        cfg.setEntityCoordinates(new EntityCoordinates(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY));
        cfg.setZoneGroups(Map.of("allowedZones", new ZoneGroupConfiguration("perimeter", GeofencingReportStrategy.REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS, false)));

        Map<String, Argument> arguments = cfg.getArguments();

        assertThat(arguments).isNotNull().hasSize(3);
        assertThat(arguments).containsKeys(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY, "allowedZones");

        Argument latitudeArgument = arguments.get(ENTITY_ID_LATITUDE_ARGUMENT_KEY);
        assertThat(latitudeArgument).isNotNull();
        assertThat(latitudeArgument.getRefDynamicSourceConfiguration()).isNull();
        assertThat(latitudeArgument.getRefEntityId()).isNull();
        assertThat(latitudeArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ArgumentType.TS_LATEST, null));

        Argument longitudeArgument = arguments.get(ENTITY_ID_LONGITUDE_ARGUMENT_KEY);
        assertThat(longitudeArgument).isNotNull();
        assertThat(longitudeArgument.getRefDynamicSourceConfiguration()).isNull();
        assertThat(longitudeArgument.getRefEntityId()).isNull();
        assertThat(longitudeArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey(ENTITY_ID_LONGITUDE_ARGUMENT_KEY, ArgumentType.TS_LATEST, null));

        Argument allowedZonesArgument = arguments.get("allowedZones");
        assertThat(allowedZonesArgument).isNotNull();
        assertThat(allowedZonesArgument.getRefDynamicSourceConfiguration()).isNull();
        assertThat(allowedZonesArgument.getRefEntityId()).isNull();
        assertThat(allowedZonesArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey("perimeter", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
    }

}

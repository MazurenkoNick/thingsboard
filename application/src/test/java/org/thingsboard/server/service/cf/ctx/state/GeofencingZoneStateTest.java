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
package org.thingsboard.server.service.cf.ctx.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingEvalResult;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingZoneState;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.INSIDE;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.OUTSIDE;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingTransitionEvent.ENTERED;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingTransitionEvent.LEFT;

public class GeofencingZoneStateTest {

    private final AssetId ZONE_ID = new AssetId(UUID.fromString("628730fd-d625-417f-9c6d-ae9fe4addbdb"));

    private GeofencingZoneState state;

    @BeforeEach
    void setUp() {
        String POLYGON = "[[50.472000, 30.504000], [50.472000, 30.506000], [50.474000, 30.506000], [50.474000, 30.504000]]";
        state = new GeofencingZoneState(ZONE_ID, new BaseAttributeKvEntry(new JsonDataEntry("zone", POLYGON), 100L, 1L));
    }

    @Test
    void evaluate_initialInside_thenInsideAgain() {
        var inside = new Coordinates(50.4730, 30.5050);
        // first evaluation: no prior state -> INSIDE
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(null, INSIDE));
        // same position again -> INSIDE (steady state)
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(null, INSIDE));
    }

    @Test
    void evaluate_initialOutside_thenOutsideAgain() {
        var outside = new Coordinates(50.4760, 30.5110);
        // first evaluation: no prior state -> OUTSIDE
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(null, OUTSIDE));
        // same position again -> OUTSIDE (steady state)
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(null, OUTSIDE));
    }

    @Test
    void evaluate_inside_thenLeave() {
        var inside = new Coordinates(50.4730, 30.5050);
        var outside = new Coordinates(50.4760, 30.5110);
        // initial eval
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(null, INSIDE));
        // leave -> LEFT
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(LEFT, OUTSIDE));
        // still outside -> OUTSIDE
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(null, OUTSIDE));
    }

    @Test
    void evaluate_outside_thenEnter() {
        var outside = new Coordinates(50.4760, 30.5110);
        var inside = new Coordinates(50.4730, 30.5050);
        // start outside
        assertThat(state.evaluate(outside)).isEqualTo(new GeofencingEvalResult(null, OUTSIDE));
        // cross boundary -> ENTERED
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(ENTERED, INSIDE));
        // remain inside -> INSIDE
        assertThat(state.evaluate(inside)).isEqualTo(new GeofencingEvalResult(null, INSIDE));
    }

    @Test
    void update_withNewerVersion_updatesState_andResetsPresence() {
        // arrange: establish a prior presence to ensure it’s reset on update
        var inside = new Coordinates(50.4730, 30.5050);
        assertThat(state.evaluate(inside)).isNotNull(); // sets lastPresence internally

        String NEW_POLYGON = "[[50.470000, 30.502000], [50.470000, 30.503000], [50.471000, 30.503000], [50.471000, 30.502000]]";
        GeofencingZoneState newer = new GeofencingZoneState(
                ZONE_ID,
                new BaseAttributeKvEntry(new JsonDataEntry("zone", NEW_POLYGON), 200L, 2L)
        );

        // act
        boolean changed = state.update(newer);

        // assert
        assertThat(changed).isTrue();
        assertThat(state.getTs()).isEqualTo(200L);
        assertThat(state.getVersion()).isEqualTo(2L);
        assertThat(state.getPerimeterDefinition()).isNotNull();
        assertThat(state.getLastPresence()).isNull(); // must be reset on successful update
    }

    @Test
    void update_withEqualVersion_doesNothing() {
        // arrange: same version (1L) but different ts/polygon should still be ignored
        String SOME_POLYGON = "[[50.472500, 30.504500], [50.472500, 30.505500], [50.473500, 30.505500], [50.473500, 30.504500]]";
        GeofencingZoneState sameVersion = new GeofencingZoneState(
                ZONE_ID,
                new BaseAttributeKvEntry(new JsonDataEntry("zone", SOME_POLYGON), 300L, 1L)
        );

        // act
        boolean changed = state.update(sameVersion);

        // assert: nothing changes
        assertThat(changed).isFalse();
        assertThat(state.getTs()).isEqualTo(100L);
        assertThat(state.getVersion()).isEqualTo(1L);
    }

    @Test
    void update_withNullNewVersion_alwaysApplies_andCopiesNull() {
        // arrange: the implementation updates if newVersion == null
        String OTHER_POLYGON = "[[50.471000, 30.506000], [50.471000, 30.507000], [50.472000, 30.507000], [50.472000, 30.506000]]";
        GeofencingZoneState nullVersion = new GeofencingZoneState(
                ZONE_ID,
                new BaseAttributeKvEntry(new JsonDataEntry("zone", OTHER_POLYGON), 400L, null)
        );

        // act
        boolean changed = state.update(nullVersion);

        // assert: applied and version copied as null
        assertThat(changed).isTrue();
        assertThat(state.getTs()).isEqualTo(400L);
        assertThat(state.getVersion()).isNull();
        assertThat(state.getLastPresence()).isNull();
    }

    @Test
    void update_withNewVersionWhenExistingIsNull_alwaysApplies_andCopiesNew() {
        // arrange: the implementation updates if newVersion == null
        String OTHER_POLYGON = "[[50.471000, 30.506000], [50.471000, 30.507000], [50.472000, 30.507000], [50.472000, 30.506000]]";
        GeofencingZoneState newVersion = new GeofencingZoneState(
                ZONE_ID,
                new BaseAttributeKvEntry(new JsonDataEntry("zone", OTHER_POLYGON), 400L, 2L)
        );
        state.setVersion(null);

        // act
        boolean changed = state.update(newVersion);

        // assert: applied and version copied as null
        assertThat(changed).isTrue();
        assertThat(state.getTs()).isEqualTo(400L);
        assertThat(state.getVersion()).isEqualTo(2);
        assertThat(state.getLastPresence()).isNull();
    }

}

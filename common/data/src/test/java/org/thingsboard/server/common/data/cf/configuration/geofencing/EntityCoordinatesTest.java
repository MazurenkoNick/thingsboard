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
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;

public class EntityCoordinatesTest {

    @Test
    void validateToArgumentsMethodCallWithoutRefEntityId() {
        var entityCoordinates = new EntityCoordinates("xPos", "yPos");

        var arguments = entityCoordinates.toArguments();
        assertThat(arguments).isNotNull().hasSize(2);

        Argument latitudeArgument = arguments.get(ENTITY_ID_LATITUDE_ARGUMENT_KEY);
        assertThat(latitudeArgument).isNotNull();
        assertThat(latitudeArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey("xPos", ArgumentType.TS_LATEST, null));
        assertThat(latitudeArgument.getRefEntityId()).isNull();
        assertThat(latitudeArgument.getRefDynamicSourceConfiguration()).isNull();

        Argument longitudeArgument = arguments.get(ENTITY_ID_LONGITUDE_ARGUMENT_KEY);
        assertThat(longitudeArgument).isNotNull();
        assertThat(longitudeArgument.getRefEntityKey()).isEqualTo(new ReferencedEntityKey("yPos", ArgumentType.TS_LATEST, null));
        assertThat(longitudeArgument.getRefEntityId()).isNull();
        assertThat(longitudeArgument.getRefDynamicSourceConfiguration()).isNull();
    }

}

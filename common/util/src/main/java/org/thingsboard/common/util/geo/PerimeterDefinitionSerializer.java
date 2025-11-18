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
package org.thingsboard.common.util.geo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;

public class PerimeterDefinitionSerializer extends JsonSerializer<PerimeterDefinition> {

    @Override
    public void serialize(PerimeterDefinition value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value instanceof CirclePerimeterDefinition c) {
            gen.writeStartObject();
            gen.writeNumberField("latitude", c.getLatitude());
            gen.writeNumberField("longitude", c.getLongitude());
            gen.writeNumberField("radius", c.getRadius());
            gen.writeEndObject();
            return;
        }
        if (value instanceof PolygonPerimeterDefinition p) {
            String raw = p.getPolygonDefinition();
            if (StringUtils.isBlank(raw)) {
                throw new IOException("Failed to serialize PolygonPerimeterDefinition with blank: " + value);
            }
            ObjectMapper mapper = (ObjectMapper) gen.getCodec();
            gen.writeTree(mapper.readTree(raw));
            return;
        }
        throw new IOException("Failed to serialize PerimeterDefinition from value: " + value);
    }
}

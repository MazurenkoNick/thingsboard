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
package org.thingsboard.server.dao.util.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityInfo;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Converter
public class EntityInfoConverter implements AttributeConverter<EntityInfo, String> {

    @Override
    public String convertToDatabaseColumn(EntityInfo attribute) {
        return JacksonUtil.toString(attribute);
    }

    @Override
    public EntityInfo convertToEntityAttribute(String s) {
        try {
            JsonNode node = JacksonUtil.fromBytes(s.getBytes(StandardCharsets.UTF_8));
            if (node.isObject()) {
                UUID id = null;
                String name = null;
                JsonNode idNode = node.get("id");
                JsonNode nameNode = node.get("name");
                JsonNode entityTypeNode = node.get("entityType");
                if (idNode != null && nameNode != null) {
                    try {
                        id = UUID.fromString(idNode.asText());
                    } catch (Exception ignored) {
                    }
                    name = nameNode.asText();
                }
                if (id != null && name != null) {
                    return new EntityInfo(id, entityTypeNode.asText(), name);
                }
            }
            return null;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert String to Entity info: " + ex.getMessage(), ex);
        }
    }

}

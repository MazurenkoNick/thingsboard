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
package org.thingsboard.rule.engine.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Jackson {@link ObjectNode} JSON Schema into a Langchain4j {@link JsonSchema} model.
 */
final class Langchain4jJsonSchemaAdapter {

    private Langchain4jJsonSchemaAdapter() {
        throw new AssertionError("Can't instantiate utility class");
    }

    /**
     * Creates a Langchain4j {@link JsonSchema} from the given root JSON Schema node.
     *
     * @param rootSchemaNode a valid JSON Schema as a Jackson {@link ObjectNode}
     * @return the corresponding Langchain4j {@link JsonSchema}
     */
    public static JsonSchema fromObjectNode(ObjectNode rootSchemaNode) {
        return JsonSchema.builder()
                .name(rootSchemaNode.get("title").textValue())
                .rootElement(parse(rootSchemaNode))
                .build();
    }

    private static JsonSchemaElement parse(JsonNode schemaNode) {
        String description = schemaNode.hasNonNull("description") ? schemaNode.get("description").textValue() : null;

        if (schemaNode.has("enum")) { // enum schemas can be defined without 'type'
            return parseEnum(schemaNode).description(description).build();
        }

        String type = schemaNode.get("type").textValue();

        return switch (type) {
            case "string" -> JsonStringSchema.builder().description(description).build();
            case "integer" -> JsonIntegerSchema.builder().description(description).build();
            case "boolean" -> JsonBooleanSchema.builder().description(description).build();
            case "number" -> JsonNumberSchema.builder().description(description).build();
            case "null" -> new JsonNullSchema();
            case "object" -> parseObject(schemaNode).description(description).build();
            case "array" -> parseArray(schemaNode).description(description).build();
            default -> throw new IllegalArgumentException("Unsupported JSON Schema type: " + type);
        };
    }

    private static JsonEnumSchema.Builder parseEnum(JsonNode enumSchema) {
        var builder = new JsonEnumSchema.Builder();

        List<String> enumValues = new ArrayList<>();
        for (JsonNode element : enumSchema.get("enum")) {
            if (!element.isTextual()) {
                throw new IllegalArgumentException("Expected each 'enum' element to be a string, but found: " + element.getNodeType());
            }
            enumValues.add(element.textValue());
        }
        builder.enumValues(enumValues);

        return builder;
    }

    private static JsonObjectSchema.Builder parseObject(JsonNode objectSchema) {
        var builder = new JsonObjectSchema.Builder();

        JsonNode propertiesNode = objectSchema.get("properties");
        if (propertiesNode != null) {
            propertiesNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                builder.addProperty(key, parse(value));
            });
        }

        List<String> required = new ArrayList<>();
        JsonNode requiredNode = objectSchema.get("required");
        if (requiredNode != null) {
            for (JsonNode value : requiredNode) {
                required.add(value.textValue());
            }
        }
        builder.required(required);

        boolean additionalProperties = true; // default value if 'additionalProperties' is not set
        JsonNode additionalPropertiesNode = objectSchema.get("additionalProperties");
        if (additionalPropertiesNode != null) {
            if (!additionalPropertiesNode.isBoolean()) {
                throw new IllegalArgumentException("Expected 'additionalProperties' to be a boolean, but found: " + additionalPropertiesNode.getNodeType());
            }
            additionalProperties = additionalPropertiesNode.booleanValue();
        }
        builder.additionalProperties(additionalProperties);

        return builder;
    }

    private static JsonArraySchema.Builder parseArray(JsonNode arraySchema) {
        var builder = new JsonArraySchema.Builder();

        if (arraySchema.hasNonNull("items")) {
            builder.items(parse(arraySchema.get("items")));
        }

        return builder;
    }

}

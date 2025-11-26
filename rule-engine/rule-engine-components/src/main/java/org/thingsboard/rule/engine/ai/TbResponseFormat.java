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
package org.thingsboard.rule.engine.ai;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import jakarta.validation.constraints.NotNull;
import org.thingsboard.server.common.data.validation.ValidJsonSchema;

import static org.thingsboard.rule.engine.ai.TbResponseFormat.TbJsonResponseFormat;
import static org.thingsboard.rule.engine.ai.TbResponseFormat.TbJsonSchemaResponseFormat;
import static org.thingsboard.rule.engine.ai.TbResponseFormat.TbTextResponseFormat;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TbTextResponseFormat.class, name = "TEXT"),
        @JsonSubTypes.Type(value = TbJsonResponseFormat.class, name = "JSON"),
        @JsonSubTypes.Type(value = TbJsonSchemaResponseFormat.class, name = "JSON_SCHEMA")
})
public sealed interface TbResponseFormat permits TbTextResponseFormat, TbJsonResponseFormat, TbJsonSchemaResponseFormat {

    TbResponseFormatType type();

    ResponseFormat toLangChainResponseFormat();

    enum TbResponseFormatType {

        TEXT,
        JSON,
        JSON_SCHEMA

    }

    record TbTextResponseFormat() implements TbResponseFormat {

        @Override
        public TbResponseFormatType type() {
            return TbResponseFormatType.TEXT;
        }

        @Override
        public ResponseFormat toLangChainResponseFormat() {
            return ResponseFormat.TEXT;
        }

    }

    record TbJsonResponseFormat() implements TbResponseFormat {

        @Override
        public TbResponseFormatType type() {
            return TbResponseFormatType.JSON;
        }

        @Override
        public ResponseFormat toLangChainResponseFormat() {
            return ResponseFormat.JSON;
        }

    }

    record TbJsonSchemaResponseFormat(@NotNull @ValidJsonSchema ObjectNode schema) implements TbResponseFormat {

        @Override
        public TbResponseFormatType type() {
            return TbResponseFormatType.JSON_SCHEMA;
        }

        @Override
        public ResponseFormat toLangChainResponseFormat() {
            return ResponseFormat.builder()
                    .type(ResponseFormatType.JSON)
                    .jsonSchema(Langchain4jJsonSchemaAdapter.fromObjectNode(schema))
                    .build();
        }

    }

}

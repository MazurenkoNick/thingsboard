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
package org.thingsboard.server.common.data.ai.dto;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;

import java.util.ArrayList;
import java.util.List;

public record TbChatRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                accessMode = Schema.AccessMode.READ_WRITE,
                description = "A system-level instruction that frames the user's input, setting the persona, tone, and constraints for the generated response",
                example = "You are a helpful assistant. Only output valid JSON."
        )
        String systemMessage,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_WRITE,
                description = "The actual user prompt that will be answered by the AI model"
        )
        @NotNull @Valid
        TbUserMessage userMessage,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.READ_WRITE,
                description = "Configuration of the AI chat model that should execute the request"
        )
        @NotNull @Valid
        AiChatModelConfig<?> chatModelConfig
) {

    public ChatRequest toLangChainChatRequest() {
        return ChatRequest.builder()
                .messages(getLangChainMessages())
                .build();
    }

    private List<ChatMessage> getLangChainMessages() {
        List<ChatMessage> messages = new ArrayList<>(2);

        if (systemMessage != null) {
            messages.add(SystemMessage.from(systemMessage));
        }

        List<Content> langChainContents = userMessage.contents().stream()
                .map(TbContent::toLangChainContent)
                .toList();

        messages.add(UserMessage.from(langChainContents));

        return messages;
    }

}

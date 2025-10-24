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
package org.thingsboard.server.service.ai;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ai.model.chat.AiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.Langchain4jChatModelConfigurer;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.secret.SecretConfigurationService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class AiChatModelServiceImpl implements AiChatModelService {

    private final Langchain4jChatModelConfigurer chatModelConfigurer;
    private final AiRequestsExecutor aiRequestsExecutor;
    private final SecretConfigurationService secretConfigurationService;

    @Override
    public <C extends AiChatModelConfig<C>> FluentFuture<ChatResponse> sendChatRequestAsync(TenantId tenantId, AiChatModelConfig<C> chatModelConfig, ChatRequest chatRequest) {
        AiChatModelConfig<C> modelConfigWithSecretsReplaced = replaceSecrets(tenantId, chatModelConfig);
        ChatModel langChainChatModel = modelConfigWithSecretsReplaced.configure(chatModelConfigurer);
        if (langChainChatModel.provider() == ModelProvider.GITHUB_MODELS) {
            chatRequest = prepareGithubChatRequest(chatRequest);
        }
        return aiRequestsExecutor.sendChatRequestAsync(langChainChatModel, chatRequest);
    }

    private <C extends AiChatModelConfig<C>> AiChatModelConfig<C> replaceSecrets(TenantId tenantId, AiChatModelConfig<C> chatModelConfig) {
        JsonNode modelConfigJson = JacksonUtil.valueToTree(chatModelConfig);
        secretConfigurationService.replaceSecretUsages(tenantId, modelConfigJson);
        return JacksonUtil.convertValue(modelConfigJson, new TypeReference<>() {});
    }

    private ChatRequest prepareGithubChatRequest(ChatRequest chatRequest) {
        List<ChatMessage> messages = chatRequest.messages().stream()
                .map(this::prepareUserMessage)
                .collect(Collectors.toList());

        return ChatRequest.builder()
                .messages(messages)
                .responseFormat(chatRequest.responseFormat())
                .build();
    }

    private ChatMessage prepareUserMessage(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            List<Content> newContents = userMessage.contents().stream()
                    .map(this::prepareContent)
                    .collect(Collectors.toList());

            return UserMessage.from(newContents);
        }
        return message;
    }

    private Content prepareContent(Content content) {
        if (content instanceof TextContent txt) {
            return new TextContent(new String(JsonStringEncoder.getInstance().quoteAsString(txt.text())));
        }
        return content;
    }

}

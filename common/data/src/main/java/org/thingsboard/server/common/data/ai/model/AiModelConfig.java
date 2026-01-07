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
package org.thingsboard.server.common.data.ai.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.ai.model.chat.AmazonBedrockChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AnthropicChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.AzureOpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GitHubModelsChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.GoogleVertexAiGeminiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.MistralAiChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OllamaChatModelConfig;
import org.thingsboard.server.common.data.ai.model.chat.OpenAiChatModelConfig;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.ai.provider.AiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AmazonBedrockProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AnthropicProviderConfig;
import org.thingsboard.server.common.data.ai.provider.AzureOpenAiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GitHubModelsProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.GoogleVertexAiGeminiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.MistralAiProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OllamaProviderConfig;
import org.thingsboard.server.common.data.ai.provider.OpenAiProviderConfig;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "provider",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenAiChatModelConfig.class, name = "OPENAI"),
        @JsonSubTypes.Type(value = AzureOpenAiChatModelConfig.class, name = "AZURE_OPENAI"),
        @JsonSubTypes.Type(value = GoogleAiGeminiChatModelConfig.class, name = "GOOGLE_AI_GEMINI"),
        @JsonSubTypes.Type(value = GoogleVertexAiGeminiChatModelConfig.class, name = "GOOGLE_VERTEX_AI_GEMINI"),
        @JsonSubTypes.Type(value = MistralAiChatModelConfig.class, name = "MISTRAL_AI"),
        @JsonSubTypes.Type(value = AnthropicChatModelConfig.class, name = "ANTHROPIC"),
        @JsonSubTypes.Type(value = AmazonBedrockChatModelConfig.class, name = "AMAZON_BEDROCK"),
        @JsonSubTypes.Type(value = GitHubModelsChatModelConfig.class, name = "GITHUB_MODELS"),
        @JsonSubTypes.Type(value = OllamaChatModelConfig.class, name = "OLLAMA")
})
public interface AiModelConfig {

    AiProvider provider();

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "provider"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = OpenAiProviderConfig.class, name = "OPENAI"),
            @JsonSubTypes.Type(value = AzureOpenAiProviderConfig.class, name = "AZURE_OPENAI"),
            @JsonSubTypes.Type(value = GoogleAiGeminiProviderConfig.class, name = "GOOGLE_AI_GEMINI"),
            @JsonSubTypes.Type(value = GoogleVertexAiGeminiProviderConfig.class, name = "GOOGLE_VERTEX_AI_GEMINI"),
            @JsonSubTypes.Type(value = MistralAiProviderConfig.class, name = "MISTRAL_AI"),
            @JsonSubTypes.Type(value = AnthropicProviderConfig.class, name = "ANTHROPIC"),
            @JsonSubTypes.Type(value = AmazonBedrockProviderConfig.class, name = "AMAZON_BEDROCK"),
            @JsonSubTypes.Type(value = GitHubModelsProviderConfig.class, name = "GITHUB_MODELS"),
            @JsonSubTypes.Type(value = OllamaProviderConfig.class, name = "OLLAMA")
    })
    AiProviderConfig providerConfig();

    AiModelType modelType();

}

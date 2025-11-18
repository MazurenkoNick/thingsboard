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
package org.thingsboard.server.common.data.ai.model.chat;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.With;
import org.thingsboard.server.common.data.ai.provider.AiProvider;
import org.thingsboard.server.common.data.ai.provider.OllamaProviderConfig;

@Builder
public record OllamaChatModelConfig(
        @NotNull @Valid OllamaProviderConfig providerConfig,
        @NotBlank String modelId,
        @PositiveOrZero Double temperature,
        @Positive @Max(1) Double topP,
        @PositiveOrZero Integer topK,
        Integer contextLength,
        Integer maxOutputTokens,
        @With @Positive Integer timeoutSeconds,
        @With @PositiveOrZero Integer maxRetries
) implements AiChatModelConfig<OllamaChatModelConfig> {

    @Override
    public AiProvider provider() {
        return AiProvider.OLLAMA;
    }

    @Override
    public ChatModel configure(Langchain4jChatModelConfigurer configurer) {
        return configurer.configureChatModel(this);
    }

    @Override
    public boolean supportsJsonMode() {
        return true;
    }

}

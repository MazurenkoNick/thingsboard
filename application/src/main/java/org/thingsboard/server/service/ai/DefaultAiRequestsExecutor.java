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
package org.thingsboard.server.service.ai;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.time.Duration;
import java.util.concurrent.Executors;

@Lazy
@Component
@RequiredArgsConstructor
class DefaultAiRequestsExecutor implements AiRequestsExecutor {

    private final AiRequestsExecutorProperties properties;

    @Data
    @Validated
    @Configuration
    @ConfigurationProperties(prefix = "actors.rule.ai-requests-thread-pool")
    private static class AiRequestsExecutorProperties {

        @NotBlank(message = "Pool name must be not blank")
        private String poolName = "ai-requests";

        @Min(value = 1, message = "Pool size must be at least 1")
        private int poolSize = 50;

        @Min(value = 1, message = "Termination timeout must be at least 1 second")
        private int terminationTimeoutSeconds = 60;

    }

    private ListeningExecutorService executorService;

    @PostConstruct
    private void init() {
        executorService = MoreExecutors.listeningDecorator(
                Executors.newFixedThreadPool(properties.getPoolSize(), ThingsBoardThreadFactory.forName(properties.getPoolName()))
        );
    }

    @Override
    public FluentFuture<ChatResponse> sendChatRequestAsync(ChatModel chatModel, ChatRequest chatRequest) {
        return FluentFuture.from(executorService.submit(() -> chatModel.chat(chatRequest)));
    }

    @PreDestroy
    private void destroy() {
        if (executorService != null) {
            MoreExecutors.shutdownAndAwaitTermination(executorService, Duration.ofSeconds(properties.getTerminationTimeoutSeconds()));
            executorService = null;
        }
    }

}

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
package org.thingsboard.rest.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TrendzApiClient {

    @Getter
    public enum TokenType {
        JWT("jwt"),
        API("api-token"),
        ;

        private final String value;

        TokenType(String value) {
            this.value = value;
        }
    }

    private static final int DEFAULT_RETRY_COUNT = 5;
    private static final int DEFAULT_RETRY_WAIT_SECONDS = 5;


    private final String url;
    private final TokenType tokenType;
    private final String token;
    private final int retryCount;
    private final int retryWaitSeconds;
    private final HttpClient httpClient;

    public TrendzApiClient(String trendzUrl, TokenType tokenType, String token) {
        this(trendzUrl, tokenType, token, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_WAIT_SECONDS);
    }

    public TrendzApiClient(String trendzUrl, TokenType tokenType, String token, int retryCount, int retryWaitSeconds) {
        this.url = trendzUrl;
        this.retryCount = retryCount;
        this.retryWaitSeconds = retryWaitSeconds;
        this.tokenType = tokenType;
        this.token = token;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }


    public boolean isTrendzServiceReachable() {
        Callable<Boolean> callable = () -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.url + "/apiTrendz/"))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(3))
                    .build();
            try {
                HttpResponse<Void> response = this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();
                if (status > 500) {
                    log.debug("Status code is {}, probably proxy responded with error", response.statusCode());
                } else {
                    log.debug("Trendz is available by URL {}, status: {}", this.url, response.statusCode());
                    return true;
                }
            } catch (IOException | InterruptedException e) {
                log.debug("Trendz is not available by URL {}: {}", this.url, e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            throw new RuntimeException("Trendz availability is not confirmed by URL: " + this.url);
        };
        return retry(callable);
    }

    public boolean sendCheckSigningKey() {
        Callable<Boolean> callable = () -> {
            HttpRequest request = makeHttpRequest("/apiTrendz/system/signingKey/check")
                    .GET()
                    .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            if (response.statusCode() == 200) {
                Boolean isOk = JacksonUtil.fromString(responseBody, Boolean.class);
                log.debug("The signing key check has been done with result: {}", isOk);
                return isOk;
            }
            if (response.statusCode() == 400 && "Forbidden for cloud instance.".equals(responseBody)) {
                log.debug("The signing key check is not applicable for Cloud instance, but Cloud instance is ok");
                return true;
            }

            throw new RuntimeException("Failed to send request for import data, reason: " + responseBody);
        };
        return retry(callable);
    }

    public boolean sendTrendzSubscriptionValid() {
        Callable<Boolean> callable = () -> {
            HttpRequest request = makeHttpRequest("/apiTrendz/system/licence/info")
                    .GET()
                    .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to send request for load task id by reference, reason: " + body);
            }

            JsonNode licenseInfoNode = JacksonUtil.toJsonNode(body);
            return licenseInfoNode.get("valid").asBoolean();
        };
        return retry(callable);
    }

    public UUID sendImportMigrationData(ObjectNode requestBody) {
        Callable<UUID> callable = () -> {
            HttpRequest request = makeHttpRequest("/apiTrendz/migration/import")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to send request for import data, reason: " + responseBody);
            }

            UUID executionId = JacksonUtil.fromString(responseBody, UUID.class);

            log.debug("Migration import task started with execution id: {}", executionId);
            return executionId;
        };
        return retry(callable);
    }

    public UUID loadTaskIdByReference(String referenceType, String referenceKey) {
        Callable<UUID> callable = () -> {
            HttpRequest request = makeHttpRequest("/apiTrendz/task/reference/" + referenceType + "/" + referenceKey)
                    .GET()
                    .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to send request for load task id by reference, reason: " + body);
            }

            JsonNode taskNode = JacksonUtil.toJsonNode(body);
            String name = taskNode.get("name").asText();
            UUID id = UUID.fromString(taskNode.get("id").asText());

            log.debug("Task is loaded with id: {}, name: {}", id, name);
            return id;
        };
        return retry(callable);
    }

    public UUID runTask(UUID taskId) {
        Callable<UUID> callable = () -> {
            HttpRequest request = makeHttpRequest("/apiTrendz/task/run/" + taskId + "?cancel=false&forced=true")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to send request for run task, reason: " + responseBody);
            }

            JsonNode executionNode = JacksonUtil.toJsonNode(responseBody);
            UUID executionId = UUID.fromString(executionNode.asText());

            log.debug("Migration import task started with execution id: {}", executionId);
            return executionId;
        };
        return retry(callable);
    }

    public JsonNode awaitTaskExecution(UUID executionId) throws TimeoutException {
        return awaitTaskExecution(executionId, 900_000, 3_000);
    }

    public JsonNode awaitTaskExecution(UUID executionId, long timeoutMs, long intervalMs) throws TimeoutException {
        long startTs = System.currentTimeMillis();

        HttpRequest request = makeHttpRequest("/apiTrendz/task/execution/poll/" + executionId)
                .GET()
                .build();

        do {
            try {
                log.debug("Waiting for execution {} to finish, time to timeout = {}", executionId, timeoutMs - (System.currentTimeMillis() - startTs));
                TimeUnit.MILLISECONDS.sleep(intervalMs);

                Callable<JsonNode> callable = () -> {
                    HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    String responseBody = response.body();
                    if (response.statusCode() != 200 && response.statusCode() != 202) {
                        throw new RuntimeException("Failed to send request for import data, reason: " + responseBody);
                    }
                    return JacksonUtil.toJsonNode(responseBody);
                };
                JsonNode taskExecutionNode = retry(callable);
                String status = taskExecutionNode.get("status").asText();

                if (!Set.of("CREATED", "RUNNING").contains(status)) {
                    JsonNode jsonResultNode = taskExecutionNode.get("jsonResult");
                    if ("FINISHED".equals(status)) {
                        return jsonResultNode;
                    } else {
                        String lastElementAsString;
                        if (jsonResultNode != null && jsonResultNode.isArray() && !jsonResultNode.isEmpty()) {
                            int lastIndex = jsonResultNode.size() - 1;
                            JsonNode lastElement = jsonResultNode.get(lastIndex);
                            lastElementAsString = lastElement.toString();
                        } else {
                            lastElementAsString = taskExecutionNode.toString();
                        }
                        throw new RuntimeException("Failed task execution with info: " + lastElementAsString);
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (System.currentTimeMillis() - startTs < timeoutMs);
        throw new TimeoutException("Could not await the Trendz execution with id " + executionId);
    }

    public void sendEnableCalculationFieldRequest(ObjectNode requestBody) {
        Callable<Void> callable = () -> {
            HttpRequest request = makeHttpRequest("/apiTrendz/calculation/taskData")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to send request for calculation field enabling, reason: " + response.body());
            }
            return null;
        };
        retry(callable);
    }

    public void sendDeleteEntity(String path, UUID id) {
        Callable<Void> callable = () -> {
            HttpRequest request = makeHttpRequest(path + id)
                    .DELETE()
                    .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 404) {
                throw new RuntimeException("Failed to send request for entity deletion, reason: " + response.body());
            }
            return null;
        };
        retry(callable);
    }


    public UUID sendRunCustomPromptExecution(UUID promptId, String data) {
        Callable<UUID> callable = () -> {
            ObjectNode requestNode = JacksonUtil.newObjectNode();
            requestNode.put("promptId", promptId.toString());
            requestNode.put("data", data);

            HttpRequest request = makeHttpRequest("/apiTrendz/agent/prompts/execute")
                    .POST(HttpRequest.BodyPublishers.ofString(requestNode.toString()))
                    .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to send request for prompt execution, reason: " + responseBody);
            }

            UUID executionId = JacksonUtil.fromString(responseBody, UUID.class);

            log.debug("Prompt execution task started with execution id: {}", executionId);
            return executionId;
        };
        return retry(callable);
    }


    private HttpRequest.Builder makeHttpRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(this.url + path))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(this.tokenType.getValue(), this.token);
    }

    private <T> T retry(Callable<T> callable) {
        Exception lastException = null;
        for (int i = 0; i < this.retryCount; i++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                try {
                    TimeUnit.SECONDS.sleep(this.retryWaitSeconds);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        throw new RuntimeException("Failed to send request with retries", lastException);
    }
}

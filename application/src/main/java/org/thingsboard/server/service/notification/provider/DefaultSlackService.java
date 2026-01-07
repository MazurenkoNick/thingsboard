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
package org.thingsboard.server.service.notification.provider;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiRequest;
import com.slack.api.methods.SlackApiTextResponse;
import com.slack.api.methods.SlackFilesUploadV2Exception;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.conversations.ConversationsOpenRequest;
import com.slack.api.methods.request.files.FilesUploadV2Request;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.ConversationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversation;
import org.thingsboard.server.common.data.notification.targets.slack.SlackConversationType;
import org.thingsboard.server.common.data.notification.targets.slack.SlackFile;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.data.util.ThrowingBiFunction;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultSlackService implements SlackService {

    private final NotificationSettingsService notificationSettingsService;
    private final SecretConfigurationService secretConfigurationService;

    private final Slack slack = Slack.getInstance();
    private final Cache<String, List<SlackConversation>> cache = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.SECONDS)
            .maximumSize(100)
            .build();
    private static final int CONVERSATIONS_LOAD_LIMIT = 1000;

    @Override
    public void sendMessage(TenantId tenantId, String token, String conversationId, String message) {
        sendMessage(tenantId, token, conversationId, message, null);
    }

    @Override
    public void sendMessage(TenantId tenantId, String token, String conversationId, String message, List<SlackFile> files) {
        if (CollectionsUtil.isNotEmpty(files)) {
            if (conversationId.startsWith("U")) { // direct message
                /*
                 * files.uploadV2 requires an existing channel ID, while chat.postMessage auto‑opens DMs
                 * */
                conversationId = sendRequest(token, ConversationsOpenRequest.builder()
                        .users(List.of(conversationId))
                        .build(), MethodsClient::conversationsOpen).getChannel().getId();
            }

            FilesUploadV2Request request = FilesUploadV2Request.builder()
                    .initialComment(message)
                    .channel(conversationId)
                    .uploadFiles(files.stream()
                            .map(file -> FilesUploadV2Request.UploadFile.builder()
                                    .filename(file.getName())
                                    .title(file.getName())
                                    .fileData(file.getData())
                                    .build())
                            .toList())
                    .build();
            sendRequest(token, request, MethodsClient::filesUploadV2);
        } else {
            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(conversationId)
                    .text(message)
                    .build();
            sendRequest(token, request, MethodsClient::chatPostMessage);
        }
    }

    @Override
    public List<SlackConversation> listConversations(TenantId tenantId, String token, SlackConversationType conversationType) {
        return cache.get(conversationType + ":" + token, k -> {
            if (conversationType == SlackConversationType.DIRECT) {
                UsersListRequest request = UsersListRequest.builder()
                        .limit(CONVERSATIONS_LOAD_LIMIT)
                        .build();

                UsersListResponse response = sendRequest(token, request, MethodsClient::usersList);
                return response.getMembers().stream()
                        .filter(user -> !user.isDeleted() && !user.isStranger() && !user.isBot())
                        .map(user -> {
                            SlackConversation conversation = new SlackConversation();
                            conversation.setType(conversationType);
                            conversation.setId(user.getId());
                            conversation.setName(user.getName());
                            conversation.setWholeName(user.getProfile() != null ? user.getProfile().getRealNameNormalized() : user.getRealName());
                            conversation.setEmail(user.getProfile() != null ? user.getProfile().getEmail() : null);
                            return conversation;
                        })
                        .collect(Collectors.toList());
            } else {
                ConversationsListRequest request = ConversationsListRequest.builder()
                        .types(List.of(conversationType == SlackConversationType.PUBLIC_CHANNEL ?
                                ConversationType.PUBLIC_CHANNEL :
                                ConversationType.PRIVATE_CHANNEL))
                        .limit(CONVERSATIONS_LOAD_LIMIT)
                        .excludeArchived(true)
                        .build();

                ConversationsListResponse response = sendRequest(token, request, MethodsClient::conversationsList);
                return response.getChannels().stream()
                        .filter(channel -> !channel.isArchived())
                        .map(channel -> {
                            SlackConversation conversation = new SlackConversation();
                            conversation.setType(conversationType);
                            conversation.setId(channel.getId());
                            conversation.setName(channel.getName());
                            conversation.setWholeName(channel.getNameNormalized());
                            return conversation;
                        })
                        .collect(Collectors.toList());
            }
        });
    }

    @Override
    public String getToken(TenantId tenantId) {
        NotificationSettings settings = notificationSettingsService.findNotificationSettings(tenantId);
        SlackNotificationDeliveryMethodConfig slackConfig = (SlackNotificationDeliveryMethodConfig)
                settings.getDeliveryMethodsConfigs().get(NotificationDeliveryMethod.SLACK);
        if (slackConfig != null) {
            return secretConfigurationService.replaceSecretUsage(tenantId, slackConfig.getBotToken());
        } else {
            return null;
        }
    }

    private <T extends SlackApiRequest, R extends SlackApiTextResponse> R sendRequest(String token, T request, ThrowingBiFunction<MethodsClient, T, R> method) {
        MethodsClient client = slack.methods(token);
        R response;
        try {
            response = method.apply(client, request);
        } catch (SlackFilesUploadV2Exception e) {
            if (e.getGetURLResponses() != null) {
                e.getGetURLResponses().forEach(this::checkResponse);
            }
            if (e.getCompleteResponse() != null) {
                checkResponse(e.getCompleteResponse());
            }
            if (e.getFileInfoResponses() != null) {
                e.getFileInfoResponses().forEach(this::checkResponse);
            }
            throw new RuntimeException("Failed to upload Slack file: " + e.toString(), e);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        checkResponse(response);
        return response;
    }

    private void checkResponse(SlackApiTextResponse response) {
        if (response.isOk()) {
            return;
        }

        String error = response.getError();
        if (error != null) {
            switch (error) {
                case "missing_scope" -> {
                    String neededScope = response.getNeeded();
                    error = "bot token scope '" + neededScope + "' is needed";
                }
                case "not_in_channel" -> {
                    error = "app needs to be added to the channel";
                }
                default -> {
                    error = null;
                }
            }
        }
        if (error == null) {
            ObjectNode responseJson = (ObjectNode) JacksonUtil.valueToTree(response);
            responseJson.remove("httpResponseHeaders");
            error = responseJson.toString();
        }
        throw new RuntimeException("Slack API error: " + error);
    }

}

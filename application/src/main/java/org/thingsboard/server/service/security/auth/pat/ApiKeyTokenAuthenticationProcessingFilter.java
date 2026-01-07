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
package org.thingsboard.server.service.security.auth.pat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.service.security.auth.extractor.TokenExtractor;
import org.thingsboard.server.service.security.model.token.ApiKeyAuthRequest;

import java.io.IOException;
import java.util.UUID;

import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.API_KEY_HEADER_PREFIX;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER;
import static org.thingsboard.server.config.ThingsboardSecurityConfiguration.AUTHORIZATION_HEADER_V2;

public class ApiKeyTokenAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String CUSTOMER_ID_HEADER = "X-Customer-Id";

    private final AuthenticationFailureHandler failureHandler;
    private final TokenExtractor tokenExtractor;

    @Autowired
    public ApiKeyTokenAuthenticationProcessingFilter(AuthenticationFailureHandler failureHandler,
                                                     @Qualifier("apiKeyHeaderTokenExtractor") TokenExtractor tokenExtractor, RequestMatcher matcher) {
        super(matcher);
        this.failureHandler = failureHandler;
        this.tokenExtractor = tokenExtractor;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String apiKeyValue = tokenExtractor.extract(request);
        UserId userId = getUserId(request);
        CustomerId customerId = getCustomerId(request);
        ApiKeyAuthRequest apiKeyAuthRequest = new ApiKeyAuthRequest(apiKeyValue, userId, customerId);
        return getAuthenticationManager().authenticate(new ApiKeyAuthenticationToken(apiKeyAuthRequest));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        if (!super.requiresAuthentication(request, response)) {
            return false;
        }
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null) {
            header = request.getHeader(AUTHORIZATION_HEADER_V2);
        }
        return header != null && header.startsWith(API_KEY_HEADER_PREFIX);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }

    private UserId getUserId(HttpServletRequest request) {
        UUID uuid = extractUuidFromHeader(request, USER_ID_HEADER);
        return uuid != null ? new UserId(uuid) : null;
    }

    private CustomerId getCustomerId(HttpServletRequest request) {
        UUID uuid = extractUuidFromHeader(request, CUSTOMER_ID_HEADER);
        return uuid != null ? new CustomerId(uuid) : null;
    }

    private UUID extractUuidFromHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (StringUtils.isNotBlank(headerValue)) {
            try {
                return UUID.fromString(headerValue);
            } catch (IllegalArgumentException e) {
                throw new AuthenticationServiceException("Invalid " + headerName + " format: " + headerValue);
            }
        }
        return null;
    }

}

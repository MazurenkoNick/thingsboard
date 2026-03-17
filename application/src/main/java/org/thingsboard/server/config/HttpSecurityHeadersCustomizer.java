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
package org.thingsboard.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpSecurityHeadersCustomizer {

    private final HttpSecurityHeadersProperties properties;

    public void customize(HeadersConfigurer<?> headers) {
        if (properties.getXContentTypeOptions().isEnabled()) {
            headers.contentTypeOptions(config -> {});
        }

        if (properties.getReferrerPolicy().isEnabled()) {
            headers.addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", properties.getReferrerPolicy().getValue()));
        }

        if (properties.getXFrameOptions().isEnabled()) {
            String value = properties.getXFrameOptions().getValue();
            if ("DENY".equalsIgnoreCase(value)) {
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
            } else {
                if (!"SAMEORIGIN".equalsIgnoreCase(value)) {
                    log.warn("Unrecognized X-Frame-Options value '{}', falling back to SAMEORIGIN. Valid values: DENY, SAMEORIGIN", value);
                }
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
            }
        }

        if (properties.getContentSecurityPolicy().isEnabled() && StringUtils.hasText(properties.getContentSecurityPolicy().getValue())) {
            headers.contentSecurityPolicy(csp -> {
                csp.policyDirectives(properties.getContentSecurityPolicy().getValue());
                if (properties.getContentSecurityPolicy().isReportOnly()) {
                    csp.reportOnly();
                }
            });
        }

    }

}

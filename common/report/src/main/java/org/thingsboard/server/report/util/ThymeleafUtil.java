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
package org.thingsboard.server.report.util;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.thymeleaf.templatemode.TemplateMode.HTML;

public class ThymeleafUtil {
    private static final TemplateEngine classEngine;
    private static final TemplateEngine stringEngine;

    static {
        classEngine = new TemplateEngine();
        ClassLoaderTemplateResolver classResolver = new ClassLoaderTemplateResolver();
        classResolver.setPrefix("/");
        classResolver.setSuffix(".html");
        classResolver.setTemplateMode(HTML);
        classResolver.setOrder(1);
        classResolver.setCharacterEncoding(UTF_8);
        classEngine.setTemplateResolver(classResolver);

        stringEngine = new TemplateEngine();
        StringTemplateResolver stringResolver = new StringTemplateResolver();
        stringResolver.setTemplateMode("HTML");
        stringResolver.setResolvablePatterns(Set.of("*"));
        stringResolver.setCacheable(false);
        stringResolver.setOrder(2);
        stringEngine.setTemplateResolver(stringResolver);
    }

    public static String render(String templateHtml, Map<String, Object> variables) {
        Context context = new Context(Locale.getDefault(), variables);
        return classEngine.process(convertToThymeleafInline(templateHtml), context);
    }

    public static String renderFromString(String html, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        return stringEngine.process(convertToThymeleafInline(sanitize(html)), context);
    }

    public static String convertToThymeleafInline(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Regex to find ${key} patterns
        return input.replaceAll("\\$\\{([^}]+)}", "\\[\\[\\${$1}\\]\\]");
    }

    private static String sanitize(String html) {
        return html.replace("\n", "");
    }
}

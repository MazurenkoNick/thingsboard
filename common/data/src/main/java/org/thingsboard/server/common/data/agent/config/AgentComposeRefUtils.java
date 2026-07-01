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
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${compose.<path>}} references against a docker compose node. The dotted path navigates node fields
 * (e.g. {@code compose.version}); a {@code svcImgRegex(<image-regex>)} segment selects the first service whose image
 * matches the regex (from the current node's {@code services}), so a service can be picked without knowing its key.
 * Segment names are arbitrary — no whitelist. Shared across steps that copy values from the compose.
 */
@Slf4j
public class AgentComposeRefUtils {

    private static final Pattern COMPOSE_REF = Pattern.compile("^\\$\\{compose\\.(.+)}$");
    private static final Pattern SVC_IMG_REGEX = Pattern.compile("svcImgRegex\\((.+)\\)");

    private AgentComposeRefUtils() {
    }

    public static boolean isComposeRef(String value) {
        return value != null && COMPOSE_REF.matcher(value).matches();
    }

    /**
     * Navigates the {@code ${compose.<path>}} reference from {@code compose} and returns the node it points at, or
     * {@code null} when the ref is not a compose ref or any path segment is absent.
     */
    public static JsonNode resolve(JsonNode compose, String ref) {
        if (compose == null || ref == null) {
            return null;
        }
        Matcher m = COMPOSE_REF.matcher(ref);
        if (!m.matches()) {
            return null;
        }
        JsonNode node = compose;
        for (String segment : splitPath(m.group(1))) {
            if (node == null) {
                break;
            }
            Matcher svc = SVC_IMG_REGEX.matcher(segment);
            node = svc.matches()
                    ? DockerComposeUtils.findServiceByImage(node, Pattern.compile(svc.group(1)))
                    : node.get(segment);
        }
        if (node == null) {
            log.warn("Compose ref [{}] did not resolve to any node", ref);
        }
        return node;
    }

    /**
     * Resolves a ref for a list-typed target, normalizing the node to strings: an array yields its elements; an object
     * (compose map form, e.g. environment) yields {@code KEY=VALUE} entries; a scalar yields a single element. An
     * unresolved ref yields an empty list (fail-open).
     */
    public static List<String> resolveAsList(JsonNode compose, String ref) {
        JsonNode node = resolve(compose, ref);
        List<String> out = new ArrayList<>();
        if (node == null || node.isNull()) {
            return out;
        }
        if (node.isArray()) {
            node.forEach(e -> out.add(e.asText()));
        } else if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                out.add(e.getKey() + "=" + e.getValue().asText());
            }
        } else {
            out.add(node.asText());
        }
        return out;
    }

    // Splits on '.' only at parenthesis-depth 0, so dots inside svcImgRegex(<regex>) stay intact.
    private static List<String> splitPath(String path) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (c == '.' && depth == 0) {
                segments.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        segments.add(current.toString());
        return segments;
    }

}

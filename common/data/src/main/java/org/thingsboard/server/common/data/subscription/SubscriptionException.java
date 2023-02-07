/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public class SubscriptionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper mapper = new ObjectMapper();

    private SubscriptionErrorCode errorCode;
    private SubscriptionEntry entry;
    private JsonNode value;

    public SubscriptionException() {
        super();
    }

    public SubscriptionException(String message,
                                 SubscriptionErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SubscriptionException(String message,
                                 SubscriptionErrorCode errorCode,
                                 Map<SubscriptionEntry, Long> entitiesCountsInfo) {
        super(message);
        this.errorCode = errorCode;
        this.value = mapper.valueToTree(entitiesCountsInfo);
    }

    public SubscriptionException(String message,
                                 SubscriptionErrorCode errorCode,
                                 JsonNode value) {
        super(message);
        this.errorCode = errorCode;
        this.value = value;
    }

    public SubscriptionException(String message, SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        super(message);
        setValues(errorCode, entry, value);
    }

    public SubscriptionException(String message, Throwable cause, SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        super(message, cause);
        setValues(errorCode, entry, value);
    }

    public SubscriptionException(Throwable cause, SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        super(cause);
        setValues(errorCode, entry, value);
    }

    public SubscriptionException(SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        super();
        setValues(errorCode, entry, value);
    }

    private void setValues(SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        this.errorCode = errorCode;
        this.entry = entry;
        this.value = mapper.createObjectNode();
        ((ObjectNode) this.value).put("value", value);
    }

    public SubscriptionErrorCode getErrorCode() {
        return errorCode;
    }

    public SubscriptionEntry getEntry() {
        return entry;
    }

    public JsonNode getValue() {
        return value;
    }

}

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
package org.thingsboard.server.common.data.trendz;

import lombok.Getter;

@Getter
public enum TrendzSynchronizationResultType {

    SYNC_NOT_INITIALIZED("Trendz synchronization is not initialized."),

    SYNC_COMPLETED("Synchronization completed successfully."),

    SYNC_DISABLED("Synchronization is disabled by Trendz configuration."),

    TRENDZ_UNSUPPORTED_VERSION("Trendz version is not supported."),

    TRENDZ_AUTH_INVALID("Trendz authentication failed. Invalid or missing Trendz API key."),

    TRENDZ_URL_UNREACHABLE("Provided Trendz URL is not reachable."),

    TB_URL_MISMATCH("Provided ThingsBoard URL does not match the one stored in ThingsBoard."),

    TB_URL_UNREACHABLE("ThingsBoard URL is not reachable."),

    TB_AUTH_INVALID("ThingsBoard authentication failed. Invalid API key."),

    SYNC_INTERNAL_ERROR("Unexpected internal synchronization error.");

    private final String message;

    TrendzSynchronizationResultType(String message) {
        this.message = message;
    }

}

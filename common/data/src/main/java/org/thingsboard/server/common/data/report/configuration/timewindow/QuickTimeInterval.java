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
package org.thingsboard.server.common.data.report.configuration.timewindow;

public enum QuickTimeInterval {
    YESTERDAY, DAY_BEFORE_YESTERDAY, THIS_DAY_LAST_WEEK, PREVIOUS_WEEK, PREVIOUS_WEEK_ISO, PREVIOUS_MONTH, PREVIOUS_QUARTER,
    PREVIOUS_HALF_YEAR, PREVIOUS_YEAR, CURRENT_HOUR, CURRENT_DAY, CURRENT_DAY_SO_FAR, CURRENT_WEEK, CURRENT_WEEK_ISO,
    CURRENT_WEEK_SO_FAR, CURRENT_WEEK_ISO_SO_FAR, CURRENT_MONTH, CURRENT_MONTH_SO_FAR, CURRENT_QUARTER, CURRENT_QUARTER_SO_FAR,
    CURRENT_HALF_YEAR, CURRENT_HALF_YEAR_SO_FAR, CURRENT_YEAR, CURRENT_YEAR_SO_FAR;
}

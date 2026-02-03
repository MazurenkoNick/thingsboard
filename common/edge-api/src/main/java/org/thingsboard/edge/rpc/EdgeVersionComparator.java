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
package org.thingsboard.edge.rpc;

import org.thingsboard.server.gen.edge.v1.EdgeVersion;

import java.util.Comparator;

public class EdgeVersionComparator implements Comparator<EdgeVersion> {

    public static final EdgeVersionComparator INSTANCE = new EdgeVersionComparator();

    @Override
    public int compare(EdgeVersion v1, EdgeVersion v2) {
        if (v1 == v2) {
            return 0;
        }
        // UNRECOGNIZED is less than any other version
        if (v1 == EdgeVersion.UNRECOGNIZED) {
            return -1;
        }
        if (v2 == EdgeVersion.UNRECOGNIZED) {
            return 1;
        }
        // V_LATEST is treated as the newest version
        if (v1 == EdgeVersion.V_LATEST) {
            v1 = getNewestEdgeVersion();
        }
        if (v2 == EdgeVersion.V_LATEST) {
            v2 = getNewestEdgeVersion();
        }
        return compareVersionParts(parseVersionParts(v1), parseVersionParts(v2));
    }

    public static EdgeVersion getNewestEdgeVersion() {
        EdgeVersion newest = null;
        for (EdgeVersion v : EdgeVersion.values()) {
            if (v == EdgeVersion.V_LATEST || v == EdgeVersion.UNRECOGNIZED) {
                continue;
            }
            if (newest == null || INSTANCE.compare(v, newest) > 0) {
                newest = v;
            }
        }
        return newest;
    }

    private static int[] parseVersionParts(EdgeVersion version) {
        String name = version.name();
        if (name.startsWith("V_")) {
            name = name.substring(2);
        }
        String[] parts = name.split("_");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }

    private static int compareVersionParts(int[] a, int[] b) {
        int maxLen = Math.max(a.length, b.length);
        for (int i = 0; i < maxLen; i++) {
            int partA = i < a.length ? a[i] : 0;
            int partB = i < b.length ? b[i] : 0;
            if (partA != partB) {
                return Integer.compare(partA, partB);
            }
        }
        return 0;
    }

}

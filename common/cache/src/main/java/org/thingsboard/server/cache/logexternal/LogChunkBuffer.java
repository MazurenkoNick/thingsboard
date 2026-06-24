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
package org.thingsboard.server.cache.logexternal;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;

import java.util.List;

public interface LogChunkBuffer {

    /**
     * Appends a chunk for the given unit and returns the monotonically increasing
     * seq assigned by the buffer. Seq is unit-scoped and survives gRPC session changes.
     */
    long append(TenantId tenantId, EntityId unitId, LogChunk chunk);

    /**
     * Returns retained chunks within the given seq range. Used by the watermark
     * broadcast path to fetch the delta since the last watermark; chunks evicted
     * past the per-unit cap are simply absent from the result.
     */
    List<LogChunk> range(TenantId tenantId, EntityId unitId, long fromSeqInclusive, long toSeqInclusive);

    /**
     * Returns the highest seq currently in the retained tail for this unit,
     * or 0 if the tail is empty.
     */
    long latestTailSeq(TenantId tenantId, EntityId unitId);

    /**
     * Returns the {@code lastLineTs} of the most recent retained chunk for this
     * unit, or 0 if no tail is retained.
     */
    long latestTailLineTs(TenantId tenantId, EntityId unitId);

    /**
     * Drops all retained chunks and the seq counter for the given unit.
     * Called when the unit itself is removed so retained logs do not outlive it.
     */
    void deleteUnit(TenantId tenantId, EntityId unitId);

}

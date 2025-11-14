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
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class TbAbstractDuplicateMsgNode<C> extends TbAbstractTransformNode<C> {

    protected ListenableFuture<List<TbMsg>> duplicate(TbContext ctx, TbMsg msg) {
        ListenableFuture<List<EntityId>> newOriginatorsFuture = getNewOriginators(ctx, msg);
        return Futures.transform(newOriginatorsFuture, newOriginators -> {
            if (newOriginators == null || newOriginators.isEmpty()) {
                return Collections.emptyList();
            }
            if (newOriginators.size() == 1) {
                return Collections.singletonList(ctx.transformMsgOriginator(msg, newOriginators.get(0)));
            } else {
                return newOriginators.stream()
                        .map(newOriginator -> duplicateMsgForOriginator(msg, newOriginator))
                        .toList();
            }
        }, ctx.getDbCallbackExecutor());
    }

    private static TbMsg duplicateMsgForOriginator(TbMsg originalMsg, EntityId newOriginator) {
        return originalMsg.transform()
                .id(UUID.randomUUID()) // effectively creating a new message
                .originator(newOriginator)
                .callback(TbMsgCallback.EMPTY)
                .build();
    }

    protected abstract ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, TbMsg msg);

}

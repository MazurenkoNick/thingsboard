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
package org.thingsboard.server.common.msg;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TbMsgProcessingCtxTest {

    private final RuleChainId RULE_CHAIN_ID = new RuleChainId(UUID.fromString("b87c4123-f9f2-41a6-9a09-e3a5b6580b11"));
    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("1ca5e2ef-1309-41d9-bafa-709e9df0e2a6"));

    @Test
    void givenEmptyStack_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

    @Test
    void givenStackWithDifferentEntry_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

    @Test
    void givenStackWithMatchingEntry_whenIsAlreadyInStack_thenReturnTrue() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isTrue();
    }

    @Test
    void givenStackWithMatchingEntryAmongOthers_whenIsAlreadyInStack_thenReturnTrue() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);
        ctx.push(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isTrue();
    }

    @Test
    void givenStackWithSameChainButDifferentNode_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(RULE_CHAIN_ID, new RuleNodeId(UUID.randomUUID()));

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

    @Test
    void givenStackWithSameNodeButDifferentChain_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(new RuleChainId(UUID.randomUUID()), RULE_NODE_ID);

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

    @Test
    void givenStackWithEntryThenPopped_whenIsAlreadyInStack_thenReturnFalse() {
        TbMsgProcessingCtx ctx = new TbMsgProcessingCtx();
        ctx.push(RULE_CHAIN_ID, RULE_NODE_ID);
        ctx.pop();

        assertThat(ctx.isAlreadyInStack(RULE_CHAIN_ID, RULE_NODE_ID)).isFalse();
    }

}

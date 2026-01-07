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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.NoSuchElementException;

import static com.google.common.util.concurrent.Futures.immediateFuture;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "customer attributes",
        configClazz = TbGetCustomerAttributeNodeConfiguration.class,
        version = 2,
        nodeDescription = "Adds message originator customer attributes or latest telemetry into message or message metadata",
        nodeDetails = "Useful in multi-customer solutions where each customer has a different configuration or threshold set " +
                "that is stored as customer attributes or telemetry data and used for dynamic message filtering, transformation, " +
                "or actions such as alarm creation if the threshold is exceeded.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbEnrichmentNodeCustomerAttributesConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/enrichment/customer-attributes/"
)
public class TbGetCustomerAttributeNode extends TbAbstractGetEntityDataNode<CustomerId> {

    private boolean preserveOriginatorIfCustomer;

    @Override
    protected TbGetCustomerAttributeNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetCustomerAttributeNodeConfiguration.class);
        checkIfMappingIsNotEmptyOrElseThrow(config.getDataMapping());
        checkDataToFetchSupportedOrElseThrow(config.getDataToFetch());
        preserveOriginatorIfCustomer = config.isPreserveOriginatorIfCustomer();
        return config;
    }

    @Override
    protected ListenableFuture<CustomerId> findEntityAsync(TbContext ctx, EntityId originator) {
        if (preserveOriginatorIfCustomer && originator.getEntityType() == EntityType.CUSTOMER) {
            return immediateFuture((CustomerId) originator);
        }
        return ctx.getEntityService().fetchEntityCustomerIdAsync(ctx.getTenantId(), originator)
                .transform(customerIdOpt -> {
                    if (customerIdOpt.isEmpty()) {
                        throw new NoSuchElementException("Originator not found");
                    }
                    if (customerIdOpt.get().isNullUid()) {
                        throw new IllegalStateException("Originator is not assigned to any customer");
                    }
                    return customerIdOpt.get();
                }, ctx.getDbCallbackExecutor());
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        ObjectNode config = (ObjectNode) oldConfiguration;
        switch (fromVersion) {
            case 0:
                config = upgradeConfigToUseFetchToAndDataToFetch((ObjectNode) oldConfiguration);
                hasChanges = true;
            case 1:
                if (!config.has("preserveOriginatorIfCustomer")) {
                    config.put("preserveOriginatorIfCustomer", true);
                    hasChanges = true;
                }
                break;
        }
        return new TbPair<>(hasChanges, config);
    }

}

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
package org.thingsboard.server.common.data.cf.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.relation.RelationPathLevel;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PropagationCalculatedFieldConfiguration extends BaseCalculatedFieldConfiguration implements HasRelationPathLevel {

    public static final String PROPAGATION_CONFIG_ARGUMENT = "propagationCtx";

    @Valid
    @NotNull
    private RelationPathLevel relation;

    private boolean applyExpressionToResolvedArguments;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.PROPAGATION;
    }

    @Override
    public void validate() {
        baseCalculatedFieldRestriction();
        propagationRestriction();
        if (!applyExpressionToResolvedArguments) {
            arguments.forEach((name, argument) -> {
                if (!currentEntitySource(argument)) {
                    throw new IllegalArgumentException("Arguments in 'Arguments only' propagation mode support only the 'Current entity' source entity type!");
                }
                if (argument.getRefEntityKey() == null) {
                    throw new IllegalArgumentException("Argument: '" + name + "' doesn't have reference entity key configured!");
                }
                if (argument.getRefEntityKey().getType() == ArgumentType.TS_ROLLING) {
                    throw new IllegalArgumentException("Argument type: 'Time series rolling' detected for argument: '" + name + "'. " +
                                                       "Only 'Attribute' or 'Latest telemetry' arguments are allowed for 'Arguments only' propagation mode!");
                }
            });
        } else {
            boolean noneMatchCurrentEntitySource = arguments.entrySet()
                    .stream()
                    .noneMatch(entry -> currentEntitySource(entry.getValue()));
            if (noneMatchCurrentEntitySource) {
                throw new IllegalArgumentException("At least one argument must be configured with the 'Current entity' " +
                                                   "source entity type for 'Expression result' propagation mode!");
            }
            if (StringUtils.isBlank(expression)) {
                throw new IllegalArgumentException("Expression must be specified for 'Expression result' propagation mode!");
            }
        }
    }

    public Argument toPropagationArgument() {
        var refDynamicSourceConfiguration = new RelationPathQueryDynamicSourceConfiguration();
        refDynamicSourceConfiguration.setLevels(List.of(relation));
        var propagationArgument = new Argument();
        propagationArgument.setRefDynamicSourceConfiguration(refDynamicSourceConfiguration);
        return propagationArgument;
    }

    private void propagationRestriction() {
        if (arguments.entrySet().stream().anyMatch(entry -> entry.getKey().equals(PROPAGATION_CONFIG_ARGUMENT))) {
            throw new IllegalArgumentException("Argument name '" + PROPAGATION_CONFIG_ARGUMENT + "' is reserved and cannot be used.");
        }
    }

    private boolean currentEntitySource(Argument argument) {
        return argument.getRefEntityId() == null && argument.getRefDynamicSourceConfiguration() == null;
    }

}

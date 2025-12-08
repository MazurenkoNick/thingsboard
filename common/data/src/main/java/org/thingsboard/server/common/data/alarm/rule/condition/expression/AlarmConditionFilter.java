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
package org.thingsboard.server.common.data.alarm.rule.condition.expression;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.ComplexFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.FilterPredicateType;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.EntityKeyValueType;

import java.io.Serializable;
import java.util.List;

@Data
public class AlarmConditionFilter implements Serializable {

    @NotBlank
    private String argument;
    @NotNull
    private EntityKeyValueType valueType;
    private ComplexOperation operation;
    @Valid
    @NotEmpty
    private List<KeyFilterPredicate> predicates;

    public boolean hasPredicate(FilterPredicateType type) {
        return containsPredicate(predicates, type);
    }

    private boolean containsPredicate(List<KeyFilterPredicate> predicates, FilterPredicateType type) {
        return predicates.stream().anyMatch(predicate -> {
            if (predicate instanceof ComplexFilterPredicate complexPredicate) {
                return containsPredicate(complexPredicate.getPredicates(), type);
            } else {
                return predicate.getType() == type;
            }
        });
    }

}

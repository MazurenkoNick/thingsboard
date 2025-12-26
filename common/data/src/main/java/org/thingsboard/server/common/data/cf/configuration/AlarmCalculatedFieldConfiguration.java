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
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static java.util.Map.Entry.comparingByKey;

@Data
public class AlarmCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration {

    private Map<String, Argument> arguments;

    @Valid
    @NotEmpty
    private Map<AlarmSeverity, AlarmRule> createRules;
    @Valid
    private AlarmRule clearRule;

    private boolean propagate;
    private boolean propagateToOwner;
    private boolean propagateToOwnerHierarchy;
    private boolean propagateToTenant;
    private List<String> propagateRelationTypes;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.ALARM;
    }

    @Override
    public Output getOutput() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean requiresScheduledReevaluation() {
        return getAllRules().anyMatch(entry -> entry.getValue().requiresScheduledReevaluation());
    }

    @JsonIgnore
    public Stream<Pair<AlarmSeverity, AlarmRule>> getAllRules() {
        Stream<Pair<AlarmSeverity, AlarmRule>> rules = createRules.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()));
        if (clearRule != null) {
            rules = Stream.concat(rules, Stream.of(Pair.of(null, clearRule)));
        }
        return rules.sorted(comparingByKey(Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public boolean rulesEqual(AlarmCalculatedFieldConfiguration other, BiPredicate<AlarmRule, AlarmRule> equalityCheck) {
        List<Pair<AlarmSeverity, AlarmRule>> thisRules = this.getAllRules().toList();
        List<Pair<AlarmSeverity, AlarmRule>> otherRules = other.getAllRules().toList();
        return CollectionsUtil.elementsEqual(thisRules, otherRules, (thisRule, otherRule) -> {
            if (!Objects.equals(thisRule.getKey(), otherRule.getKey())) {
                return false;
            }
            return equalityCheck.test(thisRule.getValue(), otherRule.getValue());
        });
    }

    public boolean propagationSettingsEqual(AlarmCalculatedFieldConfiguration other) {
        return this.propagate == other.propagate &&
               this.propagateToOwner == other.propagateToOwner &&
               this.propagateToOwnerHierarchy == other.propagateToOwnerHierarchy &&
               this.propagateToTenant == other.propagateToTenant &&
               Objects.equals(this.propagateRelationTypes, other.propagateRelationTypes);
    }

}

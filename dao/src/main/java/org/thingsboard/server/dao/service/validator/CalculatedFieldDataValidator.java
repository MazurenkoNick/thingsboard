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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.RelationPathQueryDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.aggregation.single.EntityAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.cf.CalculatedFieldDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class CalculatedFieldDataValidator extends DataValidator<CalculatedField> {

    @Autowired
    private CalculatedFieldDao calculatedFieldDao;

    @Autowired
    private ApiLimitService apiLimitService;

    @Override
    protected void validateDataImpl(TenantId tenantId, CalculatedField calculatedField) {
        validateNumberOfArgumentsPerCF(tenantId, calculatedField);
        validateCalculatedFieldConfiguration(calculatedField);
        validateSchedulingConfiguration(tenantId, calculatedField);
        validateRelationQuerySourceArguments(tenantId, calculatedField);
        validateRelatedAggregationConfiguration(tenantId, calculatedField);
        validateEntityAggregationConfiguration(tenantId, calculatedField);
    }

    @Override
    protected void validateCreate(TenantId tenantId, CalculatedField calculatedField) {
        if (calculatedField.getType() == CalculatedFieldType.ALARM) {
            return;
        }
        long maxCFsPerEntity = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxCalculatedFieldsPerEntity);
        if (maxCFsPerEntity <= 0) {
            return;
        }
        if (calculatedFieldDao.countByEntityIdAndTypeNot(tenantId, calculatedField.getEntityId(), CalculatedFieldType.ALARM) >= maxCFsPerEntity) {
            throw new DataValidationException("Calculated fields per entity limit reached!");
        }
    }

    @Override
    protected CalculatedField validateUpdate(TenantId tenantId, CalculatedField calculatedField) {
        CalculatedField old = calculatedFieldDao.findById(calculatedField.getTenantId(), calculatedField.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing calculated field!");
        }
        return old;
    }

    private void validateNumberOfArgumentsPerCF(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField instanceof ArgumentsBasedCalculatedFieldConfiguration argumentsBasedCfg)) {
            return;
        }
        long maxArgumentsPerCF = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxArgumentsPerCF);
        if (maxArgumentsPerCF <= 0) {
            return;
        }
        if (argumentsBasedCfg.getArguments().size() > maxArgumentsPerCF) {
            throw new DataValidationException("Calculated field arguments limit reached!");
        }
    }

    private void validateCalculatedFieldConfiguration(CalculatedField calculatedField) {
        wrapAsDataValidation(calculatedField.getConfiguration()::validate);
    }

    private void validateSchedulingConfiguration(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField.getConfiguration() instanceof ScheduledUpdateSupportedCalculatedFieldConfiguration scheduledUpdateCfg)
                || !scheduledUpdateCfg.isScheduledUpdateEnabled()) {
            return;
        }
        long minAllowedScheduledUpdateInterval = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMinAllowedScheduledUpdateIntervalInSecForCF);
        wrapAsDataValidation(() -> scheduledUpdateCfg.validate(minAllowedScheduledUpdateInterval));
    }

    private void validateRelationQuerySourceArguments(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField.getConfiguration() instanceof ArgumentsBasedCalculatedFieldConfiguration argumentsBasedCfg)) {
            return;
        }
        Map<String, RelationPathQueryDynamicSourceConfiguration> relationQueryBasedArguments = argumentsBasedCfg.getArguments().entrySet()
                .stream()
                .filter(entry -> entry.getValue().hasRelationQuerySource())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (RelationPathQueryDynamicSourceConfiguration) entry.getValue().getRefDynamicSourceConfiguration()));
        if (relationQueryBasedArguments.isEmpty()) {
            return;
        }
        int maxRelationLevel = (int) apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxRelationLevelPerCfArgument);
        relationQueryBasedArguments.forEach((argumentName, relationQueryDynamicSourceConfiguration) ->
                wrapAsDataValidation(() -> relationQueryDynamicSourceConfiguration.validateMaxRelationLevel(argumentName, maxRelationLevel)));
    }

    private void validateRelatedAggregationConfiguration(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField.getConfiguration() instanceof RelatedEntitiesAggregationCalculatedFieldConfiguration aggConfiguration)) {
            return;
        }
        long minDeduplicationInterval = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMinAllowedDeduplicationIntervalInSecForCF);
        if (aggConfiguration.getDeduplicationIntervalInSec() < minDeduplicationInterval) {
            throw new IllegalArgumentException("Deduplication interval is less than configured " +
                    "minimum allowed interval in tenant profile: " + minDeduplicationInterval);
        }
    }

    private void validateEntityAggregationConfiguration(TenantId tenantId, CalculatedField calculatedField) {
        if (!(calculatedField.getConfiguration() instanceof EntityAggregationCalculatedFieldConfiguration aggConfiguration)) {
            return;
        }
        long minAggregationIntervalInSec = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMinAllowedAggregationIntervalInSecForCF);
        if (minAggregationIntervalInSec <= 0) {
            return;
        }
        if (aggConfiguration.getInterval().getCurrentIntervalDurationMillis() < TimeUnit.SECONDS.toMillis(minAggregationIntervalInSec)) {
            throw new IllegalArgumentException("Aggregation interval duration is less than configured " +
                    "minimum allowed aggregation interval in tenant profile: " + minAggregationIntervalInSec + " sec.");
        }
    }

    private static void wrapAsDataValidation(Runnable validation) {
        try {
            validation.run();
        } catch (IllegalArgumentException e) {
            throw new DataValidationException(e.getMessage(), e);
        }
    }

}

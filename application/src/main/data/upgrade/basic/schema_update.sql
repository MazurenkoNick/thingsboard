--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
--
-- NOTICE: All information contained herein is, and remains
-- the property of ThingsBoard, Inc. and its suppliers,
-- if any.  The intellectual and technical concepts contained
-- herein are proprietary to ThingsBoard, Inc.
-- and its suppliers and may be covered by U.S. and Foreign Patents,
-- patents in process, and are protected by trade secret or copyright law.
--
-- Dissemination of this information or reproduction of this material is strictly forbidden
-- unless prior written permission is obtained from COMPANY.
--
-- Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
-- managers or contractors who have executed Confidentiality and Non-disclosure agreements
-- explicitly covering such access.
--
-- The copyright notice above does not evidence any actual or intended publication
-- or disclosure  of  this source code, which includes
-- information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
-- ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
-- OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
-- THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
-- AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
-- THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
-- DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
-- OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
--

-- UPDATE TENANT PROFILE CONFIGURATION START

UPDATE tenant_profile
SET profile_data = jsonb_set(
    profile_data,
    '{configuration}',
    jsonb_build_object(
        'minAllowedScheduledUpdateIntervalInSecForCF', 60,
        'maxRelationLevelPerCfArgument', 10,
        'maxRelatedEntitiesToReturnPerCfArgument', 100,
        'minAllowedDeduplicationIntervalInSecForCF', 10,
        'minAllowedAggregationIntervalInSecForCF', 60,
        'intermediateAggregationIntervalInSecForCF', 300,
        'cfReevaluationCheckInterval', 60,
        'alarmsReevaluationInterval', 60
    )
    ||
    jsonb_strip_nulls(profile_data -> 'configuration')
)
WHERE NOT (
    jsonb_strip_nulls(profile_data -> 'configuration') ?& ARRAY[
        'minAllowedScheduledUpdateIntervalInSecForCF',
        'maxRelationLevelPerCfArgument',
        'maxRelatedEntitiesToReturnPerCfArgument',
        'minAllowedDeduplicationIntervalInSecForCF',
        'minAllowedAggregationIntervalInSecForCF',
        'intermediateAggregationIntervalInSecForCF',
        'cfReevaluationCheckInterval',
        'alarmsReevaluationInterval'
    ]
);

-- UPDATE TENANT PROFILE CONFIGURATION END

-- CALCULATED FIELD UNIQUE CONSTRAINT UPDATE START

ALTER TABLE calculated_field DROP CONSTRAINT IF EXISTS calculated_field_unq_key;
ALTER TABLE calculated_field ADD CONSTRAINT calculated_field_unq_key UNIQUE (entity_id, type, name);

-- CALCULATED FIELD UNIQUE CONSTRAINT UPDATE END

-- UPDATE CFS WITH CURRENT OWNER DYNAMIC SOURCE START

UPDATE calculated_field cf
SET configuration = (jsonb_set(cf.configuration::jsonb, '{arguments}',
                               (SELECT jsonb_object_agg(k,
                                    CASE
                                        WHEN v ->> 'refDynamicSource' = 'CURRENT_OWNER'
                                            THEN
                                            (v - 'refDynamicSource') ||
                                            jsonb_build_object(
                                                    'refDynamicSourceConfiguration',
                                                    jsonb_build_object('type', 'CURRENT_OWNER'))
                                        ELSE v END)
                                FROM jsonb_each(cf.configuration::jsonb -> 'arguments') AS e(k, v)),
                               true)::text)
WHERE (configuration::jsonb) ? 'arguments'
  AND EXISTS (SELECT 1
              FROM jsonb_each(configuration::jsonb -> 'arguments') AS e(k, v)
              WHERE v ->> 'refDynamicSource' = 'CURRENT_OWNER');

-- UPDATE CFS WITH CURRENT OWNER DYNAMIC SOURCE END

-- CALCULATED FIELD UNIQUE CONSTRAINT UPDATE START

ALTER TABLE calculated_field DROP CONSTRAINT IF EXISTS calculated_field_unq_key;
ALTER TABLE calculated_field ADD CONSTRAINT calculated_field_unq_key UNIQUE (entity_id, type, name);

-- CALCULATED FIELD UNIQUE CONSTRAINT UPDATE END

-- CALCULATED FIELD OUTPUT STRATEGY UPDATE START

UPDATE calculated_field
SET configuration = jsonb_set(
        configuration::jsonb,
        '{output}',
        (configuration::jsonb -> 'output')
            || jsonb_build_object(
                'strategy',
                jsonb_build_object(
                        'type', 'RULE_CHAIN'
                )
               ),
        false
                    )
WHERE (configuration::jsonb -> 'output' -> 'strategy') IS NULL;

-- CALCULATED FIELD OUTPUT STRATEGY UPDATE END

-- REMOVAL OF CALCULATED FIELD LINKS PERSISTENCE START

DROP TABLE IF EXISTS calculated_field_link;
ANALYZE calculated_field;

-- REMOVAL OF CALCULATED FIELD LINKS PERSISTENCE END

ALTER TABLE custom_menu ADD COLUMN IF NOT EXISTS user_group_names text[];

-- REMOVAL OF OLD TRENDZ SETTINGS

DELETE FROM admin_settings AS settings WHERE settings.key = 'trendz';

-- REMOVAL OF OLD TRENDZ SETTINGS END

-- UPGRADING WL settings with overrideTrendzName flag

UPDATE white_labeling SET settings = jsonb_set(settings::jsonb, '{overrideTrendzName}', 'true', true)::text
                      WHERE type = 'GENERAL' AND customer_id = '13814000-1dd2-11b2-8080-808080808080' AND (settings::jsonb -> 'platformName' <> 'null'::jsonb);

UPDATE white_labeling g SET settings = jsonb_set(settings::jsonb, '{overrideTrendzName}', 'true', true)::text
                      WHERE type = 'GENERAL' AND customer_id = '13814000-1dd2-11b2-8080-808080808080' AND EXISTS (
                      SELECT 1 from white_labeling l WHERE l.tenant_id = g.tenant_id AND l.type = 'LOGIN'
                                                       AND (l.settings::jsonb -> 'platformName' <> 'null'::jsonb));

-- tenant specific updates
UPDATE white_labeling SET settings = jsonb_set(settings::jsonb, '{overrideTrendzName}', 'true', true)::text
WHERE type = 'GENERAL' AND tenant_id <> '13814000-1dd2-11b2-8080-808080808080' AND customer_id = '13814000-1dd2-11b2-8080-808080808080'
  AND (settings::jsonb ->> 'hideConnectivityDialog')::boolean IS TRUE;

-- sys admin specific updates
UPDATE white_labeling SET settings = jsonb_set(settings::jsonb, '{overrideTrendzName}', 'true', true)::text
                      WHERE type = 'GENERAL' AND tenant_id = '13814000-1dd2-11b2-8080-808080808080' AND
                            ((settings::jsonb ->> 'enableHelpLinks')::boolean IS FALSE OR
                             ((settings::jsonb ->> 'enableHelpLinks')::boolean IS TRUE AND (settings::jsonb ->> 'helpLinkBaseUrl') <> 'https://thingsboard.io'));

-- UPGRADING WL settings with overrideTrendzName flag END
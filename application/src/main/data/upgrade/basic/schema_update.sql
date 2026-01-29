--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
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

-- UPDATE OTA PACKAGE EXTERNAL ID START

ALTER TABLE ota_package
    ADD COLUMN IF NOT EXISTS external_id uuid;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'ota_package_external_id_unq_key') THEN
            ALTER TABLE ota_package ADD CONSTRAINT ota_package_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

-- UPDATE OTA PACKAGE EXTERNAL ID END

-- UPDATE SCHEDULER_EVENT EXTERNAL ID START

ALTER TABLE scheduler_event
    ADD COLUMN IF NOT EXISTS external_id uuid;

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'scheduler_event_external_id_unq_key') THEN
            ALTER TABLE scheduler_event ADD CONSTRAINT scheduler_event_external_id_unq_key UNIQUE (tenant_id, external_id);
        END IF;
    END;
$$;

-- UPDATE SCHEDULER_EVENT EXTERNAL ID END

-- UPDATE NEW REPORT FEATURE START

UPDATE scheduler_event SET type = 'generateDashboardReport' WHERE type = 'generateReport';
ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS report_exec varchar(32) DEFAULT 'ENABLED';

-- UPDATE NEW REPORT FEATURE END

-- DROP INDEXES THAT DUPLICATE UNIQUE CONSTRAINT START

DROP INDEX IF EXISTS idx_device_external_id;
DROP INDEX IF EXISTS idx_device_profile_external_id;
DROP INDEX IF EXISTS idx_asset_external_id;
DROP INDEX IF EXISTS idx_entity_view_external_id;
DROP INDEX IF EXISTS idx_rule_chain_external_id;
DROP INDEX IF EXISTS idx_dashboard_external_id;
DROP INDEX IF EXISTS idx_customer_external_id;
DROP INDEX IF EXISTS idx_widgets_bundle_external_id;
-- PE
DROP INDEX IF EXISTS idx_converter_external_id;
DROP INDEX IF EXISTS idx_integration_external_id;
DROP INDEX IF EXISTS idx_role_external_id;

-- DROP INDEXES THAT DUPLICATE UNIQUE CONSTRAINT END

ALTER TABLE mobile_app ADD COLUMN IF NOT EXISTS title varchar(255);

DELETE FROM integration where type = 'IBM_WATSON_IOT';

DELETE FROM converter where integration_type = 'IBM_WATSON_IOT';

-- UPDATE EDGE LICENSE KEY TO SUPPORT OFFLINE FEATURE START

DROP VIEW IF EXISTS edge_info_view CASCADE;
DROP VIEW IF EXISTS edge_active_attribute_view CASCADE;
ALTER TABLE edge ALTER COLUMN edge_license_key TYPE varchar;

-- UPDATE EDGE LICENSE KEY TO SUPPORT OFFLINE FEATURE END

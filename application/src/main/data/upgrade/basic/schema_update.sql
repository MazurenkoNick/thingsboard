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

-- CLEANUP ORPHANED OTA PACKAGE LARGE OBJECTS START

-- This script cleans up orphaned PostgreSQL large objects that are no longer referenced by the ota_package table.
-- These orphaned objects accumulate when OTA packages are deleted or updated and can consume significant disk space.

DO
$$
DECLARE
    orphan_oid bigint;
    deleted_count int := 0;
    batch_deleted int;
    batch_size int := 1000;
    total_orphans int;
    iteration int := 0;
BEGIN
    SELECT COUNT(*) INTO total_orphans
    FROM pg_largeobject_metadata m LEFT JOIN ota_package p ON p.data = m.oid
    WHERE p.data IS NULL;

    RAISE NOTICE 'Found % orphaned large objects to clean up', total_orphans;

    IF total_orphans = 0 THEN
        RAISE NOTICE 'No orphaned large objects found';
        RETURN;
    END IF;

    LOOP
        iteration := iteration + 1;
        batch_deleted := 0;

        FOR orphan_oid IN
            SELECT m.oid
            FROM pg_largeobject_metadata m
            LEFT JOIN ota_package p ON p.data = m.oid
            WHERE p.data IS NULL
            LIMIT batch_size
        LOOP
            BEGIN
                PERFORM lo_unlink(orphan_oid);
                batch_deleted := batch_deleted + 1;
                deleted_count := deleted_count + 1;

                IF deleted_count % 1000 = 0 THEN
                    RAISE NOTICE 'Cleaned up % of % orphaned large objects...', deleted_count, total_orphans;
                END IF;

            EXCEPTION WHEN OTHERS THEN
                RAISE WARNING 'Failed to delete large object with OID %: %', orphan_oid, SQLERRM;
            END;
        END LOOP;

        EXIT WHEN batch_deleted = 0;

        IF iteration % 10 = 0 THEN
            RAISE NOTICE 'Completed % iterations, deleted % objects so far', iteration, deleted_count;
        END IF;
    END LOOP;

    RAISE NOTICE 'Successfully cleaned up all % orphaned large objects', deleted_count;
END;
$$;

-- CLEANUP ORPHANED OTA PACKAGE LARGE OBJECTS END

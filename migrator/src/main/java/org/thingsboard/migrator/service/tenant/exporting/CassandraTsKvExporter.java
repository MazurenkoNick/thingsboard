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
package org.thingsboard.migrator.service.tenant.exporting;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.migrator.MigrationService;
import org.thingsboard.migrator.Table;
import org.thingsboard.migrator.utils.CassandraService;
import org.thingsboard.migrator.utils.Storage;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${mode}' == 'TENANT_DATA_EXPORT' and ${export.cassandra.enabled} == true")
@Order(2)
public class CassandraTsKvExporter extends MigrationService {

    private final Storage storage;
    private final CassandraService cassandraService;

    @Value("${export.cassandra.ts.filter.start_ts:}")
    private Long startTs;
    @Value("${export.cassandra.ts.filter.end_ts:}")
    private Long endTs;
    @Value("${export.cassandra.ts.filter.partition_size_ms:2678400000}")
    private long partitionSizeMs;

    public static final String TS_KV_TABLE = "ts_kv_cf";
    public static final String TS_KV_PARTITIONS_TABLE = "ts_kv_partitions_cf";
    public static final String TS_KV_FILE = "ts_kv";

    private Writer writer;

    @Override
    protected void start() throws Exception {
        // Validate time filter once at start
        if (startTs != null && endTs != null && startTs > endTs) {
            log.error("Invalid time range configuration: startTs {} is greater than endTs {}. Aborting Cassandra TS export.", startTs, endTs);
            return;
        }
        if (startTs != null || endTs != null) {
            log.info("Cassandra TS export filter is active. startTs={}, endTs={}, partitionSizeMs={}", startTs, endTs, partitionSizeMs);
        }

        storage.newFile(TS_KV_FILE);
        writer = storage.newWriter(TS_KV_FILE);

        storage.readAndProcess(Table.LATEST_KV.getName(), latestKvRow -> {
            executor.submit(() -> {
                try {
                    getTsHistoryAndSave(latestKvRow);
                } catch (Exception e) {
                    log.error("Failed to retrieve timeseries history for {}", latestKvRow, e);
                }
            });
        });
    }

    private void getTsHistoryAndSave(Map<String, Object> latestKvRow) {
        String entityType = (String) latestKvRow.get("table_name");
        UUID entityId = (UUID) latestKvRow.get("entity_id");
        String key = (String) latestKvRow.get("key_name");

        // Use injected timestamp bounds (epoch millis)
        final Long startTs = this.startTs;
        final Long endTs = this.endTs;

        // Build partition-bounded query to minimize DB requests
        StringBuilder pQuery = new StringBuilder("SELECT partition FROM " + TS_KV_PARTITIONS_TABLE + " WHERE entity_type = ? AND entity_id = ? AND key = ?");
        List<Object> pArgs = new ArrayList<>();
        pArgs.add(entityType);
        pArgs.add(entityId);
        pArgs.add(key);
        if (startTs != null) {
            pQuery.append(" AND partition >= ?");
            pArgs.add(startTs - partitionSizeMs); // use partition size window
        }
        if (endTs != null) {
            pQuery.append(" AND partition <= ?");
            pArgs.add(endTs);
        }

        List<Long> partitions = cassandraService.query(pQuery.toString(), Long.class, pArgs.toArray());
        for (Long partition : partitions) {
            // Safety check to skip out-of-range partitions
            if (startTs != null && partition < (startTs - partitionSizeMs)) {
                continue;
            }
            if (endTs != null && partition > endTs) {
                continue;
            }

            StringBuilder query = new StringBuilder("SELECT * FROM " + TS_KV_TABLE + " WHERE entity_type = ? AND entity_id = ? AND key = ? AND partition = ?");
            List<Object> args = new ArrayList<>();
            args.add(entityType);
            args.add(entityId);
            args.add(key);
            args.add(partition);

            if (startTs != null) {
                query.append(" AND ts >= ?");
                args.add(startTs);
            }
            if (endTs != null) {
                query.append(" AND ts <= ?");
                args.add(endTs);
            }
            query.append(" ORDER BY ts");

            ResultSet rows = cassandraService.query(query.toString(), args.toArray());
            for (Row row : rows) {
                Map<String, Object> data = new HashMap<>();
                for (ColumnDefinition columnDefinition : row.getColumnDefinitions()) {
                    String column = columnDefinition.getName().toString();
                    Object value = row.getObject(columnDefinition.getName());
                    if (column.endsWith("_v") && value == null) {
                        continue;
                    }
                    data.put(column, value);
                }
                storage.addToFile(writer, data);
                reportProcessed(TS_KV_TABLE, data);
            }
        }
    }

    @Override
    protected void afterFinished() throws Exception {
        finishedProcessing(TS_KV_TABLE);
        writer.close();
    }

}

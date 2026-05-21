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

CREATE TABLE IF NOT EXISTS rule_node_exec_stats
(
    bucket_time        bigint NOT NULL,
    tenant_id          uuid   NOT NULL,
    queue_id           uuid   NOT NULL,
    rule_chain_id      uuid   NOT NULL,
    rule_node_id       uuid   NOT NULL,
    service_id         varchar(255) NOT NULL,
    exec_count         bigint NOT NULL DEFAULT 0,
    error_count        bigint NOT NULL DEFAULT 0,
    total_duration_ms  bigint NOT NULL DEFAULT 0,
    max_duration_ms    bigint NOT NULL DEFAULT 0,
    p95_duration_ms    bigint NOT NULL DEFAULT 0,
    CONSTRAINT rule_node_exec_stats_pkey
        PRIMARY KEY (bucket_time, queue_id, tenant_id, service_id, rule_chain_id, rule_node_id)
);

CREATE TABLE IF NOT EXISTS rule_node_queue_stats
(
    bucket_time               bigint       NOT NULL,
    queue_tenant_id           uuid         NOT NULL,
    tenant_id                 uuid         NOT NULL,
    queue_id                  uuid         NOT NULL,
    rule_chain_id             uuid         NOT NULL,
    last_visited_rule_node_id uuid         NOT NULL,
    service_id                varchar(255) NOT NULL,
    timeout_count             bigint       NOT NULL DEFAULT 0,
    failure_count             bigint       NOT NULL DEFAULT 0,
    success_count             bigint       NOT NULL DEFAULT 0,
    CONSTRAINT rule_node_queue_stats_pkey
        PRIMARY KEY (bucket_time, queue_tenant_id, queue_id, tenant_id, service_id, rule_chain_id, last_visited_rule_node_id)
);

CREATE TABLE IF NOT EXISTS rule_node_queue_lag_stats
(
    bucket_time     bigint NOT NULL,
    queue_tenant_id uuid   NOT NULL,
    queue_id        uuid   NOT NULL,
    lag             bigint NOT NULL DEFAULT 0,
    CONSTRAINT rule_node_queue_lag_stats_pkey
        PRIMARY KEY (bucket_time, queue_tenant_id, queue_id)
);

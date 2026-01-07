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

DROP VIEW IF EXISTS integration_info CASCADE;
CREATE OR REPLACE VIEW integration_info as
SELECT created_time, id, tenant_id, name, type, debug_settings, enabled, is_remote,
       allow_create_devices_or_assets, is_edge_template,
       (CASE WHEN i.enabled THEN
                 (SELECT cast(json_v as varchar)
                  FROM attribute_kv
                  WHERE entity_id = i.id
                    AND attribute_type = 2
                    AND attribute_key IN (select key_id from key_dictionary where key LIKE 'integration_status_%')
                  ORDER BY last_update_ts
                 LIMIT 1) END) as status
FROM integration i;

DROP VIEW IF EXISTS report_template_info_view CASCADE;
CREATE OR REPLACE VIEW report_template_info_view as
SELECT r.*,
       c.title as owner_name
FROM report_template r
         LEFT JOIN customer c ON c.id = r.customer_id;

DROP VIEW IF EXISTS dashboard_info_view CASCADE;
CREATE OR REPLACE VIEW dashboard_info_view as
SELECT d.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = d.id
                             and re.to_type = 'DASHBOARD'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM dashboard d
         LEFT JOIN customer c ON c.id = d.customer_id;

DROP VIEW IF EXISTS asset_info_view CASCADE;
CREATE OR REPLACE VIEW asset_info_view as
SELECT a.*,
       c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = a.id
                             and re.to_type = 'ASSET'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM asset a
         LEFT JOIN customer c ON c.id = a.customer_id;

DROP VIEW IF EXISTS device_info_active_attribute_view CASCADE;
CREATE OR REPLACE VIEW device_info_active_attribute_view as
SELECT d.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = d.id
                             and re.to_type = 'DEVICE'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups,
       COALESCE(da.bool_v, FALSE) as active
FROM device d
         LEFT JOIN customer c ON c.id = d.customer_id
         LEFT JOIN attribute_kv da ON da.entity_id = d.id AND da.attribute_type = 2 AND da.attribute_key = (select key_id from key_dictionary  where key = 'active');

DROP VIEW IF EXISTS device_info_active_ts_view CASCADE;
CREATE OR REPLACE VIEW device_info_active_ts_view as
SELECT d.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = d.id
                             and re.to_type = 'DEVICE'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups,
       COALESCE(dt.bool_v, FALSE) as active
FROM device d
         LEFT JOIN customer c ON c.id = d.customer_id
         LEFT JOIN ts_kv_latest dt ON dt.entity_id = d.id and dt.key = (select key_id from key_dictionary where key = 'active');

DROP VIEW IF EXISTS device_info_view CASCADE;
CREATE OR REPLACE VIEW device_info_view AS SELECT * FROM device_info_active_attribute_view;

DROP VIEW IF EXISTS entity_view_info_view CASCADE;
CREATE OR REPLACE VIEW entity_view_info_view as
SELECT e.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = e.id
                             and re.to_type = 'ENTITY_VIEW'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM entity_view e
         LEFT JOIN customer c ON c.id = e.customer_id;

DROP VIEW IF EXISTS customer_info_view CASCADE;
CREATE OR REPLACE VIEW customer_info_view as
SELECT c.*, c2.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = c.id
                             and re.to_type = 'CUSTOMER'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM customer c
         LEFT JOIN customer c2 ON c2.id = c.parent_customer_id;

DROP VIEW IF EXISTS user_info_view CASCADE;
CREATE OR REPLACE VIEW user_info_view as
SELECT u.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = u.id
                             and re.to_type = 'USER'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM tb_user u
         LEFT JOIN customer c ON c.id = u.customer_id;

DROP VIEW IF EXISTS edge_info_view CASCADE;
CREATE OR REPLACE VIEW edge_info_view as
SELECT e.*, c.title as owner_name,
       array_to_json(ARRAY(select json_build_object('id', from_id, 'name', eg.name)
                           from relation re,
               entity_group eg
                           where re.to_id = e.id
                             and re.to_type = 'EDGE'
                             and re.relation_type_group = 'FROM_ENTITY_GROUP'
                             and re.relation_type = 'Contains'
                             and eg.id = re.from_id
                             and eg.name != 'All'
                           order by eg.name)) as groups
FROM edge e
         LEFT JOIN customer c ON c.id = e.customer_id;

DROP VIEW IF EXISTS entity_group_info_view CASCADE;
CREATE OR REPLACE VIEW entity_group_info_view as
SELECT eg.*,
       array_to_json(ARRAY(WITH RECURSIVE owner_ids(id, type, lvl) AS
                                              (SELECT eg.owner_id id, eg.owner_type::varchar(15) as type, 1 as lvl
                                               UNION
                                               SELECT (CASE
                                                           WHEN ce2.parent_customer_id IS NULL OR ce2.parent_customer_id = '13814000-1dd2-11b2-8080-808080808080' THEN ce2.tenant_id
                                                           ELSE ce2.parent_customer_id END) as id,
                                                      (CASE
                                                           WHEN ce2.parent_customer_id IS NULL OR ce2.parent_customer_id = '13814000-1dd2-11b2-8080-808080808080' THEN 'TENANT'
                                                           ELSE 'CUSTOMER' END)::varchar(15) as type,
                                                      parent.lvl + 1 as lvl
                                               FROM customer ce2, owner_ids parent WHERE ce2.id = parent.id and eg.owner_type = 'CUSTOMER')
                           SELECT json_build_object('id', id, 'entityType', type) FROM owner_ids order by lvl)) owner_ids
FROM entity_group eg;

CREATE OR REPLACE VIEW owner_info_view as
(SELECT t.id as id, t.created_time as created_time, '13814000-1dd2-11b2-8080-808080808080'::uuid as tenant_id, 'TENANT' as entity_type, t.title as name, false as is_public from tenant t
UNION
SELECT c.id as id, c.created_time as created_time, c.tenant_id as tenant_id, 'CUSTOMER' as entity_type, c.title as name,
       (CASE
            WHEN c.additional_info is not null and c.additional_info::json ->> 'isPublic' = 'true' THEN true
            ELSE false END) as is_public
FROM customer c);

DROP VIEW IF EXISTS alarm_info CASCADE;
CREATE VIEW alarm_info AS
SELECT a.*,
       (CASE WHEN a.acknowledged AND a.cleared THEN 'CLEARED_ACK'
             WHEN NOT a.acknowledged AND a.cleared THEN 'CLEARED_UNACK'
             WHEN a.acknowledged AND NOT a.cleared THEN 'ACTIVE_ACK'
             WHEN NOT a.acknowledged AND NOT a.cleared THEN 'ACTIVE_UNACK' END) as status,
       COALESCE(CASE WHEN a.originator_type = 0 THEN (select title from tenant where id = a.originator_id)
                     WHEN a.originator_type = 1 THEN (select title from customer where id = a.originator_id)
                     WHEN a.originator_type = 2 THEN (select email from tb_user where id = a.originator_id)
                     WHEN a.originator_type = 3 THEN (select title from dashboard where id = a.originator_id)
                     WHEN a.originator_type = 4 THEN (select name from asset where id = a.originator_id)
                     WHEN a.originator_type = 5 THEN (select name from device where id = a.originator_id)
                     WHEN a.originator_type = 8 THEN (select name from converter where id = a.originator_id)
                     WHEN a.originator_type = 9 THEN (select name from integration where id = a.originator_id)
                     WHEN a.originator_type = 16 THEN (select name from entity_view where id = a.originator_id)
                     WHEN a.originator_type = 22 THEN (select name from device_profile where id = a.originator_id)
                     WHEN a.originator_type = 23 THEN (select name from asset_profile where id = a.originator_id)
                     WHEN a.originator_type = 27 THEN (select name from edge where id = a.originator_id) END
           , 'Deleted') originator_name,
       COALESCE(CASE WHEN a.originator_type = 0 THEN (select title from tenant where id = a.originator_id)
                     WHEN a.originator_type = 1 THEN (select COALESCE(NULLIF(title, ''), email) from customer where id = a.originator_id)
                     WHEN a.originator_type = 2 THEN (select email from tb_user where id = a.originator_id)
                     WHEN a.originator_type = 3 THEN (select title from dashboard where id = a.originator_id)
                     WHEN a.originator_type = 4 THEN (select COALESCE(NULLIF(label, ''), name) from asset where id = a.originator_id)
                     WHEN a.originator_type = 5 THEN (select COALESCE(NULLIF(label, ''), name) from device where id = a.originator_id)
                     WHEN a.originator_type = 8 THEN (select name from converter where id = a.originator_id)
                     WHEN a.originator_type = 9 THEN (select name from integration where id = a.originator_id)
                     WHEN a.originator_type = 16 THEN (select name from entity_view where id = a.originator_id)
                     WHEN a.originator_type = 22 THEN (select name from device_profile where id = a.originator_id)
                     WHEN a.originator_type = 23 THEN (select name from asset_profile where id = a.originator_id)
                     WHEN a.originator_type = 27 THEN (select COALESCE(NULLIF(label, ''), name) from edge where id = a.originator_id) END
           , 'Deleted') as originator_label,
       u.first_name as assignee_first_name, u.last_name as assignee_last_name, u.email as assignee_email
FROM alarm a
         LEFT JOIN tb_user u ON u.id = a.assignee_id;

DROP VIEW IF EXISTS edge_active_attribute_view CASCADE;
CREATE OR REPLACE VIEW edge_active_attribute_view AS
SELECT ee.id
        , ee.created_time
        , ee.additional_info
        , ee.customer_id
        , ee.root_rule_chain_id
        , ee.type
        , ee.name
        , ee.label
        , ee.routing_key
        , ee.secret
        , ee.tenant_id
        , ee.version
        , ee.edge_license_key
        , ee.cloud_endpoint
FROM edge ee
        JOIN attribute_kv ON ee.id = attribute_kv.entity_id
        JOIN key_dictionary ON attribute_kv.attribute_key = key_dictionary.key_id
WHERE attribute_kv.bool_v = true AND key_dictionary.key = 'active'
ORDER BY ee.id;

DROP VIEW IF EXISTS widget_type_info_view CASCADE;
CREATE OR REPLACE VIEW widget_type_info_view AS
SELECT t.*,
       COALESCE((t.descriptor::json->>'type')::text, '') as widget_type,
       array_to_json(ARRAY(
           SELECT json_build_object('id', wb.widgets_bundle_id, 'name', b.title)
           FROM widgets_bundle_widget wb
           JOIN widgets_bundle b ON wb.widgets_bundle_id = b.id
           WHERE wb.widget_type_id = t.id
           ORDER BY b.title
       )) AS bundles
FROM widget_type t;

DROP VIEW IF EXISTS scheduled_reports_info_view CASCADE;
CREATE OR REPLACE VIEW scheduled_reports_info_view AS
SELECT se.*,
    c.title AS customer_title,
    cfg.report_template_id AS report_template_id,
    rt.name AS report_template_name,
    cfg.user_id AS user_id,
    u.email AS user_name
FROM
    scheduler_event se
        LEFT JOIN LATERAL (
        SELECT
            (se.configuration::json #>>'{reportTemplateId,id}')::uuid AS report_template_id,
            (se.configuration::json #>>'{userId,id}')::uuid AS user_id
        ) cfg ON true
        LEFT JOIN report_template rt ON rt.id = cfg.report_template_id
        LEFT JOIN tb_user u ON u.id = cfg.user_id
        LEFT JOIN customer c ON c.id = se.customer_id
WHERE se.type = 'generateReport';

DROP VIEW IF EXISTS report_info_view CASCADE;
CREATE OR REPLACE VIEW report_info_view AS
SELECT r.*,
    c.title AS customer_title,
    rt.name AS report_template_name,
    u.email AS user_name
FROM
    report r
        LEFT JOIN report_template rt ON rt.id = r.template_id
        LEFT JOIN tb_user u ON u.id = r.user_id
        LEFT JOIN customer c ON c.id = r.customer_id;
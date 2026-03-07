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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;

@DaoSqlTest
public class AdminSettingsEdgeTest extends AbstractEdgeTest {

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Test
    public void testAdminSettings_sysAdmin() throws Exception {
        loginSysAdmin();

        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setKey("edgeTest");
        ObjectNode jsonValue = JacksonUtil.newObjectNode();
        jsonValue.put("key1", "value1");
        adminSettings.setJsonValue(jsonValue);

        edgeImitator.expectMessageAmount(1);
        AdminSettings savedAdminSettings = doPost("/api/admin/settings", adminSettings, AdminSettings.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AdminSettingsUpdateMsg);
        AdminSettingsUpdateMsg adminSettingsUpdateMsg = (AdminSettingsUpdateMsg) latestMessage;
        AdminSettings adminSettingsMsg = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
        Assert.assertNotNull(adminSettingsMsg);
        Assert.assertEquals("edgeTest", adminSettingsMsg.getKey());
        Assert.assertEquals("value1", adminSettingsMsg.getJsonValue().get("key1").asText());

        ObjectNode updatedJsonValue = (ObjectNode) savedAdminSettings.getJsonValue();
        updatedJsonValue.put("key2", "value2");
        savedAdminSettings.setJsonValue(updatedJsonValue);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/admin/settings", savedAdminSettings, AdminSettings.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AdminSettingsUpdateMsg);
        adminSettingsUpdateMsg = (AdminSettingsUpdateMsg) latestMessage;
        adminSettingsMsg = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
        Assert.assertNotNull(adminSettingsMsg);
        Assert.assertEquals("edgeTest", adminSettingsMsg.getKey());
        Assert.assertEquals("value1", adminSettingsMsg.getJsonValue().get("key1").asText());
        Assert.assertEquals("value2", adminSettingsMsg.getJsonValue().get("key2").asText());

        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(savedAdminSettings.getTenantId(), "edgeTest");
    }

    @Test
    public void testAdminSettings_tenant() throws Exception {
        loginTenantAdmin();

        AdminSettings tenantAdminSettings = new AdminSettings();
        tenantAdminSettings.setTenantId(tenantId);
        tenantAdminSettings.setKey("tenantEdgeTest");
        ObjectNode tenantJsonValue = JacksonUtil.newObjectNode();
        tenantJsonValue.put("tenantKey1", "tenantValue1");
        tenantAdminSettings.setJsonValue(tenantJsonValue);

        edgeImitator.expectMessageAmount(1);
        AdminSettings savedTenantAdminSettings = doPost("/api/admin/settings", tenantAdminSettings, AdminSettings.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AdminSettingsUpdateMsg);
        AdminSettingsUpdateMsg adminSettingsUpdateMsg = (AdminSettingsUpdateMsg) latestMessage;
        AdminSettings adminSettingsMsg = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
        Assert.assertNotNull(adminSettingsMsg);
        Assert.assertEquals("tenantEdgeTest", adminSettingsMsg.getKey());
        Assert.assertEquals("tenantValue1", adminSettingsMsg.getJsonValue().get("tenantKey1").asText());

        // TENANT update
        ObjectNode updatedTenantJsonValue = (ObjectNode) savedTenantAdminSettings.getJsonValue();
        updatedTenantJsonValue.put("tenantKey2", "tenantValue2");
        savedTenantAdminSettings.setJsonValue(updatedTenantJsonValue);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/admin/settings", savedTenantAdminSettings, AdminSettings.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AdminSettingsUpdateMsg);
        adminSettingsUpdateMsg = (AdminSettingsUpdateMsg) latestMessage;
        adminSettingsMsg = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
        Assert.assertNotNull(adminSettingsMsg);
        Assert.assertEquals("tenantEdgeTest", adminSettingsMsg.getKey());
        Assert.assertEquals("tenantValue1", adminSettingsMsg.getJsonValue().get("tenantKey1").asText());
        Assert.assertEquals("tenantValue2", adminSettingsMsg.getJsonValue().get("tenantKey2").asText());

        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(savedTenantAdminSettings.getTenantId(), "tenantEdgeTest");
    }

}

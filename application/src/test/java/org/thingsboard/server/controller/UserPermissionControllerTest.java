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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class UserPermissionControllerTest extends AbstractControllerTest {

    @Autowired
    private CustomerService customerService;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testDashboardPermission() throws Exception {
        loginCustomerAdminUser();

        EntityGroup dashboardGroup = new EntityGroup();
        dashboardGroup.setName("Entity Group");
        dashboardGroup.setType(EntityType.DASHBOARD);
        dashboardGroup.setOwnerId(customerId);
        dashboardGroup = doPost("/api/entityGroup", dashboardGroup, EntityGroup.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Customer dashboard");
        dashboard.setCustomerId(customerId);
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/entityGroup/" + dashboardGroup.getId() + "/addEntities", List.of(savedDashboard.getId().getId().toString()));

        EntityGroup userGroup = new EntityGroup();
        userGroup.setType(EntityType.USER);
        userGroup.setOwnerId(customerId);
        userGroup.setName("UserGroup");
        userGroup = doPost("/api/entityGroup", userGroup, EntityGroup.class);

        User readUser = new User();
        readUser.setAuthority(Authority.CUSTOMER_USER);
        readUser.setTenantId(tenantId);
        readUser.setCustomerId(customerId);
        readUser.setEmail("customerUser123@thingsboard.org");
        createUser(readUser, "customer", userGroup.getId());

        User noPermissionUser = new User();
        noPermissionUser.setAuthority(Authority.CUSTOMER_USER);
        noPermissionUser.setTenantId(tenantId);
        noPermissionUser.setCustomerId(customerId);
        noPermissionUser.setEmail("noPermissionUser123@thingsboard.org");
        createUser(noPermissionUser, "customer");

        Role groupRole = createReadRole("Read dashboard", RoleType.GROUP);
        groupRole = doPost("/api/role", groupRole, Role.class);

        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(groupRole.getId());
        groupPermission.setUserGroupId(userGroup.getId());
        groupPermission.setEntityGroupId(dashboardGroup.getId());
        groupPermission.setEntityGroupType(dashboardGroup.getType());

        doPost("/api/groupPermission", groupPermission, GroupPermission.class);

        loginUser("customerUser123@thingsboard.org", "customer");

        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/READ", Boolean.class)).isTrue();
        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/ADD_TO_GROUP", Boolean.class)).isFalse();

        // check user without permission has no access
        loginUser("noPermissionUser123@thingsboard.org", "customer");
        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/READ", Boolean.class)).isFalse();

    }

    @Test
    public void testPublicDashboardPermission() throws Exception {
        loginCustomerAdminUser();

        EntityGroup dashboardGroup = new EntityGroup();
        dashboardGroup.setName("Entity Group");
        dashboardGroup.setType(EntityType.DASHBOARD);
        dashboardGroup.setOwnerId(customerId);
        dashboardGroup = doPost("/api/entityGroup", dashboardGroup, EntityGroup.class);

        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Customer dashboard");
        dashboard.setCustomerId(customerId);
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/entityGroup/" + dashboardGroup.getId() + "/addEntities", List.of(savedDashboard.getId().getId().toString()));

        doPost("/api/entityGroup/" + dashboardGroup.getUuidId() + "/makePublic").andExpect(status().isOk());
        EntityGroup publicEntityGroup = doGet("/api/entityGroup/" + dashboardGroup.getUuidId(), EntityGroup.class);
        String publicCustomerId = publicEntityGroup.getAdditionalInfo().get("publicCustomerId").asText();

        //retrieve public dashboard
        resetTokens();

        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicCustomerId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        doGet("/dashboard/" + savedDashboard.getId() + "?publicId=" + publicCustomerId).andExpect(status().isOk());
        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/READ", Boolean.class)).isTrue();

        // login non-public user
        loginCustomerAdminUser();
        User noPermissionUser = new User();
        noPermissionUser.setAuthority(Authority.CUSTOMER_USER);
        noPermissionUser.setTenantId(tenantId);
        noPermissionUser.setCustomerId(customerId);
        noPermissionUser.setEmail("noPermissionUser123@thingsboard.org");
        createUser(noPermissionUser, "customer");
        login("noPermissionUser123@thingsboard.org", "customer");

        assertThat(doGet("/api/permission/DASHBOARD/" + savedDashboard.getId() + "/READ", Boolean.class)).isFalse();
    }

    private Role createReadRole(String roleName, RoleType roleType) {
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCustomerId(customerId);
        role.setName(roleName);
        role.setType(roleType);
        role.setPermissions(JacksonUtil.toJsonNode("[\"READ\", \"READ_ATTRIBUTES\", \"READ_TELEMETRY\", \"READ_CREDENTIALS\", \"READ_CALCULATED_FIELD\"]"));
        return role;
    }

}

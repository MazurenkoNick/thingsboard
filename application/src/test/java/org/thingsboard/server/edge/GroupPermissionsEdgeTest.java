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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.GroupPermissionProto;

import java.util.List;
import java.util.Map;

@DaoSqlTest
public class GroupPermissionsEdgeTest extends AbstractEdgeTest {

    @Test
    public void testSaveGroupPermissionWithGenericRole() throws Exception {
        EntityGroup userEntityGroup = createEntityGroupAndAssignToEdge(EntityType.USER, "testSaveGroupPermissionWithGenericRole", tenantId);
        Role genericRole = saveGenericRole();
        GroupPermission groupPermission = new GroupPermission();
        groupPermission.setRoleId(genericRole.getId());
        groupPermission.setUserGroupId(userEntityGroup.getId());
        groupPermission.setEntityGroupId(null);
        groupPermission.setEntityGroupType(null);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/groupPermission", groupPermission, GroupPermission.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof GroupPermissionProto);
        GroupPermissionProto groupPermissionProto = (GroupPermissionProto) latestMessage;
        GroupPermission result = JacksonUtil.fromString(groupPermissionProto.getEntity(), GroupPermission.class, true);
        Assert.assertNotNull(result);
        Assert.assertEquals(groupPermission.getRoleId(), result.getRoleId());
        Assert.assertEquals(groupPermission.getUserGroupId(), result.getUserGroupId());
    }

    private Role saveGenericRole() throws Exception {
        Map<Resource, List<Operation>> permissions = Map.of(Resource.OTA_PACKAGE, List.of(Operation.READ));
        Role genericRole = new Role();
        genericRole.setTenantId(tenantId);
        genericRole.setName("Read Generic Role");
        genericRole.setType(RoleType.GENERIC);
        genericRole.setPermissions(JacksonUtil.valueToTree(permissions));
        edgeImitator.expectMessageAmount(1);
        genericRole = doPost("/api/role", genericRole, Role.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        return genericRole;
    }

}

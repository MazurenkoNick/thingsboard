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
package org.thingsboard.server.service.apiusage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoUnit.MONTHS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@DaoSqlTest
public class DefaultTbApiUsageStateServiceTest extends AbstractControllerTest {

    @Autowired
    DefaultTbApiUsageStateService service;

    @Autowired
    private ApiUsageStateService apiUsageStateService;

    private TenantId tenantId;
    private Tenant savedTenant;
    private TenantProfile savedTenantProfile;

    private static final int MAX_ENABLE_VALUE = 5000;
    private static final long VALUE_WARNING = 4500L;
    private static final long VALUE_DISABLE = 5500L;
    private static final double WARN_THRESHOLD_VALUE = 0.8;

    @Before
    public void init() throws Exception {
        loginSysAdmin();

        TenantProfile tenantProfile = createTenantProfile();
        savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        savedTenant = saveTenant(tenant);
        tenantId = savedTenant.getId();
        Assert.assertNotNull(savedTenant);
    }

    @Test
    public void testProcess_transitionFromWarningToDisabled() {
        TransportProtos.ToUsageStatsServiceMsg.Builder warningMsgBuilder = TransportProtos.ToUsageStatsServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setCustomerIdMSB(0)
                .setCustomerIdLSB(0)
                .setServiceId("testService");

        warningMsgBuilder.addValues(TransportProtos.UsageStatsKVProto.newBuilder()
                .setKey(ApiUsageRecordKey.STORAGE_DP_COUNT.name())
                .setValue(VALUE_WARNING)
                .build());

        TransportProtos.ToUsageStatsServiceMsg warningStatsMsg = warningMsgBuilder.build();
        TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> warningMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), warningStatsMsg);

        service.process(warningMsg, TbCallback.EMPTY);
        assertEquals(ApiUsageStateValue.WARNING, apiUsageStateService.findTenantApiUsageState(tenantId).getDbStorageState());

        TransportProtos.ToUsageStatsServiceMsg.Builder disableMsgBuilder = TransportProtos.ToUsageStatsServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setCustomerIdMSB(0)
                .setCustomerIdLSB(0)
                .setServiceId("testService");

        disableMsgBuilder.addValues(TransportProtos.UsageStatsKVProto.newBuilder()
                .setKey(ApiUsageRecordKey.STORAGE_DP_COUNT.name())
                .setValue(VALUE_DISABLE)
                .build());

        TransportProtos.ToUsageStatsServiceMsg disableStatsMsg = disableMsgBuilder.build();
        TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> disableMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), disableStatsMsg);

        service.process(disableMsg, TbCallback.EMPTY);
        assertEquals(ApiUsageStateValue.DISABLED, apiUsageStateService.findTenantApiUsageState(tenantId).getDbStorageState());
    }

    @Test
    public void checkStartOfNextCycle_setsNextCycleToNextMonth() throws Exception {
        ApiUsageState apiUsageState = new ApiUsageState(new ApiUsageStateId(UUID.randomUUID()));
        apiUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        apiUsageState.setAlarmExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setSmsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTbelExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setReExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTransportState(ApiUsageStateValue.ENABLED);
        apiUsageState.setEmailExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setJsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTenantId(tenantId);
        apiUsageState.setEntityId(tenantId);

        long now = System.currentTimeMillis();
        long currentCycleTs = now - TimeUnit.DAYS.toMillis(30);
        long nextCycleTs = now - TimeUnit.MINUTES.toMillis(5); // < 1h ago
        TenantApiUsageState tenantApiUsageState = new TenantApiUsageState(savedTenantProfile, apiUsageState);
        tenantApiUsageState.setCycles(currentCycleTs, nextCycleTs);
        Map<EntityId, BaseApiUsageState> map = new HashMap<>();
        map.put(tenantId, tenantApiUsageState);

        Field fieldToSet = DefaultTbApiUsageStateService.class.getDeclaredField("myUsageStates");
        fieldToSet.setAccessible(true);
        fieldToSet.set(service, map);

        service.checkStartOfNextCycle();

        long firstOfNextMonth = LocalDate.now()
                .with((temporal) -> temporal.with(DAY_OF_MONTH, 1)
                        .plus(1, MONTHS))
                .atStartOfDay(UTC).toInstant().toEpochMilli();
        assertThat(tenantApiUsageState.getNextCycleTs()).isEqualTo(firstOfNextMonth);
    }

    private TenantProfile createTenantProfile() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Tenant Profile");
        tenantProfile.setDescription("Tenant Profile" + " Test");

        TenantProfileData tenantProfileData = new TenantProfileData();
        DefaultTenantProfileConfiguration config = DefaultTenantProfileConfiguration.builder()
                .maxDPStorageDays(MAX_ENABLE_VALUE)
                .warnThreshold(WARN_THRESHOLD_VALUE)
                .build();

        tenantProfileData.setConfiguration(config);
        tenantProfile.setProfileData(tenantProfileData);
        return tenantProfile;
    }

}
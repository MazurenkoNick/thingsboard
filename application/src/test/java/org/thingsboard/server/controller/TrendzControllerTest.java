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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.trendz.TrendzConfiguration;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResultType;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationStatus;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.dao.trendz.TrendzSyncService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class TrendzControllerTest extends AbstractControllerTest {

    @MockitoBean
    private TrendzSyncService trendzSyncService;

    @Autowired
    private TrendzSettingsService trendzSettingsService;

    private static final String TB_URL = "https://tb.example.com";
    private static final String TRENDZ_URL = "https://trendz.example.com";
    private static final String TRENDZ_VERSION = "1.15.0";

    @Before
    public void setUp() throws Exception {
        loginSysAdmin();
    }

    @Test
    public void testGetTrendzConfig_asSysAdmin() throws Exception {
        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        trendzSettingsService.saveTrendzSettings(settings);

        TrendzConfiguration result = doGet("/api/trendz/config", TrendzConfiguration.class);

        assertThat(result).isNotNull();
        assertThat(result.trendzUrl()).isEqualTo(TRENDZ_URL);
        assertThat(result.tbUrl()).isEqualTo(TB_URL);
    }

    @Test
    public void testGetTrendzConfig_asTenantAdmin_forbidden() throws Exception {
        loginTenantAdmin();

        doGet("/api/trendz/config").andExpect(status().isForbidden());
    }

    @Test
    public void testSaveTrendzConfig_asSysAdmin() {
        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);

        TrendzConfiguration result = doPost("/api/trendz/config", config, TrendzConfiguration.class);

        assertThat(result).isNotNull();
        assertThat(result.trendzUrl()).isEqualTo(TRENDZ_URL);
        assertThat(result.tbUrl()).isEqualTo(TB_URL);

        TrendzSettings savedSettings = trendzSettingsService.findTrendzSettings();
        assertThat(savedSettings).isNotNull();
        assertThat(savedSettings.configuration().trendzUrl()).isEqualTo(TRENDZ_URL);
        assertThat(savedSettings.configuration().tbUrl()).isEqualTo(TB_URL);
    }

    @Test
    public void testSaveTrendzConfig_asTenantAdmin_forbidden() throws Exception {
        loginTenantAdmin();

        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);

        doPost("/api/trendz/config", config).andExpect(status().isForbidden());
    }

    @Test
    public void testGetTrendzSync_asSysAdmin_whenNotExists_thenNull() throws Exception {
        TrendzSynchronizationResult result = doGet("/api/trendz/sync", TrendzSynchronizationResult.class);

        assertThat(result).isNotNull();
        assertThat(result.version()).isNull();
        assertEquals(0, result.updatedTs());
        assertThat(result.type()).isEqualTo(TrendzSynchronizationResultType.SYNC_NOT_INITIALIZED);
        assertThat(result.status()).isEqualTo(TrendzSynchronizationStatus.NOT_AVAILABLE);
    }

    @Test
    public void testGetTrendzSync_asSysAdmin() throws Exception {
        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        trendzSettingsService.saveTrendzSettings(settings);

        TrendzSynchronizationResult result = doGet("/api/trendz/sync", TrendzSynchronizationResult.class);

        assertThat(result).isNotNull();
        assertThat(result.version()).isEqualTo(TRENDZ_VERSION);
        assertThat(result.type()).isEqualTo(TrendzSynchronizationResultType.SYNC_COMPLETED);
        assertThat(result.status()).isEqualTo(TrendzSynchronizationStatus.SYNCED);
    }

    @Test
    public void testGetTrendzSync_asTenantAdmin() throws Exception {
        loginTenantAdmin();

        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        trendzSettingsService.saveTrendzSettings(settings);

        TrendzSynchronizationResult result = doGet("/api/trendz/sync", TrendzSynchronizationResult.class);

        assertThat(result).isNotNull();
        assertThat(result.version()).isEqualTo(TRENDZ_VERSION);
    }

    @Test
    public void testPerformTrendzHealthcheck_asSysAdmin() throws Exception {
        TrendzHealthcheckResult healthcheckResult = new TrendzHealthcheckResult(
                TRENDZ_VERSION,
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED,
                "Healthcheck passed"
        );

        when(trendzSyncService.performHealthcheck()).thenReturn(healthcheckResult);

        TrendzHealthcheckResult result = doGet("/api/trendz/healthcheck", TrendzHealthcheckResult.class);

        assertThat(result).isNotNull();
        assertThat(result.version()).isEqualTo(TRENDZ_VERSION);
        assertThat(result.message()).isEqualTo("Healthcheck passed");
    }

    @Test
    public void testPerformTrendzHealthcheck_asTenantAdmin() throws Exception {
        loginTenantAdmin();

        TrendzHealthcheckResult healthcheckResult = new TrendzHealthcheckResult(
                TRENDZ_VERSION,
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED,
                "Healthcheck passed"
        );

        when(trendzSyncService.performHealthcheck()).thenReturn(healthcheckResult);

        TrendzHealthcheckResult result = doGet("/api/trendz/healthcheck", TrendzHealthcheckResult.class);

        assertThat(result).isNotNull();
    }

    @Test
    public void testConnectToTrendz_asSysAdmin() {
        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        when(trendzSyncService.performSync()).thenReturn(settings);

        TrendzSynchronizationResult result = doPost("/api/trendz/connect", TrendzSynchronizationResult.class);

        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo(TrendzSynchronizationResultType.SYNC_COMPLETED);
        assertThat(result.status()).isEqualTo(TrendzSynchronizationStatus.SYNCED);
    }

    @Test
    public void testConnectToTrendz_asTenantAdmin_forbidden() throws Exception {
        loginTenantAdmin();

        doPost("/api/trendz/connect").andExpect(status().isForbidden());
    }

    @Test
    public void testPublicConnectToTrendz() throws Exception {
        logout();

        TrendzConfiguration config = new TrendzConfiguration(TRENDZ_URL, TB_URL);
        TrendzSynchronizationResult syncResult = new TrendzSynchronizationResult(
                TRENDZ_VERSION, System.currentTimeMillis(),
                TrendzSynchronizationResultType.SYNC_COMPLETED,
                TrendzSynchronizationStatus.SYNCED
        );
        TrendzSettings settings = new TrendzSettings(config, syncResult);

        when(trendzSyncService.performSync())
                .thenReturn(settings);

        String result = doPost("/api/trendz/public/connect", String.class);
        assertTrue(result.isEmpty());
    }
}

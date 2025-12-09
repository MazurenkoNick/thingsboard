/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 * <p>
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 * <p>
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * <p>
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 * <p>
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 * <p>
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

import org.junit.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.trendz.TrendzPaginationData;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzViewConfig;
import org.thingsboard.server.common.data.trendz.TrendzViewConfigLite;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.trendz.TrendzClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class TrendzApiControllerTest extends AbstractControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TrendzClient trendzClient;

    @Test
    public void testEndpoints_asSysAdmin() throws Exception {
        loginSysAdmin();
        doGet("/api/trendz/view/all", Map.of("page", 0, "pageSize", 10))
                .andExpect(status().isForbidden());
        doGet("/api/trendz/view/849a474d-9944-4e00-bb64-43ede180c6ee", Collections.emptyMap())
                .andExpect(status().isForbidden());
        doGet("/api/trendz/summary", Collections.emptyMap())
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetTrendzViews_asTenantAdmin() throws Exception {
        loginTenantAdmin();

        int pageSize = 10;
        int page = 2;
        String textSearch = "Trendz View";
        String sortProperty = "name";
        String sortOrder = "DESC";

        PageLink expectedPageLink = new PageLink(pageSize, page, textSearch, new SortOrder(sortProperty, SortOrder.Direction.valueOf(sortOrder)));
        List<TrendzViewConfigLite> trendzViews = List.of(
                new TrendzViewConfigLite(UUID.fromString("36dacff7-3282-4300-a7fe-a3e31259ab06"), "Trendz View 1"),
                new TrendzViewConfigLite(UUID.fromString("1d537953-bbb8-4961-a0ec-764d7d05ce9a"), "Trendz View 2"),
                new TrendzViewConfigLite(UUID.fromString("9d1381e1-05db-4e79-8c49-c4851701b0bc"), "Trendz View 3")
        );
        int totalPages = 5;
        long totalElements = 45;

        PageData<TrendzViewConfigLite> expected = new PageData<>(trendzViews, totalPages, totalElements, true);


        when(trendzClient.getAllTrendzViews(eq(expectedPageLink), any()))
                .thenReturn(new TrendzPaginationData<>(trendzViews, page, totalPages, totalElements));

        doGet("/api/trendz/view/all?page={page}&pageSize={pageSize}&textSearch={textSearch}&sortProperty={sortProperty}&sortOrder={sortOrder}",
                page, pageSize, textSearch, sortProperty, sortOrder
        ).andExpectAll(
                r -> assertEquals(200, r.getResponse().getStatus()),
                r -> assertEquals(expected, read(r, new TypeReference<PageData<TrendzViewConfigLite>>() {}))
        );
    }

    @Test
    public void testGetTrendzViewById_asTenantAdmin() throws Exception {
        loginTenantAdmin();

        UUID viewId = UUID.fromString("36dacff7-3282-4300-a7fe-a3e31259ab06");

        Map<String, Object> filter = Map.of(
                "name", "thermostat",
                "options", List.of("Thermostat T1")
        );
        TrendzViewConfig expected = new TrendzViewConfig(viewId, "Trendz View 1", List.of(filter));

        when(trendzClient.getTrendzViewById(eq(viewId), any()))
                .thenReturn(expected);

        doGet("/api/trendz/view/{viewId}", viewId)
                .andExpectAll(
                        r -> assertEquals(200, r.getResponse().getStatus()),
                        r -> assertEquals(expected, read(r, new TypeReference<TrendzViewConfig>() {}))
                );
    }

    @Test
    public void testGetTrendzSummary() throws Exception {
        loginTenantAdmin();

        TrendzSummary expected = new TrendzSummary(
            List.of(Map.of("itemName", "Thermostat T1")),
            List.of(Map.of("modelName", "Thermostat Anomaly Model")),
            List.of(Map.of("calculationName", "Thermostat Calculation Field")),
            List.of(Map.of("modelName", "Thermostat Prediction Model")),
            List.of(Map.of("viewName", "Trendz View 1")),
            List.of(Map.of("chatSummary", "Thermostat view builder"))
        );

        when(trendzClient.getTrendzSummary(any()))
                .thenReturn(expected);

        doGet("/api/trendz/summary")
                .andExpectAll(
                        r -> assertEquals(200, r.getResponse().getStatus()),
                        r -> assertEquals(expected, read(r, new TypeReference<TrendzSummary>() {}))
                );
    }

    private <T> T read(MvcResult r, TypeReference<T> typeReference) throws IOException {
        return objectMapper.readValue(
                r.getResponse().getContentAsString(),
                typeReference
        );
    }
}

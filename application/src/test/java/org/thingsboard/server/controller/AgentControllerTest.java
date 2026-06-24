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

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AgentControllerTest extends AbstractControllerTest {

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testSaveGetAndDeleteAgent() throws Exception {
        Agent saved = createAgent("Controller Agent");
        Assert.assertNotNull(saved.getId());
        Assert.assertEquals(tenantId, saved.getTenantId());

        Agent found = doGet("/api/agent/" + saved.getId().getId(), Agent.class);
        Assert.assertEquals(saved.getId(), found.getId());

        doDelete("/api/agent/" + saved.getId().getId()).andExpect(status().isOk());
        doGet("/api/agent/" + saved.getId().getId()).andExpect(status().is4xxClientError());
    }

    @Test
    public void testGetTenantAgents_paging() throws Exception {
        createAgent("Paging Agent A");
        createAgent("Paging Agent B");

        PageData<Agent> page = doGetTypedWithPageLink("/api/tenant/agents?",
                new TypeReference<>() {}, new PageLink(100));
        Assert.assertTrue(page.getTotalElements() >= 2);
    }

    @Test
    public void testDeleteAgent_customerForbidden() throws Exception {
        Agent saved = createAgent("Customer Forbidden Agent");

        loginCustomerUser();
        doDelete("/api/agent/" + saved.getId().getId()).andExpect(status().isForbidden());

        loginTenantAdmin();
    }

    @Test
    public void testGetAgent_crossTenantDenied() throws Exception {
        Agent saved = createAgent("Cross Tenant Agent");

        loginDifferentTenant();
        doGet("/api/agent/" + saved.getId().getId()).andExpect(status().is4xxClientError());

        loginTenantAdmin();
    }

    @Test
    public void testGetNonExistentAgent_notFound() throws Exception {
        doGet("/api/agent/" + UUID.randomUUID()).andExpect(status().is4xxClientError());
    }

    private Agent createAgent(String name) throws Exception {
        Agent agent = new Agent();
        agent.setName(name);
        agent.setRoutingKey(StringUtils.randomAlphanumeric(20));
        agent.setSecret(StringUtils.randomAlphanumeric(20));
        return doPost("/api/agent", agent, Agent.class);
    }
}

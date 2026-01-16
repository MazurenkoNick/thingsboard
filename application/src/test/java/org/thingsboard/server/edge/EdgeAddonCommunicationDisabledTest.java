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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.edge.exception.EdgeConnectionException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.subscription.SubscriptionServiceStub;

import static org.mockito.ArgumentMatchers.any;

@DaoSqlTest
public class EdgeAddonCommunicationDisabledTest extends AbstractEdgeTest {

    @MockitoSpyBean
    private SubscriptionServiceStub subscriptionService;

    @Test
    public void testAddonEdgeConnectionRejectedWhenCommunicationDisabled() {
        Mockito.when(subscriptionService.edgeEnabled(any(TenantId.class))).thenReturn(false);
        Mockito.when(subscriptionService.getLicenseVersion()).thenReturn(2);

        edgeImitator.expectClose();
        edgeImitator.connect();

        Assert.assertTrue("Edge imitator should be closed due to disabled edge add-on", edgeImitator.waitForClose());
        Assert.assertNotNull("Expected connection error", edgeImitator.getCloseException());
        Assert.assertTrue("Expected EdgeConnectionException but got: " + edgeImitator.getCloseException().getClass(),
                edgeImitator.getCloseException() instanceof EdgeConnectionException);
        Assert.assertTrue("Expected BAD_CREDENTIALS in error message, but was: " + edgeImitator.getCloseException().getMessage(),
                edgeImitator.getCloseException().getMessage() != null && edgeImitator.getCloseException().getMessage().contains("BAD_CREDENTIALS"));
    }
}



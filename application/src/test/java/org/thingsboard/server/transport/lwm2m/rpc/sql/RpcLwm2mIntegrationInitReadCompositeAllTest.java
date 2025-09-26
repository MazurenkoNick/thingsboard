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
package org.thingsboard.server.transport.lwm2m.rpc.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.thingsboard.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_2;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_6;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_7;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;

@Slf4j
public class RpcLwm2mIntegrationInitReadCompositeAllTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     "      \"/3_1.2/0/9\": \"batteryLevel\",     - Telemetry
     "      \"/3_1.2/0/20\": \"batteryStatus\"    - Observe, Telemetry
     "      \"/5_1.2/0/6\": \"pkgname\"           - Attributes
     "      \"/5_1.2/0/7\": \"pkgversion\"        - Attributes
     "      \"/5_1.2/0/9\": \"firmwareUpdateDeliveryMethod\"\  - Telemetry
     "      \"/19_1.1/0/2\": \"dataCreationTime\" - Telemetry
     *      "observeStrategy": 1
     */
    @Test
    public void testInitReadCompositeAsObserveStrategyCompositeAll() throws Exception {


        // init test
        String RESOURCE_3_9 = "batteryLevel";
        String RESOURCE_3_20 = "batteryStatus";
        String RESOURCE_5_6 = "pkgname";
        String RESOURCE_5_7 = "pkgversion";
        String RESOURCE_5_9 = "firmwareUpdateDeliveryMethod";
        String RESOURCE_19_2 = "dataCreationTime";

        String idVwr_3_0_20 = idVer_3_0_9 = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + 20;
        String IdVer5_0_6 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_6;
        String IdVer5_0_7 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_7;
        String IdVer5_0_9 = objectInstanceIdVer_5 + "/" + RESOURCE_ID_9;
        String idVer_19_0_2 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_2;
        countUpdateAttrTelemetryResource(idVer_3_0_9);
        countUpdateAttrTelemetryResource(idVwr_3_0_20);
        countUpdateAttrTelemetryResource(IdVer5_0_6);
        countUpdateAttrTelemetryResource(IdVer5_0_7);
        countUpdateAttrTelemetryResource(IdVer5_0_9);
        countUpdateAttrTelemetryResource(idVer_19_0_2);


        AtomicReference<ObjectNode> actualValues = new AtomicReference<>();
        await().atMost(40, SECONDS).until(() -> {
            actualValues.set(doGetAsync(
                    "/api/plugins/telemetry/DEVICE/" + lwM2MTestClient.getDeviceIdStr() + "/values/timeseries?keys="
                            + RESOURCE_3_9 + "," + RESOURCE_3_20 + "," + RESOURCE_5_9 + "," + RESOURCE_19_2, ObjectNode.class));
            return actualValues.get() != null && !actualValues.get().isEmpty()
                    && !actualValues.get().get(RESOURCE_3_9).isEmpty()
                    && !actualValues.get().get(RESOURCE_3_20).isEmpty()
                    && !actualValues.get().get(RESOURCE_5_9).isEmpty()
                    && !actualValues.get().get(RESOURCE_19_2).isEmpty();
        });

        AtomicReference<List<String>> actualKeys =new AtomicReference<>();
        await().atMost(40, SECONDS).until(() -> {
            actualKeys.set(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + lwM2MTestClient.getDeviceIdStr() + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {
            }));
            return actualKeys.get() != null && !actualKeys.get().isEmpty() && !actualKeys.get().isEmpty()
                    && actualKeys.get().contains(RESOURCE_5_6)&& actualKeys.get().contains(RESOURCE_5_7);
        });
    }
}

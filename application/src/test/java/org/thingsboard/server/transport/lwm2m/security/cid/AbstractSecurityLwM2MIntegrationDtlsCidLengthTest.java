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
package org.thingsboard.server.transport.lwm2m.security.cid;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.dtls.Connection;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.InMemoryReadWriteLockConnectionStore;
import org.eclipse.californium.scandium.dtls.ResumptionSupportingConnectionStore;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpoint;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.peer.IpPeer;
import org.junit.Assert;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_READ_CONNECTION_ID;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_WRITE_CONNECTION_ID;

@DaoSqlTest
@Slf4j
public abstract class AbstractSecurityLwM2MIntegrationDtlsCidLengthTest extends AbstractSecurityLwM2MIntegrationTest {

    protected String awaitAlias;

    protected void testNoSecDtlsCidLength(Integer clientDtlsCidLength, Integer serverDtlsCidLength) throws Exception {
        initDeviceCredentialsNoSek();
        basicTestConnectionDtlsCidLength(clientDtlsCidLength, serverDtlsCidLength);
    }
    protected void testPskDtlsCidLength(Integer clientDtlsCidLength, Integer serverDtlsCidLength) throws Exception {
        initDeviceCredentialsPsk();
        basicTestConnectionDtlsCidLength(clientDtlsCidLength, serverDtlsCidLength);
    }

    protected void basicTestConnectionDtlsCidLength(Integer clientDtlsCidLength,
                                                    Integer serverDtlsCidLength) throws Exception {
        DeviceProfile deviceProfile = createLwm2mDeviceProfile("profileFor" + clientEndpoint, transportConfiguration);
        final Device device = createLwm2mDevice(deviceCredentials, clientEndpoint, deviceProfile.getId());
        createNewClient(security, null, true, clientEndpoint, clientDtlsCidLength, device.getId().getId().toString());
        lwM2MTestClient.start(true);

        awaitUpdateReg(1);
        await(awaitAlias)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> lwM2MTestClient.getClientStates().contains(ON_UPDATE_SUCCESS));
        Assert.assertTrue(lwM2MTestClient.getClientStates().containsAll(expectedStatusesRegistrationLwm2mSuccess));

        Configuration clientCoapConfig = ((CaliforniumClientEndpoint)((CaliforniumClientEndpointsProvider)lwM2MTestClient
                .getLeshanClient().getEndpointsProvider().toArray()[0]).getEndpoints().toArray()[0]).getCoapEndpoint().getConfig();
        Assert.assertEquals(clientDtlsCidLength, clientCoapConfig.get(DTLS_CONNECTION_ID_LENGTH));

        if (security.equals(SECURITY_NO_SEC)) {
            Assert.assertTrue(lwM2MTestClient.getClientDtlsCid().isEmpty());
        } else {
            Assert.assertEquals(2L, lwM2MTestClient.getClientDtlsCid().size());
            Assert.assertTrue(lwM2MTestClient.getClientDtlsCid().containsKey(ON_READ_CONNECTION_ID));
            Assert.assertTrue(lwM2MTestClient.getClientDtlsCid().containsKey(ON_WRITE_CONNECTION_ID));

            LwM2mServer lwM2mServer = lwM2MTestClient.getLeshanClient().getRegisteredServers().entrySet().stream().findFirst().get().getValue();
            CaliforniumClientEndpoint  lwM2mClientEndpoint  = (CaliforniumClientEndpoint) lwM2MTestClient.getLeshanClient().getEndpoint(lwM2mServer);
            Connection connection = getConnection(lwM2mClientEndpoint, lwM2mServer);
            ConnectionId clientCid = connection.getConnectionId();
            ConnectionId readCid = connection.getEstablishedDtlsContext().getReadConnectionId();
            ConnectionId serverCid = connection.getEstablishedDtlsContext().getWriteConnectionId();
            if (serverDtlsCidLength == null || clientDtlsCidLength == null) {
                // cid is not used
                Assert.assertNull(lwM2MTestClient.getClientDtlsCid().get(ON_WRITE_CONNECTION_ID));
                Assert.assertNull(lwM2MTestClient.getClientDtlsCid().get(ON_READ_CONNECTION_ID));
                Assert.assertNull(readCid);
                Assert.assertNull(serverCid);
            } else {
                Assert.assertEquals(serverDtlsCidLength, lwM2MTestClient.getClientDtlsCid().get(ON_WRITE_CONNECTION_ID));
                Assert.assertEquals(clientDtlsCidLength, lwM2MTestClient.getClientDtlsCid().get(ON_READ_CONNECTION_ID));
                // cid used
                Assert.assertNotNull(clientCid);
                Assert.assertNotNull(readCid);
                if (clientDtlsCidLength > 0) {
                    Assert.assertEquals(clientCid, readCid);
                }
                Assert.assertNotNull(serverCid);
                int actualServerCidLength = serverCid.getBytes().length;
                int expectedServerCidLength = serverDtlsCidLength;
                Assert.assertEquals(expectedServerCidLength, actualServerCidLength);
            }

            if (clientCid != null) {
                int actualClientCidLength = clientCid.getBytes().length;
                int expectedClientCidLength;
                if (clientDtlsCidLength == null || clientDtlsCidLength == 0) {
                    expectedClientCidLength = 3;
                } else {
                    expectedClientCidLength = clientDtlsCidLength;
                }
                Assert.assertEquals(expectedClientCidLength, actualClientCidLength);
            }
        }
    }

    private static Connection getConnection(CaliforniumClientEndpoint lwM2mClientEndpoint, LwM2mServer lwM2mServer) throws NoSuchFieldException, IllegalAccessException {
        DTLSConnector connector = (DTLSConnector) lwM2mClientEndpoint.getCoapEndpoint().getConnector();
        Field field = DTLSConnector.class.getDeclaredField("connectionStore");
        field.setAccessible(true);
        ResumptionSupportingConnectionStore connectionStore = (InMemoryReadWriteLockConnectionStore) field.get(connector);
        InetSocketAddress serverAddr = ((IpPeer) lwM2mServer.getTransportData()).getSocketAddress();
        return connectionStore.get(serverAddr);
    }
}

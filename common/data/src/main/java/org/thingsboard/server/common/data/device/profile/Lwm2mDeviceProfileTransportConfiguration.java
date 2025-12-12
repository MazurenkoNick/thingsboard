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
package org.thingsboard.server.common.data.device.profile;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;

import static org.eclipse.leshan.core.LwM2m.Version.V1_0;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.SINGLE;

import java.util.Collections;
import java.util.List;

@Data
public class Lwm2mDeviceProfileTransportConfiguration implements DeviceProfileTransportConfiguration {

    private static final long serialVersionUID = 6257277825459600068L;

    private TelemetryMappingConfiguration observeAttr;
    private boolean bootstrapServerUpdateEnable;
    private List<LwM2MBootstrapServerCredential> bootstrap;
    private OtherConfiguration clientLwM2mSettings;

    public Lwm2mDeviceProfileTransportConfiguration() {
        updateDefault();
    }

    @Override
    public DeviceTransportType getType() {
        return DeviceTransportType.LWM2M;
    }

    private void updateDefault(){
        this.setBootstrap(Collections.emptyList());
        this.setClientLwM2mSettings(new OtherConfiguration(false,1, 1, 1, PowerMode.DRX, null, null, null, null, null, V1_0.toString()));
        this.setObserveAttr(new TelemetryMappingConfiguration(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyMap(), SINGLE));
    }
}

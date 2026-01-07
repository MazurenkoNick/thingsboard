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
package org.thingsboard.server.service.entitiy.ota.group;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.ota.OtaPackageStateService;

import java.util.Optional;

@Slf4j
@Service
@TbCoreComponent
@AllArgsConstructor
public class DefaultTbDeviceGroupOtaPackageService extends AbstractTbEntityService implements TbDeviceGroupOtaPackageService {

    private final OtaPackageStateService otaPackageStateService;
    private final DeviceGroupOtaPackageService deviceGroupOtaPackageService;

    @Override
    public DeviceGroupOtaPackage saveDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage, User user) throws Exception {
        DeviceGroupOtaPackage oldDeviceGroupOtaPackage = Optional.ofNullable(deviceGroupOtaPackage.getId())
                .map(deviceGroupOtaPackageService::findDeviceGroupOtaPackageById).orElse(null);

        DeviceGroupOtaPackage savedDeviceGroupOtaPackage = deviceGroupOtaPackageService.saveDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        otaPackageStateService.update(tenantId, savedDeviceGroupOtaPackage, oldDeviceGroupOtaPackage);
        autoCommit(user, EntityType.DEVICE, savedDeviceGroupOtaPackage.getGroupId());
        return savedDeviceGroupOtaPackage;
    }

    @Override
    public void deleteDeviceGroupOtaPackage(TenantId tenantId, DeviceGroupOtaPackage deviceGroupOtaPackage) {
        deviceGroupOtaPackageService.deleteDeviceGroupOtaPackage(tenantId, deviceGroupOtaPackage);
        otaPackageStateService.update(tenantId, null, deviceGroupOtaPackage);
    }

}

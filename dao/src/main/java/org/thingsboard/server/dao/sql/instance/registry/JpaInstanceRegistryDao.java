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
package org.thingsboard.server.dao.sql.instance.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.license.client.InstanceRegistry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.instance.registry.InstanceRegistryDao;
import org.thingsboard.server.dao.model.sql.InstanceRegistryEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaInstanceRegistryDao implements InstanceRegistryDao {

    private final InstanceRegistryRepository instanceRegistryRepository;

    @Override
    public InstanceRegistry save(InstanceRegistry instanceRegistry) {
        return DaoUtil.getData(instanceRegistryRepository.save(new InstanceRegistryEntity(instanceRegistry)));
    }

    @Override
    public InstanceRegistry findByServiceId(String serviceId) {
        return DaoUtil.getData(instanceRegistryRepository.findById(serviceId));
    }

    @Override
    public List<InstanceRegistry> findAll() {
        return DaoUtil.convertDataList(instanceRegistryRepository.findAll());
    }

    @Override
    public void deleteByServiceId(String serviceId) {
        instanceRegistryRepository.deleteById(serviceId);
    }

}

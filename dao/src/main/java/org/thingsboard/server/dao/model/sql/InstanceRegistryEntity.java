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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.license.client.InstanceRegistry;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@Entity
@Table(name = ModelConstants.INSTANCE_REGISTRY_TABLE_NAME)
@NoArgsConstructor
public class InstanceRegistryEntity implements ToData<InstanceRegistry> {

    @Id
    @Column(name = ModelConstants.INSTANCE_REGISTRY_SERVICE_ID_PROPERTY)
    private String serviceId;
    @Column(name = ModelConstants.CREATED_TIME_PROPERTY)
    private long createdTime;
    @Column(name = ModelConstants.INSTANCE_REGISTRY_LAST_ACTIVITY_TS_PROPERTY)
    private long lastActivityTs;

    public InstanceRegistryEntity(InstanceRegistry instanceRegistry) {
        this.serviceId = instanceRegistry.getServiceId();
        this.createdTime = instanceRegistry.getCreatedTime();
        this.lastActivityTs = instanceRegistry.getLastActivityTs();
    }

    @Override
    public InstanceRegistry toData() {
        InstanceRegistry instanceRegistry = new InstanceRegistry();
        instanceRegistry.setServiceId(serviceId);
        instanceRegistry.setCreatedTime(createdTime);
        instanceRegistry.setLastActivityTs(lastActivityTs);
        return instanceRegistry;
    }

}

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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.AGENT_APP_UNIT_TABLE_NAME)
public class AgentAppUnitEntity extends BaseSqlEntity<AgentAppUnit> {

    @Column(name = ModelConstants.AGENT_APP_UNIT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.AGENT_APP_UNIT_AGENT_APPLICATION_ID_PROPERTY)
    private UUID agentApplicationId;

    @Column(name = ModelConstants.AGENT_APP_UNIT_IDENTIFIER_PROPERTY)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.AGENT_APP_UNIT_TYPE_PROPERTY)
    private AgentAppUnitType type;

    public AgentAppUnitEntity() {
        super();
    }

    public AgentAppUnitEntity(AgentAppUnit unit) {
        super(unit);
        if (unit.getTenantId() != null) {
            this.tenantId = unit.getTenantId().getId();
        }
        if (unit.getAgentApplicationId() != null) {
            this.agentApplicationId = unit.getAgentApplicationId().getId();
        }
        this.identifier = unit.getIdentifier();
        this.type = unit.getType();
    }

    public AgentAppUnitEntity(AgentAppUnitEntity entity) {
        super(entity);
        this.tenantId = entity.tenantId;
        this.agentApplicationId = entity.agentApplicationId;
        this.identifier = entity.identifier;
        this.type = entity.type;
    }

    @Override
    public AgentAppUnit toData() {
        AgentAppUnit unit = new AgentAppUnit(new AgentAppUnitId(id));
        unit.setCreatedTime(createdTime);
        if (tenantId != null) {
            unit.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (agentApplicationId != null) {
            unit.setAgentApplicationId(new AgentApplicationId(agentApplicationId));
        }
        unit.setIdentifier(identifier);
        unit.setType(type);
        return unit;
    }
}

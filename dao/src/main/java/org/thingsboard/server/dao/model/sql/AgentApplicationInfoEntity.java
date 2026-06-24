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
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentApplicationInfoEntity extends AgentApplicationEntity {

    private String currentVersion;
    private String nextVersion;
    private Long profileVersion;
    private String profileName;
    private String agentName;
    private UUID relatedEntityId;
    private String relatedEntityType;

    public AgentApplicationInfoEntity() {
        super();
    }

    public AgentApplicationInfoEntity(AgentApplicationEntity entity,
                                      String currentVersion,
                                      String nextVersion,
                                      Long profileVersion,
                                      String profileName,
                                      UUID relatedEntityId,
                                      String relatedEntityType) {
        this(entity, currentVersion, nextVersion, profileVersion, profileName, null, relatedEntityId, relatedEntityType);
    }

    public AgentApplicationInfoEntity(AgentApplicationEntity entity,
                                      String currentVersion,
                                      String nextVersion,
                                      Long profileVersion,
                                      String profileName,
                                      String agentName,
                                      UUID relatedEntityId,
                                      String relatedEntityType) {
        super(entity);
        this.currentVersion = currentVersion;
        this.nextVersion = nextVersion;
        this.profileVersion = profileVersion;
        this.profileName = profileName;
        this.agentName = agentName;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
    }

    @Override
    public AgentApplicationInfo toData() {
        AgentApplicationInfo info = new AgentApplicationInfo(super.toData(), currentVersion, nextVersion);
        info.setProfileConfigOutdated(profileVersion != null
                && !profileVersion.equals(info.getProfileConfigVersion()));
        info.setProfileName(profileName);
        info.setAgentName(agentName);
        if (relatedEntityId != null && relatedEntityType != null) {
            info.setRelatedEntityId(EntityIdFactory.getByTypeAndUuid(relatedEntityType, relatedEntityId));
        }
        return info;
    }

}

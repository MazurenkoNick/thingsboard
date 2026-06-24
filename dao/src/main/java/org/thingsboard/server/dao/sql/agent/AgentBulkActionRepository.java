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
package org.thingsboard.server.dao.sql.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.AgentBulkActionEntity;

import java.util.UUID;

public interface AgentBulkActionRepository extends JpaRepository<AgentBulkActionEntity, UUID> {

    @Query("""
           SELECT a FROM AgentBulkActionEntity a
           WHERE (a.status = 'IN_PROGRESS' AND a.processingStartedTime < :threshold)
              OR (a.status = 'QUEUED' AND a.createdTime < :threshold)
           """)
    Page<AgentBulkActionEntity> findStuckBulkActions(@Param("threshold") long threshold, Pageable pageable);

    @Query("SELECT a FROM AgentBulkActionEntity a WHERE a.agentProfileId = :agentProfileId")
    Page<AgentBulkActionEntity> findByAgentProfileId(@Param("agentProfileId") UUID agentProfileId, Pageable pageable);

    @Query("""
           SELECT a FROM AgentBulkActionEntity a
           WHERE a.agentProfileId = :agentProfileId
             AND a.applicationProfileId = :applicationProfileId
           """)
    Page<AgentBulkActionEntity> findByAgentProfileIdAndApplicationProfileId(
            @Param("agentProfileId") UUID agentProfileId,
            @Param("applicationProfileId") UUID applicationProfileId,
            Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentAppEventEntity e WHERE e.bulkActionId IS NOT NULL AND e.createdTime < :expirationTs")
    void deleteEventsByBulkActionCreatedTimeBefore(@Param("expirationTs") long expirationTs);

    @Transactional
    @Modifying
    @Query("DELETE FROM AgentBulkActionEntity a WHERE a.createdTime < :expirationTs")
    void deleteBulkActionsCreatedTimeBefore(@Param("expirationTs") long expirationTs);
}

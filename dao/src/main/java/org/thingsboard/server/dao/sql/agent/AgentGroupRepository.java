/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AgentGroupEntity;
import org.thingsboard.server.dao.model.sql.AgentGroupInfoEntity;

import java.util.UUID;

public interface AgentGroupRepository extends JpaRepository<AgentGroupEntity, UUID> {

    @Query("SELECT g FROM AgentGroupEntity g WHERE g.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentGroupEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                           @Param("textSearch") String textSearch,
                                           Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentGroupInfoEntity(g, c.title, c.additionalInfo) " +
            "FROM AgentGroupEntity g " +
            "LEFT JOIN CustomerEntity c on c.id = g.customerId " +
            "WHERE g.id = :groupId")
    AgentGroupInfoEntity findAgentGroupInfoById(@Param("groupId") UUID groupId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.AgentGroupInfoEntity(g, c.title, c.additionalInfo) " +
            "FROM AgentGroupEntity g " +
            "LEFT JOIN CustomerEntity c on c.id = g.customerId " +
            "WHERE g.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true " +
            "  OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentGroupInfoEntity> findAgentGroupInfosByTenantId(@Param("tenantId") UUID tenantId,
                                                              @Param("textSearch") String textSearch,
                                                              Pageable pageable);

    @Query("SELECT g FROM AgentGroupEntity g WHERE g.tenantId = :tenantId " +
            "AND g.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(g.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<AgentGroupEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                        @Param("customerId") UUID customerId,
                                                        @Param("textSearch") String textSearch,
                                                        Pageable pageable);

    @Query("SELECT count(*) FROM AgentGroupEntity g WHERE g.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);
}

package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.TenantId;

public interface AgentAppRelationService {

    void relateToParentEntity(TenantId tenantId, AgentApplication app);
}

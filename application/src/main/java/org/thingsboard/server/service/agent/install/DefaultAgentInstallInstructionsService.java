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
package org.thingsboard.server.service.agent.install;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInstructions;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
@Slf4j
public class DefaultAgentInstallInstructionsService implements AgentInstallInstructionsService {

    private static final String METHOD_DOCKER = "docker";

    private static final String DOCKER_INSTALL_TEMPLATE = """
            docker run -d \\
              --name=tb-agent \\
              --restart=always \\
              -v /var/run/docker.sock:/var/run/docker.sock:ro \\
              -v tb-agent-data:/root/.tb-agent \\
              -v /:/host:ro \\
              -e TB_SERVER_ADDR=${BASE_URL}:${RPC_PORT} \\
              -e TB_AGENT_ROUTING_KEY=${ROUTING_KEY} \\
              -e TB_AGENT_ROUTING_SECRET=${ROUTING_SECRET} \\
              thingsboard/tb-agent:latest""";

    private static final String DOCKER_PROVISION_TEMPLATE = """
            docker run -d \\
              --name=tb-agent \\
              --restart=always \\
              -v /var/run/docker.sock:/var/run/docker.sock:ro \\
              -v tb-agent-data:/root/.tb-agent \\
              -v /:/host:ro \\
              -e TB_SERVER_ADDR=${BASE_URL}:${RPC_PORT} \\
              -e AUTO_PROVISION=true \\
              -e TB_PROVISION_KEY=${PROVISION_KEY} \\
              -e TB_PROVISION_SECRET=${PROVISION_SECRET} \\
              thingsboard/tb-agent:latest""";

    @Value("${edges.rpc.port:7070}")
    private int rpcPort;

    @Override
    public AgentInstructions getInstallInstructions(Agent agent, String method, HttpServletRequest request) {
        if (!METHOD_DOCKER.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("Unsupported installation method for Agent: " + method);
        }
        String resolved = DOCKER_INSTALL_TEMPLATE
                .replace("${BASE_URL}", resolveBaseUrl(request))
                .replace("${RPC_PORT}", Integer.toString(rpcPort))
                .replace("${ROUTING_KEY}", nullSafe(agent.getRoutingKey()))
                .replace("${ROUTING_SECRET}", nullSafe(agent.getSecret()));
        return new AgentInstructions(resolved);
    }

    @Override
    public AgentInstructions getProvisionInstructions(AgentProfile profile, String method, HttpServletRequest request) {
        if (!METHOD_DOCKER.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("Unsupported provision method for Agent profile: " + method);
        }
        String resolved = DOCKER_PROVISION_TEMPLATE
                .replace("${BASE_URL}", resolveBaseUrl(request))
                .replace("${RPC_PORT}", Integer.toString(rpcPort))
                .replace("${PROVISION_KEY}", nullSafe(profile.getProvisionKey()))
                .replace("${PROVISION_SECRET}", nullSafe(profile.getProvisionSecret()));
        return new AgentInstructions(resolved);
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String serverName = request.getServerName();
        return DeviceConnectivityUtil.isLocalhost(serverName) ? DeviceConnectivityUtil.HOST_DOCKER_INTERNAL : serverName;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

}

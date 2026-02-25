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
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.event.AgentEventProcessor;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Lazy
@Getter
@Component
@TbCoreComponent
public class AgentContextComponent {

    @Autowired
    private AgentEventProcessor agentEventProcessor;

    @Value("${agents.event.executor_pool_size:4}")
    private int executorPoolSize;

    private ListeningExecutorService agentEventExecutor;

    @PostConstruct
    public void init() {
        this.agentEventExecutor = MoreExecutors.listeningDecorator(
                Executors.newFixedThreadPool(executorPoolSize, ThingsBoardThreadFactory.forName("agent-event-processor")));
    }

    @PreDestroy
    public void destroy() {
        if (agentEventExecutor != null) {
            MoreExecutors.shutdownAndAwaitTermination(agentEventExecutor, 30, TimeUnit.SECONDS);
        }
    }
}

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
package org.thingsboard.server.service.agent.template.merge;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.agent.template.TemplateMergeCtx;

import java.util.List;

@Component
public class AgentAppTemplateMergeOrchestrator {

    private final List<AppTemplateMergeRule> rules;

    public AgentAppTemplateMergeOrchestrator(List<AppTemplateMergeRule> rules) {
        this.rules = rules;
    }

    public void merge(AgentApplication agentApplication, AgentAppTemplate template, TemplateMergeCtx ctx) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        for (AppTemplateMergeRule rule : rules) {
            if (rule.supports(agentApplication, template, ctx)) {
                rule.apply(agentApplication, template, ctx);
            }
        }
    }
}

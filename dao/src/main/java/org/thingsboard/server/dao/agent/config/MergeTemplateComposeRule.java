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
package org.thingsboard.server.dao.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.HasAgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;

import java.util.Iterator;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MergeTemplateComposeRule implements AppConfigMergeRule {

    @Override
    public boolean supports(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx) {
        return isTemplateComposeMergeRequested(ctx);
    }

    private static boolean isTemplateComposeMergeRequested(AppConfigMergeCtx ctx) {
        return ctx != null && ctx.getTemplate() != null && StringUtils.isNoneBlank(ctx.getSelectedComposeType());
    }

    @Override
    public void apply(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx) {
        AgentAppTemplate template = ctx.getTemplate();
        Optional<ComposeTypeChoiceStep> choiceStep = StepLinkedListUtils.getByType(
                AgentAppStepType.COMPOSE_TEMPLATE, ComposeTypeChoiceStep.class, template.getStartSteps()
        );
        if (choiceStep.isEmpty()) {
            log.trace("Compose Choice Step from template or Compose Step from app couldn't be found: choice: {}", choiceStep);
            return;
        }
        ComposeTypeChoiceStep composeTypeChoiceStep = choiceStep.get();
        mergeComposeBySelectedType(hasAgentAppConfig, composeTypeChoiceStep, ctx);
    }

    private void mergeComposeBySelectedType(HasAgentAppConfig hasAgentAppConfig, ComposeTypeChoiceStep choiceStep, AppConfigMergeCtx ctx) {
        String selectedComposeType = ctx.getSelectedComposeType();
        JsonNode templateCompose = getTemplateComposeByType(choiceStep, selectedComposeType);
        AgentAppConfig appConfig = hasAgentAppConfig.getConfig();

        if (appConfig == null) {
            DockerComposeConfig config = new DockerComposeConfig();
            config.setCompose(templateCompose.deepCopy());
            config.setComposeType(selectedComposeType);
            hasAgentAppConfig.setConfig(config);
            return;
        }
        mergeExisting(appConfig, templateCompose, selectedComposeType);
    }

    private void mergeExisting(AgentAppConfig appConfig, JsonNode templateCompose, String selectedComposeType) {
        if (!(appConfig instanceof DockerComposeConfig appDockerConfig)) {
            throw new IllegalStateException("Can't set compose to the non-compose configuration " + appConfig);
        }
        appDockerConfig.setComposeType(selectedComposeType);
        JsonNode appCompose = appDockerConfig.getCompose();

        if (appCompose == null || appCompose.isNull() || !appCompose.isObject()) {
            appDockerConfig.setCompose(templateCompose.deepCopy());
            return;
        }
        deepMerge((ObjectNode) appCompose, templateCompose);
    }

    /**
     * Overlays {@code templateNode} onto {@code appNode}:
     * <ul>
     *   <li>Every key declared by the template replaces the corresponding app value (deep-copied).</li>
     *   <li>Keys the app has but the template doesn't are preserved as-is.</li>
     * </ul>
     * Credential env vars wiped by this overlay are re-populated downstream by
     * {@link MergeCredentialsToConfigRule} whenever the merge context carries a
     * {@code relatedEntityId}.
     */
    private void deepMerge(ObjectNode appNode, JsonNode templateNode) {
        Iterator<String> templateFields = templateNode.fieldNames();
        while (templateFields.hasNext()) {
            String key = templateFields.next();
            appNode.set(key, templateNode.get(key).deepCopy());
        }
    }

    private JsonNode getTemplateComposeByType(ComposeTypeChoiceStep choiceStep, String selectedComposeType) {
        return choiceStep.getTemplateByType(selectedComposeType)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("There's no '%s' compose type in: %s", selectedComposeType, choiceStep.getComposeTypes())
                ));
    }
}

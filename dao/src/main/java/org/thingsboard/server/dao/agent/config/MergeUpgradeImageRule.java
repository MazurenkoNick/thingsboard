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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.HasAgentAppConfig;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.step.AgentAppStepType;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MergeUpgradeImageRule implements AppConfigMergeRule {

    @Override
    public boolean supports(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx) {
        return ctx != null
                && ctx.getActionType() == AgentAppEventActionType.UPGRADE
                && ctx.getTemplate() != null
                && (ctx.getTemplate().getAppType() == AgentApplicationType.EDGE ||
                    ctx.getTemplate().getAppType() == AgentApplicationType.GATEWAY);
    }

    @Override
    public void apply(HasAgentAppConfig hasAgentAppConfig, AppConfigMergeCtx ctx) {
        AgentAppTemplate template = ctx.getTemplate();
        AgentApplicationType appType = template.getAppType();
        if (appType == null || appType.getMainImagePattern() == null) {
            log.trace("Skipping upgrade image merge: appType [{}] has no main image pattern", appType);
            return;
        }
        Pattern mainImagePattern = appType.getMainImagePattern();

        JsonNode templateCompose = resolveTemplateCompose(template, ctx);
        if (templateCompose == null) {
            log.trace("Skipping upgrade image merge: no compose template found");
            return;
        }
        String newImage = DockerComposeUtils.getMainImage(templateCompose, mainImagePattern);
        if (StringUtils.isBlank(newImage)) {
            log.trace("Skipping upgrade image merge: no main image found in template compose");
            return;
        }

        JsonNode targetCompose = getCompose(hasAgentAppConfig.getConfig());
        if (targetCompose == null) {
            log.trace("Skipping upgrade image merge: target compose is null");
            return;
        }
        DockerComposeUtils.setMainImage(targetCompose, mainImagePattern, newImage);
    }

    // The main image is identical across compose-type variants, so any variant works;
    // honor the selected type when present, else take the first variant.
    private static JsonNode resolveTemplateCompose(AgentAppTemplate template, AppConfigMergeCtx ctx) {
        Optional<ComposeTypeChoiceStep> choiceStep = StepLinkedListUtils.getByType(
                AgentAppStepType.COMPOSE_TEMPLATE, ComposeTypeChoiceStep.class, template.getStartSteps());
        if (choiceStep.isPresent()) {
            Map<String, JsonNode> variants = choiceStep.get().getComposeTemplates();
            if (variants != null && !variants.isEmpty()) {
                String selected = ctx.getSelectedComposeType();
                if (StringUtils.isNotBlank(selected) && variants.containsKey(selected)) {
                    return variants.get(selected);
                }
                return variants.values().iterator().next();
            }
        }
        return null;
    }

    private static JsonNode getCompose(AgentAppConfig config) {
        return config instanceof DockerComposeConfig composeConfig ? composeConfig.getCompose() : null;
    }
}

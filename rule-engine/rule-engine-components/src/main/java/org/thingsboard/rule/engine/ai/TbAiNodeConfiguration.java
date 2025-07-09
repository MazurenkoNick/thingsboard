/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rule.engine.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.AiModelId;
import org.thingsboard.server.common.data.validation.Length;

import static org.thingsboard.rule.engine.ai.TbResponseFormat.TbJsonResponseFormat;

@Data
public class TbAiNodeConfiguration implements NodeConfiguration<TbAiNodeConfiguration> {

    @NotNull
    private AiModelId aiModelId;

    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Length(min = 1, max = 10000)
    private String systemPrompt;

    @NotBlank
    @Length(min = 1, max = 10000)
    private String userPrompt;

    @NotNull
    @Valid
    private TbResponseFormat responseFormat;

    @Min(value = 1, message = "must be at least 1 second")
    @Max(value = 600, message = "cannot exceed 600 seconds (10 minutes)")
    private int timeoutSeconds;

    @Override
    public TbAiNodeConfiguration defaultConfiguration() {
        var configuration = new TbAiNodeConfiguration();
        configuration.setSystemPrompt("You are helpful assistant. Your response must be in JSON format.");
        configuration.setUserPrompt("Tell me a joke.");
        configuration.setResponseFormat(new TbJsonResponseFormat());
        configuration.setTimeoutSeconds(60);
        return configuration;
    }

}

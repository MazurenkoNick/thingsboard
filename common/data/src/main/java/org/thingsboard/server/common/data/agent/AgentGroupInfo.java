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
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentGroupInfo extends AgentGroup {

    @Serial
    private static final long serialVersionUID = -5509870435345957907L;

    @Schema(description = "Title of the Customer that owns the group.", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerTitle;
    @Schema(description = "Indicates special 'Public' Customer.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean customerIsPublic;

    public AgentGroupInfo() {
        super();
    }

    public AgentGroupInfo(AgentGroup group, String customerTitle, boolean customerIsPublic) {
        super(group);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }
}

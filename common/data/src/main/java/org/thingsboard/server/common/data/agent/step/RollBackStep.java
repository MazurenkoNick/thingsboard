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
package org.thingsboard.server.common.data.agent.step;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class RollBackStep extends AgentAppStep {

    private static final UUID ID = UUID.fromString("232cfd18-71ca-410c-aecb-d1151e9c936e");

    public static final RollBackStep INSTANCE = new RollBackStep();

    private RollBackStep() {
        this.id = ID;
        this.title = "Rollback Step After Server Exception";
    }

    @Override
    public AgentAppStepType getType() {
        return AgentAppStepType.ROLLBACK;
    }
}

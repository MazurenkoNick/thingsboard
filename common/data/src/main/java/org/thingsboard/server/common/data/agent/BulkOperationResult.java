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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@NoArgsConstructor
@Schema
public class BulkOperationResult {

    @Schema(description = "Total number of apps targeted")
    private AtomicInteger total = new AtomicInteger();
    @Schema(description = "Number of apps for which the operation was submitted")
    private AtomicInteger submitted = new AtomicInteger();
    @Schema(description = "List of skipped apps with reasons")
    private Collection<SkippedApp> skipped = new ConcurrentLinkedDeque<>();

    public void incrementTotal() {
        total.incrementAndGet();
    }

    public void incrementSubmitted() {
        submitted.incrementAndGet();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema
    public static class SkippedApp {
        @Schema(description = "Application Id")
        private String applicationId;
        @Schema(description = "Agent name")
        private String agentName;
        @Schema(description = "Reason for skipping")
        private SkipReason reason;
        @Schema(description = "Optional message in case of a failure")
        private String msg;

        public SkippedApp(String applicationId, String agentName, SkipReason reason) {
            this.applicationId = applicationId;
            this.agentName = agentName;
            this.reason = reason;
        }
    }

    public enum SkipReason {
        VERSION_MISMATCH,
        ACTIVE_EVENT,
        ERROR
    }
}

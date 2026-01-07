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
package org.thingsboard.server.report.context;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@SuperBuilder
public abstract class TbReportCtx implements Closeable {

    private final TenantId tenantId;
    private final ReportTemplateConfig configuration;
    private final String timeZone;
    private final UserId userId;
    private final EntityId userOwnerId;
    private final String accessToken;
    private final long accessTokenExpTs;
    private final String reportCreatedTime;

    private final Map<String, Object> params = new HashMap<>();
    private final Map<String, UUID> scripts = new HashMap<>();
    private final TbelInvokeService tbelInvokeService;

    public abstract TbReportCtx createSubReportCxt(ReportTemplateConfig reportTemplateConfig);

    @Override
    public void close() throws IOException {
        getScripts().values().forEach(scriptId -> {
            if (scriptId != null) {
                tbelInvokeService.release(scriptId);
            }
        });
    }

}

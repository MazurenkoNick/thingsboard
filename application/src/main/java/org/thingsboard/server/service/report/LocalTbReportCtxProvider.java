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
package org.thingsboard.server.service.report;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.TbReportCtxProvider;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import static org.thingsboard.server.report.util.ReportUtils.formatTimestamp;

@RequiredArgsConstructor
@Primary
@Service
public class LocalTbReportCtxProvider implements TbReportCtxProvider {

    private final JwtTokenFactory tokenFactory;
    private final TbelInvokeService tbelInvokeService;

    @Override
    public LocalTbReportCtx newContext(ReportTask task) {
        SecurityUser securityUser = tokenFactory.parseAccessJwtToken(task.getAccessToken());
        return LocalTbReportCtx.builder()
                .tenantId(securityUser.getTenantId())
                .tbelInvokeService(tbelInvokeService)
                .configuration(task.getReportTemplateConfig())
                .userId(task.getUserId())
                .userOwnerId(task.getUserOwnerId())
                .timeZone(task.getTimezone())
                .accessToken(task.getAccessToken())
                .accessTokenExpTs(task.getAccessTokenExpirationTs())
                .securityUser(securityUser)
                .reportCreatedTime(formatTimestamp(System.currentTimeMillis(), task.getReportTemplateConfig().getTimeDataPattern(), task.getTimezone()))
                .build();
    }

    @Data
    @SuperBuilder
    public static class LocalTbReportCtx extends TbReportCtx {

        private final SecurityUser securityUser;

        @Override
        public TbReportCtx createSubReportCxt(ReportTemplateConfig reportTemplateConfig) {
            LocalTbReportCtx copy = LocalTbReportCtx.builder()
                    .tenantId(this.getTenantId())
                    .tbelInvokeService(this.getTbelInvokeService())
                    .configuration(reportTemplateConfig)
                    .userId(this.getUserId())
                    .userOwnerId(this.getUserOwnerId())
                    .timeZone(this.getTimeZone())
                    .accessToken(this.getAccessToken())
                    .accessTokenExpTs(this.getAccessTokenExpTs())
                    .securityUser(this.getSecurityUser())
                    .reportCreatedTime(this.getReportCreatedTime())
                    .build();

            copy.getParams().putAll(this.getParams());

            return copy;
        }
    }

}

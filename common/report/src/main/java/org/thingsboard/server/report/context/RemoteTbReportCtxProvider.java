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

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;

import java.io.IOException;

import static org.thingsboard.server.report.util.ReportUtils.formatTimestamp;

@RequiredArgsConstructor
@ConditionalOnExpression("'${service.type:null}' == 'tb-report'")
@Service
public class RemoteTbReportCtxProvider implements TbReportCtxProvider {

    @Value("${service.tb_core.base_url:http://localhost:${server.port}}")
    private String tbCoreBaseUrl;

    private final TbelInvokeService tbelInvokeService;

    @Override
    public TbReportCtx newContext(ReportTask task) {
        return RemoteTbReportCtx.builder()
                .tenantId(task.getTenantId())
                .tbelInvokeService(tbelInvokeService)
                .configuration(task.getReportTemplateConfig())
                .userId(task.getUserId())
                .userOwnerId(task.getUserOwnerId())
                .timeZone(task.getTimezone())
                .accessToken(task.getAccessToken())
                .accessTokenExpTs(task.getAccessTokenExpirationTs())
                .restClient(new RestClient(new RestTemplate(), tbCoreBaseUrl, RestClient.AuthType.JWT, task.getAccessToken()))
                .reportCreatedTime(formatTimestamp(System.currentTimeMillis(), task.getReportTemplateConfig().getTimeDataPattern(), task.getTimezone()))
                .build();
    }

    @Data
    @SuperBuilder
    public static class RemoteTbReportCtx extends TbReportCtx {

        private final RestClient restClient;

        @Override
        public void close() throws IOException {
            super.close();
            restClient.close();
        }

        @Override
        public TbReportCtx createSubReportCxt(ReportTemplateConfig reportTemplateConfig) {
            RemoteTbReportCtx copy = RemoteTbReportCtx.builder()
                    .tenantId(this.getTenantId())
                    .tbelInvokeService(this.getTbelInvokeService())
                    .configuration(reportTemplateConfig)
                    .userId(this.getUserId())
                    .userOwnerId(this.getUserOwnerId())
                    .timeZone(this.getTimeZone())
                    .accessToken(this.getAccessToken())
                    .accessTokenExpTs(this.getAccessTokenExpTs())
                    .restClient(this.getRestClient())
                    .reportCreatedTime(this.getReportCreatedTime())
                    .build();

            copy.getParams().putAll(this.getParams());

            return copy;
        }

    }

}

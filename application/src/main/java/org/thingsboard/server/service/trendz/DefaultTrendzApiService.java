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
package org.thingsboard.server.service.trendz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzUsage;
import org.thingsboard.server.common.data.trendz.TrendzViewConfig;
import org.thingsboard.server.common.data.trendz.TrendzViewConfigLite;
import org.thingsboard.server.dao.trendz.TrendzApiService;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrendzApiService implements TrendzApiService {
    private final TrendzClient trendzClient;

    @Override
    public TrendzViewConfig getViewById(User user, UUID viewId) throws ThingsboardException {
        return trendzClient.getTrendzViewById(viewId, user);
    }

    @Override
    public PageData<TrendzViewConfigLite> getAllViews(User user, PageLink pageLink) throws ThingsboardException {
        return trendzClient.getAllTrendzViews(pageLink, user)
                .toPageData();
    }

    @Override
    public TrendzSummary getTrendzSummary(User user) throws ThingsboardException {
        return trendzClient.getTrendzSummary(user);
    }

    @Override
    public TrendzUsage getTrendzUsage(User user) throws ThingsboardException {
        return trendzClient.getTrendzUsage(user);
    }
}

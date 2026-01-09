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
package org.thingsboard.server.service.resource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.GeneralFileDescriptor;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceDataInfo;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.resource.TbResourceDataCache;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DaoSqlTest
public class DefaultResourceDataCacheTest extends AbstractControllerTest {

    @MockitoSpyBean
    private ResourceService resourceService;
    @Autowired
    private TbResourceService tbResourceService;
    @MockitoSpyBean
    private TbResourceDataCache resourceDataCache;

    @Test
    public void testGetCachedResourceData() throws Exception {
        loginTenantAdmin();

        TbResource resource = new TbResource();
        resource.setTenantId(tenantId);
        resource.setTitle("File for AI request");
        resource.setResourceType(ResourceType.GENERAL);
        resource.setFileName("myTestJson.json");
        GeneralFileDescriptor descriptor = new GeneralFileDescriptor("application/json");
        resource.setDescriptorValue(descriptor);
        byte[] data = "This is a test prompt for AI request.".getBytes();
        resource.setData(data);
        TbResourceInfo savedResource = tbResourceService.save(resource);
        verify(resourceDataCache, timeout(2000).times(1)).evictResourceData(tenantId, savedResource.getId());

        TbResourceDataInfo cachedData = resourceDataCache.getResourceDataInfoAsync(tenantId, savedResource.getId()).get();
        assertThat(cachedData.getData()).isEqualTo(data);
        assertThat(JacksonUtil.treeToValue(cachedData.getDescriptor(), GeneralFileDescriptor.class)).isEqualTo(descriptor);
        verify(resourceService).getResourceDataInfo(tenantId, savedResource.getId());

        // retrieve resource data second time
        clearInvocations(resourceService);
        TbResourceDataInfo cachedData2 = resourceDataCache.getResourceDataInfoAsync(tenantId, savedResource.getId()).get();
        assertThat(cachedData2.getData()).isEqualTo(data);
        verifyNoMoreInteractions(resourceService);

        // delete resource, check cache
        TbResource resourceById = resourceService.findResourceById(tenantId, savedResource.getId());
        tbResourceService.delete(resourceById, true, null);
        verify(resourceDataCache, timeout(2000).times(2)).evictResourceData(tenantId, savedResource.getId());
        TbResourceDataInfo cachedDataAfterDeletion = resourceDataCache.getResourceDataInfoAsync(tenantId, savedResource.getId()).get();
        assertThat(cachedDataAfterDeletion).isEqualTo(null);
    }

}

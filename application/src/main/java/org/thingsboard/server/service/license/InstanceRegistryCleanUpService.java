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
package org.thingsboard.server.service.license;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.instance.registry.InstanceRegistryService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class InstanceRegistryCleanUpService extends TbApplicationEventListener<ServiceListChangedEvent> {

    private final InstanceRegistryService instanceRegistryService;
    private final PartitionService partitionService;

    private Set<String> currentIds = new HashSet<>();

    @Override
    protected void onTbApplicationEvent(ServiceListChangedEvent event) {
        List<TransportProtos.ServiceInfo> otherServices = event.getOtherServices().stream()
                .filter(serviceInfo -> serviceInfo.getServiceTypesList().contains("TB_CORE") || serviceInfo.getServiceTypesList().contains("TB_RULE_ENGINE"))
                .collect(Collectors.toList());

        Set<String> serviceTypes = otherServices.stream().flatMap(s -> s.getServiceTypesList().stream()).collect(Collectors.toSet());
        serviceTypes.addAll(event.getCurrentService().getServiceTypesList());

        ServiceType serviceType = serviceTypes.contains("TB_CORE") ? ServiceType.TB_CORE : ServiceType.TB_RULE_ENGINE;

        Set<String> newIds = otherServices.stream().map(TransportProtos.ServiceInfo::getServiceId).collect(Collectors.toSet());
        Set<String> toRemove = CollectionsUtil.diffSets(newIds, currentIds );

        currentIds = newIds;
        if (!toRemove.isEmpty() && partitionService.isSystemTenantPartitionMine(serviceType)) {
            log.debug("Going to remove outdated instance registries: {}", toRemove);
            toRemove.forEach(instanceRegistryService::deleteByServiceId);
        }
    }

}

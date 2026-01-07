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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

public class DeviceDescriptor extends EdgeNodeDescriptor {

    private final String deviceId;
    private final String descriptorString;

    public DeviceDescriptor(String groupId, String edgeNodeId, String deviceId) {
        super(groupId, edgeNodeId);
        this.deviceId = deviceId;
        this.descriptorString = groupId + "/" + edgeNodeId + "/" + deviceId;
    }

    public DeviceDescriptor(String descriptorString) {
        super(descriptorString.substring(0, descriptorString.lastIndexOf("/")));
        this.deviceId = descriptorString.substring(descriptorString.lastIndexOf("/") + 1);
        this.descriptorString = descriptorString;
    }

    public DeviceDescriptor(EdgeNodeDescriptor edgeNodeDescriptor, String deviceId) {
        super(edgeNodeDescriptor.getGroupId(), edgeNodeDescriptor.getEdgeNodeId());
        this.deviceId = deviceId;
        this.descriptorString = edgeNodeDescriptor.getDescriptorString() + "/" + deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Returns a {@link String} representing the Device's Descriptor of the form:
     * "<groupName>/<edgeNodeName>/<deviceId>".
     *
     * @return a {@link String} representing the Device's Descriptor.
     */
    @Override
    public String getDescriptorString() {
        return descriptorString;
    }

    public String getEdgeNodeDescriptorString() {
        return super.getDescriptorString();
    }

    @Override
    public int hashCode() {
        return this.getDescriptorString().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DeviceDescriptor) {
            return this.getDescriptorString().equals(((DeviceDescriptor) object).getDescriptorString());
        }
        return this.getDescriptorString().equals(object);
    }

    @Override
    public String toString() {
        return getDescriptorString();
    }
}

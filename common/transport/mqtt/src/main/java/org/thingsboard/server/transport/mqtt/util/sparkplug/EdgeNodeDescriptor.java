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

import com.fasterxml.jackson.annotation.JsonValue;

public class EdgeNodeDescriptor implements SparkplugDescriptor{

    private final String groupId;
    private final String edgeNodeId;
    private final String descriptorString;

    public EdgeNodeDescriptor(String groupId, String edgeNodeId) {
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.descriptorString = groupId + "/" + edgeNodeId;
    }

    /**
     * Creates and EdgeNodeDescriptor from a {@link String} of the form group_name/edge_node_name
     *
     * @param descriptorString the {@link String} representation of an EdgeNodeDescriptor
     */
    public EdgeNodeDescriptor(String descriptorString) {
        String[] tokens = descriptorString.split("/");
        this.groupId = tokens[0];
        this.edgeNodeId = tokens[1];
        this.descriptorString = descriptorString;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getEdgeNodeId() {
        return edgeNodeId;
    }

    /**
     * Returns a {@link String} representing the Edge Node's Descriptor of the form: "<groupId>/<edgeNodeId>".
     *
     * @return a {@link String} representing the Edge Node's Descriptor.
     */
    @Override
    public String getDescriptorString() {
        return descriptorString;
    }

    @Override
    public int hashCode() {
        return this.getDescriptorString().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof EdgeNodeDescriptor) {
            return this.getDescriptorString().equals(((EdgeNodeDescriptor) object).getDescriptorString());
        }
        return this.getDescriptorString().equals(object);
    }

    @Override
    @JsonValue
    public String toString() {
        return getDescriptorString();
    }
}

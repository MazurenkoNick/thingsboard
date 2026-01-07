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
package org.thingsboard.server.common.data.device.credentials.lwm2m;

/**
 * Enum representing predefined LwM2M Short Server Identifiers.
 * <p>
 * See OMA Lightweight M2M Specification for details about the server identifier space.
 */
public enum Lwm2mServerIdentifier {

    /**
     * Not used for identifying an LwM2M Server (0).
     */
    NOT_USED_IDENTIFYING_LWM2M_SERVER_MIN(0, "Bootstrap Short Server ID", false),

    /**
     * Primary LwM2M Server Short Server ID (1).
     * Upper boundary for valid LwM2M Server Identifiers (1–65534).
     */
    PRIMARY_LWM2M_SERVER(1, "LwM2M Server Short Server ID", true),

    /**
     * Maximum valid LwM2M Server ID (65534).
     * Upper boundary for valid LwM2M Server Identifiers (1–65534).
     */
    LWM2M_SERVER_MAX(65534, "LwM2M Server Short Server ID", true),

    /**
     * Not used for identifying an LwM2M Server (65535).
     * Reserved sentinel value representing "no server associated" or "invalid ID".
     * MUST NOT be assigned to any LwM2M Server according to OMA-TS-LightweightM2M-Core, §6.2.1.
     * OMA LwM2M Core / v1.2: Server / Short Server ID): «MAX_ID 65535 is a reserved value and MUST NOT be used for identifying an Object»
     */
    NOT_USED_IDENTIFYING_LWM2M_SERVER_MAX(65535, "Reserved sentinel value (no active server)", false);

    private final Integer id;
    private final String description;
    private final boolean isLwm2mServer;

    Lwm2mServerIdentifier(Integer id, String description, boolean isLwm2mServer) {
        this.id = id;
        this.description = description;
        this.isLwm2mServer = isLwm2mServer;
    }

    /**
     * @return the integer value of this Short Server ID.
     */
    public Integer getId() {
        return id;
    }

    /**
     * @return a human-readable description of this Server ID.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true if this ID represents a Lwm2m Server.
     */
    public boolean isLwm2mServer() {
        return isLwm2mServer;
    }

    /**
     * Checks whether a given ID represents a valid LwM2M Server (1–65534).
     * @param id Short Server ID value.
     * @return true if the ID belongs to a standard LwM2M Server.
     */
    public static boolean isLwm2mServer(Integer id) {
        return id != null && id >= PRIMARY_LWM2M_SERVER.id && id <= LWM2M_SERVER_MAX.id;
    }
    public static boolean isNotLwm2mServer(Integer id) {
        return id == null || id < PRIMARY_LWM2M_SERVER.id || id > LWM2M_SERVER_MAX.id;
    }

    /**
     * Returns a {@link Lwm2mServerIdentifier} instance matching the given ID.
     * @param id numeric ID.
     * @return corresponding enum constant.
     * @throws IllegalArgumentException if no constant matches the given ID.
     */
    public static Lwm2mServerIdentifier fromId(Integer id) {
        for (Lwm2mServerIdentifier s : values()) {
            if (s.id == id) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown Lwm2mServerIdentifier: " + id);
    }

    @Override
    public String toString() {
        return name() + "(" + id + ") - " + description;
    }
}

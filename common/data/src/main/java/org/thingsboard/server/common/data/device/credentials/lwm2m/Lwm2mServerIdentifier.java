/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
     * Bootstrap Short Server ID (0).
     * Reserved for the Bootstrap Server — used exclusively during the bootstrap phase.
     */
    BOOTSTRAP(0, "Bootstrap Short Server ID", true),

    /**
     * Primary LwM2M Server Short Server ID (1).
     * Upper boundary for valid LwM2M Server Identifiers (1–65534).
     */
    PRIMARY_LWM2M_SERVER(1, "LwM2M Server Short Server ID", false),

    /**
     * Maximum valid LwM2M Server ID (65534).
     * Upper boundary for valid LwM2M Server Identifiers (1–65534).
     */
    LWM2M_SERVER_MAX(65534, "LwM2M Server Short Server ID", false),

    /**
     * Not used for identifying an LwM2M Server (65535).
     * Reserved sentinel value representing "no server associated" or "invalid ID".
     * MUST NOT be assigned to any LwM2M Server according to OMA-TS-LightweightM2M-Core, §6.2.1.
     * OMA LwM2M Core / v1.2: Server / Short Server ID): «MAX_ID 65535 is a reserved value and MUST NOT be used for identifying an Object»
     */
    NOT_USED_IDENTIFYING_LWM2M_SERVER(65535, "Reserved sentinel value (no active server)", false);

    private final int id;
    private final String description;
    private final boolean isBootstrap;

    Lwm2mServerIdentifier(int id, String description, boolean isBootstrap) {
        this.id = id;
        this.description = description;
        this.isBootstrap = isBootstrap;
    }

    /**
     * @return the integer value of this Short Server ID.
     */
    public int getId() {
        return id;
    }

    /**
     * @return a human-readable description of this Server ID.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true if this ID represents a Bootstrap Server.
     */
    public boolean isBootstrap() {
        return isBootstrap;
    }

    /**
     * Checks whether a given numeric ID belongs to the Bootstrap Server (0).
     * OMA Spec (LwM2M v1.0 / v1.1):
     * Short Server ID Resource (Resource ID: 0)
     * The Short Server ID identifies a Server Object Instance.
     * The value 0 is reserved for the Bootstrap Server.
     * A value between 1 and 65534 identifies a LwM2M Server.
     * The value 65535 MUST NOT be used.
     * @param id Short Server ID value.
     * @return true if id == 0.
     */
    public static boolean isBootstrap(int id) {
        return id == BOOTSTRAP.id;
    }

    /**
     * Checks whether a given ID represents a valid LwM2M Server (1–65534).
     *
     * @param id Short Server ID value.
     * @return true if the ID belongs to a standard LwM2M Server.
     */
    public static boolean isLwm2mServer(int id) {
        return id >= PRIMARY_LWM2M_SERVER.id && id <= LWM2M_SERVER_MAX.id;
    }

    /**
     * Checks whether the provided ID is within the valid LwM2M range [0–65535].
     *
     * @param id ID to check.
     * @return true if valid, false otherwise.
     */
    public static boolean isValid(int id) {
        return id >= 0 && id <= 65535;
    }

    /**
     * Returns a {@link Lwm2mServerIdentifier} instance matching the given ID.
     *
     * @param id numeric ID.
     * @return corresponding enum constant.
     * @throws IllegalArgumentException if no constant matches the given ID.
     */
    public static Lwm2mServerIdentifier fromId(int id) {
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

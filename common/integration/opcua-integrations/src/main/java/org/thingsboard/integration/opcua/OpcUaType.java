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
package org.thingsboard.integration.opcua;

import java.math.BigInteger;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ulong;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

public enum OpcUaType {

    INT8("Int8", "SByte") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).byteValue();
        }
    },
    UINT8("UInt8", "Byte") {
        @Override
        public Object convertValue(Object raw) {
            return ubyte(((Number) raw).intValue());
        }
    },
    INT16("Int16") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).shortValue();
        }
    },
    UINT16("UInt16") {
        @Override
        public Object convertValue(Object raw) {
            return ushort(((Number) raw).intValue());
        }
    },
    INT32("Int32") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).intValue();
        }
    },
    UINT32("UInt32") {
        @Override
        public Object convertValue(Object raw) {
            return uint(((Number) raw).longValue());
        }
    },
    INT64("Int64") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).longValue();
        }
    },
    UINT64("UInt64") {
        @Override
        public Object convertValue(Object raw) {
            return ulong((BigInteger) (raw));
        }
    },

    FLOAT("Float") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).floatValue();
        }
    },
    DOUBLE("Double") {
        @Override
        public Object convertValue(Object raw) {
            return ((Number) raw).doubleValue();
        }
    },

    BOOLEAN("Boolean") {
        @Override
        public Object convertValue(Object raw) {
            return raw;
        }
    },

    STRING("String") {
        @Override
        public Object convertValue(Object raw) {
            return String.valueOf(raw);
        }
    };

    private final String[] typeAliases;

    OpcUaType(String... aliases) {
        this.typeAliases = aliases;
    }

    public abstract Object convertValue(Object raw);

    public static OpcUaType fromOpcUaType(String name) {
        for (OpcUaType t : values())
            for (String a : t.typeAliases)
                if (a.equals(name))
                    return t;

        return STRING;
    }

}

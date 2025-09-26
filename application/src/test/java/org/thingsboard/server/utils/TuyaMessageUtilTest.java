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
package org.thingsboard.server.utils;

import org.junit.jupiter.api.Test;
import org.thingsboard.integration.tuya.mq.EncryptionMethod;
import org.thingsboard.integration.tuya.mq.TuyaMessageUtil;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TuyaMessageUtilTest {

    private static final String KEY16 = "0123456789abcdef";
    private static final String TEXT = "Tuya integration";

    private static final String ECB_EMPTY_B64 = "N3Ii4GGpJMWRzZwn6hY+1A==";
    private static final String ECB_TUYA_B64  = "4meOZ8Yi2JjV8YX1MP+l9zdyIuBhqSTFkc2cJ+oWPtQ=";

    private static final byte[] GCM_NONCE_FIXED = new byte[]{0,1,2,3,4,5,6,7,8,9,10,11};
    private static final String GCM_EMPTY_B64_FIXED = "AAECAwQFBgcICQoLktErOnQP8+0KKKZ/rVw9dA==";
    private static final String GCM_TUYA_B64_FIXED  = "AAECAwQFBgcICQoLqUq9fD4dVUKNQXV7tPLfQ8qvGgeo7e48FbUTj5oKyuw=";

    @Test
    void testEcbMethod() throws Exception {
        assertEquals(ECB_EMPTY_B64, TuyaMessageUtil.encryptEcb("", KEY16));
        assertEquals("", TuyaMessageUtil.decryptEcb(Base64.getDecoder().decode(ECB_EMPTY_B64), KEY16));

        assertEquals(ECB_TUYA_B64, TuyaMessageUtil.encryptEcb(TEXT, KEY16));
        assertEquals(TEXT, TuyaMessageUtil.decryptEcb(Base64.getDecoder().decode(ECB_TUYA_B64), KEY16));
    }

    @Test
    void testGcmMethodFixedNonce() throws Exception {
        assertEquals(GCM_EMPTY_B64_FIXED, TuyaMessageUtil.encryptGcm("", KEY16, GCM_NONCE_FIXED));
        assertEquals("", TuyaMessageUtil.decryptGcm(Base64.getDecoder().decode(GCM_EMPTY_B64_FIXED), KEY16));

        assertEquals(GCM_TUYA_B64_FIXED, TuyaMessageUtil.encryptGcm(TEXT, KEY16, GCM_NONCE_FIXED));
        assertEquals(TEXT, TuyaMessageUtil.decryptGcm(Base64.getDecoder().decode(GCM_TUYA_B64_FIXED), KEY16));
    }

    @Test
    void testDecryptKnownMethod() throws Exception {
        assertEquals("", TuyaMessageUtil.decrypt(ECB_EMPTY_B64, KEY16, EncryptionMethod.AES_ECB));
        assertEquals("", TuyaMessageUtil.decrypt(GCM_EMPTY_B64_FIXED, KEY16, EncryptionMethod.AES_GCM));
        assertEquals(TEXT, TuyaMessageUtil.decrypt(ECB_TUYA_B64, KEY16, EncryptionMethod.AES_ECB));
        assertEquals(TEXT, TuyaMessageUtil.decrypt(GCM_TUYA_B64_FIXED, KEY16, EncryptionMethod.AES_GCM));
    }

    @Test
    void testEncryptKnownMethod() throws Exception {
        assertEquals(ECB_EMPTY_B64, TuyaMessageUtil.encrypt("", KEY16, EncryptionMethod.AES_ECB));
        assertEquals(ECB_TUYA_B64,  TuyaMessageUtil.encrypt(TEXT, KEY16, EncryptionMethod.AES_ECB));

        String enc1 = TuyaMessageUtil.encrypt(TEXT, KEY16, EncryptionMethod.AES_GCM);
        String enc2 = TuyaMessageUtil.encrypt(TEXT, KEY16, EncryptionMethod.AES_GCM);
        assertNotNull(enc1);
        assertNotNull(enc2);
        assertNotEquals(enc1, enc2);
    }


    @Test
    void testEcbRoundTrip() throws Exception {
        String enc = TuyaMessageUtil.encrypt(TEXT, KEY16, EncryptionMethod.AES_ECB);
        String dec = TuyaMessageUtil.decrypt(enc, KEY16, EncryptionMethod.AES_ECB);
        assertEquals(TEXT, dec);
    }

    @Test
    void testGcmRoundTrip() throws Exception {
        String enc = TuyaMessageUtil.encrypt(TEXT, KEY16, EncryptionMethod.AES_GCM);
        String dec = TuyaMessageUtil.decrypt(enc, KEY16, EncryptionMethod.AES_GCM);
        assertEquals(TEXT, dec);
    }

}

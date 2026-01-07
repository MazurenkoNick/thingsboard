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
package org.thingsboard.integration.tuya.mq;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;


public class TuyaMessageUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String decrypt(String base64, String key16, EncryptionMethod em) throws GeneralSecurityException {
        byte[] raw = Base64.decodeBase64(base64);
        return em == EncryptionMethod.AES_GCM ? decryptGcm(raw, key16) : decryptEcb(raw, key16);
    }

    public static String decryptEcb(byte[] raw, String key16) throws GeneralSecurityException {
        if ((raw.length % 16) != 0)
            throw new IllegalArgumentException("ECB payload length must be multiple of 16 bytes");

        Key key = generateKey(key16, EncryptionMethod.AES_ECB.getAlgorithm());
        Cipher c = Cipher.getInstance(EncryptionMethod.AES_ECB.getTransform());
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] pt = c.doFinal(raw);
        return new String(pt, StandardCharsets.UTF_8);
    }

    public static String decryptGcm(byte[] raw, String key16) throws GeneralSecurityException {
        if (raw.length < EncryptionMethod.AES_GCM.getNonceLen() + EncryptionMethod.AES_GCM.getTagBits() / 8)
            throw new IllegalArgumentException("GCM payload is shorter than required");

        byte[] nonce = Arrays.copyOfRange(raw, 0, EncryptionMethod.AES_GCM.getNonceLen());
        byte[] ctTag = Arrays.copyOfRange(raw, EncryptionMethod.AES_GCM.getNonceLen(), raw.length);

        Key key = generateKey(key16, EncryptionMethod.AES_GCM.getAlgorithm());
        Cipher c = Cipher.getInstance(EncryptionMethod.AES_GCM.getTransform());
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(EncryptionMethod.AES_GCM.getTagBits(), nonce));
        byte[] pt = c.doFinal(ctTag);
        return new String(pt, StandardCharsets.UTF_8);
    }

    public static String encrypt(String plaintext, String key16, EncryptionMethod em) throws GeneralSecurityException {
        if (plaintext == null) throw new IllegalArgumentException("Plaintext cannot be null");
        return (em == EncryptionMethod.AES_GCM)
                ? encryptGcm(plaintext, key16)
                : encryptEcb(plaintext, key16);
    }

    public static String encryptGcm(String plaintext, String key16) throws GeneralSecurityException {
        byte[] nonce12 = generateNonce(EncryptionMethod.AES_GCM.getNonceLen());
        return encryptGcm(plaintext, key16, nonce12);
    }

    public static String encryptEcb(String plaintext, String key16) throws GeneralSecurityException {
        Key key = generateKey(key16, EncryptionMethod.AES_ECB.getAlgorithm());
        Cipher c = Cipher.getInstance(EncryptionMethod.AES_ECB.getTransform());
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(ct);
    }

    public static String encryptGcm(String plaintext, String key16, byte[] nonce12) throws GeneralSecurityException {
        if (nonce12 == null || nonce12.length != EncryptionMethod.AES_GCM.getNonceLen())
            throw new IllegalArgumentException("GCM nonce must be " + EncryptionMethod.AES_GCM.getNonceLen() + " bytes");

        Key key = generateKey(key16, EncryptionMethod.AES_GCM.getAlgorithm());
        Cipher c = Cipher.getInstance(EncryptionMethod.AES_GCM.getTransform());
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(EncryptionMethod.AES_GCM.getTagBits(), nonce12));
        byte[] ctTag = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        ByteBuffer buf = ByteBuffer.allocate(nonce12.length + ctTag.length);
        buf.put(nonce12).put(ctTag);
        return Base64.encodeBase64String(buf.array());
    }

    private static Key generateKey(String secretKey, String algorithm) {
        return new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm);
    }

    private static byte[] generateNonce(int length) {
        byte[] iv = new byte[length];
        RANDOM.nextBytes(iv);
        return iv;
    }

}
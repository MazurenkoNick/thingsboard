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
package org.thingsboard.server.common.data.secret;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.SecretId;

import java.io.Serial;
import java.util.Base64;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class Secret extends SecretInfo {

    @Serial
    private static final long serialVersionUID = 3671364019778017637L;

    @EqualsAndHashCode.Exclude
    private String value;

    @JsonIgnore
    private byte[] encryptedValue;

    @JsonGetter("encryptedValue")
    public String getEncryptedValueBase64() {
        return encryptedValue != null ? Base64.getEncoder().encodeToString(encryptedValue) : null;
    }

    @JsonSetter("encryptedValue")
    public void setEncryptedValueBase64(String value) {
        this.encryptedValue = value != null ? Base64.getDecoder().decode(value) : null;
    }


    public Secret() {
        super();
    }

    public Secret(SecretId id) {
        super(id);
    }

    public Secret(Secret secret) {
        super(secret);
        this.value = secret.getValue();
        this.encryptedValue = secret.getEncryptedValue();
    }

    public Secret(SecretInfo secretInfo) {
        super(secretInfo);
        this.value = null;
        this.encryptedValue = null;
    }

    public Secret(SecretInfo secretInfo, byte[] encryptedValue) {
        super(secretInfo);
        this.encryptedValue = encryptedValue;
    }

}

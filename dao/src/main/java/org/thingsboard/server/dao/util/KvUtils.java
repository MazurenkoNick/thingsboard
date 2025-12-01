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
package org.thingsboard.server.dao.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.NoXssValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KvUtils {

    private static final Cache<String, Boolean> validatedKeys;

    static {
        validatedKeys = Caffeine.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .maximumSize(50000).build();
    }

    public static void validate(List<? extends KvEntry> tsKvEntries, boolean valueNoXssValidation) {
        tsKvEntries.forEach(tsKvEntry -> validate(tsKvEntry, valueNoXssValidation));
    }

    public static void validate(KvEntry tsKvEntry, boolean valueNoXssValidation) {
        if (tsKvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        }

        String key = tsKvEntry.getKey();

        if (StringUtils.isBlank(key)) {
            throw new DataValidationException("Key can't be null or empty");
        }

        if (key.length() > 255) {
            throw new DataValidationException("Validation error: key length must be equal or less than 255");
        }

        Boolean isValid = validatedKeys.asMap().get(key);
        if (isValid == null) {
            isValid = NoXssValidator.isValid(key);
            validatedKeys.put(key, isValid);
        }
        if (!isValid) {
            throw new DataValidationException("Validation error: key is malformed");
        }

        if (valueNoXssValidation) {
            Object value = tsKvEntry.getValue();
            if (value instanceof CharSequence || value instanceof JsonNode) {
                if (!NoXssValidator.isValid(value.toString())) {
                    throw new DataValidationException("Validation error: value is malformed");
                }
            }
        }
    }

    public static List<TsKvEntry> toTsKvEntryList(Map<Long, List<KvEntry>> tsKvMap) {
        List<TsKvEntry> tsKvEntryList = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> tsKvEntry : tsKvMap.entrySet()) {
            for (KvEntry kvEntry : tsKvEntry.getValue()) {
                tsKvEntryList.add(new BasicTsKvEntry(tsKvEntry.getKey(), kvEntry));
            }
        }
        return tsKvEntryList;
    }

    public static List<AttributeKvEntry> filterChangedAttr(List<AttributeKvEntry> currentAttributes, List<AttributeKvEntry> newAttributes) {
        if (currentAttributes == null || currentAttributes.isEmpty()) {
            return newAttributes;
        }

        Map<String, AttributeKvEntry> currentAttrMap = currentAttributes.stream()
                .collect(Collectors.toMap(AttributeKvEntry::getKey, Function.identity(), (existing, replacement) -> existing));

        return newAttributes.stream()
                .filter(item -> {
                    AttributeKvEntry cacheAttr = currentAttrMap.get(item.getKey());
                    return cacheAttr == null
                            || !Objects.equals(item.getValue(), cacheAttr.getValue()) //JSON and String can be equals by value, but different by type
                            || !Objects.equals(item.getDataType(), cacheAttr.getDataType());
                })
                .collect(Collectors.toList());
    }

}

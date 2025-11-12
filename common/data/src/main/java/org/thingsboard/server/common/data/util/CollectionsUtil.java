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
package org.thingsboard.server.common.data.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class CollectionsUtil {
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Returns new set with elements that are present in set B(new) but absent in set A(old).
     */
    public static <T> Set<T> diffSets(Set<T> a, Set<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toSet());
    }

    /**
     * Returns new list with elements that are present in list B(new) but absent in list A(old).
     */
    public static <T> List<T> diffLists(List<T> a, List<T> b) {
        return b.stream().filter(p -> !a.contains(p)).collect(Collectors.toList());
    }

    public static <T> boolean contains(Collection<T> collection, T element) {
        return isNotEmpty(collection) && collection.contains(element);
    }

    public static <T> int countNonNull(T[] array) {
        int count = 0;
        for (T t : array) {
            if (t != null) count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<T, T> mapOf(T... kvs) {
        if (kvs.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of parameters");
        }
        Map<T, T> map = new HashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            T key = kvs[i];
            T value = kvs[i + 1];
            map.put(key, value);
        }
        return map;
    }

    public static <V> boolean emptyOrContains(Collection<V> collection, V element) {
        return isEmpty(collection) || collection.contains(element);
    }

    public static <V> HashSet<V> concat(Set<V> set1, Set<V> set2) {
        HashSet<V> result = new HashSet<>();
        result.addAll(set1);
        result.addAll(set2);
        return result;
    }

    public static <V> boolean isOneOf(V value, V... others) {
        if (value == null) {
            return false;
        }
        for (V other : others) {
            if (value.equals(other)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean elementsEqual(Iterable<T> iterable1, Iterable<T> iterable2, BiPredicate<T, T> equalityCheck) {
        if (iterable1 instanceof Collection<?> collection1 && iterable2 instanceof Collection<?> collection2) {
            if (collection1.size() != collection2.size()) {
                return false;
            }
        }

        Iterator<T> iterator1 = iterable1.iterator();
        Iterator<T> iterator2 = iterable2.iterator();
        while (true) {
            if (iterator1.hasNext()) {
                if (!iterator2.hasNext()) {
                    return false;
                }

                T o1 = iterator1.next();
                T o2 = iterator2.next();
                if (equalityCheck.test(o1, o2)) {
                    continue;
                } else {
                    return false;
                }
            }
            return !iterator2.hasNext();
        }
    }

    public static <T> Set<T> addToSet(Set<T> existing, T value) {
        if (existing == null || existing.isEmpty()) {
            return Set.of(value);
        }
        if (existing.contains(value)) {
            return existing;
        }
        Set<T> newSet = new HashSet<>(existing.size() + 1);
        newSet.addAll(existing);
        newSet.add(value);
        return (Set<T>) Set.of(newSet.toArray());
    }

}

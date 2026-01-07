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
package org.thingsboard.server.dao.sqlts.dictionary;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryCompositeKey;
import org.thingsboard.server.dao.model.sqlts.dictionary.KeyDictionaryEntry;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class KeyDictionaryDaoTest extends AbstractServiceTest {

    @Autowired
    private KeyDictionaryDao keyDictionaryDao;

    @Autowired
    private KeyDictionaryRepository keyDictionaryRepository;

    private static final String KEY = "testKeyDictionaryDaoTestKey";

    @Test
    public void testGetOrSaveKeyId_concurrent() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        CountDownLatch allReady = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(threads);

        Integer[] keyIds = new Integer[threads];

        try {
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                executor.submit(() -> {
                    allReady.countDown();
                    try {
                        // wait until all threads are ready
                        start.await();
                        // concurrent call
                        Integer id = keyDictionaryDao.getOrSaveKeyId(KEY);
                        keyIds[idx] = id;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        allDone.countDown();
                    }
                });
            }

            // ensure all threads are queued
            allReady.await(5, TimeUnit.SECONDS);
            // fire the start gun
            start.countDown();
            // wait for all to finish
            allDone.await(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        // basic sanity
        for (int i = 0; i < threads; i++) {
            assertThat(keyIds[i])
                    .as("keyId[%s]", i)
                    .isNotNull()
                    .isGreaterThan(0);
        }

        // all threads must see the same keyId
        int first = keyIds[0];
        assertThat(first).isGreaterThan(0);
        assertThat(Arrays.stream(keyIds).distinct().count())
                .as("all threads should get the same keyId")
                .isEqualTo(1);

        // DB must have exactly one row for this key and the same id
        KeyDictionaryCompositeKey id = new KeyDictionaryCompositeKey(KEY);
        Optional<KeyDictionaryEntry> entry = keyDictionaryRepository.findById(id);

        assertThat(entry.isPresent()).isTrue();
        assertThat(entry.get().getKeyId()).isEqualTo(first);

        keyDictionaryRepository.deleteById(id);
    }

}

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
package org.thingsboard.server.dao.nosql;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TbResultSetTest {

    @Test
    void allRows_withinLimit_returnsAllRows() throws Exception {
        Row row = mock(Row.class);
        AsyncResultSet asyncResultSet = createMockResultSet(List.of(row), false, 1000);
        Statement<?> statement = mock(Statement.class);

        TbResultSet tbResultSet = new TbResultSet(statement, asyncResultSet, s -> null);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor(), 5000);

        List<Row> result = future.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(row);
    }

    @Test
    void allRows_exceedsLimitOnFirstPage_failsWithException() {
        Row row = mock(Row.class);
        AsyncResultSet asyncResultSet = createMockResultSet(List.of(row), false, 6000);
        Statement<?> statement = mock(Statement.class);

        TbResultSet tbResultSet = new TbResultSet(statement, asyncResultSet, s -> null);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor(), 5000);

        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ResultSetSizeLimitExceededException.class);
    }

    @Test
    void allRows_exceedsLimitOnSecondPage_failsAfterSecondPage() {
        Row row1 = mock(Row.class);
        Row row2 = mock(Row.class);
        Statement<?> statement = mock(Statement.class);
        doReturn(statement).when(statement).setPagingState((ByteBuffer) null);

        AsyncResultSet page2 = createMockResultSet(List.of(row2), false, 3000);
        TbResultSet tbResultSetPage2 = new TbResultSet(statement, page2, s -> null);
        SettableFuture<TbResultSet> page2Future = SettableFuture.create();
        page2Future.set(tbResultSetPage2);
        TbResultSetFuture tbPage2Future = new TbResultSetFuture(page2Future);

        ExecutionInfo page1ExecInfo = mock(ExecutionInfo.class);
        when(page1ExecInfo.getResponseSizeInBytes()).thenReturn(3000);
        when(page1ExecInfo.getPagingState()).thenReturn(null);

        AsyncResultSet page1 = createMockResultSet(List.of(row1), true, 3000);
        when(page1.getExecutionInfo()).thenReturn(page1ExecInfo);

        Function<Statement, TbResultSetFuture> executeAsync = s -> tbPage2Future;
        TbResultSet tbResultSet = new TbResultSet(statement, page1, executeAsync);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor(), 5000);

        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ResultSetSizeLimitExceededException.class);
    }

    @Test
    void allRows_unlimitedWithZero_returnsAllRowsRegardlessOfSize() throws Exception {
        Row row = mock(Row.class);
        AsyncResultSet asyncResultSet = createMockResultSet(List.of(row), false, 999999);
        Statement<?> statement = mock(Statement.class);

        TbResultSet tbResultSet = new TbResultSet(statement, asyncResultSet, s -> null);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor(), 0);

        List<Row> result = future.get();
        assertThat(result).hasSize(1);
    }

    @Test
    void allRows_noLimitOverload_returnsAllRows() throws Exception {
        Row row = mock(Row.class);
        AsyncResultSet asyncResultSet = createMockResultSet(List.of(row), false, 999999);
        Statement<?> statement = mock(Statement.class);

        TbResultSet tbResultSet = new TbResultSet(statement, asyncResultSet, s -> null);
        ListenableFuture<List<Row>> future = tbResultSet.allRows(MoreExecutors.directExecutor());

        List<Row> result = future.get();
        assertThat(result).hasSize(1);
    }

    private AsyncResultSet createMockResultSet(List<Row> rows, boolean hasMorePages, int responseSizeInBytes) {
        AsyncResultSet resultSet = mock(AsyncResultSet.class);
        ExecutionInfo executionInfo = mock(ExecutionInfo.class);
        ColumnDefinitions columnDefs = mock(ColumnDefinitions.class);

        when(executionInfo.getResponseSizeInBytes()).thenReturn(responseSizeInBytes);
        when(executionInfo.getPagingState()).thenReturn(null);
        when(resultSet.getExecutionInfo()).thenReturn(executionInfo);
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefs);
        when(resultSet.currentPage()).thenReturn(rows);
        when(resultSet.hasMorePages()).thenReturn(hasMorePages);
        when(resultSet.remaining()).thenReturn(rows.size());

        return resultSet;
    }

}

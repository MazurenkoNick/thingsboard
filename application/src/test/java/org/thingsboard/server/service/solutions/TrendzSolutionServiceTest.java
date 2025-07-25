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
package org.thingsboard.server.service.solutions;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.thingsboard.rest.client.TrendzApiClient;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.solutions.data.SolutionInstallContext;
import org.thingsboard.server.service.solutions.trendz.TrendzEntityPreprocessor;
import org.thingsboard.server.service.solutions.trendz.TrendzEntityPreprocessorManager;
import org.thingsboard.server.service.solutions.trendz.data.TrendzEntityType;
import org.thingsboard.server.service.solutions.trendz.data.TrendzPreprocessConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TrendzSolutionServiceTest {

    @Mock
    private TrendzSettingsService trendzSettingsService;
    @Mock
    private JwtTokenFactory jwtTokenFactory;
    @Mock
    private TrendzEntityPreprocessorManager trendzEntityPreprocessorManager;
    @Mock
    private TrendzEntityPreprocessor trendzEntityPreprocessor;

    @InjectMocks
    private TrendzSolutionService trendzSolutionService;

    private AutoCloseable closeable;
    private Path tempSolutionDir;

    @Before
    public void setUp() throws IOException {
        closeable = MockitoAnnotations.openMocks(this);
        tempSolutionDir = Files.createTempDirectory("solutionDir");
    }

    @After
    public void tearDown() throws Exception {
        if (tempSolutionDir != null && Files.exists(tempSolutionDir)) {
            Files.walk(tempSolutionDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        closeable.close();
    }

    private void createMockSolutionFolder(String solutionId, String dummyJson) throws IOException {
        Path entitiesDir = tempSolutionDir.resolve(Paths.get(solutionId, "entities"));
        Files.createDirectories(entitiesDir);
        Path migrationFile = entitiesDir.resolve("trendz_migration_data.json");
        Files.write(migrationFile, dummyJson.getBytes(StandardCharsets.UTF_8));
    }


    @Test
    public void testGetTrendzUrlMocking() throws Exception {
        String solutionId = "testSolution";
        UUID dummyUserId = UUID.randomUUID();
        String dummyUrl = "https://trendz.test.cloud.tb-trendz.com/trendz/";
        String dummyApiKey = "dummyApiKey";
        String dummyTokenString = "dummyJwtToken";
        UUID dummyImportExecutionId = UUID.randomUUID();
        String dummyJson = """
                {
                    "businessEntities":[],
                    "viewConfigs":[],
                    "viewCollections":[],
                    "calculationFields":[],
                    "predictionModels":[],
                    "anomalyModels":[],
                    "modelToItemToLastPointMap":{},
                    "segmentData":[],
                    "tasks":[],
                    "taskSequences":[]
                }
                """;
        ObjectNode dummyResult = JsonNodeFactory.instance.objectNode();

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        SolutionInstallContext ctx = new SolutionInstallContext(tenantId, solutionId, new SecurityUser(new UserId(dummyUserId)), null);

        createMockSolutionFolder(solutionId, dummyJson);

        TrendzSettings trendzSettings = new TrendzSettings(true, dummyUrl, dummyApiKey);

        doReturn(trendzSettings)
                .when(this.trendzSettingsService)
                .findTrendzSettings(any(TenantId.class));

        AccessJwtToken dummyToken = mock(AccessJwtToken.class);
        when(dummyToken.getToken()).thenReturn(dummyTokenString);

        doReturn(dummyToken)
                .when(this.jwtTokenFactory)
                .createAccessJwtToken(any(SecurityUser.class));


        when(this.trendzEntityPreprocessor.getEntityType())
                .thenAnswer(invocation -> TrendzEntityType.BUSINESS_ENTITY);

        doNothing()
                .when(this.trendzEntityPreprocessor)
                .preprocess(any(TrendzPreprocessConfig.class));

        for (TrendzEntityType type : TrendzEntityType.values()) {
            when(this.trendzEntityPreprocessorManager.getPreprocessor(eq(type)))
                    .thenAnswer(invocation -> this.trendzEntityPreprocessor);
        }

        try (MockedConstruction<TrendzApiClient> mc = Mockito.mockConstruction(
                TrendzApiClient.class,
                (mock, context) -> {
                    when(mock.isTrendzServiceReachable()).thenReturn(true);
                    when(mock.sendTrendzSubscriptionValid()).thenReturn(true);
                    when(mock.sendCheckSigningKey()).thenReturn(true);
                    when(mock.sendImportMigrationData(any(ObjectNode.class)))
                            .thenReturn(dummyImportExecutionId);
                    when(mock.awaitTaskExecution(dummyImportExecutionId))
                            .thenReturn(dummyResult);
                })) {

            this.trendzSolutionService.provisionTrendzSolution(tempSolutionDir, ctx);

            List<TrendzApiClient> constructed = mc.constructed();
            assertEquals(1, constructed.size());
            TrendzApiClient client = constructed.get(0);

            verify(client).isTrendzServiceReachable();
            verify(client).sendTrendzSubscriptionValid();
            verify(client).sendCheckSigningKey();
            verify(client).sendImportMigrationData(any());
            verify(client).awaitTaskExecution(dummyImportExecutionId);
        }
    }
}

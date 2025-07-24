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
package org.thingsboard.server.service.solutions.trendz.preprocessor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.solutions.trendz.TrendzEntityPreprocessor;
import org.thingsboard.server.service.solutions.trendz.data.TrendzEntityType;
import org.thingsboard.server.service.solutions.trendz.data.TrendzPreprocessConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class AnomalyModelPreprocessor extends TrendzEntityPreprocessor {

    private static final Set<String> VIEW_FIELD_FIELDS_NAMES = Set.of("id", "entityFieldId", "businessEntityId", "anomalyModelId", "selectedAnomalyField", "predictionModelId", "predictionModelOrigEntityFieldId");

    @Override
    public TrendzEntityType getEntityType() {
        return TrendzEntityType.ANOMALY_MODEL;
    }

    @Override
    public void preprocess(TrendzPreprocessConfig config) {
        Map<String, Object> importData = config.getImportData();
        Map<UUID, UUID> oldToNewIdMap = config.getOldToNewIdMap();

        String collectionName = getEntityType().getCollectionName();
        List<Object> anomalyModels = (List<Object>) importData.get(collectionName);
        for (Object anomalyModel : anomalyModels) {
            setNewId(oldToNewIdMap, anomalyModel, "id");

            Map<String, Object> anomalyModelMap = (Map<String, Object>) anomalyModel;

            // User

            Map<String, Object> userMap = (Map<String, Object>) anomalyModelMap.get("user");
            setUserIds(oldToNewIdMap, userMap, config);

            // Properties

            Map<String, Object> properties = (Map<String, Object>) anomalyModelMap.get("properties");
            setNewId(oldToNewIdMap, properties, "id");

            // DatasetConfig

            Map<String, Object> datasetConfig = (Map<String, Object>) anomalyModelMap.get("datasetConfig");
            setNewId(oldToNewIdMap, datasetConfig, "id");
            setNewId(oldToNewIdMap, datasetConfig, "businessEntityId");

            List<Object> fields = (List<Object>) datasetConfig.get("fields");
            List<Object> hiddenFields = (List<Object>) datasetConfig.get("hiddenFields");

            List<Object> viewFields = Stream.of(fields, hiddenFields)
                    .flatMap(Collection::stream)
                    .toList();

            for (Object viewField : viewFields) {
                for (String fieldName : VIEW_FIELD_FIELDS_NAMES) {
                    setNewId(oldToNewIdMap, viewField, fieldName);
                }
                setNewIdList(oldToNewIdMap, viewField, "multivariablePredictionFieldIdList");
            }

            List<Object> runtimeFilters = (List<Object>) datasetConfig.get("runtimeFilters");
            for (Object runtimeFilter : runtimeFilters) {
                setNewId(oldToNewIdMap, runtimeFilter, "viewFieldId");
            }

            // Clusters

            List<Object> clusters = (List<Object>) ((Map<String, Object>) anomalyModel).get("clusters");
            for (Object clusterInfo : clusters) {
                setNewId(oldToNewIdMap, clusterInfo, "id");
                setNewIdMap(oldToNewIdMap, clusterInfo, "clusterExamples");
            }
        }
    }
}

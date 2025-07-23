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

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class PredictionModelPreprocessor extends TrendzEntityPreprocessor {

    private static final Set<String> PREDICTION_MODEL_FIELDS_NAMES = Set.of("id", "associatedBusinessEntityFieldId");
    private static final Set<String> DATASOURCE_FIELDS_NAMES = Set.of("businessEntityId", "businessEntityFieldId");

    @Override
    public TrendzEntityType getEntityType() {
        return TrendzEntityType.PREDICTION_MODEL;
    }

    @Override
    public void preprocess(TrendzPreprocessConfig config) {
        Map<String, Object> importData = config.getImportData();
        Map<UUID, UUID> oldToNewIdMap = config.getOldToNewIdMap();
        Map<String, String> nameToIdMap = config.getNameToIdMap();

        ZonedDateTime startDate = config.getStartDate();
        ZonedDateTime endDate = config.getEndDate();

        String collectionName = getEntityType().getCollectionName();
        List<Object> predictionModels = (List<Object>) importData.get(collectionName);

        // Prepare item ids
        for (Object predictionModel : predictionModels) {
            List<Object> trainedItemsSet = (List<Object>) ((Map<String, Object>) predictionModel).get("trainedItemsSet");
            for (Object item : trainedItemsSet) {
                Map<String, Object> itemMap = (Map<String, Object>) item;
                String itemName = itemMap.get("name").toString();
                UUID oldItemId = UUID.fromString(itemMap.get("id").toString());
                UUID newItemId = nameToIdMap.containsKey(itemName) ? UUID.fromString(nameToIdMap.get(itemName)) : null;
                if (newItemId != null) {
                    oldToNewIdMap.put(oldItemId, newItemId);
                }
            }
        }

        // Prepare data

        importData.put("modelToItemToLastPointMap", Collections.emptyMap());
        importData.put("segmentData", Collections.emptyList());

        for (Object predictionModel : predictionModels) {
            setUserIds(oldToNewIdMap, predictionModel, config);
            for (String fieldName : PREDICTION_MODEL_FIELDS_NAMES) {
                setNewId(oldToNewIdMap, predictionModel, fieldName);
            }
            setNewIdMap(oldToNewIdMap, predictionModel, "itemIdToStateMap");

            List<Object> trainedItemsSet = (List<Object>) ((Map<String, Object>) predictionModel).get("trainedItemsSet");
            for (Object item : trainedItemsSet) {
                Map<String, Object> itemMap = (Map<String, Object>) item;
                UUID oldItemId = UUID.fromString(itemMap.get("id").toString());
                if (oldToNewIdMap.containsKey(oldItemId)) {
                    setNewId(oldToNewIdMap, item, "id");
                }
            }

            // Datasource parameters

            Object datasourceParameters = ((Map<String, Object>) predictionModel).get("datasourceParameters");
            for (String fieldName : DATASOURCE_FIELDS_NAMES) {
                setNewId(oldToNewIdMap, datasourceParameters, fieldName);
            }

            List<Object> itemSet = (List<Object>) ((Map<String, Object>) datasourceParameters).get("itemSet");
            for (Object item : itemSet) {
                Map<String, Object> itemMap = (Map<String, Object>) item;
                UUID oldItemId = UUID.fromString(itemMap.get("id").toString());
                if (oldToNewIdMap.containsKey(oldItemId)) {
                    setNewId(oldToNewIdMap, item, "id");
                }
            }

            Map<String, Object> datasourceParametersMap = (Map<String, Object>) datasourceParameters;
            datasourceParametersMap.put("trainStartTs", startDate.toInstant().toEpochMilli());
            datasourceParametersMap.put("trainEndTs", endDate.toInstant().toEpochMilli());
        }
    }
}

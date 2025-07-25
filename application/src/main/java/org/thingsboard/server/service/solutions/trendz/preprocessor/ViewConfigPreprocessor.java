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
public class ViewConfigPreprocessor extends TrendzEntityPreprocessor {

    private static final Set<String> VIEW_CONFIG_FIELDS_NAMES = Set.of("id", "collectionId", "rootEntityId", "rowClickEntityId", "selectedFilterViewFieldId");
    private static final Set<String> VIEW_FIELD_FIELDS_NAMES = Set.of("id", "entityFieldId", "businessEntityId", "anomalyModelId", "selectedAnomalyField", "predictionModelId", "predictionModelOrigEntityFieldId");
    private static final Set<String> DATASET_FIELDS_NAMES = Set.of("datasetEnableMap", "datasetItemMap", "datasetDataMap");

    @Override
    public TrendzEntityType getEntityType() {
        return TrendzEntityType.VIEW_CONFIG;
    }

    @Override
    public void preprocess(TrendzPreprocessConfig config) {
        Map<String, Object> importData = config.getImportData();
        Map<UUID, UUID> oldToNewIdMap = config.getOldToNewIdMap();

        String collectionName = getEntityType().getCollectionName();
        List<Object> viewConfigs = (List<Object>) importData.get(collectionName);
        for (Object viewConfig : viewConfigs) {
            setUserIds(oldToNewIdMap, viewConfig, config);

            for (String fieldName : VIEW_CONFIG_FIELDS_NAMES) {
                setNewId(oldToNewIdMap, viewConfig, fieldName);
            }

            List<Object> xAxisFields = (List<Object>) ((Map<String, Object>) viewConfig).get("xAxis");
            List<Object> yAxisFields = (List<Object>) ((Map<String, Object>) viewConfig).get("yAxis");
            List<Object> seriesFields = (List<Object>) ((Map<String, Object>) viewConfig).get("series");
            List<Object> hiddenFields = (List<Object>) ((Map<String, Object>) viewConfig).get("hiddenFields");

            List<Object> viewFields = Stream.of(xAxisFields, yAxisFields, seriesFields, hiddenFields)
                    .flatMap(Collection::stream)
                    .toList();

            for (Object viewField : viewFields) {
                for (String fieldName : VIEW_FIELD_FIELDS_NAMES) {
                    setNewId(oldToNewIdMap, viewField, fieldName);
                }
                setNewIdList(oldToNewIdMap, viewField, "multivariablePredictionFieldIdList");
            }

            List<Object> runtimeFilters = (List<Object>) ((Map<String, Object>) viewConfig).get("runtimeFilters");
            for (Object runtimeFilter : runtimeFilters) {
                setNewId(oldToNewIdMap, runtimeFilter, "viewFieldId");
            }

            for (String fieldName : DATASET_FIELDS_NAMES) {
                setNewIdMap(oldToNewIdMap, viewConfig, fieldName);
            }
        }
    }
}

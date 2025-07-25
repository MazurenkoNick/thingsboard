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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class TaskSequencePreprocessor extends TrendzEntityPreprocessor {

    @Override
    public TrendzEntityType getEntityType() {
        return TrendzEntityType.TASK_SEQUENCE;
    }

    @Override
    public void preprocess(TrendzPreprocessConfig config) {
        Map<String, Object> importData = config.getImportData();
        Map<UUID, UUID> oldToNewIdMap = config.getOldToNewIdMap();

        String collectionName = getEntityType().getCollectionName();
        List<Object> taskSequences = (List<Object>) importData.get(collectionName);
        for (Object taskSequence : taskSequences) {
            Map<String, Object> user = (Map<String, Object>) ((Map<String, Object>) taskSequence).get("user");
            setUserIds(oldToNewIdMap, user, config);
            setNewId(oldToNewIdMap, taskSequence, "id");

            List<Object> items = (List<Object>) ((Map<String, Object>) taskSequence).get("sequenceItems");
            for (Object item : items) {
                setNewId(oldToNewIdMap, item, "id");

                Map<String, Object> reference = (Map<String, Object>) ((Map<String, Object>) item).get("reference");
                setNewId(oldToNewIdMap, reference, "key");
            }
        }
    }
}

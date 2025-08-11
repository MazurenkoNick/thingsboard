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
package org.thingsboard.server.common.data.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.DataReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.common.data.util.DataUtils.getChildObjects;

@Schema
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ReportTemplate extends BaseReportTemplate {

    @Serial
    private static final long serialVersionUID = 1729877416392618039L;

    @Schema(description = "a JSON value with report template configuration")
    @Valid
    @NotNull
    private ReportTemplateConfig configuration;

    public ReportTemplate() {
        super();
    }

    public ReportTemplate(ReportTemplateId id) {
        super(id);
    }

    public ReportTemplate(BaseReportTemplate reportTemplate) {
        super(reportTemplate);
    }

    public ReportTemplate(ReportTemplate reportTemplate) {
        super(reportTemplate);
        if (reportTemplate.getConfiguration() != null) {
            this.configuration = mapper.convertValue(
                    mapper.valueToTree(reportTemplate.getConfiguration()),
                    ReportTemplateConfig.class
            );
        }
    }

    @JsonIgnore
    public List<ObjectNode> getEntityAliasesConfig() {
        return getChildObjects("entityAliases", mapper.valueToTree(configuration));
    }

    @JsonIgnore
    public List<ObjectNode> getComponentDataSources() {
        List<ReportComponent> components = configuration.getComponents();
        if (components == null || components.isEmpty()) {
            return Collections.emptyList();
        }
        return components.stream()
                .filter(component -> component instanceof DataReportComponent)
                .flatMap(component -> ((DataReportComponent) component).getDataSources().stream())
                .map(fromValue -> (ObjectNode) mapper.valueToTree(fromValue))
                .toList();
    }

}

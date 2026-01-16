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
package org.thingsboard.server.report.renderer;

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.components.LayoutReportComponent;
import org.thingsboard.server.common.data.report.configuration.style.Insets;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.util.ColorUtils;
import org.thingsboard.server.report.util.ThymeleafUtil;

import java.util.HashMap;
import java.util.Map;

public abstract class ReportComponentWithLayoutRenderer<C extends LayoutReportComponent> implements PdfReportComponentRenderer<C> {

    private static final int DEFAULT_COMPONENT_MARGIN_SIZE = 0;
    private static final int DEFAULT_COMPONENT_PADDING_SIZE = 0;
    private static final int DEFAULT_COMPONENT_BORDER_SIZE = 0;

    protected int layoutWidthPx;

    @Override
    public String render(C component, ComponentData reportDataSource) {
        if (StringUtils.isNotBlank(reportDataSource.getError())) {
            return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", Map.of("errorMessage", reportDataSource.getError()));
        }
        Insets margins = component.getMargins();
        if (margins == null) {
            margins = new Insets(DEFAULT_COMPONENT_MARGIN_SIZE);
        }
        Insets paddings = component.getPaddings();
        if (paddings == null) {
            paddings = new Insets(DEFAULT_COMPONENT_PADDING_SIZE);
        }
        Integer borderWidth = component.getBorderWidth();
        if (borderWidth == null) {
            borderWidth = DEFAULT_COMPONENT_BORDER_SIZE;
        }

        this.layoutWidthPx = reportDataSource.getUsablePageWidthPx();
        this.layoutWidthPx = (int)((float)this.layoutWidthPx - (float)(margins.getLeft() + margins.getRight()) * 4f / 3f);
        this.layoutWidthPx = (int)((float)this.layoutWidthPx - (float)(paddings.getLeft() + paddings.getRight()) * 4f / 3f);
        this.layoutWidthPx = (int)((float)this.layoutWidthPx - (float)(borderWidth * 2) * 4f / 3f);

        String content = this.renderContent(component, reportDataSource);
        Map<String, Object> layoutVariables = new HashMap<>();
        layoutVariables.put("htmlContent", content);
        layoutVariables.put("background", component.getBackground() != null ? ColorUtils.normalizeCssColor(component.getBackground()) : "transparent");
        layoutVariables.put("borderWidth", borderWidth);
        layoutVariables.put("borderRadius", component.getBorderRadius() != null ? component.getBorderRadius() : "0");
        layoutVariables.put("borderColor", component.getBorderColor() != null ? ColorUtils.normalizeCssColor(component.getBorderColor()) : "transparent");

        layoutVariables.put("leftMargin", margins.getLeft());
        layoutVariables.put("rightMargin", margins.getRight());
        layoutVariables.put("topMargin", margins.getTop());
        layoutVariables.put("bottomMargin", margins.getBottom());

        layoutVariables.put("leftPadding", paddings.getLeft());
        layoutVariables.put("rightPadding", paddings.getRight());
        layoutVariables.put("topPadding", paddings.getTop());
        layoutVariables.put("bottomPadding", paddings.getBottom());

        return ThymeleafUtil.renderFromHtmlTemplate("html/components/component-layout", layoutVariables);
    }

    protected abstract String renderContent(C component, ComponentData reportDataSource);
}

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
package org.thingsboard.server.report.util;

import net.sf.jasperreports.engine.JRTextField;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import org.thingsboard.server.report.context.ReportLayoutContext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JasperReportUtils {

    public static final Pattern REPORT_NAME_DATE_PATTERN = Pattern.compile("%d\\{([^\\}]*)\\}");
    public static final String DEFAULT_NAME_PATTERN = "report-%d{yyyy-MM-dd_HH:mm:ss}";

    public static JRDesignTextField createJRTextField(int layoutWidth) {
        JRDesignTextField htmlField = new JRDesignTextField();
        htmlField.setX(0);
        htmlField.setY(0);
        htmlField.setWidth(layoutWidth);
        htmlField.setHeight(1);
        htmlField.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
        return htmlField;
    }

    public static String toJRExpression(String template) {
        StringBuilder expr = new StringBuilder("\"");
        int pos = 0;

        while (pos < template.length()) {
            int start = template.indexOf("${", pos);
            if (start == -1) {
                // No more placeholders
                expr.append(template.substring(pos).replace("\"", "\\\""));
                break;
            }

            // Append literal text before placeholder
            expr.append(template.substring(pos, start).replace("\"", "\\\""));
            expr.append("\" + ");

            int end = template.indexOf('}', start);
            if (end == -1) {
                throw new IllegalArgumentException("Unmatched '${' in template: " + template);
            }

            String fieldName = template.substring(start + 2, end).trim();
            expr.append("$F{" + fieldName + "} + \"");

            pos = end + 1;
        }

        expr.append("\"");
        return expr.toString();
    }

    public static void addTextElement(ReportLayoutContext layoutCtx, JRTextField jrTextField) {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(1);
        detailBand.setSplitType(SplitTypeEnum.STRETCH);

        detailBand.addElement(jrTextField);

        JRDesignSection detailSection = (JRDesignSection) layoutCtx.getJasperDesign().getDetailSection();
        detailSection.addBand(detailBand);
    }

    public static JRDesignTextField createTextField(String expression, int x, int y) {
        JRDesignTextField field = new JRDesignTextField();
        field.setX(x);
        field.setY(y);
        field.setWidth(180);
        field.setHeight(20);
        field.setBlankWhenNull(true);
        field.setExpression(new JRDesignExpression(expression));
        return field;
    }

    public static String prepareReportName(String namePattern, Date reportDate, TimeZone tz) {
        String name = (namePattern == null || namePattern.isEmpty()) ? DEFAULT_NAME_PATTERN : namePattern;
        Matcher matcher = REPORT_NAME_DATE_PATTERN.matcher(name);
        while (matcher.find()) {
            String toReplace = matcher.group(0);
            SimpleDateFormat dateFormat = new SimpleDateFormat(matcher.group(1));
            dateFormat.setTimeZone(tz);
            String replacement = dateFormat.format(reportDate);
            name = name.replace(toReplace, replacement);
        }
        return name;
    }

}

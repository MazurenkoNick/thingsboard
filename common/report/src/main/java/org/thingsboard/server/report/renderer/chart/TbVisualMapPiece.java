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
package org.thingsboard.server.report.renderer.chart;

import lombok.Data;

import java.awt.Color;
import java.awt.Paint;

import static org.thingsboard.server.report.renderer.chart.ChartUtils.createFillPaint;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

@Data
public class TbVisualMapPiece {

    private Double lt;
    private Double gte;
    private Double value;
    private String color;

    private Paint paint;
    private Paint fillPaint;

    public static TbVisualMapPiece fromRange(String color, Double from, Double to) {
        TbVisualMapPiece piece = new TbVisualMapPiece();
        piece.color = color;
        if (isNumber(from) && isNumber(to)) {
            if (from.compareTo(to) == 0) {
                piece.value = from;
            } else {
                piece.gte = from;
                piece.lt = to;
            }
        } else if (isNumber(from)) {
            piece.gte = from;
        } else if (isNumber(to)) {
            piece.lt = to;
        }
        return piece;
    }

    public void setupPaints(boolean fillArea, float fillAreaOpacity) {
        Color paint = safeParseCssColor(color);
        this.paint = paint;
        this.fillPaint = createFillPaint(fillArea, fillAreaOpacity, paint);
    }

    public boolean matchValue(double value) {
        if (isNumber(gte) && isNumber(lt)) {
            return value >= gte && value < lt;
        } else if (isNumber(gte)) {
            return value >= gte;
        } else if (isNumber(lt)) {
            return value < lt;
        } else if (isNumber(this.value)) {
            return this.value == value;
        } else {
            return false;
        }
    }

    public boolean matchLower(double lower) {
        if (isNumber(gte) && isNumber(lt)) {
            return lower >= gte && lower < lt;
        } else if (isNumber(gte)) {
            return lower >= gte;
        } else if (isNumber(lt)) {
            return lower < lt;
        } else {
            return false;
        }
    }

    public boolean greaterLower(double lower) {
        if (isNumber(gte)) {
            return gte > lower;
        }
        if (isNumber(lt)) {
            return lower < lt;
        }
        return false;
    }

    public double getUpper(double maxUpper) {
        if (isNumber(lt)) {
            return Math.min(maxUpper, lt);
        } else {
            return maxUpper;
        }
    }

    public double getNearestUpper(double maxUpper) {
        if (isNumber(gte)) {
            return Math.min(maxUpper, gte);
        }
        if (isNumber(lt)) {
            return Math.min(maxUpper, lt);
        } else {
            return maxUpper;
        }
    }

    private static boolean isNumber(Double number) {
        return number != null && Double.isFinite(number);
    }

}

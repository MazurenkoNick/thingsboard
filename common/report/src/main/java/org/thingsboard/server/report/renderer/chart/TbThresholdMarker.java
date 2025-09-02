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

import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.ui.RectangleInsets;

import java.awt.*;

public class TbThresholdMarker extends ValueMarker {

    private static RectangleInsets DEFAULT_LABEL_OFFSET = new RectangleInsets(5,5,5,5);
    private static RectangleInsets BACKGROUND_LABEL_OFFSET = new RectangleInsets(7,8,7,8);

    private Shape startShape;
    private Shape endShape;

    private Paint startShapeFillPaint;
    private Stroke startShapeOutlineStroke;

    private Paint endShapeFillPaint;
    private Stroke endShapeOutlineStroke;

    private boolean drawLabelBackground;

    public TbThresholdMarker(double value) {
        super(value);
        setLabelOffset(DEFAULT_LABEL_OFFSET);
        setAlpha(1f);
    }

    public void setStartShape(Shape startShape) {
        this.startShape = startShape;
    }

    public Shape getStartShape() {
        return startShape;
    }

    public void setEndShape(Shape endShape) {
        this.endShape = endShape;
    }

    public Shape getEndShape() {
        return endShape;
    }

    public Paint getStartShapeFillPaint() {
        return startShapeFillPaint;
    }

    public void setStartShapeFillPaint(Paint startShapeFillPaint) {
        this.startShapeFillPaint = startShapeFillPaint;
    }

    public Stroke getStartShapeOutlineStroke() {
        return startShapeOutlineStroke;
    }

    public void setStartShapeOutlineStroke(Stroke startShapeOutlineStroke) {
        this.startShapeOutlineStroke = startShapeOutlineStroke;
    }

    public Paint getEndShapeFillPaint() {
        return endShapeFillPaint;
    }

    public void setEndShapeFillPaint(Paint endShapeFillPaint) {
        this.endShapeFillPaint = endShapeFillPaint;
    }

    public Stroke getEndShapeOutlineStroke() {
        return endShapeOutlineStroke;
    }

    public void setEndShapeOutlineStroke(Stroke endShapeOutlineStroke) {
        this.endShapeOutlineStroke = endShapeOutlineStroke;
    }

    public boolean isDrawLabelBackground() {
        return drawLabelBackground;
    }

    public void setDrawLabelBackground(boolean drawLabelBackground) {
        this.drawLabelBackground = drawLabelBackground;
        if (this.drawLabelBackground) {
            setLabelOffset(BACKGROUND_LABEL_OFFSET);
        } else {
            setLabelOffset(DEFAULT_LABEL_OFFSET);
        }
    }
}

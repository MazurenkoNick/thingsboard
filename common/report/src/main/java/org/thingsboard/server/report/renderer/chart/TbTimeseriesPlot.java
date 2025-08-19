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

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public class TbTimeseriesPlot extends XYPlot {

    private Paint topOutlinePaint;

    private Paint rightOutlinePaint;

    public TbTimeseriesPlot() {
        super();
    }

    public void setTopOutlinePaint(Paint topOutlinePaint) {
        this.topOutlinePaint = topOutlinePaint;
    }

    public void setRightOutlinePaint(Paint rightOutlinePaint) {
        this.rightOutlinePaint = rightOutlinePaint;
    }

    @Override
    public void drawOutline(Graphics2D g2, Rectangle2D dataArea) {
        if (getOutlinePaint() != null && getOutlineStroke() != null) {
            super.drawOutline(g2, dataArea);
        } else if (topOutlinePaint != null || rightOutlinePaint != null) {
           // Shape originalClip = g2.getClip();
            //g2.clip(dataArea);
            Object saved = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            g2.setStroke(getOutlineStroke());

            double x1 = dataArea.getMinX();
            double y1 = dataArea.getMinY();
            double x2 = dataArea.getMaxX();
            double y2 = dataArea.getMaxY();

            if (topOutlinePaint != null) {
                g2.setPaint(topOutlinePaint);
                // Draw the top line
                g2.draw(new Line2D.Double(x1, y1, x2, y1));
            }

            if (rightOutlinePaint != null) {
                g2.setPaint(rightOutlinePaint);
                // Draw the right line
                g2.draw(new Line2D.Double(x2, y1, x2, y2));
            }
           // g2.setClip(originalClip);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved);
        }
    }
}

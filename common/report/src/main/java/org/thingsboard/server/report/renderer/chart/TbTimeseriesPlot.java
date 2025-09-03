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

import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TbTimeseriesPlot extends XYPlot {

    public TbTimeseriesPlot() {
        super();
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
                     PlotState parentState, PlotRenderingInfo info) {

        this.updateInsets(g2, area);

        super.draw(g2, area, anchor, parentState, info);
    }

    @Override
    public void drawOutline(Graphics2D g2, Rectangle2D dataArea) {
        super.drawOutline(g2, dataArea);
        if (isRangeGridlinesVisible() && getRangeGridlinePaint() != null) {
            g2.setStroke(getRangeGridlineStroke());
            g2.setPaint(getRangeGridlinePaint());
            this.drawBorderLines(g2, dataArea, true);
        }
        if (isDomainGridlinesVisible() && getDomainGridlinePaint() != null) {
            g2.setStroke(getDomainGridlineStroke());
            g2.setPaint(getDomainGridlinePaint());
            this.drawBorderLines(g2, dataArea, false);
        }
    }

    private void drawBorderLines(Graphics2D g2, Rectangle2D dataArea, boolean horizontal) {
        Object saved = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        double x1 = dataArea.getMinX();
        double y1 = dataArea.getMinY();
        double x2 = dataArea.getMaxX();
        double y2 = dataArea.getMaxY();
        if (horizontal) {
            // Draw the top line
            g2.draw(new Line2D.Double(x1, y1, x2, y1));
            // Draw the bottom line
            g2.draw(new Line2D.Double(x1, y2, x2, y2));
        } else {
            // Draw the left line
            g2.draw(new Line2D.Double(x1, y1, x1, y2));
            // Draw the right line
            g2.draw(new Line2D.Double(x2, y1, x2, y2));
        }
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, saved);
    }

    private void updateInsets(Graphics2D g2, Rectangle2D area) {

        Rectangle2D measureArea = (Rectangle2D ) area.clone();
        RectangleInsets insets = getInsets();
        insets.trim(measureArea);
        AxisSpace space = calculateAxisSpace(g2, measureArea);
        Rectangle2D dataArea = space.shrink(measureArea, null);
        this.getAxisOffset().trim(dataArea);
        dataArea = integerise(dataArea);

        double dataAreaLeft = dataArea.getMinX();
        double dataAreaRight = area.getMaxX() - dataArea.getMaxX();

        List<TbThresholdMarker> thresholdMarkers = this.computeVisibleThresholdMarkers();
        double[] offset = new double[]{0,0};
        for (TbThresholdMarker marker : thresholdMarkers) {
            double[] markerOffset = marker.measureOffset(g2);
            offset[0] = Math.max(offset[0], markerOffset[0]);
            offset[1] = Math.max(offset[1], markerOffset[1]);
        }

        offset[0] -= dataAreaLeft;
        offset[1] -= dataAreaRight;

        if (offset[0] > 0 || offset[1] > 0) {
            double leftOffset = offset[0] > 0 ? offset[0] : 0;
            double rightOffset = offset[1] > 0 ? offset[1] : 0;
            leftOffset += insets.getLeft();
            rightOffset += insets.getRight();
            setInsets(new RectangleInsets(insets.getTop(), leftOffset, insets.getBottom(), rightOffset));
        }
    }

    private List<TbThresholdMarker> computeVisibleThresholdMarkers() {
        List<TbThresholdMarker> thresholdMarkers = new ArrayList<>();
        for (XYDataset dataset : this.getDatasets().values()) {
            int datasetIndex = indexOf(dataset);
            Collection<Marker> markers = this.getRangeMarkers(datasetIndex, Layer.FOREGROUND);
            ValueAxis axis = getRangeAxisForDataset(datasetIndex);
            if (markers != null && axis != null) {
                Range range = axis.getRange();
                markers.forEach(marker -> {
                    if (marker instanceof TbThresholdMarker thresholdMarker) {
                        double value = thresholdMarker.getValue();
                        if (range.contains(value)) {
                            thresholdMarkers.add(thresholdMarker);
                        }
                    }
                });
            }
        }
        return thresholdMarkers;
    }

    private Rectangle integerise(Rectangle2D rect) {
        int x0 = (int) Math.ceil(rect.getMinX());
        int y0 = (int) Math.ceil(rect.getMinY());
        int x1 = (int) Math.floor(rect.getMaxX());
        int y1 = (int) Math.floor(rect.getMaxY());
        return new Rectangle(x0, y0, (x1 - x0), (y1 - y0));
    }
}

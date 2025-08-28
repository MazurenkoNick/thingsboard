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
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.GradientPaintTransformer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.StandardGradientPaintTransformer;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.LineUtils;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TbXYStepRenderer extends XYLineAndShapeRenderer {

    public static enum FillType {

        /** No fill. */
        NONE,

        /** Fill down to zero. */
        TO_ZERO,

        /** Fill to the lower bound. */
        TO_LOWER_BOUND,

        /** Fill to the upper bound. */
        TO_UPPER_BOUND
    }

    public static class TbXYStepState extends State {

        /** The area to fill under the curve. */
        public GeneralPath fillArea;

        /** The points. */
        public List<Point2D> points;

        /**
         * Creates a new state instance.
         *
         * @param info  the plot rendering info.
         */
        public TbXYStepState(PlotRenderingInfo info) {
            super(info);
            this.fillArea = new GeneralPath();
            this.points = new ArrayList<>();
        }
    }

    private double stepPoint = 1.0d;

    private TbXYStepRenderer.FillType fillType;

    private GradientPaintTransformer gradientPaintTransformer;

    public TbXYStepRenderer(FillType fillType) {
        super();
        Args.nullNotPermitted(fillType, "fillType");
        this.fillType = fillType;
        this.gradientPaintTransformer = new StandardGradientPaintTransformer();
    }

    public double getStepPoint() {
        return this.stepPoint;
    }

    public void setStepPoint(double stepPoint) {
        if (stepPoint < 0.0d || stepPoint > 1.0d) {
            throw new IllegalArgumentException(
                    "Requires stepPoint in [0.0;1.0]");
        }
        this.stepPoint = stepPoint;
        fireChangeEvent();
    }

    public FillType getFillType() {
        return this.fillType;
    }

    public void setFillType(FillType fillType) {
        this.fillType = fillType;
        fireChangeEvent();
    }

    public GradientPaintTransformer getGradientPaintTransformer() {
        return this.gradientPaintTransformer;
    }

    public void setGradientPaintTransformer(GradientPaintTransformer gradientPaintTransformer) {
        this.gradientPaintTransformer = gradientPaintTransformer;
        fireChangeEvent();
    }

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea,
                                          XYPlot plot, XYDataset data, PlotRenderingInfo info) {

        setDrawSeriesLineAsPath(true);
        TbXYStepState state = new TbXYStepState(info);
        state.setProcessVisibleItemsOnly(false);
        return state;
    }

    @Override
    protected void drawPrimaryLineAsPath(XYItemRendererState state,
                                         Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
                                         int series, int item, ValueAxis xAxis, ValueAxis yAxis,
                                         Rectangle2D dataArea) {
        if (!getItemVisible(series, item)) {
            return;
        }
        TbXYStepState s = (TbXYStepState) state;

        PlotOrientation orientation = plot.getOrientation();

        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);

        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
        double transX1 = xAxis.valueToJava2D(x1, dataArea, xAxisLocation);
        double transY1 = (Double.isNaN(y1) ? Double.NaN
                : yAxis.valueToJava2D(y1, dataArea, yAxisLocation));

        if (item > 0) {
            // get the previous data point...
            double x0 = dataset.getXValue(series, item - 1);
            double y0 = dataset.getYValue(series, item - 1);
            double transX0 = xAxis.valueToJava2D(x0, dataArea,
                    xAxisLocation);
            double transY0 = (Double.isNaN(y0) ? Double.NaN
                    : yAxis.valueToJava2D(y0, dataArea, yAxisLocation));

            if (orientation == PlotOrientation.HORIZONTAL) {
                if (transY0 == transY1) {
                    // this represents the situation
                    // for drawing a horizontal bar.
                    storeLine(s, transY0, transX0, transY1,
                            transX1, dataArea);
                }
                else {  //this handles the need to perform a 'step'.

                    // calculate the step point
                    double transXs = transX0 + (getStepPoint()
                            * (transX1 - transX0));
                    storeLine(s, transY0, transX0, transY0,
                            transXs, dataArea);
                    storeLine(s, transY0, transXs, transY1,
                            transXs, dataArea);
                    storeLine(s, transY1, transXs, transY1,
                            transX1, dataArea);
                }
            } else if (orientation == PlotOrientation.VERTICAL) {
                if (transY0 == transY1) { // this represents the situation
                    // for drawing a horizontal bar.
                    storeLine(s, transX0, transY0, transX1,
                            transY1, dataArea);
                }
                else {  //this handles the need to perform a 'step'.
                    // calculate the step point
                    double transXs = transX0 + (getStepPoint()
                            * (transX1 - transX0));
                    storeLine(s, transX0, transY0, transXs,
                            transY0, dataArea);
                    storeLine(s, transXs, transY0, transXs,
                            transY1, dataArea);
                    storeLine(s, transXs, transY1, transX1,
                            transY1, dataArea);
                }
            }
        }
        if (item == dataset.getItemCount(series) - 1) {
            if (s.points.size() > 1) {
                Point2D origin;
                if (this.fillType == FillType.TO_ZERO) {
                    float xz = (float) xAxis.valueToJava2D(0, dataArea,
                            yAxisLocation);
                    float yz = (float) yAxis.valueToJava2D(0, dataArea,
                            yAxisLocation);
                    origin = plot.getOrientation() == PlotOrientation.HORIZONTAL
                            ? new Point2D.Float(yz, xz)
                            : new Point2D.Float(xz, yz);
                } else if (this.fillType == FillType.TO_LOWER_BOUND) {
                    float xlb = (float) xAxis.valueToJava2D(
                            xAxis.getLowerBound(), dataArea, xAxisLocation);
                    float ylb = (float) yAxis.valueToJava2D(
                            yAxis.getLowerBound(), dataArea, yAxisLocation);
                    origin = plot.getOrientation() == PlotOrientation.HORIZONTAL
                            ? new Point2D.Float(ylb, xlb)
                            : new Point2D.Float(xlb, ylb);
                } else {// fillType == TO_UPPER_BOUND
                    float xub = (float) xAxis.valueToJava2D(
                            xAxis.getUpperBound(), dataArea, xAxisLocation);
                    float yub = (float) yAxis.valueToJava2D(
                            yAxis.getUpperBound(), dataArea, yAxisLocation);
                    origin = plot.getOrientation() == PlotOrientation.HORIZONTAL
                            ? new Point2D.Float(yub, xub)
                            : new Point2D.Float(xub, yub);
                }

                Point2D cp0 = s.points.get(0);
                s.seriesPath.moveTo(cp0.getX(), cp0.getY());
                if (this.fillType != FillType.NONE) {
                    if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        s.fillArea.moveTo(origin.getX(), cp0.getY());
                    } else {
                        s.fillArea.moveTo(cp0.getX(), origin.getY());
                    }
                    s.fillArea.lineTo(cp0.getX(), cp0.getY());
                }
                for (int i = 1; i < s.points.size(); i++) {
                    Point2D p = s.points.get(i);
                    s.seriesPath.lineTo(p.getX(), p.getY());
                    if (this.fillType != FillType.NONE) {
                        s.fillArea.lineTo(p.getX(), p.getY());
                    }
                }
                // Add last point @ y=0 for fillPath and close path
                if (this.fillType != FillType.NONE) {
                    if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        s.fillArea.lineTo(origin.getX(), s.points.get(
                                s.points.size() - 1).getY());
                    } else {
                        s.fillArea.lineTo(s.points.get(
                                s.points.size() - 1).getX(), origin.getY());
                    }
                    s.fillArea.closePath();
                }
                // fill under the curve...
                if (this.fillType != FillType.NONE) {
                    Paint fp = getSeriesFillPaint(series);
                    if (this.gradientPaintTransformer != null
                            && fp instanceof GradientPaint) {
                        GradientPaint gp = this.gradientPaintTransformer
                                .transform((GradientPaint) fp, s.fillArea);
                        g2.setPaint(gp);
                    } else {
                        g2.setPaint(fp);
                    }
                    g2.fill(s.fillArea);
                    s.fillArea.reset();
                }
                // then draw the line...
                drawFirstPassShape(g2, pass, series, item, s.seriesPath);
            }
            // reset points vector
            s.points = new ArrayList<>();
        }
    }

    private void storeLine(TbXYStepState state, double x0, double y0, double x1, double y1, Rectangle2D dataArea) {
        if (Double.isNaN(x0) || Double.isNaN(x1) || Double.isNaN(y0)
                || Double.isNaN(y1)) {
            return;
        }
        state.workingLine.setLine(x0, y0, x1, y1);
        boolean visible = LineUtils.clipLine(state.workingLine, dataArea);
        if (visible) {
            if (!state.points.contains(state.workingLine.getP1())) {
               state.points.add(state.workingLine.getP1());
            }
            if (!state.points.contains(state.workingLine.getP2())) {
                state.points.add(state.workingLine.getP2());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TbXYStepRenderer)) {
            return false;
        }
        TbXYStepRenderer that = (TbXYStepRenderer) obj;
        if (this.stepPoint != that.stepPoint) {
            return false;
        }
        if (this.fillType != that.fillType) {
            return false;
        }
        if (!Objects.equals(this.gradientPaintTransformer, that.gradientPaintTransformer)) {
            return false;
        }
        return super.equals(obj);
    }

}

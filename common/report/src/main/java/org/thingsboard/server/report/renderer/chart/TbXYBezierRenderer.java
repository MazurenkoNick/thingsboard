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

import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.GradientPaintTransformer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.StandardGradientPaintTransformer;
import org.jfree.chart.util.Args;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;


public class TbXYBezierRenderer extends TbXYLineAndShapeRenderer {

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

    public static class TbXYBezierState extends State {

        /** The area to fill under the curve. */
        public GeneralPath fillArea;

        /** The points. */
        public List<Point2D> points;

        public List<Point2D> stackPoints;

        /**
         * Creates a new state instance.
         *
         * @param info  the plot rendering info. 
         */
        public TbXYBezierState(PlotRenderingInfo info) {
            super(info);
            this.fillArea = new GeneralPath();
            this.points = new ArrayList<>();
            this.stackPoints = new ArrayList<>();
        }
    }

    private int precision;

    private double tension;

    private FillType fillType;

    private GradientPaintTransformer gradientPaintTransformer;

    public TbXYBezierRenderer() {
        this(5, 25, FillType.NONE, false);
    }

    public TbXYBezierRenderer(int precision, double tension) {
        this(precision, tension, FillType.NONE, false);
    }

    public TbXYBezierRenderer(int precision, double tension, FillType fillType, boolean stackMode) {
        super(stackMode);
        if (precision <= 0) {
            throw new IllegalArgumentException("Requires precision > 0.");
        }
        if (tension <= 0) {
            throw new IllegalArgumentException("Requires precision > 0.");
        }
        Args.nullNotPermitted(fillType, "fillType");
        this.precision = precision;
        this.tension = tension;
        this.fillType = fillType;
        this.gradientPaintTransformer = new StandardGradientPaintTransformer();
    }

    public int getPrecision() {
        return this.precision;
    }

    public void setPrecision(int p) {
        if (p <= 0) {
            throw new IllegalArgumentException("Requires p > 0.");
        }
        this.precision = p;
        fireChangeEvent();
    }
    public double getTension() {
        return this.tension;
    }

    public void setTension(double t) {
        if (t <= 0) {
            throw new IllegalArgumentException("Requires tension > 0.");
        }
        this.tension = t;
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

    public void setGradientPaintTransformer(GradientPaintTransformer gpt) {
        this.gradientPaintTransformer = gpt;
        fireChangeEvent();
    }

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea,
                                          XYPlot plot, XYDataset data, PlotRenderingInfo info) {

        TbXYBezierState state = new TbXYBezierState(info);
        state.setProcessVisibleItemsOnly(false);
        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state,
                         Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                         ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
                         int series, int item, CrosshairState crosshairState, int pass) {

        // do nothing if item is not visible
        if (!getItemVisible(series, item)) {
            return;
        }

        // first pass draws the background (lines, for instance)
        if (isLinePass(pass)) {
            if (getItemLineVisible(series, item) || this.fillType != FillType.NONE) {
                this.drawLineAndArea(state, g2, plot, dataset, pass,
                        series, item, domainAxis, rangeAxis, dataArea);
            }
        } else {
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
        }
    }

    private void drawLineAndArea(XYItemRendererState state,
                                 Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
                                 int series, int item, ValueAxis xAxis, ValueAxis yAxis,
                                 Rectangle2D dataArea) {

        TbXYBezierState s = (TbXYBezierState) state;
        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

        // get the data points
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);

        if (!this.getStackMode()) {
            double transX1 = xAxis.valueToJava2D(x1, dataArea, xAxisLocation);
            double transY1 = yAxis.valueToJava2D(y1, dataArea, yAxisLocation);

            // Collect points
            if (!Double.isNaN(transX1) && !Double.isNaN(transY1)) {
                Point2D p = plot.getOrientation() == PlotOrientation.HORIZONTAL
                        ? new Point2D.Float((float) transY1, (float) transX1)
                        : new Point2D.Float((float) transX1, (float) transY1);
                if (!s.points.contains(p))
                    s.points.add(p);
            }
        } else { // stack mode
            TableXYDataset tdataset = (TableXYDataset) dataset;
            if (Double.isNaN(y1)) {
                y1 = 0.0;
            }
            PlotOrientation orientation = plot.getOrientation();
            double[] stack1 = getStackValues(tdataset, series, item);
            double x0 = dataset.getXValue(series, Math.max(item - 1, 0));
            double y0 = dataset.getYValue(series, Math.max(item - 1, 0));
            if (Double.isNaN(y0)) {
                y0 = 0.0;
            }
            double[] stack0 = getStackValues(tdataset, series, Math.max(item - 1,
                    0));
            int itemCount = dataset.getItemCount(series);
            double x2 = dataset.getXValue(series, Math.min(item + 1,
                    itemCount - 1));
            double y2 = dataset.getYValue(series, Math.min(item + 1,
                    itemCount - 1));
            if (Double.isNaN(y2)) {
                y2 = 0.0;
            }
            double[] stack2 = getStackValues(tdataset, series, Math.min(item + 1,
                    itemCount - 1));
            double xleft = (x0 + x1) / 2.0;
            double xright = (x1 + x2) / 2.0;
            double[] stackLeft = averageStackValues(stack0, stack1);
            double[] stackRight = averageStackValues(stack1, stack2);
            double[] adjStackLeft = adjustedStackValues(stack0, stack1);
            double[] adjStackRight = adjustedStackValues(stack1, stack2);

            RectangleEdge edge0 = plot.getDomainAxisEdge();
            float transX1 = (float) xAxis.valueToJava2D(x1, dataArea, edge0);
            float transXLeft = (float) xAxis.valueToJava2D(xleft, dataArea,
                    edge0);
            float transXRight = (float) xAxis.valueToJava2D(xright, dataArea,
                    edge0);
            if (this.getRoundXCoordinates()) {
                transX1 = Math.round(transX1);
                transXLeft = Math.round(transXLeft);
                transXRight = Math.round(transXRight);
            }
            float transY1;
            RectangleEdge edge1 = plot.getRangeAxisEdge();
            if (y1 >= 0.0) {  // handle positive value
                transY1 = (float) yAxis.valueToJava2D(y1 + stack1[1], dataArea,
                        edge1);
                float transStack1 = (float) yAxis.valueToJava2D(stack1[1],
                        dataArea, edge1);
                float transStackLeft = (float) yAxis.valueToJava2D(
                        adjStackLeft[1], dataArea, edge1);

                // LEFT POLYGON
                if (y0 >= 0.0) {
                    double yleft = (y0 + y1) / 2.0 + stackLeft[1];
                    float transYLeft
                            = (float) yAxis.valueToJava2D(yleft, dataArea, edge1);
                    if (orientation == PlotOrientation.VERTICAL) {
                        storePoint(s, transX1, transY1);
                        storeStackPoint(s, transX1, transStack1);
                        storePoint(s, transXLeft, transYLeft);
                        storeStackPoint(s, transXLeft, transStackLeft);
                    }  else {
                        storePoint(s, transY1, transX1);
                        storeStackPoint(s, transStack1, transX1);
                        storePoint(s, transYLeft, transXLeft);
                        storeStackPoint(s, transStackLeft, transXLeft);
                    }
                } else {
                    if (orientation == PlotOrientation.VERTICAL) {
                        storePoint(s, transX1, transY1);
                        storeStackPoint(s, transX1, transStack1);
                        storePoint(s, transXLeft, transStackLeft);
                        storeStackPoint(s, transXLeft, transStackLeft);
                    } else {
                        storePoint(s, transY1, transX1);
                        storeStackPoint(s, transStack1, transX1);
                        storePoint(s, transStackLeft, transXLeft);
                        storeStackPoint(s, transStackLeft, transXLeft);
                    }
                }

                float transStackRight = (float) yAxis.valueToJava2D(
                        adjStackRight[1], dataArea, edge1);
                // RIGHT POLYGON
                if (y2 >= 0.0) {
                    double yright = (y1 + y2) / 2.0 + stackRight[1];
                    float transYRight
                            = (float) yAxis.valueToJava2D(yright, dataArea, edge1);
                    if (orientation == PlotOrientation.VERTICAL) {
                        storePoint(s, transX1, transY1);
                        storeStackPoint(s, transX1, transStack1);
                        storePoint(s, transXRight, transYRight);
                        storeStackPoint(s, transXRight, transStackRight);
                    } else {
                        storePoint(s, transY1, transX1);
                        storeStackPoint(s, transStack1, transX1);
                        storePoint(s, transYRight, transXRight);
                        storeStackPoint(s, transStackRight, transXRight);
                    }
                } else {
                    if (orientation == PlotOrientation.VERTICAL) {
                        storePoint(s, transX1, transY1);
                        storeStackPoint(s, transX1, transStack1);
                        storePoint(s, transXRight, transStackRight);
                        storeStackPoint(s, transXRight, transStackRight);
                    } else {
                        storePoint(s, transY1, transX1);
                        storeStackPoint(s, transStack1, transX1);
                        storePoint(s, transStackRight, transXRight);
                        storeStackPoint(s, transStackRight, transXRight);
                    }
                }
            } else {  // handle negative value
                transY1 = (float) yAxis.valueToJava2D(y1 + stack1[0], dataArea,
                        edge1);
                float transStack1 = (float) yAxis.valueToJava2D(stack1[0],
                        dataArea, edge1);
                float transStackLeft = (float) yAxis.valueToJava2D(
                        adjStackLeft[0], dataArea, edge1);

                // LEFT POLYGON
                if (y0 >= 0.0) {
                    if (orientation == PlotOrientation.VERTICAL) {
                        storePoint(s, transX1, transY1);
                        storeStackPoint(s, transX1, transStack1);
                        storePoint(s, transXLeft, transStackLeft);
                        storeStackPoint(s, transXLeft, transStackLeft);
                    } else {
                        storePoint(s, transY1, transX1);
                        storeStackPoint(s, transStack1, transX1);
                        storePoint(s, transStackLeft, transXLeft);
                        storeStackPoint(s, transStackLeft, transXLeft);
                    }
                } else {
                    double yleft = (y0 + y1) / 2.0 + stackLeft[0];
                    float transYLeft = (float) yAxis.valueToJava2D(yleft,
                            dataArea, edge1);
                    if (orientation == PlotOrientation.VERTICAL) {
                        storePoint(s, transX1, transY1);
                        storeStackPoint(s, transX1, transStack1);
                        storePoint(s, transXLeft, transYLeft);
                        storeStackPoint(s, transXLeft, transStackLeft);
                    } else {
                        storePoint(s, transY1, transX1);
                        storeStackPoint(s, transStack1, transX1);
                        storePoint(s, transYLeft, transXLeft);
                        storeStackPoint(s, transStackLeft, transXLeft);
                    }
                }
                float transStackRight = (float) yAxis.valueToJava2D(
                        adjStackRight[0], dataArea, edge1);

                // RIGHT POLYGON
                if (y2 >= 0.0) {
                    if (orientation == PlotOrientation.VERTICAL) {
                        storePoint(s, transX1, transY1);
                        storeStackPoint(s, transX1, transStack1);
                        storePoint(s, transXRight, transStackRight);
                        storeStackPoint(s, transXRight, transStackRight);
                    } else {
                        storePoint(s, transY1, transX1);
                        storeStackPoint(s, transStack1, transX1);
                        storePoint(s, transStackRight, transXRight);
                        storeStackPoint(s, transStackRight, transXRight);
                    }
                } else {
                    double yright = (y1 + y2) / 2.0 + stackRight[0];
                    float transYRight = (float) yAxis.valueToJava2D(yright,
                            dataArea, edge1);
                    if (orientation == PlotOrientation.VERTICAL) {
                        storePoint(s, transX1, transY1);
                        storeStackPoint(s, transX1, transStack1);
                        storePoint(s, transXRight, transYRight);
                        storeStackPoint(s, transXRight, transStackRight);
                    } else {
                        storePoint(s, transY1, transX1);
                        storeStackPoint(s, transStack1, transX1);
                        storePoint(s, transYRight, transXRight);
                        storeStackPoint(s, transStackRight, transXRight);
                    }
                }
            }
        }

        if (item == dataset.getItemCount(series) - 1) {     // construct path
            if (s.points.size() > 1) {
                this.sortPoints(s, plot.getOrientation());
                List<Point2D> interpolated = this.interpolatePoints(s.points);
                this.drawPointsToPath(interpolated, s.seriesPath, true);
                if (this.fillType != FillType.NONE) {
                    List<Point2D> stackPoints = null;
                    if (getStackMode()) {
                        stackPoints = this.interpolatePoints(s.stackPoints);
                    }
                    this.drawFillArea(plot, xAxis, yAxis, dataArea, s.fillArea, interpolated, stackPoints);
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
                if (getItemLineVisible(series, item)) {
                    // then draw the line...
                    drawFirstPassShape(g2, pass, series, item, s.seriesPath);
                }
            }
            // reset points vector
            s.points = new ArrayList<>();
            s.stackPoints = new ArrayList<>();
        }
    }

    private List<Point2D> interpolatePoints(List<Point2D> points) {
        List<Point2D> interpolated = new ArrayList<>();
        if (points.size() > 1) {
            Point2D cp0 = points.get(0);
            interpolated.add(cp0);
            if (points.size() == 2) {
                Point2D cp1 = points.get(1);
                interpolated.add(cp1);
            } else if (points.size() == 3) {
                Point2D[] pInitial = getInitalPoints(points);
                addBezierPoints(pInitial, interpolated);
                Point2D[] pFinal = getFinalPoints(points);
                addBezierPoints(pFinal, interpolated);
            } else {
                // construct Bezier curve
                int np = points.size(); // number of points
                for(int i = 0; i < np - 1; i++) {
                    if(i == 0) {
                        // 3 points, 2 lines (initial an final Bezier curves)
                        Point2D[] initial3Points = new Point2D[3];
                        initial3Points[0] = points.get(0);
                        initial3Points[1] = points.get(1);
                        initial3Points[2] = points.get(2);
                        Point2D[] pInitial = calcSegmentPointsInitial(initial3Points);
                        addBezierPoints(pInitial, interpolated);
                    }
                    if(i == np - 2) {
                        Point2D[] final3Points = new Point2D[4];
                        final3Points[1] = points.get(np-3);
                        final3Points[2] = points.get(np-2);
                        final3Points[3] = points.get(np-1);
                        // No need for final3Points[0]. Not required
                        Point2D[] pFinal = calcSegmentPointsFinal(final3Points);
                        addBezierPoints(pFinal, interpolated);
                    }
                    if ((i != 0) && (i != (np - 2))){
                        Point2D[] original4Points = new Point2D[4];
                        original4Points[0] = points.get(i - 1);
                        original4Points[1] = points.get(i);
                        original4Points[2] = points.get(i + 1);
                        original4Points[3] = points.get(i + 2);
                        Point2D[] pMedium = calculateSegmentPoints(original4Points);
                        addBezierPoints(pMedium, interpolated);
                    }
                }
            }
        }
        return interpolated;
    }

    private void drawPointsToPath(List<Point2D> points, GeneralPath targetPath, boolean firstPointMove) {
        if (points.size() > 1) {
            Point2D cp0 = points.get(0);
            if (firstPointMove) {
                targetPath.moveTo(cp0.getX(), cp0.getY());
            } else {
                targetPath.lineTo(cp0.getX(), cp0.getY());
            }
            for (int i = 1; i < points.size(); i++) {
                Point2D cp = points.get(i);
                targetPath.lineTo(cp.getX(), cp.getY());
            }
        }
    }

    private void drawFillArea(XYPlot plot, ValueAxis xAxis,
                              ValueAxis yAxis,
                              Rectangle2D dataArea,
                              GeneralPath fillArea,
                              List<Point2D> seriesPoints,
                              List<Point2D> stackPoints) {
        if (getStackMode()) {
            if (seriesPoints.size() > 1) {
                this.drawPointsToPath(seriesPoints, fillArea, true);
                Collections.reverse(stackPoints);
                this.drawPointsToPath(stackPoints, fillArea, false);
                fillArea.closePath();
            }
        } else {
            RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
            RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
            if (seriesPoints.size() > 1) {
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
                Point2D cp0 = seriesPoints.get(0);
                if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    fillArea.moveTo(origin.getX(), cp0.getY());
                } else {
                    fillArea.moveTo(cp0.getX(), origin.getY());
                }
                this.drawPointsToPath(seriesPoints, fillArea, false);
                if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    fillArea.lineTo(origin.getX(), seriesPoints.get(
                            seriesPoints.size() - 1).getY());
                } else {
                    fillArea.lineTo(seriesPoints.get(
                            seriesPoints.size() - 1).getX(), origin.getY());
                }
                fillArea.closePath();
            }
        }
    }

    private void addBezierPoints(Point2D[] segmentPoints, List<Point2D> targetPoints) {
        double x;
        double y;
        for (int t = 0 ; t <= this.precision; t++) {
            double k = (double)t / this.precision;
            double r = 1- k;

            x = Math.pow(r, 3) * segmentPoints[0].getX() + 3 * k * Math.pow(r, 2) * segmentPoints[1].getX()
                    + 3 * Math.pow(k, 2) * (1 - k) * segmentPoints[2].getX() + Math.pow(k, 3) * segmentPoints[3].getX();
            y = Math.pow(r, 3) * segmentPoints[0].getY() + 3 * k * Math.pow(r, 2) * segmentPoints[1].getY()
                    + 3 * Math.pow(k, 2) * (1 - k) * segmentPoints[2].getY() + Math.pow(k, 3) * segmentPoints[3].getY();
            targetPoints.add(new Point2D.Double(x, y));
        }
    }

    private Point2D[] getFinalPoints(List<Point2D> points) {
        Point2D[] final3Points = new Point2D[4];
        final3Points[1] = points.get(0);
        final3Points[2] = points.get(1);
        final3Points[3] = points.get(2);
        // No need for final3Points[0]. Not required
        Point2D[] pFinal = calcSegmentPointsFinal(final3Points);//TENSION = 1.5
        return pFinal;
    }

    private Point2D[] getInitalPoints(List<Point2D> points) {
        Point2D[] initial3Points = new Point2D[3];
        initial3Points[0] = points.get(0);
        initial3Points[1] = points.get(1);
        initial3Points[2] = points.get(2);
        Point2D[] pInitial = calcSegmentPointsInitial(initial3Points);// TENSION = 1.5
        return pInitial;
    }

    private Point2D[] calculateSegmentPoints(Point2D[] original4Points) {
        Point2D[] points = new Point2D[4];
        points[0] = original4Points[1];
        points[3] = original4Points[2];
        for(int i = 1; i < 3; i++) {
            Point2D aux1 = calcUnitaryVector(original4Points[i-1], original4Points[i]);
            Point2D aux2 = calcUnitaryVector(original4Points[i+1], original4Points[i]);
            Point2D aux3 = calcUnitaryVector(aux2, aux1);

            double x = original4Points[i].getX() + Math.pow(-1.0, i+1) * tension * aux3.getX();
            double y = original4Points[i].getY() + Math.pow(-1.0, i+1) * tension * aux3.getY();
            points[i] = new Point2D.Double(x, y);
        }
        return points;
    }

    private Point2D[] calcSegmentPointsInitial(Point2D[] original3P) {
        Point2D[] points = new Point2D[4];
        points[0] = original3P[0];// Endpoint 1
        points[3] = original3P[1];// Endpoint 2
        // Control point 1
        Point2D auxInitial = calcUnitaryVector(original3P[0], original3P[1]);
        points[1] = original3P[0];// new Point2D.Double(x0, y0);
        // Control point 2
        Point2D aux2 = calcUnitaryVector(original3P[2], original3P[1]);
        Point2D aux3 = calcUnitaryVector(auxInitial, aux2);
        double x = original3P[1].getX() + tension * aux3.getX();
        double y = original3P[1].getY() + tension * aux3.getY();
        points[2] = new Point2D.Double(x, y);
        return points;
    }

    private Point2D[] calcSegmentPointsFinal(Point2D[] original3P) {
        /*
         * Each segment is defined by its two endpoints and two control points. A
         * control point determines the tangent at the corresponding endpoint.
         */
        Point2D[] points = new Point2D[4];
        points[0] = original3P[2];// Endpoint 1
        points[3] = original3P[3];// Endpoint 2
        // Control point 2: points[2]
        Point2D auxInitial = calcUnitaryVector(original3P[3], original3P[2]);
        points[2] = original3P[3];// new Point2D.Double(x0, y0);
        // Control point 1
        Point2D aux1 = calcUnitaryVector(original3P[3], original3P[2]);
        Point2D aux2 = calcUnitaryVector(original3P[1], original3P[2]);
        Point2D aux3 = calcUnitaryVector(aux1, aux2);
        double x = original3P[2].getX() + tension * aux3.getX();
        double y = original3P[2].getY() + tension * aux3.getY();
        points[1] = new Point2D.Double(x, y);
        return points;
    }

    private Point2D calcUnitaryVector(Point2D pOrigin, Point2D pEnd) {
        double module = Math.sqrt(Math.pow(pEnd.getX() - pOrigin.getX(), 2) +
                Math.pow(pEnd.getY() - pOrigin.getY(), 2));
        if (module == 0) {
            return null;
        }
        return new Point2D.Double((pEnd.getX() - pOrigin.getX()) / module,
                (pEnd.getY() - pOrigin.getY()) /module);
    }

    private void storePoint(TbXYBezierState s, double x, double y) {
        Point2D p = new Point2D.Float((float)x, (float)y);
        if (!s.points.contains(p)) {
            s.points.add(p);
        }
    }

    private void storeStackPoint(TbXYBezierState s, double x, double y) {
        Point2D p = new Point2D.Float((float)x, (float)y);
        if (!s.stackPoints.contains(p)) {
            s.stackPoints.add(p);
        }
    }

    private void sortPoints(TbXYBezierState s, PlotOrientation orientation) {
        if (orientation == PlotOrientation.VERTICAL) {
            s.points.sort((p1, p2) -> (int)(p1.getX() - p2.getX()));
        } else {
            s.points.sort((p1, p2) -> (int)(p1.getY() - p2.getY()));
        }
        if (this.getStackMode()) {
            if (orientation == PlotOrientation.VERTICAL) {
                s.stackPoints.sort((p1, p2) -> (int)(p1.getX() - p2.getX()));
            } else {
                s.stackPoints.sort((p1, p2) -> (int)(p1.getY() - p2.getY()));
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TbXYBezierRenderer)) {
            return false;
        }
        TbXYBezierRenderer that = (TbXYBezierRenderer) obj;
        if (this.precision != that.precision) {
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
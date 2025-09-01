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

import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.BooleanList;
import org.jfree.chart.util.PaintList;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.Range;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbXYLineAndShapeRenderer extends XYLineAndShapeRenderer {

    private PaintList shapeFillPaintList;
    private transient Paint defaultShapeFillPaint;

    private BooleanList itemLabelsBackgroundVisibleList;
    private boolean defaultItemLabelsBackgroundVisible;
    private PaintList itemLabelsBackgroundPaintList;
    private transient Paint defaultItemLabelBackgroundPaint;

    private final boolean stackMode;
    private boolean roundXCoordinates;

    public TbXYLineAndShapeRenderer() {
        this(false);
    }

    public TbXYLineAndShapeRenderer(boolean stackMode) {
        super();
        this.stackMode = stackMode;
        this.roundXCoordinates = true;
        this.shapeFillPaintList = new PaintList();
        this.itemLabelsBackgroundVisibleList = new BooleanList();
        this.defaultItemLabelsBackgroundVisible = false;
        this.itemLabelsBackgroundPaintList = new PaintList();
        this.defaultItemLabelBackgroundPaint = safeParseCssColor("rgba(255,255,255,0.56)");
    }

    public boolean getStackMode() {
        return this.stackMode;
    }

    public boolean getRoundXCoordinates() {
        return this.roundXCoordinates;
    }

    public void setRoundXCoordinates(boolean round) {
        this.roundXCoordinates = round;
        fireChangeEvent();
    }

    @Override
    public Range findRangeBounds(XYDataset dataset) {
        if (this.stackMode) {
            if (dataset == null) {
                return null;
            }
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            TableXYDataset d = (TableXYDataset) dataset;
            int itemCount = d.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                double[] stackValues = getStackValues((TableXYDataset) dataset,
                        d.getSeriesCount(), i);
                min = Math.min(min, stackValues[0]);
                max = Math.max(max, stackValues[1]);
            }
            if (min == Double.POSITIVE_INFINITY) {
                return null;
            }
            return new Range(min, max);
        } else {
            return super.findRangeBounds(dataset);
        }
    }

    public Paint getShapeFillPaint(int series, int item) {
        return lookupSeriesShapeFillPaint(series);
    }

    public Paint lookupSeriesShapeFillPaint(int series) {
        Paint seriesShapesFillPaint = getSeriesShapesFillPaint(series);
        if (seriesShapesFillPaint == null) {
            seriesShapesFillPaint = defaultShapeFillPaint;
            if (seriesShapesFillPaint == null) {
                seriesShapesFillPaint = lookupSeriesPaint(series);
            }
        }
        return seriesShapesFillPaint;
    }

    public Paint getSeriesShapesFillPaint(int series) {
        return this.shapeFillPaintList.getPaint(series);
    }

    public void setSeriesShapesFillPaint(int series, Paint paint) {
        setSeriesShapesFillPaint(series, paint, true);
    }

    public void setSeriesShapesFillPaint(int series, Paint paint, boolean notify) {
        this.shapeFillPaintList.setPaint(series, paint);
        if (notify) {
            fireChangeEvent();
        }
    }

    public void setDefaultShapeFillPaint(Paint paint) {
        this.defaultShapeFillPaint = paint;
    }

    public boolean isItemLabelBackgroundVisible(int row, int column) {
        return isSeriesItemLabelsBackgroundVisible(row);
    }

    public boolean isSeriesItemLabelsBackgroundVisible(int series) {
        Boolean b = this.itemLabelsBackgroundVisibleList.getBoolean(series);
        if (b == null) {
            return this.defaultItemLabelsBackgroundVisible;
        }
        return b;
    }

    public void setSeriesItemLabelsBackgroundVisible(int series, boolean visible) {
        setSeriesItemLabelsBackgroundVisible(series, Boolean.valueOf(visible));
    }

    public void setSeriesItemLabelsBackgroundVisible(int series, Boolean visible) {
        setSeriesItemLabelsBackgroundVisible(series, visible, true);
    }

    public void setSeriesItemLabelsBackgroundVisible(int series, Boolean visible,
                                                     boolean notify) {
        this.itemLabelsBackgroundVisibleList.setBoolean(series, visible);
        if (notify) {
            fireChangeEvent();
        }
    }

    public boolean getDefaultItemLabelsBackgroundVisible() {
        return this.defaultItemLabelsBackgroundVisible;
    }

    public void setDefaultItemLabelsBackgroundVisible(boolean visible) {
        setDefaultItemLabelsBackgroundVisible(visible, true);
    }

    public void setDefaultItemLabelsBackgroundVisible(boolean visible, boolean notify) {
        this.defaultItemLabelsBackgroundVisible = visible;
        if (notify) {
            fireChangeEvent();
        }
    }

    public Paint getItemLabelBackgroundPaint(int row, int column) {
        Paint result = getSeriesItemLabelsBackgroundPaint(row);
        if (result == null) {
            result = this.defaultItemLabelBackgroundPaint;
        }
        return result;
    }

    public Paint getSeriesItemLabelsBackgroundPaint(int series) {
        return this.itemLabelsBackgroundPaintList.getPaint(series);
    }

    public void setSeriesItemLabelsBackgroundPaint(int series, Paint paint) {
        setSeriesItemLabelsBackgroundPaint(series, paint, true);
    }

    public void setSeriesItemLabelsBackgroundPaint(int series, Paint paint,
                                                   boolean notify) {
        this.itemLabelsBackgroundPaintList.setPaint(series, paint);
        if (notify) {
            fireChangeEvent();
        }
    }

    public Paint getDefaultItemLabelsBackgroundPaint() {
        return this.defaultItemLabelBackgroundPaint;
    }

    public void setDefaultItemLabelsBackgroundPaint(Paint paint) {
        // defer argument checking...
        setDefaultItemLabelsBackgroundPaint(paint, true);
    }

    public void setDefaultItemLabelsBackgroundPaint(Paint paint, boolean notify) {
        Args.nullNotPermitted(paint, "paint");
        this.defaultItemLabelBackgroundPaint = paint;
        if (notify) {
            fireChangeEvent();
        }
    }

    protected void drawSecondaryPass(Graphics2D g2, XYPlot plot,
                                     XYDataset dataset, int pass, int series, int item,
                                     ValueAxis domainAxis, Rectangle2D dataArea, ValueAxis rangeAxis,
                                     CrosshairState crosshairState, EntityCollection entities) {

        Shape entityArea = null;

        // get the data point...
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);
        if (Double.isNaN(y1) || Double.isNaN(x1)) {
            return;
        }

        PlotOrientation orientation = plot.getOrientation();
        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
        double transX1 = domainAxis.valueToJava2D(x1, dataArea, xAxisLocation);
        double transY1;
        if (this.stackMode) {
            RectangleEdge edge1 = plot.getRangeAxisEdge();
            TableXYDataset tdataset = (TableXYDataset) dataset;
            double[] stack = getStackValues(tdataset, series, item);
            transY1 = (float) rangeAxis.valueToJava2D(y1 + (y1 >= 0.0 ? stack[1] : stack[0]), dataArea,
                    edge1);
        } else {
            transY1 = rangeAxis.valueToJava2D(y1, dataArea, yAxisLocation);
        }

        if (getItemShapeVisible(series, item)) {
            Shape shape = getItemShape(series, item);
            if (orientation == PlotOrientation.HORIZONTAL) {
                shape = ShapeUtils.createTranslatedShape(shape, transY1, transX1);
            }
            else if (orientation == PlotOrientation.VERTICAL) {
                shape = ShapeUtils.createTranslatedShape(shape, transX1, transY1);
            }
            entityArea = shape;
            if (shape.intersects(dataArea)) {
                if (getItemShapeFilled(series, item)) {
                    if (this.getUseFillPaint()) {
                        g2.setPaint(getItemFillPaint(series, item));
                    }
                    else {
                        g2.setPaint(getShapeFillPaint(series, item));
                    }
                    g2.fill(shape);
                }
                if (this.getDrawOutlines()) {
                    if (getUseOutlinePaint()) {
                        g2.setPaint(getItemOutlinePaint(series, item));
                    }
                    else {
                        g2.setPaint(getItemPaint(series, item));
                    }
                    g2.setStroke(getItemOutlineStroke(series, item));
                    g2.draw(shape);
                }
            }
        }

        double xx = transX1;
        double yy = transY1;
        if (orientation == PlotOrientation.HORIZONTAL) {
            xx = transY1;
            yy = transX1;
        }

        // draw the item label if there is one...
        if (isItemLabelVisible(series, item)) {
            drawItemLabel(g2, orientation, dataset, series, item, xx, yy,
                    entityArea != null ? entityArea.getBounds2D() : null,
                    (y1 < 0.0));
        }

        int datasetIndex = plot.indexOf(dataset);
        updateCrosshairValues(crosshairState, x1, y1, datasetIndex,
                transX1, transY1, orientation);

        // add an entity for the item, but only if it falls within the data
        // area...
        if (entities != null && ShapeUtils.isPointInRect(dataArea, xx, yy)) {
            addEntity(entities, entityArea, dataset, series, item, xx, yy);
        }
    }

    protected void drawItemLabel(Graphics2D g2, PlotOrientation orientation,
                                 XYDataset dataset, int series, int item, double x, double y,
                                 Rectangle2D itemShapeBounds,
                                 boolean negative) {

        XYItemLabelGenerator generator = getItemLabelGenerator(series, item);
        if (generator != null) {
            Font labelFont = getItemLabelFont(series, item);
            g2.setFont(labelFont);
            String label = generator.generateLabel(dataset, series, item);

            // get the label position..
            ItemLabelPosition position;
            if (!negative) {
                position = getPositiveItemLabelPosition(series, item);
            }
            else {
                position = getNegativeItemLabelPosition(series, item);
            }

            double shapeHeight = itemShapeBounds != null ? itemShapeBounds.getHeight() : 0.0;
            if (position.getTextAnchor() == TextAnchor.BOTTOM_CENTER) {
                y -= (5 + shapeHeight / 2);
            } else if (position.getTextAnchor() == TextAnchor.TOP_CENTER) {
                y += (5 + shapeHeight / 2);
            }

            // work out the label anchor point...
            Point2D anchorPoint = calculateLabelAnchorPoint(
                    position.getItemLabelAnchor(), x, y, orientation);
            if (isItemLabelBackgroundVisible(series, item)) {
                Rectangle2D bounds = TextUtils.calculateRotatedStringBounds(label, g2,
                        (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                        position.getTextAnchor(), position.getAngle(),
                        position.getRotationAnchor()).getBounds2D();
                g2.setPaint(getItemLabelBackgroundPaint(series, item));
                g2.setStroke(new BasicStroke(0));
                g2.fillRoundRect((int)bounds.getX()-3, (int)bounds.getY()-2, (int)bounds.getWidth()+6, (int)bounds.getHeight()+4, 4, 4 );
            }
            Paint paint = getItemLabelPaint(series, item);
            g2.setPaint(paint);
            TextUtils.drawRotatedString(label, g2,
                    (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                    position.getTextAnchor(), position.getAngle(),
                    position.getRotationAnchor());
        }

    }

    @Override
    public LegendItem getLegendItem(int datasetIndex, int series) {
        XYPlot plot = getPlot();
        if (plot == null) {
            return null;
        }

        XYDataset dataset = plot.getDataset(datasetIndex);
        if (dataset == null) {
            return null;
        }

        if (!getItemVisible(series, 0)) {
            return null;
        }
        String label = getLegendItemLabelGenerator().generateLabel(dataset,
                series);
        String description = label;
        String toolTipText = null;
        if (getLegendItemToolTipGenerator() != null) {
            toolTipText = getLegendItemToolTipGenerator().generateLabel(
                    dataset, series);
        }
        String urlText = null;
        if (getLegendItemURLGenerator() != null) {
            urlText = getLegendItemURLGenerator().generateLabel(dataset,
                    series);
        }
        boolean shapeIsVisible = getItemShapeVisible(series, 0);
        Shape shape = lookupLegendShape(series);
        boolean shapeIsFilled = getItemShapeFilled(series, 0);
        Paint fillPaint = (this.getUseFillPaint() ? lookupSeriesFillPaint(series)
                : lookupSeriesShapeFillPaint(series));
        boolean shapeOutlineVisible = this.getDrawOutlines();
        Paint outlinePaint = (this.getUseOutlinePaint() ? lookupSeriesOutlinePaint(
                series) : lookupSeriesPaint(series));
        Stroke outlineStroke = lookupSeriesOutlineStroke(series);
        boolean lineVisible = getItemLineVisible(series, 0);
        Stroke lineStroke = lookupSeriesStroke(series);
        Paint linePaint = lookupSeriesPaint(series);
        LegendItem result = new LegendItem(label, description, toolTipText,
                urlText, shapeIsVisible, shape, shapeIsFilled, fillPaint,
                shapeOutlineVisible, outlinePaint, outlineStroke, lineVisible,
                this.getLegendLine(), lineStroke, linePaint);
        result.setLabelFont(lookupLegendTextFont(series));
        Paint labelPaint = lookupLegendTextPaint(series);
        if (labelPaint != null) {
            result.setLabelPaint(labelPaint);
        }
        result.setSeriesKey(dataset.getSeriesKey(series));
        result.setSeriesIndex(series);
        result.setDataset(dataset);
        result.setDatasetIndex(datasetIndex);

        return result;
    }

    protected double[] getStackValues(TableXYDataset dataset,
                                    int series, int index) {
        double[] result = new double[2];
        for (int i = 0; i < series; i++) {
            double v = dataset.getYValue(i, index);
            if (!Double.isNaN(v)) {
                if (v >= 0.0) {
                    result[1] += v;
                }
                else {
                    result[0] += v;
                }
            }
        }
        return result;
    }

    protected double[] averageStackValues(double[] stack1, double[] stack2) {
        double[] result = new double[2];
        result[0] = (stack1[0] + stack2[0]) / 2.0;
        result[1] = (stack1[1] + stack2[1]) / 2.0;
        return result;
    }

    protected double[] adjustedStackValues(double[] stack1, double[] stack2) {
        double[] result = new double[2];
        if (stack1[0] == 0.0 || stack2[0] == 0.0) {
            result[0] = 0.0;
        }
        else {
            result[0] = (stack1[0] + stack2[0]) / 2.0;
        }
        if (stack1[1] == 0.0 || stack2[1] == 0.0) {
            result[1] = 0.0;
        }
        else {
            result[1] = (stack1[1] + stack2[1]) / 2.0;
        }
        return result;
    }

}

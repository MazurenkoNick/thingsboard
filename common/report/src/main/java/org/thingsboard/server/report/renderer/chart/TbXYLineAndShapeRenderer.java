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
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.PaintList;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class TbXYLineAndShapeRenderer extends XYLineAndShapeRenderer {

    private PaintList shapeFillPaintList;

    private transient Paint defaultShapeFillPaint;

    public TbXYLineAndShapeRenderer() {
        super();
        this.shapeFillPaintList = new PaintList();
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
        double transY1 = rangeAxis.valueToJava2D(y1, dataArea, yAxisLocation);

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

}

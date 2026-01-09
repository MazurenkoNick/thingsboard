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
package org.thingsboard.server.report.renderer.chart;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.Args;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbDateAxis extends DateAxis {

    private boolean gridlinesVisible;
    private transient Stroke gridlineStroke;
    private transient Paint gridlinePaint;

    public TbDateAxis(String label, TimeZone zone, Locale locale) {
        super(label, zone, locale);
        this.gridlinesVisible = true;
        this.gridlineStroke = new BasicStroke(1.0f);
        this.gridlinePaint = safeParseCssColor("rgba(0, 0, 0, 0.12)");
    }

    public boolean isGridlinesVisible() {
        return this.gridlinesVisible;
    }

    public void setGridlinesVisible(boolean visible) {
        if (this.gridlinesVisible != visible) {
            this.gridlinesVisible = visible;
            fireChangeEvent();
        }
    }

    public Stroke getGridlineStroke() {
        return this.gridlineStroke;
    }

    public void setGridlineStroke(Stroke stroke) {
        Args.nullNotPermitted(stroke, "stroke");
        this.gridlineStroke = stroke;
        fireChangeEvent();
    }

    public Paint getGridlinePaint() {
        return this.gridlinePaint;
    }

    public void setGridlinePaint(Paint paint) {
        Args.nullNotPermitted(paint, "paint");
        this.gridlinePaint = paint;
        fireChangeEvent();
    }

    @Override
    public AxisState draw(Graphics2D g2, double cursor, Rectangle2D plotArea,
                          Rectangle2D dataArea, RectangleEdge edge,
                          PlotRenderingInfo plotState) {
        AxisState state = super.draw(g2, cursor, plotArea, dataArea, edge, plotState);
        if (isVisible()) {
            drawGridlines(g2, dataArea, state.getTicks());
        }
        return state;
    }

    protected void drawGridlines(Graphics2D g2, Rectangle2D area,
                                 List<ValueTick> ticks) {
        if (isGridlinesVisible()) {
            for (ValueTick tick : ticks) {
                if (tick.getTickType() == TickType.MAJOR) {
                    XYPlot xyPlot = (XYPlot) getPlot();
                    xyPlot.getRenderer().drawDomainLine(g2, xyPlot, this,
                            area, tick.getValue(), getGridlinePaint(), getGridlineStroke());
                }
            }
        }
    }
}

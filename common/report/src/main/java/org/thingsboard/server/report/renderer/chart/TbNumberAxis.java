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

import lombok.Getter;
import lombok.Setter;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.Tick;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class TbNumberAxis extends NumberAxis {

    @Setter
    private Double axisMin;
    @Setter
    private Double axisMax;
    @Setter
    private Integer splitNumber;
    private TbNumberTickUnitSource tbNumberTickUnitSource = new TbNumberTickUnitSource();
    private TbNumberAxisTicks ticks = new TbNumberAxisTicks();

    private TbNumberAxis parentAxis = null;

    public TbNumberAxis() {
        this(null, null);
    }

    public TbNumberAxis(String label) {
        this(label, null);
    }

    public TbNumberAxis(String label, TbNumberAxis parentAxis) {
        super(label);
        this.parentAxis = parentAxis;
    }

    @Override
    protected void selectAutoTickUnit(Graphics2D g2, Rectangle2D dataArea,
                                      RectangleEdge edge) {
        super.selectAutoTickUnit(g2, dataArea, edge);
        this.autoAdjustRange();
    }

    @Override
    protected void autoAdjustRange() {
        super.autoAdjustRange();
        double lower = this.axisMin != null ? this.axisMin : getRange().getLowerBound();
        double upper = this.axisMax != null ? this.axisMax : getRange().getUpperBound();
        if (upper <= lower) {
            upper = lower + 1.0;
        }
        double length = upper - lower;
        NumberTickUnit tickUnit = getTickUnit();
        double size = tickUnit.getSize();
        if (splitNumber != null) {
            size = length / splitNumber;
        }
        if (isAutoTickUnitSelection()) {
            tickUnit = (NumberTickUnit) this.tbNumberTickUnitSource.getCeilingTickUnit(size);
            setTickUnit(tickUnit, false, false);
        }

        double currentAxisMin;
        double currentAxisMax;
        lower = getRange().getLowerBound();
        upper = getRange().getUpperBound();
        if (this.axisMin == null) {
            double rest = lower % tickUnit.getSize();
            if (rest > 0) {
                lower -= rest;
            }
            currentAxisMin = lower;
        } else {
            currentAxisMin = this.axisMin;
        }
        if (this.axisMax == null) {
            double rest = (upper - lower) % tickUnit.getSize();
            if (rest > 0) {
                double upperAdjust = tickUnit.getSize() - rest;
                upper += upperAdjust;
            }
            currentAxisMax = upper;
        } else {
            currentAxisMax = this.axisMax;
        }
        if (currentAxisMax <= currentAxisMin) {
            currentAxisMax = currentAxisMin + 1.0;
        }
        setRange(new Range(currentAxisMin, currentAxisMax), false, false);
        this.calculateTicks();
    }

    @Override
    protected List<Tick> refreshTicksVertical(Graphics2D g2,
                                              Rectangle2D dataArea, RectangleEdge edge) {

        List<Tick> result = new ArrayList<>();
        Font tickLabelFont = getTickLabelFont();
        g2.setFont(tickLabelFont);
        if (isAutoTickUnitSelection()) {
            selectAutoTickUnit(g2, dataArea, edge);
        }

        if (ticks.getTicksCount() <= ValueAxis.MAXIMUM_TICK_COUNT) {

            boolean drawFirstLabel = true;
            boolean drawLastLabel = true;

            TbNumberAxisTicks ticks = getTicks();
            if (ticks.checkFirstTickIntersection()) {
                double labelHeight = estimateMaximumTickLabelHeight(g2);
                double firstLabelPos = valueToJava2D(ticks.firstTickValue(getRange()), dataArea, edge);
                double nextLabelPos = valueToJava2D(ticks.getTickValue(1, getRange()), dataArea, edge);
                if ((firstLabelPos - labelHeight / 2) < (nextLabelPos + labelHeight / 2)) {
                    drawFirstLabel = false;
                }
            }

            if (ticks.checkLastTickIntersection()) {
                double labelHeight = estimateMaximumTickLabelHeight(g2);
                double lastLabelPos = valueToJava2D(ticks.lastTickValue(getRange()), dataArea, edge);
                double prevLabelPos = valueToJava2D(ticks.getTickValue(ticks.getTicksCount()-2, getRange()), dataArea, edge);
                if ((lastLabelPos + labelHeight / 2) > (prevLabelPos - labelHeight / 2)) {
                    drawLastLabel = false;
                }
            }

            for (int i = 0; i < ticks.getTicksCount(); i++) {
                double currentTickValue = ticks.getTickValue(i, getRange());
                if (currentTickValue == 0.0 && Double.compare(currentTickValue, 0.0) < 0) {
                    currentTickValue = 0.0;
                }
                String tickLabel = null;
                boolean drawLabel = (i == 0 && drawFirstLabel) || (i == ticks.getTicksCount() - 1 && drawLastLabel) || i > 0 && i < ticks.getTicksCount() - 1;
                if (drawLabel) {
                    NumberFormat formatter = getNumberFormatOverride();
                    if (formatter != null) {
                        tickLabel = formatter.format(currentTickValue);
                    } else {
                        tickLabel = getTickUnit().valueToString(currentTickValue);
                    }
                }

                TextAnchor anchor;
                TextAnchor rotationAnchor;
                double angle = 0.0;
                if (edge == RectangleEdge.LEFT) {
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                }
                else {
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                }
                Tick tick = new NumberTick(currentTickValue, tickLabel, anchor,
                        rotationAnchor, angle);
                result.add(tick);
            }
        }
        return result;
    }

    private TbNumberAxisTicks getTicks() {
        if (this.parentAxis != null) {
            return this.parentAxis.getTicks();
        } else {
            return this.ticks;
        }
    }

    private void calculateTicks() {
        if (this.parentAxis == null) {
            TickUnit tu = getTickUnit();
            double size = tu.getSize();
            this.ticks.clear();
            double currentTickValue = getRange().getLowerBound();
            double maxTickValue = getRange().getUpperBound();
            this.ticks.addTickValue(currentTickValue, getRange());
            double lowestVisibleTickValue = isAutoTickUnitSelection() ? calculateLowestVisibleTickValue() : currentTickValue;
            if (lowestVisibleTickValue > currentTickValue) {
                this.ticks.setAdditionalFistTick(true);
                currentTickValue = lowestVisibleTickValue;
            } else {
                currentTickValue += size;
            }
            while (currentTickValue < maxTickValue) {
                this.ticks.addTickValue(currentTickValue, getRange());
                currentTickValue += size;
            }
            if (maxTickValue > getRange().getLowerBound()) {
                if (maxTickValue != currentTickValue) {
                    this.ticks.setAdditionalLastTick(true);
                }
                this.ticks.addTickValue(maxTickValue, getRange());
            }
        }
    }

    private static class TbNumberAxisTicks {

        private final List<Double> tickValues = new ArrayList<>();

        @Getter
        @Setter
        private boolean additionalFistTick = false;

        @Getter
        @Setter
        private boolean additionalLastTick = false;

        public TbNumberAxisTicks() {}

        public void addTickValue(double tickValue, Range range) {
            tickValues.add((tickValue - range.getLowerBound()) / range.getLength());
        }

        public int getTicksCount() {
            return tickValues.size();
        }

        public Double getTickValue(int index, Range range) {
            return range.getLowerBound()  + tickValues.get(index) * range.getLength();
        }

        public Double firstTickValue(Range range) {
            return getTickValue(0, range);
        }

        public Double lastTickValue(Range range) {
            return getTickValue(tickValues.size() - 1, range);
        }

        public void clear() {
            this.tickValues.clear();
            this.additionalFistTick = false;
            this.additionalLastTick = false;
        }

        public boolean checkFirstTickIntersection() {
            return this.additionalFistTick && this.tickValues.size() > 2;
        }

        public boolean checkLastTickIntersection() {
            return this.additionalLastTick && this.tickValues.size() > 2;
        }

    }


}

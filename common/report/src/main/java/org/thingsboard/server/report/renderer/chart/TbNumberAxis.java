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
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.Range;

import java.awt.*;
import java.awt.font.FontRenderContext;
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

    private final TbNumberTickUnitSource tbNumberTickUnitSource = new TbNumberTickUnitSource();
    private final TbNumberAxis parentAxis;

    private TbNumberAxisTicks ticks;

    public TbNumberAxis(String label, TbNumberAxis parentAxis) {
        super(label);
        if (parentAxis != null) {
            this.setUpperMargin(0);
            this.setLowerMargin(0);
        }
        this.parentAxis = parentAxis;
    }

    @Override
    protected List<Tick> refreshTicksVertical(Graphics2D g2,
                                              Rectangle2D dataArea, RectangleEdge edge) {

        List<Tick> result = new ArrayList<>();
        Font tickLabelFont = getTickLabelFont();
        g2.setFont(tickLabelFont);
        if ((splitNumber == null || splitNumber == 0) && isAutoTickUnitSelection()) {
            selectAutoTickUnit(g2, dataArea, edge);
        }
        if (this.ticks == null) {
            this.adjustTicksAndRange();
        }

        if (ticks.getTicksCount() <= ValueAxis.MAXIMUM_TICK_COUNT) {

            boolean drawFirstLabel = true;
            boolean drawLastLabel = true;

            if (ticks.checkFirstTickIntersection()) {
                double labelHeight = calculateTickLabelHeight(g2, "123");
                double firstLabelPos = valueToJava2D(ticks.firstTickValue(getRange()), dataArea, edge);
                double nextLabelPos = valueToJava2D(ticks.getTickValue(1, getRange()), dataArea, edge);
                if ((firstLabelPos - labelHeight / 2) < (nextLabelPos + labelHeight / 2)) {
                    drawFirstLabel = false;
                }
            }

            if (ticks.checkLastTickIntersection()) {
                double labelHeight = calculateTickLabelHeight(g2, "123");
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

    @Override
    protected double estimateMaximumTickLabelHeight(Graphics2D g2) {
        double result = calculateTickLabelHeight(g2, "123");
        return result * 2;
    }

    private double calculateTickLabelHeight(Graphics2D g2, String label) {
        RectangleInsets tickLabelInsets = getTickLabelInsets();
        double result = tickLabelInsets.getTop() + tickLabelInsets.getBottom();

        Font tickLabelFont = getTickLabelFont();
        FontRenderContext frc = g2.getFontRenderContext();
        result += tickLabelFont.getLineMetrics(label, frc).getHeight();
        return result;
    }

    private void adjustTicksAndRange() {
        double lower = this.axisMin != null ? this.axisMin : getRange().getLowerBound();
        double upper = this.axisMax != null ? this.axisMax : getRange().getUpperBound();
        if (upper <= lower) {
            upper = lower + 1.0;
        }
        double length = upper - lower;
        NumberTickUnit tickUnit = getTickUnit();
        double size = tickUnit.getSize();
        if (this.parentAxis != null) {
            if (this.axisMin == null) {
                if (this.parentAxis.getRange().getLowerBound() < 0) {
                    double zeroDistance = Math.abs(this.parentAxis.getRange().getLowerBound()) / this.parentAxis.getRange().getLength();
                    lower -= zeroDistance * length;
                    length = upper - lower;
                }
            }
            size = calculateChildSize(length);
            int parentSplitCount = this.parentAxis.getTicks().getTicksCount() - 1;
            length = parentSplitCount * size;
            if (this.axisMin == null) {
                lower = nearestLower(lower, size);
            }
            if (this.axisMax == null) {
                upper = lower + length;
                if (upper < getRange().getUpperBound()) {
                    double newLength = getRange().getUpperBound() - lower;
                    size = calculateChildSize(newLength);
                    length = parentSplitCount * size;
                    upper = lower + length;
                }
                setRange(new Range(lower, upper), false, false);
            }
        } else {
            if (splitNumber != null && splitNumber > 0) {
                size = length / splitNumber;
            }
            if (isAutoTickUnitSelection()) {
                tickUnit = (NumberTickUnit) this.tbNumberTickUnitSource.getCeilingTickUnit(size);
                setTickUnit(tickUnit, false, false);
            }
        }

        double currentAxisMin;
        double currentAxisMax;
        size = tickUnit.getSize();
        lower = getRange().getLowerBound();
        upper = getRange().getUpperBound();

        if (this.axisMin == null) {
            if (this.parentAxis == null) {
                lower = nearestLower(lower, size);
            }
            currentAxisMin = lower;
        } else {
            currentAxisMin = this.axisMin;
        }
        if (this.axisMax == null) {
            if (this.parentAxis == null) {
                upper = nearestUpper(lower, upper, size);
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

    private double calculateChildSize(double length) {
        NumberTickUnit tickUnit = getTickUnit();
        double size = tickUnit.getSize();
        int parentSplitCount = this.parentAxis.getTicks().getTicksCount() - 1;
        boolean roundToNearest = true;
        if (splitNumber != null && splitNumber > 0) {
            size = length / splitNumber;
        } else if (isAutoTickUnitSelection()) {
            size = length / parentSplitCount;
            roundToNearest = false;
        }
        if (isAutoTickUnitSelection()) {
            tickUnit = (NumberTickUnit) this.tbNumberTickUnitSource.getCeilingTickUnit(size, roundToNearest);
            setTickUnit(tickUnit, false, false);
            size = tickUnit.getSize();
        }
        return size;
    }

    private double nearestLower(double lower, double size) {
        double rest = Math.abs(lower) % size;
        if (rest > 0) {
            if (lower < 0) {
                rest = size - rest;
            }
            lower -= rest;
        }
        return lower;
    }

    private double nearestUpper(double lower, double upper, double size) {
        double rest = (upper - lower) % size;
        if (rest > 0) {
            double upperAdjust = size - rest;
            upper += upperAdjust;
        }
        return upper;
    }

    private TbNumberAxisTicks getTicks() {
        return this.ticks;
    }

    private void calculateTicks() {
        if (this.parentAxis == null) {
            TickUnit tu = getTickUnit();
            double size = tu.getSize();
            this.ticks = new TbNumberAxisTicks();
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
        } else {
            Double unitSize = !isAutoTickUnitSelection() ? getTickUnit().getSize() : null;
            this.ticks = this.parentAxis.getTicks().computeChildTicks(this.splitNumber, unitSize, getRange());
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
           addTick((tickValue - range.getLowerBound()) / range.getLength());
        }

        public void addTick(double tick) {
            tickValues.add(tick);
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

        public boolean checkFirstTickIntersection() {
            return this.additionalFistTick && this.tickValues.size() > 2;
        }

        public boolean checkLastTickIntersection() {
            return this.additionalLastTick && this.tickValues.size() > 2;
        }

        public TbNumberAxisTicks computeChildTicks(Integer splitCount, Double unitSize, Range range) {
            if (this.additionalFistTick || this.additionalLastTick || ((splitCount == null || splitCount == 0) && unitSize == null)) {
                return this;
            } else {
                Integer step = null;
                int parentSplitCount = this.tickValues.size() - 1;
                if (splitCount == null || splitCount == 0) {
                    double unitStep =  unitSize / range.getLength();
                    splitCount = (int) (1 / unitStep);
                }
                if (parentSplitCount != splitCount && parentSplitCount % splitCount == 0) {
                    step = parentSplitCount / splitCount;
                }
                if (step == null && unitSize != null) {
                    double unitStep =  unitSize / range.getLength();
                    double parentStep = 1.0 / (this.tickValues.size() - 1);
                    if (unitStep != parentStep && unitStep % parentStep == 0) {
                        step = (int)(unitStep / parentStep);
                    }
                }
                if (step != null) {
                    TbNumberAxisTicks ticks = new TbNumberAxisTicks();
                    for (int i = 0; i < this.tickValues.size(); i += step) {
                        ticks.addTick(this.tickValues.get(i));
                    }
                    if ((this.tickValues.size() - 1) % step != 0) {
                        ticks.addTick(this.tickValues.get(this.tickValues.size() - 1));
                    }
                    return ticks;
                } else {
                    return this;
                }
            }
        }
    }
}

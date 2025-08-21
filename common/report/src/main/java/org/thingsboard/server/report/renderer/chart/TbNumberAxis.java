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

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class TbNumberAxis extends NumberAxis {

    private Integer splitNumber;

    public TbNumberAxis() {
        super();
    }

    public TbNumberAxis(String label) {
        super(label);
    }

    public void setSplitNumber(Integer splitNumber) {
        this.splitNumber = splitNumber;
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
        double lower = getRange().getLowerBound();
        double upper = getRange().getUpperBound();
        if (upper > lower) {
            NumberTickUnit tickUnit = getTickUnit();
            if (splitNumber != null) {
                double size = getRange().getLength() / (splitNumber + 1);
                tickUnit = (NumberTickUnit) this.getStandardTickUnits().getCeilingTickUnit(size);
                setTickUnit(tickUnit);
            }
            double rest = lower % tickUnit.getSize();
            if (rest > 0) {
                lower -= rest;
            }
            rest = (upper - lower) % tickUnit.getSize();
            if (rest > 0) {
                double upperAdjust = tickUnit.getSize() - rest;
                upper += upperAdjust;
            }
            setRange(new Range(lower, upper), false, false);
        }
    }

    @Override
    protected int calculateVisibleTickCount() {
        //if (splitNumber != null) {
        //    return splitNumber + 1;
        //} else {
            double unit = getTickUnit().getSize();
            Range range = getRange();
            return (int) (Math.floor(range.getUpperBound() / unit)
                    - Math.ceil(range.getLowerBound() / unit) + 1);
        //}
    }

}

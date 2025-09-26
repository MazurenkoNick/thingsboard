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

import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnitSource;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;

public class TbNumberTickUnitSource implements TickUnitSource, Serializable {

    private int power;

    private int factor;

    private final NumberFormat formatter;

    public TbNumberTickUnitSource() {
        this(null);
    }

    public TbNumberTickUnitSource(NumberFormat formatter) {
        this.formatter = formatter;
        this.power = 0;
        this.factor = 1;
    }

    @Override
    public TickUnit getLargerTickUnit(TickUnit unit) {
        TickUnit t = getCeilingTickUnit(unit);
        if (t.equals(unit)) {
            next();
            t = new NumberTickUnit(getTickSize(), getTickLabelFormat(),
                    getMinorTickCount());
        }
        return t;
    }

    @Override
    public TickUnit getCeilingTickUnit(TickUnit unit) {
        return getCeilingTickUnit(unit.getSize());
    }

    @Override
    public TickUnit getCeilingTickUnit(double size) {
        return getCeilingTickUnit(size, true);
    }

    public TickUnit getCeilingTickUnit(double size, boolean roundToNearest) {
        if (Double.isInfinite(size)) {
            throw new IllegalArgumentException("Must be finite.");
        }
        if (roundToNearest) {
            size = this.nearestRoundedNumber(size);
        }
        this.power = (int) Math.ceil(Math.log10(size));
        this.factor = 1;
        boolean done = false;
        while (!done) {
            done = !previous();
            if (getTickSize() < size) {
                next();
                done = true;
            }
        }
        return new NumberTickUnit(getTickSize(), getTickLabelFormat(),
                getMinorTickCount());
    }

    private double nearestRoundedNumber(double number) {
        double magnitude = Math.floor(Math.log10(number));
        double roundBase = Math.pow(10, magnitude);
        BigDecimal bd = new BigDecimal(Double.toString(number));
        BigDecimal rounded = bd.divide(new BigDecimal(roundBase), 0, RoundingMode.HALF_UP).multiply(new BigDecimal(roundBase));
        return rounded.doubleValue();
    }

    private boolean next() {
        if (factor == 1) {
            factor = 2;
            return true;
        } else if (factor == 2) {
            factor = 3;
            return true;
        } else if (factor == 3) {
            factor = 5;
            return true;
        } else if (factor == 5) {
            if (power == 300) {
                return false;
            }
            power++;
            factor = 1;
            return true;
        }
        throw new IllegalStateException("We should never get here.");
    }

    private boolean previous() {
        if (factor == 1) {
            factor = 5;
            power--;
            return true;
        } else if (factor == 2) {
            factor = 1;
            return true;
        } else if (factor == 3) {
            factor = 2;
            return true;
        } else if (factor == 5) {
            factor = 3;
            return true;
        }
        throw new IllegalStateException("We should never get here.");
    }

    private double getTickSize() {
        return this.factor * Math.pow(10.0, this.power);
    }

    private final DecimalFormat dfNeg4 = new DecimalFormat("0.0000");
    private final DecimalFormat dfNeg3 = new DecimalFormat("0.000");
    private final DecimalFormat dfNeg2 = new DecimalFormat("0.00");
    private final DecimalFormat dfNeg1 = new DecimalFormat("0.0");
    private final DecimalFormat df0 = new DecimalFormat("#,##0");
    private final DecimalFormat df = new DecimalFormat("#.######E0");

    private NumberFormat getTickLabelFormat() {
        if (this.formatter != null) {
            return this.formatter;
        }
        if (power == -4) {
            return dfNeg4;
        }
        if (power == -3) {
            return dfNeg3;
        }
        if (power == -2) {
            return dfNeg2;
        }
        if (power == -1) {
            return dfNeg1;
        }
        if (power >= 0 && power <= 6) {
            return df0;
        }
        return df;
    }

    private int getMinorTickCount() {
        if (factor == 1) {
            return 10;
        } else if (factor == 5) {
            return 5;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TbNumberTickUnitSource)) {
            return false;
        }
        TbNumberTickUnitSource that = (TbNumberTickUnitSource) obj;
        if (!Objects.equals(this.formatter, that.formatter)) {
            return false;
        }
        if (this.power != that.power) {
            return false;
        }
        if (this.factor != that.factor) {
            return false;
        }
        return true;
    }
}

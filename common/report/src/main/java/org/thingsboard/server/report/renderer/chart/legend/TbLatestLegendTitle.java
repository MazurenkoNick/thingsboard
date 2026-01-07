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
package org.thingsboard.server.report.renderer.chart.legend;

import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.EmptyBlock;
import org.jfree.chart.block.LabelBlock;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;
import org.thingsboard.server.report.renderer.chart.layout.TbContentJustify;
import org.thingsboard.server.report.renderer.chart.layout.TbFlowArrangement;
import org.thingsboard.server.report.renderer.chart.layout.TbTableBlockContainer;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static org.thingsboard.server.report.util.AwtFontUtils.newFont;
import static org.thingsboard.server.report.util.ColorUtils.safeParseCssColor;

public class TbLatestLegendTitle extends Title {

    private static final Shape LEGEND_ITEM_SHAPE = new Ellipse2D.Double(-4f, -4f, 8f, 8f);
    private static final Paint TOTAL_SHAPE_FILL = safeParseCssColor("rgba(0, 0, 0, 0.06)");
    private static final Paint DISABLED_ITEM_PAINT = safeParseCssColor("#ccc");

    private final List<TbLatestChartLegendItem> legendItems;

    private Block legendContainer;

    private Font legendLabelFont;
    private Paint legendLabelPaint;
    private Font legendValueFont;
    private Paint legendValuePaint;

    private double maxRelativeWidth = 0;
    private double maxRelativeHeight = 0;

    public TbLatestLegendTitle(List<TbLatestChartLegendItem> legendItems) {
        this.legendItems = legendItems;

        this.legendLabelFont = newFont("Roboto", Font.PLAIN, 12);
        this.legendLabelPaint = safeParseCssColor("rgba(0, 0, 0, 0.38)");

        this.legendValueFont = newFont("Roboto", Font.BOLD, 14);
        this.legendValuePaint = safeParseCssColor("rgba(0, 0, 0, 0.87)");
    }

    public void setLegendLabelFont(Font legendLabelFont) {
        this.legendLabelFont = legendLabelFont;
    }

    public void setLegendLabelPaint(Paint legendLabelPaint) {
        this.legendLabelPaint = legendLabelPaint;
    }

    public void setLegendValueFont(Font legendValueFont) {
        this.legendValueFont = legendValueFont;
    }

    public void setLegendValuePaint(Paint legendValuePaint) {
        this.legendValuePaint = legendValuePaint;
    }

    public void setMaxRelativeWidth(double maxRelativeWidth) {
        this.maxRelativeWidth = maxRelativeWidth;
    }

    public void setMaxRelativeHeight(double maxRelativeHeight) {
        this.maxRelativeHeight = maxRelativeHeight;
    }

    @Override
    public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
        this.buildLegendItems();
        RectangleEdge p = getPosition();
        RectangleConstraint targetConstraint = constraint;
        if (RectangleEdge.isTopOrBottom(p)) {
            if (this.maxRelativeHeight > 0) {
                double maxHeight = constraint.getHeight() * this.maxRelativeHeight;
                targetConstraint = toContentConstraint(new RectangleConstraint(constraint.getWidth(), maxHeight));
            } else {
                targetConstraint = toContentConstraint(constraint.toFixedWidth(constraint.getWidth()));
            }
        } else {
            if (this.maxRelativeWidth > 0) {
                double maxWidth = constraint.getWidth() * this.maxRelativeWidth;
                targetConstraint = toContentConstraint(new RectangleConstraint(maxWidth, constraint.getHeight()));
            }
        }
        Size2D size = this.legendContainer.arrange(g2, targetConstraint);
        Size2D result = new Size2D();
        result.height = calculateTotalHeight(size.height);
        result.width = calculateTotalWidth(size.width);
        return result;
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area) {
        draw(g2, area, null);
    }

    @Override
    public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
        Rectangle2D target = (Rectangle2D) area.clone();
        target = trimMargin(target);
        target = trimBorder(target);
        target = trimPadding(target);
        return this.legendContainer.draw(g2, target, params);
    }

    private void buildLegendItems() {
        if (RectangleEdge.isTopOrBottom(getPosition())) {
            TbFlowArrangement legendArrangement = new TbFlowArrangement(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, TbContentJustify.SPACE_AROUND,8.0, 8.0);
            this.legendContainer = new BlockContainer(legendArrangement);
        } else {
            this.legendContainer = new TbTableBlockContainer(4.0, 8.0);
        }
        RectangleEdge p = getPosition();
        if (RectangleEdge.isTopOrBottom(p)) {
            this.buildVerticalLegendItems();
        } else {
            this.buildHorizontalLegendItems();
        }
    }

    private void buildVerticalLegendItems() {
        BlockContainer container = (BlockContainer) this.legendContainer;
        for (TbLatestChartLegendItem item : legendItems) {
            Block itemBlock = this.buildVerticalLegendItem(item);
            container.add(itemBlock);
        }
    }

    private void buildHorizontalLegendItems() {
        TbTableBlockContainer tableContainer = (TbTableBlockContainer) this.legendContainer;
        int legendShape = tableContainer.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        tableContainer.setColumnHeader(new EmptyBlock(0,0), legendShape);
        int legendLabel = tableContainer.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        tableContainer.setColumnHeader(new EmptyBlock(0,0), legendLabel);
        int legendValue = tableContainer.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.RIGHT, false);
        tableContainer.setColumnHeader(new EmptyBlock(0,0), legendValue);
        for (TbLatestChartLegendItem item : legendItems) {
            Block shapeBlock = this.buildLegendShape(item);
            tableContainer.addColumnCell(shapeBlock, legendShape);
            Block labelBlock = this.buildLegendLabel(item);
            tableContainer.addColumnCell(labelBlock, legendLabel);
            Block valueBlock = this.buildLegendValue(item);
            tableContainer.addColumnCell(valueBlock, legendValue);
        }
    }

    private Block buildVerticalLegendItem(TbLatestChartLegendItem item) {
        TbTableBlockContainer legendBlock = new TbTableBlockContainer(4.0, 0.0);

        int shape = legendBlock.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        Block shapeBlock = this.buildLegendShape(item);
        legendBlock.setColumnHeader(shapeBlock, shape);
        legendBlock.addColumnCell(new EmptyBlock(0,0), shape);

        int label = legendBlock.addColumn(HorizontalAlignment.LEFT, HorizontalAlignment.LEFT, false);
        Block labelBlock = this.buildLegendLabel(item);
        legendBlock.setColumnHeader(labelBlock, label);
        Block valueBlock = this.buildLegendValue(item);
        legendBlock.addColumnCell(valueBlock, label);

        return legendBlock;
    }

    private Block buildLegendShape(TbLatestChartLegendItem item) {
        Paint shapeFill;
        if (!item.isHasValue()) {
            shapeFill = DISABLED_ITEM_PAINT;
        } else {
            if (!item.isTotal()) {
                shapeFill = safeParseCssColor(item.getColor());
            } else {
                shapeFill = TOTAL_SHAPE_FILL;
            }
        }
        LegendGraphic lg = new LegendGraphic(LEGEND_ITEM_SHAPE, shapeFill);
        lg.setPadding(RectangleInsets.ZERO_INSETS);
        return lg;
    }

    private Block buildLegendLabel(TbLatestChartLegendItem item) {
        String label = item.getLabel();
        Paint labelPaint = this.legendLabelPaint;
        if (!item.isHasValue()) {
            labelPaint = DISABLED_ITEM_PAINT;
        }
        return new LabelBlock(label, this.legendLabelFont, labelPaint);
    }

    private Block buildLegendValue(TbLatestChartLegendItem item) {
        String value = item.getValue();
        Paint labelPaint = this.legendValuePaint;
        if (!item.isHasValue()) {
            labelPaint = DISABLED_ITEM_PAINT;
        }
        return new LabelBlock(value, this.legendValueFont, labelPaint);
    }
}

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

import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.FlowArrangement;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.Size2D;
import org.jfree.chart.ui.VerticalAlignment;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class TbFlowArrangement extends FlowArrangement {

    private final HorizontalAlignment horizontalAlignment;
    private final VerticalAlignment verticalAlignment;
    private final double horizontalGap;
    private final double verticalGap;

    private double maxRelativeHeight = 0;

    public TbFlowArrangement() {
        this(HorizontalAlignment.CENTER, VerticalAlignment.CENTER, 2.0, 2.0);
    }

    public TbFlowArrangement(HorizontalAlignment hAlign, VerticalAlignment vAlign,
                             double hGap, double vGap) {
        super(hAlign, vAlign, hGap, vGap);
        this.horizontalAlignment = hAlign;
        this.verticalAlignment = vAlign;
        this.horizontalGap = hGap;
        this.verticalGap = vGap;
    }

    public void setMaxRelativeHeight(double maxRelativeHeight) {
        this.maxRelativeHeight = maxRelativeHeight;
    }

    @Override
    public Size2D arrange(BlockContainer container, Graphics2D g2,
                          RectangleConstraint constraint) {
        if (this.maxRelativeHeight > 0) {
            double maxHeight = constraint.getHeight() * this.maxRelativeHeight;
            constraint = constraint.toFixedHeight(maxHeight);
        }
        return super.arrange(container, g2, constraint);
    }

    @Override
    protected Size2D arrangeFN(BlockContainer container, Graphics2D g2,
                               RectangleConstraint constraint) {

        List<Block> blocks = (List<Block>) container.getBlocks();
        double width = constraint.getWidth();

        double x = 0.0;
        double y = 0.0;
        double maxHeight = 0.0;
        List<Block> itemsInRow = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            Size2D size = block.arrange(g2, RectangleConstraint.NONE);
            if (x + size.width <= width) {
                itemsInRow.add(block);
                block.setBounds(
                        new Rectangle2D.Double(x, y, size.width, size.height)
                );
                x = x + size.width + this.horizontalGap;
                maxHeight = Math.max(maxHeight, size.height);
            }
            else {
                if (itemsInRow.isEmpty()) {
                    // place in this row (truncated) anyway
                    block.setBounds(
                            new Rectangle2D.Double(
                                    x, y, Math.min(size.width, width - x), size.height
                            )
                    );
                    x = 0.0;
                    y = y + size.height + this.verticalGap;
                }
                else {
                    alignItems(itemsInRow, width);
                    // start new row
                    itemsInRow.clear();
                    x = 0.0;
                    y = y + maxHeight + this.verticalGap;
                    maxHeight = size.height;
                    block.setBounds(
                            new Rectangle2D.Double(
                                    x, y, Math.min(size.width, width), size.height
                            )
                    );
                    x = size.width + this.horizontalGap;
                    itemsInRow.add(block);
                }
            }
        }
        alignItems(itemsInRow, width);
        return new Size2D(constraint.getWidth(), y + maxHeight);
    }

    protected Size2D arrangeFF(BlockContainer container, Graphics2D g2,
                               RectangleConstraint constraint) {
        Size2D s = arrangeFN(container, g2, constraint);
        if (s.height > constraint.getHeight()) {
            List<Block> blocks = (List<Block>) container.getBlocks();
            List<Block> visibleBlocks = new ArrayList<>();
            for (Block b : blocks) {
                Rectangle2D bounds = b.getBounds();
                double bottom = bounds.getMaxY();
                if (bottom <= constraint.getHeight()) {
                    visibleBlocks.add(b);
                }
            }
            container.clear();
            for (Block b : visibleBlocks) {
                container.add(b);
            }
            return arrangeFN(container, g2, constraint);
        }
        return s;
    }

    private void alignItems(List<Block> items, double width) {
        double itemsWidth = this.horizontalGap * (items.size() - 1);
        for (Block item : items) {
            itemsWidth += item.getBounds().getWidth();
        }
        if (itemsWidth < width) {
            double movement = 0;
            if (horizontalAlignment == HorizontalAlignment.CENTER) {
                movement = (width - itemsWidth) / 2;
            } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
                movement = width - itemsWidth;
            }
            if (movement > 0) {
                for (Block item : items) {
                    Rectangle2D bounds = item.getBounds();
                    item.setBounds(new Rectangle2D.Double(bounds.getX() + movement, bounds.getY(), bounds.getWidth(), bounds.getHeight()));
                }
            }
        }
    }
}

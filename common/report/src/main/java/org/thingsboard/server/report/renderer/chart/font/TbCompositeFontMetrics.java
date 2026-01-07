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
package org.thingsboard.server.report.renderer.chart.font;

import org.jetbrains.annotations.NotNull;

import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;

public class TbCompositeFontMetrics extends FontMetrics {

    private final FontMetrics delegate;
    private final TbCompositeFont compositeFont;

    public TbCompositeFontMetrics(FontMetrics delegate, TbCompositeFont font) {
        super(font);
        this.delegate = delegate;
        this.compositeFont = font;
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return this.delegate.getFontRenderContext();
    }

    @Override
    public int charWidth(char ch) {
        return this.delegate.charWidth(ch);
    }

    @Override
    public int charWidth(int ch) {
        return this.delegate.charWidth(ch);
    }

    @Override
    public int stringWidth(@NotNull String str) {
        return this.compositeFont.stringWidth(delegate, str);
    }

    @Override
    public int charsWidth(char[] data, int off, int len) {
        return this.compositeFont.charsWidth(delegate, data, off, len);
    }

    @Override
    public int[] getWidths() {
        return this.delegate.getWidths();
    }

    @Override
    public int getMaxAdvance() {
        return this.delegate.getMaxAdvance();
    }

    @Override
    public int getAscent() {
        return this.delegate.getAscent();
    }

    @Override
    public int getDescent() {
        return this.delegate.getDescent();
    }

    @Override
    public int getLeading() {
        return this.delegate.getLeading();
    }

    @Override
    public int getHeight() {
        return this.delegate.getHeight();
    }
}

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
package org.thingsboard.server.report.util.itext;

import com.google.errorprone.annotations.CheckReturnValue;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.Size;
import org.xhtmlrenderer.pdf.ITextFSImage;

public class PdfSvgImage extends ITextFSImage {

    private final PdfSvgDocument _svgDocument;
    private byte[] _image;
    private final float dotsPerPixel;
    private final int usablePageWidthPx;

    public PdfSvgImage(PdfSvgDocument svgDocument, float dotsPerPixel, int usablePageWidthPx) {
        this(svgDocument, dotsPerPixel, usablePageWidthPx, new Size((int)(svgDocument.size().width * dotsPerPixel), (int)(svgDocument.size().height * dotsPerPixel)));
    }

    public PdfSvgImage(PdfSvgDocument svgDocument, float dotsPerPixel, int usablePageWidthPx, Size size) {
        super(null, size, null);
        this._svgDocument = svgDocument;
        this.dotsPerPixel = dotsPerPixel;
        this.usablePageWidthPx = usablePageWidthPx;
    }

    @CheckReturnValue
    @Override
    public FSImage scale(int width, int height) {
        Size newSize = size.scale(width, height);
        if (size != newSize) {
           return new PdfSvgImage(_svgDocument, dotsPerPixel, usablePageWidthPx, newSize);
        }
        return this;
    }

    @Override
    public byte[] getImage() {
        if (_image == null) {
            try {
                _image = this._svgDocument.render((float) getWidth() / this.dotsPerPixel,
                        (float) getHeight() / this.dotsPerPixel, this.usablePageWidthPx);
            } catch (Exception e) {}
        }
        return _image;
    }
}

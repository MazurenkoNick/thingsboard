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
package org.thingsboard.server.report.util.itext;

import com.lowagie.text.Image;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.pdf.ITextFSImage;

import java.awt.image.BufferedImage;

public class PdfSvgFSImage extends ITextFSImage {

    private final PdfSvgDocument _svgDocument;
    private Image _image;
    private final float factor;
    private final int width;
    private final int height;

    public PdfSvgFSImage(PdfSvgDocument svgDocument, float factor) {
        this(svgDocument, factor, (int)(svgDocument.size().width * factor), (int)(svgDocument.size().height * factor));
    }

    public PdfSvgFSImage(PdfSvgDocument svgDocument, float factor, int width, int height) {
        super(null);
        this._svgDocument = svgDocument;
        this.factor = factor;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public FSImage scale(int width, int height) {
        if (width > 0 || height > 0) {
            int currentWith = getWidth();
            int currentHeight = getHeight();
            int targetWidth = width;
            int targetHeight = height;

            if (targetWidth == -1) {
                targetWidth = (int)(currentWith * ((double)targetHeight / currentHeight));
            }

            if (targetHeight == -1) {
                targetHeight = (int)(currentHeight * ((double)targetWidth / currentWith));
            }

            if (currentWith != targetWidth || currentHeight != targetHeight) {
                return new PdfSvgFSImage(this._svgDocument, this.factor, targetWidth, targetHeight);
            }
        }
        return this;
    }

    @Override
    public Image getImage() {
        if (_image == null) {
            try {
                BufferedImage bufferedImage = this._svgDocument.render(this.factor, this.width, this.height);
                _image = Image.getInstance(bufferedImage, null);
            } catch (Exception e) {}
        }
        return _image;
    }


    @Override
    public Object clone() {
        return new PdfSvgFSImage(_svgDocument, this.factor, this.width, this.height);
    }

}

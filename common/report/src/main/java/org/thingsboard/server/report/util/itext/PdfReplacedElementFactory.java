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

import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.BookmarkElement;
import org.xhtmlrenderer.pdf.CheckboxFormField;
import org.xhtmlrenderer.pdf.EmptyReplacedElement;
import org.xhtmlrenderer.pdf.ITextImageElement;
import org.xhtmlrenderer.pdf.TextFormField;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

public class PdfReplacedElementFactory implements ReplacedElementFactory {

    public PdfReplacedElementFactory() {
    }

    @Override
    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box,
                                                 UserAgentCallback uac, int cssWidth, int cssHeight) {
        Element e = box.getElement();
        if (e == null) {
            return null;
        }

        String nodeName = e.getNodeName();
        switch (nodeName) {
            case "img":
                String srcAttr = e.getAttribute("src");
                if (srcAttr.isEmpty()) {
                    srcAttr = "/assets/report/components/image-placeholder.svg";
                }
                if (srcAttr.equals("noImage")) {
                    return null;
                }
                FSImage fsImage = uac.getImageResource(srcAttr).getImage();
                if (fsImage != null) {
                    if (cssWidth != -1 || cssHeight != -1) {
                        fsImage = fsImage.scale(cssWidth, cssHeight);
                    }
                    return new ITextImageElement(fsImage);
                }
                break;
            case "input":
                String type = e.getAttribute("type");
                switch (type) {
                    case "hidden":
                        return new EmptyReplacedElement(1, 1);
                    case "checkbox":
                        return new CheckboxFormField(c, box, cssWidth, cssHeight);
                    default:
                        return new TextFormField(c, box, cssWidth, cssHeight);
                }
            case "bookmark":
                if (e.hasAttribute("name")) {
                    String name = e.getAttribute("name");
                    c.addBoxId(name, box);
                    return new BookmarkElement(name);
                }
                return new BookmarkElement(null);
        }

        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void remove(Element e) {

    }

    public void remove(String fieldName) {
    }

    @Override
    public void setFormSubmissionListener(FormSubmissionListener listener) {
        // nothing to do, form submission is handled by pdf readers
    }
}

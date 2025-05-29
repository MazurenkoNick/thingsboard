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
package org.thingsboard.server.report.util;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.DefaultParserProvider;
import com.github.weisj.jsvg.parser.DomProcessor;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.ParserProvider;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.jetbrains.annotations.Nullable;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.report.util.itext.PdfSvgDocument;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicReference;

public class ImageUtils {

    public static PdfSvgDocument checkAndLoadSvg(byte[] data) {

        SVGLoader loader = new SVGLoader();
        try {
            AtomicReference<ViewBox> viewBoxRef = new AtomicReference<>();
            ParserProvider parserProvider = new DefaultParserProvider() {
                public @Nullable DomProcessor createPreProcessor() {
                    return root -> {
                        viewBoxRef.set(root.attributeNode().getViewBox());
                    };
                }
            };

            SVGDocument document = loader.load(new ByteArrayInputStream(data), null, LoaderContext.builder()
                    .parserProvider(parserProvider)
                    .build());
            return new PdfSvgDocument(document, viewBoxRef.get());
        } catch (Exception e) {
            // Invalid SVG or not SVG
            return null;
        }
    }

    public static boolean isTbImage(String uri) {
        return isInternalTbImage(uri) || isPublicTbImage(uri);
    }

    public static boolean isInternalTbImage(String uri) {
        return uri.startsWith(DataConstants.TB_IMAGE_PREFIX + "/api/images");
    }

    public static boolean isPublicTbImage(String uri) {
        return uri.startsWith("/api/images/public");
    }
}

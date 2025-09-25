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

import com.lowagie.text.Image;
import org.xhtmlrenderer.extend.Size;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageUtils {

    public static final String EMPTY_IMAGE_URI = "tb-empty-image";

    static {
        Logger.getLogger("com.github.weisj.jsvg").setLevel(Level.OFF);
    }

    public static Size getOriginalImageSize(byte[] pngImage, int dotsPerPixel) throws IOException {
        Image img = Image.getInstance(pngImage);
        return new Size((int) img.getPlainWidth() * dotsPerPixel, (int) img.getPlainHeight() * dotsPerPixel);
    }

    public static boolean isTbImage(String uri) {
        return isInternalTbImage(uri) || isPublicTbImage(uri);
    }

    public static boolean isInternalTbImage(String uri) {
        return uri.startsWith("/api/images/tenant/") ||
               uri.startsWith("/api/images/system/");
    }

    public static boolean isPublicTbImage(String uri) {
        return uri.startsWith("/api/images/public/");
    }

    public static boolean isEmptyImage(String uri) {
        return EMPTY_IMAGE_URI.equals(uri);
    }
}

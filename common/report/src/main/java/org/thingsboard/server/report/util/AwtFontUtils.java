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

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AwtFontUtils {

    private static final org.thingsboard.server.common.data.report.configuration.style.Font fallbackFont =
            org.thingsboard.server.common.data.report.configuration.style.Font.builder().family("Roboto")
                    .size(16f)
                    .weight(FontWeight.NORMAL)
                    .style(FontStyle.NORMAL).build();

    public static Font toAwtFont(org.thingsboard.server.common.data.report.configuration.style.Font font) {
        return toAwtFont(font, fallbackFont);
    }

    public static Font toAwtFont(org.thingsboard.server.common.data.report.configuration.style.Font font,
                                 org.thingsboard.server.common.data.report.configuration.style.Font defaultFont) {
        String family = font != null ? font.getFamily() : defaultFont.getFamily();
        if (StringUtils.isBlank(family)) {
            family = defaultFont.getFamily();
        }
        FontWeight weight = font != null ? font.getWeight() : defaultFont.getWeight();
        if (weight == null) {
            weight = defaultFont.getWeight();
        }
        String targetWeight = "";
        if (weight != null) {
            switch (weight) {
                case NORMAL -> targetWeight = "";
                case BOLD -> targetWeight = "Bold";
                case WEIGHT_500 -> targetWeight = "Medium";
            }
        }
        FontStyle fontStyle = font != null ? font.getStyle() : defaultFont.getStyle();
        if (fontStyle == null) {
            fontStyle = defaultFont.getStyle();
        }
        String targetStyle = "";
        if (fontStyle != null) {
            switch (fontStyle) {
                case NORMAL -> targetStyle = "";
                case ITALIC -> targetStyle = "Italic";
            }
        }
        String targetFont = family + targetWeight + targetStyle;
        Font awtFont = fontMap.get(targetFont);
        if (awtFont == null) {
            awtFont = fontMap.get("Roboto");
        }
        float size = (font != null && font.getSize() != null) ? font.getSize() : defaultFont.getSize();
        if (size <= 0) {
            size = defaultFont.getSize();
        }
        return awtFont.deriveFont(size);
    }

    private static final Map<String, Font> fontMap = new ConcurrentHashMap<>();
    static {
        registerFonts();
    }

    public static final Font ZERO_FONT = fontMap.get("Roboto").deriveFont(0f);

    private static void registerFonts() {
        fontMap.put("Roboto", createFont("/fonts/roboto/Roboto-Regular.ttf"));
        fontMap.put("RobotoItalic", createFont("/fonts/roboto/Roboto-Italic.ttf"));
        fontMap.put("RobotoMedium", createFont("/fonts/roboto/Roboto-Medium.ttf"));
        fontMap.put("RobotoMediumItalic", createFont("/fonts/roboto/Roboto-MediumItalic.ttf"));
        fontMap.put("RobotoBold", createFont("/fonts/roboto/Roboto-Bold.ttf"));
        fontMap.put("RobotoBoldItalic", createFont("/fonts/roboto/Roboto-BoldItalic.ttf"));

        Font monospace = createFont("/fonts/monospace/DejaVuSansMono.ttf");
        Font monospaceItalic = createFont("/fonts/monospace/DejaVuSansMono-Oblique.ttf");

        fontMap.put("monospace", monospace);
        fontMap.put("monospaceItalic", monospaceItalic);
        fontMap.put("monospaceMedium", monospace);
        fontMap.put("monospaceMediumItalic", monospaceItalic);
        fontMap.put("monospaceBold", createFont("/fonts/monospace/DejaVuSansMono-Bold.ttf"));
        fontMap.put("monospaceBoldItalic", createFont("/fonts/monospace/DejaVuSansMono-BoldOblique.ttf"));

        Font sansSerif = createFont("/fonts/sansserif/LiberationSans-Regular.ttf");
        Font sansSerifItalic = createFont("/fonts/sansserif/LiberationSans-Italic.ttf");

        fontMap.put("sans-serif", sansSerif);
        fontMap.put("sans-serifItalic", sansSerifItalic);
        fontMap.put("sans-serifMedium", sansSerif);
        fontMap.put("sans-serifMediumItalic", sansSerifItalic);
        fontMap.put("sans-serifBold", createFont("/fonts/sansserif/LiberationSans-Bold.ttf"));
        fontMap.put("sans-serifBoldItalic", createFont("/fonts/sansserif/LiberationSans-BoldItalic.ttf"));

        Font serif = createFont("/fonts/serif/LiberationSerif-Regular.ttf");
        Font serifItalic = createFont("/fonts/serif/LiberationSerif-Italic.ttf");

        fontMap.put("serif", serif);
        fontMap.put("serifItalic", serifItalic);
        fontMap.put("serifMedium", serif);
        fontMap.put("serifMediumItalic", serifItalic);
        fontMap.put("serifBold", createFont("/fonts/serif/LiberationSerif-Bold.ttf"));
        fontMap.put("serifBoldItalic", createFont("/fonts/serif/LiberationSerif-BoldItalic.ttf"));
    }

    private static Font createFont(String uri) {
        URL url = AwtFontUtils.class.getResource(uri);
        if (url == null) {
            throw new RuntimeException("Could not find resource " + uri);
        }
        try (java.io.InputStream is = url.openStream()) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);
            return font;
        } catch (Exception e) {
            throw new RuntimeException("Could not read font from uri " + uri, e);
        }
    }
}

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
import org.thingsboard.server.report.renderer.chart.font.TbCompositeFont;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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

    public static Font newFont(String name, int style, float size) {
        String family = name;
        if (StringUtils.isBlank(family)) {
            family = fallbackFont.getFamily();
        }
        String targetWeight = "";
        String targetStyle = "";
        if (style != 0) {
            if ((style & Font.BOLD) != 0) {
                targetWeight = "Bold";
            }
            if ((style & Font.ITALIC) != 0) {
                targetStyle = "Italic";
            }
        }
        String targetFont = family + targetWeight + targetStyle;
        Font awtFont = fontMap.get(targetFont);
        if (awtFont == null) {
            awtFont = fontMap.get("Roboto");
        }
        if (size <= 0) {
            size = fallbackFont.getSize();
        }
        return awtFont.deriveFont(size);
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

        Map<String, Font> localMap = new LinkedHashMap<>();

        localMap.put("RobotoRegular", createFont("/fonts/roboto/Roboto-Regular.ttf"));
        localMap.put("RobotoItalic", createFont("/fonts/roboto/Roboto-Italic.ttf"));
        localMap.put("RobotoMedium", createFont("/fonts/roboto/Roboto-Medium.ttf"));
        localMap.put("RobotoMediumItalic", createFont("/fonts/roboto/Roboto-MediumItalic.ttf"));
        localMap.put("RobotoBold", createFont("/fonts/roboto/Roboto-Bold.ttf"));
        localMap.put("RobotoBoldItalic", createFont("/fonts/roboto/Roboto-BoldItalic.ttf"));

        localMap.put("noto-sansRegular", createFont("/fonts/cjk/NotoSansSC-Regular.ttf"));
        localMap.put("noto-sansItalic", createFont("/fonts/cjk/NotoSans-Italic.ttf"));
        localMap.put("noto-sansMedium", createFont("/fonts/cjk/NotoSansSC-Medium.ttf"));
        localMap.put("noto-sansMediumItalic", createFont("/fonts/cjk/NotoSans-MediumItalic.ttf"));
        localMap.put("noto-sansBold", createFont("/fonts/cjk/NotoSansSC-Bold.ttf"));
        localMap.put("noto-sansBoldItalic", createFont("/fonts/cjk/NotoSans-BoldItalic.ttf"));

        Font notoSansArabic = createFont("/fonts/notoSansArabic/NotoSansArabic-Regular.ttf");
        Font notoSansArabicMedium = createFont("/fonts/notoSansArabic/NotoSansArabic-Medium.ttf");
        Font notoSansArabicBold = createFont("/fonts/notoSansArabic/NotoSansArabic-Bold.ttf");

        localMap.put("noto-sans-arabicRegular", notoSansArabic);
        localMap.put("noto-sans-arabicItalic", notoSansArabic);
        localMap.put("noto-sans-arabicMedium", notoSansArabicMedium);
        localMap.put("noto-sans-arabicMediumItalic", notoSansArabicMedium);
        localMap.put("noto-sans-arabicBold", notoSansArabicBold);
        localMap.put("noto-sans-arabicBoldItalic", notoSansArabicBold);

        Font sansSerif = createFont("/fonts/sansserif/LiberationSans-Regular.ttf");
        Font sansSerifItalic = createFont("/fonts/sansserif/LiberationSans-Italic.ttf");

        localMap.put("sans-serifRegular", sansSerif);
        localMap.put("sans-serifItalic", sansSerifItalic);
        localMap.put("sans-serifMedium", sansSerif);
        localMap.put("sans-serifMediumItalic", sansSerifItalic);
        localMap.put("sans-serifBold", createFont("/fonts/sansserif/LiberationSans-Bold.ttf"));
        localMap.put("sans-serifBoldItalic", createFont("/fonts/sansserif/LiberationSans-BoldItalic.ttf"));

        Font serif = createFont("/fonts/serif/LiberationSerif-Regular.ttf");
        Font serifItalic = createFont("/fonts/serif/LiberationSerif-Italic.ttf");

        localMap.put("serifRegular", serif);
        localMap.put("serifItalic", serifItalic);
        localMap.put("serifMedium", serif);
        localMap.put("serifMediumItalic", serifItalic);
        localMap.put("serifBold", createFont("/fonts/serif/LiberationSerif-Bold.ttf"));
        localMap.put("serifBoldItalic", createFont("/fonts/serif/LiberationSerif-BoldItalic.ttf"));

        Font monospace = createFont("/fonts/monospace/DejaVuSansMono.ttf");
        Font monospaceItalic = createFont("/fonts/monospace/DejaVuSansMono-Oblique.ttf");

        localMap.put("monospaceRegular", monospace);
        localMap.put("monospaceItalic", monospaceItalic);
        localMap.put("monospaceMedium", monospace);
        localMap.put("monospaceMediumItalic", monospaceItalic);
        localMap.put("monospaceBold", createFont("/fonts/monospace/DejaVuSansMono-Bold.ttf"));
        localMap.put("monospaceBoldItalic", createFont("/fonts/monospace/DejaVuSansMono-BoldOblique.ttf"));

        List<String> fontFamilies = Arrays.asList("Roboto", "noto-sans", "noto-sans-arabic", "sans-serif", "serif", "monospace");
        List<String> fontVariants = Arrays.asList("Regular", "Italic", "Medium", "MediumItalic", "Bold", "BoldItalic");
        for (String fontFamily : fontFamilies) {
            for (String fontVariant : fontVariants) {
                registerFont(fontFamily, fontVariant, fontFamilies, localMap);
            }
        }
    }

    private static void registerFont(String fontFamily, String fontVariant, List<String> fontFamilies, Map<String, Font> localMap) {
        String fontName = fontFamily + fontVariant;
        Font font = localMap.get(fontName);
        List<Font> fallbackFonts = new ArrayList<>();
        for (String family : fontFamilies) {
            if (!family.equals(fontFamily)) {
                String fallBackFontName = family + fontVariant;
                fallbackFonts.add(localMap.get(fallBackFontName));
            }
        }
        TbCompositeFont compositeFont = new TbCompositeFont(font, fallbackFonts);
        String registeredName = fontFamily + (!fontVariant.equals("Regular") ? fontVariant : "");
        fontMap.put(registeredName, compositeFont);
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

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

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HSL_PATTERN = Pattern.compile("hsla?\\(\\s*(\\d+)\\s*,\\s*(\\d+)%\\s*,\\s*(\\d+)%\\s*(,\\s*([0-1]\\.\\d+))?\\s*\\)");

    public static String normalizeCssColor(String color) {
        Color c = parseCssColor(color);
        if (c.getAlpha() == 255) {
            return "rgb(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")";
        } else {
            return "rgba(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + Math.round(c.getAlpha() / 255.0f * 100.0) / 100.0 + ")";
        }
    }

    public static Object normalizeCssColorOrDefault(String color, String defaultColor) {
        return color != null ? ColorUtils.normalizeCssColor(color) : defaultColor;
    }

    public static Color parseCssColor(String cssColor) {
        if (cssColor == null || cssColor.trim().isEmpty()) {
            throw new IllegalArgumentException("CSS color cannot be null or empty");
        }

        cssColor = cssColor.trim().toLowerCase();

        Color namedColor = getNamedColor(cssColor);
        if (namedColor != null) {
            return namedColor;
        }

        if (cssColor.startsWith("#")) {
            return parseHexColor(cssColor);
        }

        if (cssColor.startsWith("rgb")) {
            return parseRgbColor(cssColor);
        }

        if (cssColor.startsWith("hsl")) {
            return parseHslColor(cssColor);
        }

        throw new IllegalArgumentException("Unsupported CSS color format: " + cssColor);
    }

    private static Color getNamedColor(String name) {
        return switch (name) {
            case "red" -> Color.RED;
            case "blue" -> Color.BLUE;
            case "green" -> Color.GREEN;
            case "black" -> Color.BLACK;
            case "white" -> Color.WHITE;
            case "yellow" -> Color.YELLOW;
            case "cyan" -> Color.CYAN;
            case "magenta" -> Color.MAGENTA;
            case "gray" -> Color.GRAY;
            default -> null;
        };
    }

    private static Color parseHexColor(String hex) {
        String hexClean = hex.replace("#", "");
        if (hexClean.length() == 3) {
            hexClean = "" + hexClean.charAt(0) + hexClean.charAt(0) +
                    hexClean.charAt(1) + hexClean.charAt(1) +
                    hexClean.charAt(2) + hexClean.charAt(2);
        }
        if (hexClean.length() == 6 || hexClean.length() == 8) {
            try {
                int r = Integer.parseInt(hexClean.substring(0, 2), 16);
                int g = Integer.parseInt(hexClean.substring(2, 4), 16);
                int b = Integer.parseInt(hexClean.substring(4, 6), 16);
                float a = hexClean.length() == 8 ? Integer.parseInt(hexClean.substring(6, 8), 16) / 255.0f : 1.0f;
                return new Color(r / 255.0f, g / 255.0f, b / 255.0f, a);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid HEX color: " + hex, e);
            }
        }
        throw new IllegalArgumentException("Invalid HEX color length: " + hex);
    }

    private static Color parseRgbColor(String rgb) {
        Pattern pattern = Pattern.compile("rgba?\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*(,\\s*([0-1]\\.\\d+))?\\s*\\)");
        Matcher matcher = pattern.matcher(rgb);
        if (matcher.matches()) {
            int r = Integer.parseInt(matcher.group(1));
            int g = Integer.parseInt(matcher.group(2));
            int b = Integer.parseInt(matcher.group(3));
            float a = matcher.group(5) != null ? Float.parseFloat(matcher.group(5)) : 1.0f;
            validateRgbValues(r, g, b, a);
            return new Color(r / 255.0f, g / 255.0f, b / 255.0f, a);
        }
        throw new IllegalArgumentException("Invalid RGB/RGBA color: " + rgb);
    }

    private static Color parseHslColor(String hsl) {
        Matcher matcher = HSL_PATTERN.matcher(hsl);
        if (matcher.matches()) {
            float h = Float.parseFloat(matcher.group(1)) / 360.0f; // Нормалізація до [0, 1]
            float s = Float.parseFloat(matcher.group(2)) / 100.0f;
            float l = Float.parseFloat(matcher.group(3)) / 100.0f;
            float a = matcher.group(5) != null ? Float.parseFloat(matcher.group(5)) : 1.0f;
            validateHslValues(h * 360, s * 100, l * 100, a);
            return hslToRgb(h, s, l, a);
        }
        throw new IllegalArgumentException("Invalid HSL/HSLA color: " + hsl);
    }

    private static void validateRgbValues(int r, int g, int b, float a) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255 || a < 0 || a > 1) {
            throw new IllegalArgumentException("RGB values must be in [0, 255], alpha in [0, 1]");
        }
    }

    private static void validateHslValues(float h, float s, float l, float a) {
        if (h < 0 || h > 360 || s < 0 || s > 100 || l < 0 || l > 100 || a < 0 || a > 1) {
            throw new IllegalArgumentException("HSL values must be: H in [0, 360], S/L in [0, 100], alpha in [0, 1]");
        }
    }

    private static Color hslToRgb(float h, float s, float l, float a) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = l - c / 2;

        float r, g, b;
        int sector = (int) (h * 6);
        switch (sector) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }

        r = (r + m) * 255;
        g = (g + m) * 255;
        b = (b + m) * 255;

        return new Color(r / 255.0f, g / 255.0f, b / 255.0f, a);
    }

}

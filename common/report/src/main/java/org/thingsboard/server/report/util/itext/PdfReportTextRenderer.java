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

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;
import com.lowagie.text.pdf.BaseFont;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.pdf.FontDescription;
import org.xhtmlrenderer.pdf.ITextFSFont;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextTextRenderer;
import org.xhtmlrenderer.render.JustificationInfo;

import java.util.ArrayList;
import java.util.List;

public class PdfReportTextRenderer extends ITextTextRenderer {

    private final List<BaseFont> fallbacks = new ArrayList<>();

    public PdfReportTextRenderer(List<BaseFont> fallbackFonts) {
        if (fallbackFonts != null) fallbacks.addAll(fallbackFonts);
    }

    @Override
    public void drawString(OutputDevice outputDevice, String s, float x, float y) {
        drawString(outputDevice, s, x, y, null);
    }

    @Override
    public void drawString(OutputDevice outputDevice, String s, float x, float y, JustificationInfo info) {
        ITextOutputDevice iod = (ITextOutputDevice) outputDevice;

        if (!(iod.getSharedContext().getFont(iod.getFontSpecification()) instanceof ITextFSFont curFont)) {
            iod.drawString(s, x, y, info);
            return;
        }

        String vis = shapeAndReorderLTRParagraph(s);

        BaseFont primary = curFont.getFontDescription().getFont();
        float size = curFont.getSize2D();

        List<BaseFont> candidates = new ArrayList<>(1 + fallbacks.size());
        candidates.add(primary);
        candidates.addAll(fallbacks);

        List<Run> runs = shapeRunsByBaseFont(vis, candidates, size);

        float cursor = x;
        for (Run r : runs) {
            ITextFSFont runFsFont = new ITextFSFont(new FontDescription(r.baseFont, false), size);
            iod.setFont(runFsFont);

            iod.drawString(r.text, cursor, y, info);
            cursor += r.widthPt;
        }

        iod.setFont(curFont);
    }

    private static String shapeAndReorderLTRParagraph(String logical) {
        try {
            int shapeFlags =
                    ArabicShaping.LETTERS_SHAPE |
                            ArabicShaping.TASHKEEL_REPLACE_BY_TATWEEL |
                            ArabicShaping.LENGTH_FIXED_SPACES_NEAR |
                            ArabicShaping.TEXT_DIRECTION_LOGICAL;

            String shaped = new ArabicShaping(shapeFlags).shape(logical);
            Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_LEFT_TO_RIGHT);
            int opts = Bidi.DO_MIRRORING | Bidi.INSERT_LRM_FOR_NUMERIC;
            return bidi.writeReordered(opts);
        } catch (Exception e) {
            return logical;
        }
    }

    private static List<Run> shapeRunsByBaseFont(String s, List<BaseFont> candidates, float size) {
        List<Run> out = new ArrayList<>();
        if (s.isEmpty()) return out;

        int i = 0, len = s.length();
        int cp = s.codePointAt(0);
        BaseFont cur = pick(candidates, cp);
        int start = 0;
        for (i = Character.charCount(cp); i < len; ) {
            cp = s.codePointAt(i);
            BaseFont bf = pick(candidates, cp);
            if (bf != cur) {
                String slice = s.substring(start, i);
                out.add(run(slice, cur, size));
                cur = bf;
                start = i;
            }
            i += Character.charCount(cp);
        }
        out.add(run(s.substring(start), cur, size));
        return out;
    }

    private static BaseFont pick(List<BaseFont> candidates, int codePoint) {
        for (BaseFont bf : candidates) {
            if (bf.charExists(codePoint)) return bf;
        }
        return candidates.get(0);
    }

    private static Run run(String text, BaseFont bf, float size) {
        float w = bf.getWidthPointKerned(text, size);
        return new Run(text, bf, w);
    }

    private record Run(String text, BaseFont baseFont, float widthPt) {}
}

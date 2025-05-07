package org.thingsboard.server.report.util;

import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.HeadingComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.style.Font;
import org.thingsboard.server.common.data.report.configuration.style.FontStyle;
import org.thingsboard.server.common.data.report.configuration.style.FontWeight;
import org.thingsboard.server.common.data.report.configuration.style.TextAlignment;
import org.thingsboard.server.common.data.report.configuration.style.VerticalAlignment;

import java.awt.*;
import java.util.Map;
import java.util.Optional;

@Component
public class HeadingRenderer implements ReportComponentRenderer {

    private static final Map<String, String> FONT_MAP = Map.of(
            "Roboto", "Roboto",
            "monospace", "Monospaced",
            "sans-serif", "SansSerif",
            "serif", "Serif"
    );

    @Override
    public void render(ReportLayoutContext layoutCtx, ReportComponent component) {
        HeadingComponent headingComponent = (HeadingComponent) component;
        JRDesignTextField textField = new JRDesignTextField();
        textField.setX(0);
        textField.setY(0);
        textField.setHeight(1);
        textField.setWidth(layoutCtx.getUsablePageWidth());
        textField.setPositionType(PositionTypeEnum.FLOAT);
        textField.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);

        // Set text color if provided
        String color = headingComponent.getColor();
        if (color != null) {
            textField.setForecolor(Color.decode(color));
        }

        // Set font style if provided
        Font font = headingComponent.getFont();
        if (font != null) {
            textField.setBold(font.getWeight() == FontWeight.bold);
            textField.setItalic(font.getStyle() == FontStyle.italic);
            String fontFamily = font.getFamily();
            if (fontFamily != null) {
                String fontName = Optional.ofNullable(FONT_MAP.get(fontFamily)).orElse("Roboto");
                textField.setFontName(fontName);
            }
            Float fontSize = font.getSize();
            if (fontSize != null) {
                textField.setFontSize(fontSize);
            }
        }

        // Set horizontal and vertical alignment
        TextAlignment textAlignment = headingComponent.getTextAlignment();
        if (textAlignment != null) {
            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.valueOf(textAlignment.getValue()));
        }
        VerticalAlignment verticalAlignment = headingComponent.getVerticalAlignment();
        if (verticalAlignment != null) {
            textField.setVerticalTextAlign(VerticalTextAlignEnum.valueOf(verticalAlignment.getValue()));
        }

        // Set the text content as a string literal
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("\"" + headingComponent.getValue() + "\"");
        textField.setExpression(expression);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(1);
        detailBand.setSplitType(SplitTypeEnum.STRETCH);
        detailBand.addElement(textField);

        JRDesignSection detailSection = (JRDesignSection) layoutCtx.getJasperDesign().getDetailSection();
        detailSection.addBand(detailBand);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.HEADING;
    }

}

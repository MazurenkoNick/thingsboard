package org.thingsboard.server.report.util;

import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.RichTextComponent;

@Component
public class RichTextRenderer implements ReportComponentRenderer {

    @Override
    public void render(ReportLayoutContext layoutCtx, ReportComponent component) {
        RichTextComponent richTextComponent = (RichTextComponent) component;
        JRDesignTextField htmlField = new JRDesignTextField();
        htmlField.setX(0);
        htmlField.setY(0);
        htmlField.setWidth(500);
        htmlField.setHeight(1);
        htmlField.setPositionType(PositionTypeEnum.FLOAT);
        htmlField.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
        htmlField.setMarkup("html");

        JRDesignExpression expression = new JRDesignExpression();
        expression.setText(escapeHtmlForJasperExpression(richTextComponent.getValue()));
        htmlField.setExpression(expression);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(1);
        detailBand.setSplitType(SplitTypeEnum.STRETCH);
        detailBand.addElement(htmlField);

        JRDesignSection detailSection = (JRDesignSection) layoutCtx.getJasperDesign().getDetailSection();
        detailSection.addBand(detailBand);
    }

    private String escapeHtmlForJasperExpression(String rawHtml) {
        String escaped = rawHtml
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.RICH_TEXT;
    }

}

package org.thingsboard.server.report.util;

import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

@Component
public class DashboardRenderer implements ReportComponentRenderer {

    @Override
    public void render(ReportLayoutContext layoutCtx, ReportComponent component) {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(500); // Make sure it’s tall enough for the image

        JRDesignImage image = new JRDesignImage(layoutCtx.getJasperDesign());
        image.setX(0);
        image.setY(0);
        image.setWidth(500);
        image.setHeight(500);
        image.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);

        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("new java.io.ByteArrayInputStream($F{data})");
        image.setExpression(expression);
        detailBand.addElement(image);

        // Set the detail band into the design
        JRDesignSection detailSection = (JRDesignSection) layoutCtx.getJasperDesign().getDetailSection();
        detailSection.addBand(detailBand);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.DASHBOARD;
    }

}

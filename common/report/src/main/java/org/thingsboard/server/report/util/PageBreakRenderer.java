package org.thingsboard.server.report.util;

import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignBreak;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.type.BreakTypeEnum;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;


@Component
public class PageBreakRenderer implements ReportComponentRenderer {

    @Override
    public void render(ReportLayoutContext layoutCtx, ReportComponent component) {
        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(1);

        JRDesignBreak pageBreak = new JRDesignBreak();
        pageBreak.setType(BreakTypeEnum.PAGE);
        detailBand.addElement(pageBreak);
        ((JRDesignSection) layoutCtx.getJasperDesign().getDetailSection()).addBand(detailBand);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.PAGE_BREAK;
    }

}

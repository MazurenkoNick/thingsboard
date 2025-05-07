package org.thingsboard.server.report.util;

import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.ModeEnum;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.report.util.JasperReportUtils.createTextField;

@Component
public class AlarmTableRenderer implements ReportComponentRenderer {

    @Override
    public void render(ReportLayoutContext layoutCtx, ReportComponent richTextComponent) {
        AlarmTableComponent component = (AlarmTableComponent) richTextComponent;
        List<DataKey> dataKeys = component.getAlarmSource().getDataKeys();
        List<String> entityKeys = dataKeys.stream().map(DataKey::getName).collect(Collectors.toList());
        List<String> columnsHeaders = dataKeys.stream().map(DataKey::getLabel).collect(Collectors.toList());

        addColumnHeader(layoutCtx, columnsHeaders);
        addTableDetailBand(layoutCtx, entityKeys);
    }

    public void addColumnHeader(ReportLayoutContext builder, List<String> titles) {
        JasperDesign jasperDesign = builder.getJasperDesign();
        JRDesignBand columnHeader = new JRDesignBand();
        columnHeader.setHeight(20);
        int x = 0;
        int columnWidth = Math.min(jasperDesign.getColumnWidth(), builder.getUsablePageWidth() /titles.size());
        for (String title : titles) {
            columnHeader.addElement(createHeaderText(title, x, columnWidth));
            x += columnWidth;
        }
        jasperDesign.setColumnHeader(columnHeader);
    }

    private JRDesignStaticText createHeaderText(String text, int x, int width) {
        JRDesignStaticText header = new JRDesignStaticText();
        header.setX(x);
        header.setY(0);
        header.setWidth(width);
        header.setHeight(20);
        //header.setHorizontalAlignment(HorizontalAlignEnum.CENTER);
        header.setText(text);
        header.setForecolor(Color.WHITE);
        header.setBackcolor(Color.GRAY);
        header.setMode(ModeEnum.OPAQUE);
        return header;
    }

    public void addTableDetailBand(ReportLayoutContext builder, List<String> entityKeys)  {

        JasperDesign jasperDesign = builder.getJasperDesign();

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(20);
        int x = 0;
        int columnWidth = Math.min(jasperDesign.getColumnWidth(), builder.getUsablePageWidth() /entityKeys.size());
        for (String entityKey : entityKeys) {
            detailBand.addElement(createTextField("$F{" + entityKey + "}", x, 0));
            x += columnWidth;
        }
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(detailBand);
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ALARM_TABLE;
    }

}

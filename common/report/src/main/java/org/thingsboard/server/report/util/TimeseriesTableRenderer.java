package org.thingsboard.server.report.util;

import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.ModeEnum;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.report.configuration.DataKey;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.report.util.JasperReportUtils.createTextField;
import static org.thingsboard.server.report.util.ReportLayoutContext.getSingleDataSource;

@Component
public class TimeseriesTableRenderer implements ReportComponentRenderer {

    @Override
    public void render(ReportLayoutContext layoutCtx, ReportComponent component) {
        TimeseriesTableComponent tsTableComponent = (TimeseriesTableComponent) component;
        List<DataKey> dataKeys = getSingleDataSource(tsTableComponent).getDataKeys();
        List<String> entityKeys = dataKeys.stream().map(DataKey::getName).collect(Collectors.toList());
        List<String> columnsHeaders = dataKeys.stream().map(DataKey::getLabel).collect(Collectors.toList());

        entityKeys.add(0, "ts");
        columnsHeaders.add(0, "Timestamp");

        addColumnHeader(layoutCtx, columnsHeaders);
        addTableDetailBand(layoutCtx, entityKeys);
    }

    public void addColumnHeader(ReportLayoutContext layoutCtx, List<String> titles) {
        JasperDesign jasperDesign = layoutCtx.getJasperDesign();
        JRDesignBand columnHeader = new JRDesignBand();
        columnHeader.setHeight(20);
        int x = 0;
        int columnWidth = Math.min(jasperDesign.getColumnWidth(), layoutCtx.getUsablePageWidth() /titles.size());
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
        return ReportComponentType.TIME_SERIES_TABLE;
    }

}

package org.thingsboard.server.report.util;

import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

public interface ReportComponentRenderer {

    void render(ReportLayoutContext layoutCtx, ReportComponent component);

    ReportComponentType getType();

}

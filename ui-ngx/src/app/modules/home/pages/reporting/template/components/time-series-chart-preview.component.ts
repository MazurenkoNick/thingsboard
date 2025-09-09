///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import {
  AfterViewInit,
  Component,
  ComponentRef,
  inject,
  OnDestroy,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  reportTimeSeriesChartDefaultSettings,
  ReportTimeSeriesChartSettings,
  TimeseriesChartReportComponentConfig
} from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { ReportWidgetContextService } from '@home/pages/reporting/template/components/report-widget-context.service';
import { DatasourceType, widgetType } from '@shared/models/widget.models';
import { TimeSeriesChartWidgetComponent } from '@home/components/widget/lib/chart/time-series-chart-widget.component';
import { IWidgetSubscription, WidgetSubscriptionCallbacks } from '@core/api/widget-api.models';
import { debounce, deepClone, mergeDeep } from '@core/utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { BackgroundType, ComponentStyle, textStyle, ValueSourceType } from '@shared/models/widget-settings.models';
import { TimeSeriesChartWidgetSettings } from '@home/components/widget/lib/chart/time-series-chart-widget.models';

@Component({
  selector: 'tb-time-series-chart-preview',
  templateUrl: './time-series-chart-preview.component.html',
  styleUrls: ['./time-series-chart-preview.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartPreviewComponent extends AbstractReportComponentPreview<TimeseriesChartReportComponentConfig>
  implements AfterViewInit, OnDestroy, WidgetSubscriptionCallbacks {

  @ViewChild('widgetContent', {read: ViewContainerRef, static: false}) widgetContainer: ViewContainerRef;

  private reportWidgetContextService = inject(ReportWidgetContextService);

  imageWidth: string = '100%';
  imageHeightPx: number = 400;

  imageAlign: string = 'center';

  showTitle: boolean;
  title: string;
  titleStyle: ComponentStyle;

  hasData = false;
  noDataMessage: string;

  private viewInited = false;

  private widgetContext: WidgetContext;
  private widgetComponentRef: ComponentRef<TimeSeriesChartWidgetComponent>;
  private widgetComponent: TimeSeriesChartWidgetComponent;

  private updateWidgetPreview = debounce(() => {
    this.updateTimeSeriesWidgetPreview();
  }, 150);

  onComponentUpdated() {
    this.imageWidth = '100%';
    if (this.reportComponent.widthType === 'original') {
      this.imageWidth = 'auto';
    } else if (this.reportComponent.widthType === 'custom') {
      const customWidth = this.reportComponent.customWidth || 100;
      this.imageWidth = customWidth + 'px';
    }
    this.imageAlign = this.reportComponent.alignment || 'center';
    this.imageHeightPx = this.reportComponent.height || 400;

    this.showTitle = this.reportComponent.timeSeriesChartSettings.showTitle;
    this.title = this.reportComponent.timeSeriesChartSettings.title;
    this.titleStyle = textStyle(this.reportComponent.timeSeriesChartSettings.titleFont);
    this.titleStyle.color = this.reportComponent.timeSeriesChartSettings.titleColor;
    this.titleStyle.textAlign = this.reportComponent.timeSeriesChartSettings.titleAlignment;

    const datasources = this.reportComponent.dataSources;
    if (datasources?.length) {
      const datasource = datasources[0];
      if (datasource.type === DatasourceType.device && datasource.deviceId || datasource.type === DatasourceType.entity && datasource.entityAliasId) {
        if (datasource.dataKeys?.length) {
          this.hasData = true;
        } else {
          this.hasData = false;
          this.noDataMessage = 'report-template.component.time-series-chart.no-series-configured';
        }
      } else {
        this.hasData = false;
        this.noDataMessage = 'report-template.component.time-series-chart.no-datasource-configured';
      }
    } else {
      this.hasData = false;
      this.noDataMessage = 'report-template.component.time-series-chart.no-datasource-configured';
    }

    if (this.viewInited) {
      this.updateWidgetPreview();
    }
  }

  ngAfterViewInit() {
    this.viewInited = true;
    this.updateTimeSeriesWidgetPreview();
  }

  ngOnDestroy() {
    this.destroyWidget();
  }

  onDataUpdated(_subscription: IWidgetSubscription, _detectChanges: boolean): void {
    if (this.widgetComponent) {
      this.widgetComponent.onDataUpdated();
    }
  }

  onLatestDataUpdated(_subscription: IWidgetSubscription, _detectChanges: boolean): void {
    if (this.widgetComponent) {
      this.widgetComponent.onLatestDataUpdated();
    }
  }

  private destroyWidget() {
    if (this.widgetContext) {
      this.reportWidgetContextService.destroyWidgetContext(this.widgetContext);
      this.widgetContext = null;
    }
    if (this.widgetComponentRef) {
      this.widgetComponentRef.destroy();
      this.widgetComponentRef = null;
      this.widgetComponent = null;
    }
  }

  private updateTimeSeriesWidgetPreview() {
    this.destroyWidget();
    if (this.widgetContainer) {
      this.widgetContainer.clear();
    }
    if (!this.hasData) {
      return;
    }
    const datasources = deepClone(this.reportComponent.dataSources || []);
    const settings: ReportTimeSeriesChartSettings =
      mergeDeep<ReportTimeSeriesChartSettings>({} as ReportTimeSeriesChartSettings, reportTimeSeriesChartDefaultSettings, this.reportComponent.timeSeriesChartSettings, {
        barWidthSettings: reportTimeSeriesChartDefaultSettings.barWidthSettings,
        dataZoom: false,
        animation: {
          animation: false
        }
      } as ReportTimeSeriesChartSettings);
    (settings as TimeSeriesChartWidgetSettings).padding = '0';
    (settings as TimeSeriesChartWidgetSettings).background = {
      type: BackgroundType.color,
      color: 'rgba(0,0,0,0)',
      overlay: {
        enabled: false,
        color: 'rgba(255,255,255,0.72)',
        blur: 3
      }
    };
    if (settings.thresholds?.length) {
      for (const threshold of settings.thresholds) {
        if (threshold.type === ValueSourceType.entity) {
          threshold.type = ValueSourceType.latestKey;
          threshold.latestKeyType = threshold.entityKeyType;
          threshold.latestKey = threshold.entityKey;
          if (datasources.length) {
            const datasource = datasources[0];
            if (!datasource.latestDataKeys) {
              datasource.latestDataKeys = [];
            }
            let dataKey = datasource.latestDataKeys.find(d => d.type === threshold.latestKeyType && d.name === threshold.latestKey);
            if (!dataKey) {
              dataKey = {
                type: threshold.latestKeyType,
                name: threshold.latestKey,
                label: threshold.latestKey
              };
              datasource.latestDataKeys.push(dataKey);
            }
          }
        }
      }
    }
    this.reportWidgetContextService.createWidgetContext(widgetType.timeseries,
      settings, this.reportComponent.timewindow, datasources, this)
    .subscribe((ctx) => {
      this.widgetContext = ctx;
      this.widgetComponentRef = this.widgetContainer.createComponent(TimeSeriesChartWidgetComponent);
      this.widgetContext.$container = $(this.widgetComponentRef.location.nativeElement);
      this.widgetContext.$containerParent = ctx.$container.parent();
      this.widgetComponent = this.widgetComponentRef.instance;
      this.widgetComponent.reportMode = true;
      this.widgetComponent.ctx = this.widgetContext;
      this.widgetContext.defaultSubscription.subscribe();
    });
  }

}

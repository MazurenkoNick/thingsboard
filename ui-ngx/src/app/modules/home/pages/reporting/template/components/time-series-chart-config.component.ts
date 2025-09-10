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

import { Component, Input, ViewEncapsulation } from '@angular/core';
import { FormGroup, UntypedFormGroup, Validators } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  imageAlignments,
  imageAlignmentTranslations,
  imageWidthTypeTranslations,
  reportTimeSeriesChartDefaultSettings,
  ReportTimeSeriesChartSettings,
  TimeseriesChartReportComponentConfig,
  toReportTimeSeriesChartKeySettings,
  toTimeSeriesChartKeySettings
} from '@shared/models/report-component.models';
import {
  DataKey,
  Datasource,
  Widget,
  WidgetConfig,
  WidgetConfigMode,
  widgetType,
  WidgetTypeParameters
} from '@shared/models/widget.models';
import {
  TimeSeriesChartKeySettings, TimeSeriesChartType,
  TimeSeriesChartYAxes,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { deepClone, mergeDeep } from '@core/utils';
import { merge } from 'rxjs';
import { TbTimeSeriesChart } from '@home/components/widget/lib/chart/time-series-chart';
import { WidgetInfo, WidgetWithInfo } from '@home/models/widget-component.models';

@Component({
  selector: 'tb-time-series-chart-config',
  templateUrl: './time-series-chart-config.component.html',
  styleUrls: ['./report-component-config.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartConfigComponent extends AbstractReportComponentConfig<TimeseriesChartReportComponentConfig> {

  @Input()
  chartType: TimeSeriesChartType = TimeSeriesChartType.default;

  TimeSeriesChartType = TimeSeriesChartType;

  public get yAxisIds(): TimeSeriesChartYAxisId[] {
    const yAxes: TimeSeriesChartYAxes = this.reportConfigForm.get('yAxes').value;
    return yAxes ? Object.keys(yAxes) : [];
  }

  public get widget(): WidgetWithInfo {
    return {
      type: widgetType.timeseries,
      config: {
        settings: this.reportComponentConfig.timeSeriesChartSettings
      } as WidgetConfig,
      widgetInfo: {
        typeParameters: {
          chartType: this.chartType
        } as WidgetTypeParameters
      } as WidgetInfo
    } as WidgetWithInfo;
  }

  TbTimeSeriesChart = TbTimeSeriesChart;

  imageWidthTypes = ['fitWidth', 'custom'];
  imageWidthTypeTranslations = imageWidthTypeTranslations;

  imageAlignments = imageAlignments;
  imageAlignmentTranslations = imageAlignmentTranslations;

  basicMode = WidgetConfigMode.basic;

  settingsTab: 'data' | 'layout' = 'data';

  seriesMode = 'series';

  protected buildForm(reportComponentConfig: TimeseriesChartReportComponentConfig): FormGroup {
    const timeSeriesChartSettings: ReportTimeSeriesChartSettings =
      mergeDeep<ReportTimeSeriesChartSettings>({} as ReportTimeSeriesChartSettings, reportTimeSeriesChartDefaultSettings, reportComponentConfig.timeSeriesChartSettings);
    const form: UntypedFormGroup = this.fb.group({
      timewindow: [reportComponentConfig.timewindow, []],
      dataSources: [reportComponentConfig.dataSources, []],
      widthType: [reportComponentConfig.widthType || 'fitWidth', []],
      customWidth: [reportComponentConfig.customWidth || 100, [Validators.min(1)]],
      height: [reportComponentConfig.height || 400, [Validators.min(1)]],
      alignment: [reportComponentConfig.alignment || 'center', []],

      yAxes: [timeSeriesChartSettings.yAxes, []],
      series: [this.getSeries(reportComponentConfig.dataSources), []],

      comparisonEnabled: [timeSeriesChartSettings.comparisonEnabled, []],
      timeForComparison: [timeSeriesChartSettings.timeForComparison, []],
      comparisonCustomIntervalValue: [timeSeriesChartSettings.comparisonCustomIntervalValue, [Validators.min(0)]],
      comparisonXAxis: [timeSeriesChartSettings.comparisonXAxis, []],

      thresholds: [timeSeriesChartSettings.thresholds, []],

      showTitle: [timeSeriesChartSettings.showTitle, []],
      title: [timeSeriesChartSettings.title, []],
      titleFont: [timeSeriesChartSettings.titleFont, []],
      titleColor: [timeSeriesChartSettings.titleColor, []],
      titleAlignment: [timeSeriesChartSettings.titleAlignment, []],

      stack: [timeSeriesChartSettings.stack, []],

      grid: [timeSeriesChartSettings.grid, []],

      xAxis: [timeSeriesChartSettings.xAxis, []],

      noAggregationBarWidthSettings: [timeSeriesChartSettings.noAggregationBarWidthSettings, []],

      showLegend: [timeSeriesChartSettings.showLegend, []],
      legendLabelFont: [timeSeriesChartSettings.legendLabelFont, []],
      legendLabelColor: [timeSeriesChartSettings.legendLabelColor, []],
      legendValueFont: [timeSeriesChartSettings.legendValueFont, []],
      legendValueColor: [timeSeriesChartSettings.legendValueColor, []],
      legendColumnTitleFont: [timeSeriesChartSettings.legendColumnTitleFont, []],
      legendColumnTitleColor: [timeSeriesChartSettings.legendColumnTitleColor, []],
      legendConfig: [timeSeriesChartSettings.legendConfig, []]

    });

    if (this.chartType === TimeSeriesChartType.state) {
      form.addControl('states', this.fb.control(timeSeriesChartSettings.states, []));
    }

    form.get('widthType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateCustomWidth();
    });

    form.get('comparisonEnabled').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateSeriesState());

    merge(form.get('comparisonEnabled').valueChanges,
          form.get('showTitle').valueChanges,
          form.get('showLegend').valueChanges).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators(form);
    });

    this.updateValidators(form);

    return form;
  }

  protected prepareOutputConfig(config: any): any {
    this.setSeries(config.series, config.dataSources);
    delete config.series;
    if (!config.timeSeriesChartSettings) {
      config.timeSeriesChartSettings = {};
    }
    const timeSeriesChartSettings: ReportTimeSeriesChartSettings = config.timeSeriesChartSettings

    timeSeriesChartSettings.yAxes = config.yAxes;
    delete config.yAxes;

    timeSeriesChartSettings.comparisonEnabled = config.comparisonEnabled;
    delete config.comparisonEnabled;
    timeSeriesChartSettings.timeForComparison = config.timeForComparison;
    delete config.timeForComparison;
    timeSeriesChartSettings.comparisonCustomIntervalValue = config.comparisonCustomIntervalValue;
    delete config.comparisonCustomIntervalValue;
    timeSeriesChartSettings.comparisonXAxis = config.comparisonXAxis;
    delete config.comparisonXAxis;

    timeSeriesChartSettings.thresholds = config.thresholds;
    delete config.thresholds;

    timeSeriesChartSettings.showTitle = config.showTitle;
    delete config.showTitle;
    timeSeriesChartSettings.title = config.title;
    delete config.title;
    timeSeriesChartSettings.titleFont = config.titleFont;
    delete config.titleFont;
    timeSeriesChartSettings.titleColor = config.titleColor;
    delete config.titleColor;
    timeSeriesChartSettings.titleAlignment = config.titleAlignment;
    delete config.titleAlignment;

    timeSeriesChartSettings.stack = config.stack;
    delete config.stack;

    timeSeriesChartSettings.grid = config.grid;
    delete config.grid;

    timeSeriesChartSettings.xAxis = config.xAxis;
    delete config.xAxis;

    timeSeriesChartSettings.noAggregationBarWidthSettings = config.noAggregationBarWidthSettings;
    delete config.noAggregationBarWidthSettings;

    timeSeriesChartSettings.showLegend = config.showLegend;
    delete config.showLegend;


    timeSeriesChartSettings.legendColumnTitleFont = config.legendColumnTitleFont;
    delete config.legendColumnTitleFont;
    timeSeriesChartSettings.legendColumnTitleColor = config.legendColumnTitleColor;
    delete config.legendColumnTitleColor;

    timeSeriesChartSettings.legendLabelFont = config.legendLabelFont;
    delete config.legendLabelFont;
    timeSeriesChartSettings.legendLabelColor = config.legendLabelColor;
    delete config.legendLabelColor;

    timeSeriesChartSettings.legendValueFont = config.legendValueFont;
    delete config.legendValueFont;
    timeSeriesChartSettings.legendValueColor = config.legendValueColor;
    delete config.legendValueColor;

    timeSeriesChartSettings.legendConfig = config.legendConfig;
    delete config.legendConfig;

    if (this.chartType === TimeSeriesChartType.state) {
      timeSeriesChartSettings.states = config.states;
      delete config.states;
    }

    return config;
  }

  seriesModeChange(seriesMode: string) {
    this.seriesMode = seriesMode;
    this.updateSeriesState();
  }

  public yAxisRemoved(yAxisId: TimeSeriesChartYAxisId): void {
    if (this.reportComponentConfig.dataSources && this.reportComponentConfig.dataSources.length > 1) {
      for (let i = 1; i < this.reportComponentConfig.dataSources.length; i++) {
        const datasource = this.reportComponentConfig.dataSources[i];
        this.removeYaxisId(datasource.dataKeys, yAxisId);
      }
    }
  }

  private removeYaxisId(series: DataKey[], yAxisId: TimeSeriesChartYAxisId): boolean {
    let changed = false;
    if (series) {
      series.forEach(key => {
        const keySettings = ((key.settings || {}) as TimeSeriesChartKeySettings);
        if (keySettings.yAxisId === yAxisId) {
          keySettings.yAxisId = 'default';
          changed = true;
        }
      });
    }
    return changed;
  }

  private updateSeriesState() {
    if (this.seriesMode === 'series') {
      this.reportConfigForm.get('series').enable({emitEvent: false});
    } else {
      const comparisonEnabled = this.reportConfigForm.get('comparisonEnabled').value;
      if (comparisonEnabled) {
        this.reportConfigForm.get('series').enable({emitEvent: false});
      } else {
        this.reportConfigForm.get('series').disable({emitEvent: false});
      }
    }
  }

  private updateCustomWidth() {
    if (!this.reportConfigForm.get('customWidth').touched) {
      const size = 200;
      this.reportConfigForm.get('customWidth').patchValue(size);
    }
  }

  private getSeries(datasources?: Datasource[]): DataKey[] {
    let dataKeys: DataKey[] = [];
    if (datasources && datasources.length) {
      dataKeys = datasources[0].dataKeys || [];
    }
    dataKeys = dataKeys.map(key => {
      key = deepClone(key);
      key.settings = toTimeSeriesChartKeySettings(key.settings);
      return key;
    });
    return dataKeys;
  }

  private setSeries(series: DataKey[], datasources?: Datasource[]) {
    if (datasources && datasources.length) {
      series = series.map(key => {
        key = deepClone(key);
        key.settings = toReportTimeSeriesChartKeySettings(key.settings);
        return key;
      });
      datasources[0].dataKeys = series;
    }
  }

  private updateValidators(form: FormGroup) {
    const comparisonEnabled: boolean = form.get('comparisonEnabled').value;
    const showTitle: boolean = form.get('showTitle').value;
    const showLegend: boolean = form.get('showLegend').value;

    if (comparisonEnabled) {
      form.get('timeForComparison').enable({emitEvent: false});
      form.get('comparisonCustomIntervalValue').enable({emitEvent: false});
      form.get('comparisonXAxis').enable({emitEvent: false});
    } else {
      form.get('timeForComparison').disable({emitEvent: false});
      form.get('comparisonCustomIntervalValue').disable({emitEvent: false});
      form.get('comparisonXAxis').disable({emitEvent: false});
    }

    if (showTitle) {
      form.get('title').enable({emitEvent: false});
      form.get('titleFont').enable({emitEvent: false});
      form.get('titleColor').enable({emitEvent: false});
      form.get('titleAlignment').enable({emitEvent: false});
    } else {
      form.get('title').disable({emitEvent: false});
      form.get('titleFont').disable({emitEvent: false});
      form.get('titleColor').disable({emitEvent: false});
      form.get('titleAlignment').disable({emitEvent: false});
    }

    if (showLegend) {
      form.get('legendColumnTitleFont').enable({emitEvent: false});
      form.get('legendColumnTitleColor').enable({emitEvent: false});
      form.get('legendLabelFont').enable({emitEvent: false});
      form.get('legendLabelColor').enable({emitEvent: false});
      form.get('legendValueFont').enable({emitEvent: false});
      form.get('legendValueColor').enable({emitEvent: false});
      form.get('legendConfig').enable({emitEvent: false});
    } else {
      form.get('legendColumnTitleFont').disable({emitEvent: false});
      form.get('legendColumnTitleColor').disable({emitEvent: false});
      form.get('legendLabelFont').disable({emitEvent: false});
      form.get('legendLabelColor').disable({emitEvent: false});
      form.get('legendValueFont').disable({emitEvent: false});
      form.get('legendValueColor').disable({emitEvent: false});
      form.get('legendConfig').disable({emitEvent: false});
    }

  }
}

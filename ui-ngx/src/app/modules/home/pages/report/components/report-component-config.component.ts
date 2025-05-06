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
  Component,
  ComponentRef,
  DestroyRef,
  Directive,
  EventEmitter, HostBinding,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ReportComponentConfig } from '@shared/models/report-component.models';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { FormGroup } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { genNextLabel, isObject, mergeDeep } from '@core/utils';
import { ReportComponentContext, reportComponentTypeMap } from '@home/pages/report/components/report-component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { Observable, of } from 'rxjs';
import { DataKey, Datasource, widgetType } from '@shared/models/widget.models';
import { catchError, mergeMap } from 'rxjs/operators';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { FormProperty } from '@shared/models/dynamic-form.models';
import { DataKeySettingsFunction } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { alarmFields } from '@shared/models/alarm.models';
import { entityFields } from '@shared/models/entity.models';
import { singleEntityFilterFromDeviceId } from '@shared/models/query/query.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-report-component-config',
  templateUrl: './report-component-config.component.html',
  styleUrls: ['./report-component-config.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportComponentConfigComponent implements OnInit, OnChanges {

  @HostBinding('style.width') width = '100%';

  @Input()
  context: ReportComponentContext;

  @Input()
  reportComponent: ReportComponentConfig;

  @Output()
  reportComponentUpdated = new EventEmitter();

  @ViewChild('reportConfigContainer', {static: true}) reportConfigContainer: TbAnchorComponent;

  reportConfigForm: FormGroup;

  private reportConfigComponentRef: ComponentRef<AbstractReportComponentConfig>;
  private reportConfigComponent: AbstractReportComponentConfig;

  constructor() {}

  ngOnInit() {
    this.init();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'reportComponent') {
          this.init();
        }
      }
    }
  }

  private init() {
    this.reportConfigForm = null;
    if (this.reportConfigComponentRef) {
      this.reportConfigComponentRef.destroy();
      this.reportConfigComponentRef = null;
    }
    this.reportConfigContainer.viewContainerRef.clear();
    if (this.reportComponent) {
      const typeData = reportComponentTypeMap.get(this.reportComponent.type);
      if (typeData) {
        this.reportConfigComponentRef = this.reportConfigContainer.viewContainerRef.createComponent(typeData.configComponent);
        this.reportConfigComponent = this.reportConfigComponentRef.instance;
        this.reportConfigComponent.context = this.context;
        this.reportConfigComponent.reportConfigUpdated.subscribe((updated) => {
          Object.assign(this.reportComponent, updated);
          this.reportComponentUpdated.emit();
        });
        this.reportConfigForm = this.reportConfigComponent.setupConfig(this.reportComponent);
      }
    }
  }
}

@Directive()
export abstract class AbstractReportComponentConfig<C extends ReportComponentConfig = ReportComponentConfig> {

  @Input()
  context: ReportComponentContext;

  @Output()
  reportConfigUpdated = new EventEmitter<C>();

  widgetType = widgetType;

  callbacks: WidgetConfigCallbacks = {
    generateDataKey: this.generateDataKey.bind(this),
    fetchEntityKeys: this.fetchEntityKeys.bind(this),
    fetchEntityKeysForDevice: this.fetchEntityKeysForDevice.bind(this)
  } as any;

  reportConfigForm: FormGroup;

  private reportComponentConfig: C;

  protected constructor(private destroyRef: DestroyRef) {}

  setupConfig(reportComponentConfig: C): FormGroup {
    this.reportComponentConfig = reportComponentConfig;
    this.reportConfigForm = this.buildForm(reportComponentConfig);
    this.reportConfigForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    return this.reportConfigForm;
  }

  private updateModel() {
    this.reportComponentConfig = {
      type: this.reportComponentConfig.type,
      ...this.reportConfigForm.getRawValue()
    };
    this.reportConfigUpdated.emit(this.reportComponentConfig);
  }

  private fetchEntityKeys(entityAliasId: string, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
    return this.context.aliasController.getAliasInfo(entityAliasId).pipe(
      mergeMap((aliasInfo) => this.context.entityService.getEntityKeysByEntityFilter(
        aliasInfo.entityFilter,
        dataKeyTypes,  [],
        {ignoreLoading: true, ignoreErrors: true}
      ).pipe(
        catchError(() => of([]))
      )),
      catchError(() => of([] as Array<DataKey>))
    );
  }

  private fetchEntityKeysForDevice(deviceId: string, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
    const entityFilter = singleEntityFilterFromDeviceId(deviceId);
    return this.context.entityService.getEntityKeysByEntityFilter(
      entityFilter,
      dataKeyTypes, [EntityType.DEVICE],
      {ignoreLoading: true, ignoreErrors: true}
    ).pipe(
      catchError(() => of([]))
    );
  }

  private generateDataKey(chip: any, type: DataKeyType, dataKeySettingsForm: FormProperty[],
                          isLatestDataKey: boolean, dataKeySettingsFunction: DataKeySettingsFunction): DataKey {
    if (isObject(chip)) {
      (chip as DataKey)._hash = Math.random();
      return chip;
    } else {
      let label: string = chip;
      if (type === DataKeyType.alarm || type === DataKeyType.entityField) {
        const keyField = type === DataKeyType.alarm ? alarmFields[label] : entityFields[chip];
        if (keyField) {
          label = this.context.translate.instant(keyField.name);
        }
      }
      const datasources = this.getDataSources();
      label = genNextLabel(label, datasources);
      const result: DataKey = {
        name: chip,
        type,
        label,
        color: this.genNextColor(),
        settings: {},
        _hash: Math.random()
      };
      if (type === DataKeyType.count) {
        result.name = 'count';
      }
      return result;
    }
  }

  private genNextColor(): string {
    let i = 0;
    const datasources = this.getDataSources();
    if (datasources) {
      datasources.forEach((datasource) => {
        if (datasource && (datasource.dataKeys || datasource.latestDataKeys)) {
          i += ((datasource.dataKeys ? datasource.dataKeys.length : 0) +
            (datasource.latestDataKeys ? datasource.latestDataKeys.length : 0));
        }
      });
    }
    return this.context.utils.getMaterialColor(i);
  }

  protected abstract buildForm(reportComponentConfig: C): FormGroup;

  protected getDataSources(): Datasource[] {
    if (this.reportConfigForm.get('dataSources')) {
      return this.reportConfigForm.get('dataSources').value;
    } else {
      return [];
    }
  };
}

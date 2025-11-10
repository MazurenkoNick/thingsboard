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
  booleanAttribute,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  Renderer2,
  ViewChild,
  ViewContainerRef,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  AggFunctionTranslations,
  AggInputTypeTranslations,
  CalculatedFieldAggMetric,
  CalculatedFieldAggMetricValue,
} from '@shared/models/calculated-field.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { TbTableDatasource } from '@shared/components/table/table-datasource.abstract';
import { MatSort, SortDirection } from '@angular/material/sort';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  CalculatedFieldMetricsPanelComponent
} from '@home/components/calculated-fields/components/related-entities-aggregation-configuration/calculated-field-metrics-panel.component';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { AceHighlightRules } from '@shared/models/ace/ace.models';

@Component({
  selector: 'tb-calculated-field-metrics-table',
  templateUrl: './calculated-field-metrics-table.component.html',
  styleUrls: [`../calculated-field-arguments/calculated-field-arguments-table.component.scss`],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldMetricsTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldMetricsTableComponent),
      multi: true
    }
  ],
})
export class CalculatedFieldMetricsTableComponent implements ControlValueAccessor, Validator, AfterViewInit {

  @Input() arguments: Array<string>;
  @Input() editorCompleter: TbEditorCompleter;
  @Input() highlightRules: AceHighlightRules;
  @Input({ transform: booleanAttribute }) readonly: boolean;

  @ViewChild(MatSort, { static: true }) sort: MatSort;

  errorText = '';
  metricsFormArray = this.fb.array<CalculatedFieldAggMetricValue>([]);
  sortOrder = { direction: 'asc' as SortDirection, property: '' };
  dataSource = new CalculatedFieldMetricsDatasource();

  displayColumns = ['name', 'function', 'filter', 'valueSource', 'actions']

  readonly AggFunctionTranslations = AggFunctionTranslations;
  readonly AggInputTypeTranslations = AggInputTypeTranslations;
  readonly maxArgumentsPerCF = getCurrentAuthState(this.store).maxArgumentsPerCF - 2;

  private popoverComponent: TbPopoverComponent<CalculatedFieldMetricsPanelComponent>;
  private propagateChange: (zonesObj: Record<string, CalculatedFieldAggMetric>) => void = () => {};

  constructor(
    private fb: FormBuilder,
    private popoverService: TbPopoverService,
    private viewContainerRef: ViewContainerRef,
    private cd: ChangeDetectorRef,
    private renderer: Renderer2,
    private destroyRef: DestroyRef,
    private store: Store<AppState>
  ) {
    this.metricsFormArray.valueChanges.pipe(takeUntilDestroyed()).subscribe(value => {
      this.updateDataSource(value);
      this.propagateChange(this.getMetricsObject(value));
    });
  }

  ngAfterViewInit(): void {
    this.sort.sortChange.asObservable().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.sortOrder.property = this.sort.active;
      this.sortOrder.direction = this.sort.direction;
      this.updateDataSource(this.metricsFormArray.value);
    });
  }

  registerOnChange(fn: (zonesObj: Record<string, CalculatedFieldAggMetric>) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {}

  validate(): ValidationErrors | null {
    this.updateErrorText();
    return this.errorText ? { metricsFormArray: false } : null;
  }

  onDelete($event: Event, metric: CalculatedFieldAggMetricValue): void {
    $event.stopPropagation();
    const index = this.metricsFormArray.controls.findIndex(control => isEqual(control.value, metric));
    this.metricsFormArray.removeAt(index);
    this.metricsFormArray.markAsDirty();
  }

  manageMetrics($event: Event, matButton: MatButton, metric = {} as CalculatedFieldAggMetricValue): void {
    $event?.stopPropagation();
    if (this.popoverComponent && !this.popoverComponent.tbHidden) {
      this.popoverComponent.hide();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const index = this.metricsFormArray.controls.findIndex(control => isEqual(control.value, metric));
      const isExists = index !== -1;
      const ctx = {
        index,
        metric,
        buttonTitle: isExists ? 'action.apply' : 'action.add',
        usedNames: this.metricsFormArray.value.map(({ name }) => name).filter(name => name !== metric.name),
        arguments: this.arguments,
        editorCompleter: this.editorCompleter,
        highlightRules: this.highlightRules
      };
      this.popoverComponent = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: CalculatedFieldMetricsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: isExists ? ['leftOnly', 'leftTopOnly', 'leftBottomOnly'] : ['rightOnly', 'rightTopOnly', 'rightBottomOnly'],
        context: ctx,
        isModal: true
      });
      this.popoverComponent.tbComponentRef.instance.metricDataApplied.subscribe((value) => {
        this.popoverComponent.hide();
        if (isExists) {
          this.metricsFormArray.at(index).setValue(value);
        } else {
          this.metricsFormArray.push(this.fb.control(value));
        }
        this.cd.markForCheck();
      });
    }
  }

  private updateDataSource(value: CalculatedFieldAggMetricValue[]): void {
    const sortedValue = this.sortData(value);
    this.dataSource.loadData(sortedValue);
  }

  private updateErrorText(): void {
    if (!this.metricsFormArray.controls.length) {
      this.errorText = 'calculated-fields.metrics.metrics-empty';
    } else {
      this.errorText = '';
    }
  }

  private getMetricsObject(value: CalculatedFieldAggMetricValue[]): Record<string, CalculatedFieldAggMetric> {
    return value.reduce((acc, metricValue) => {
      const { name, ...metric } = metricValue;
      acc[name] = metric;
      return acc;
    }, {} as Record<string, CalculatedFieldAggMetric>);
  }

  writeValue(metrics: Record<string, CalculatedFieldAggMetric>): void {
    this.metricsFormArray.clear();
    this.populateZonesFormArray(metrics);
  }

  private populateZonesFormArray(metrics: Record<string, CalculatedFieldAggMetric>): void {
    Object.keys(metrics).forEach(key => {
      const value: CalculatedFieldAggMetricValue = {
        ...metrics[key],
        name: key
      };
      this.metricsFormArray.push(this.fb.control(value), { emitEvent: false });
    });
    this.metricsFormArray.updateValueAndValidity();
  }

  private getSortValue(metric: CalculatedFieldAggMetricValue, column: string): string {
    switch (column) {
      case 'function':
        return metric.function;
      case 'valueSource':
        return metric.input?.type;
      case 'filter':
        return isDefinedAndNotNull(metric.filter).toString();
      default:
        return metric.name;
    }
  }

  private sortData(data: CalculatedFieldAggMetricValue[]): CalculatedFieldAggMetricValue[] {
    return data.sort((a, b) => {
      const valA = this.getSortValue(a, this.sortOrder.property) ?? '';
      const valB = this.getSortValue(b, this.sortOrder.property) ?? '';
      return (this.sortOrder.direction === 'asc' ? 1 : -1) * valA.localeCompare(valB);
    });
  }
}

class CalculatedFieldMetricsDatasource extends TbTableDatasource<CalculatedFieldAggMetricValue> {
  constructor() {
    super();
  }
}

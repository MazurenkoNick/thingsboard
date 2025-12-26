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

import { Component, Input, OnInit, output } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, Validators } from '@angular/forms';
import { charsWithNumRegex } from '@shared/models/regex.constants';
import {
  AggFunction,
  AggFunctionTranslations,
  AggInputType,
  AggInputTypeTranslations,
  CalculatedFieldAggMetricValue,
  FORBIDDEN_NAMES,
  forbiddenNamesValidator,
  uniqueNameValidator
} from '@shared/models/calculated-field.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityFilter } from '@shared/models/query/query.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { AceHighlightRules } from '@shared/models/ace/ace.models';
import { Observable } from "rxjs";

interface CalculatedFieldAggMetricValuePanel extends CalculatedFieldAggMetricValue {
  allowFilter: boolean;
}

@Component({
  selector: 'tb-calculated-field-metrics-panel',
  templateUrl: './calculated-field-metrics-panel.component.html',
  styleUrl: '../common/calculated-field-panel.scss',
})
export class CalculatedFieldMetricsPanelComponent implements OnInit {

  @Input() buttonTitle: string;
  @Input() metric: CalculatedFieldAggMetricValue;
  @Input() usedNames: string[];
  @Input() arguments: Array<string>;
  @Input() simpleMode: boolean;
  @Input() editorCompleter: TbEditorCompleter;
  @Input() highlightRules: AceHighlightRules;
  @Input({required: true}) testScript: (expression?: string) => Observable<string>;

  metricDataApplied = output<CalculatedFieldAggMetricValue>();
  filterExpanded = false;
  functionArgs: Array<string>

  metricForm = this.fb.group({
    name: ['', [Validators.required, forbiddenNamesValidator(FORBIDDEN_NAMES), Validators.pattern(charsWithNumRegex), Validators.maxLength(255)]],
    function: [AggFunction.AVG],
    allowFilter: [false],
    filter: ['', Validators.required],
    input: this.fb.group({
      type: [AggInputType.key],
      key: ['', Validators.required],
      function: ['', Validators.required],
    }),
    defaultValue: [null]
  });

  entityFilter: EntityFilter;

  AggFunctions = Object.values(AggFunction) as AggFunction[];
  readonly AggFunctionTranslations = AggFunctionTranslations;
  readonly ScriptLanguage = ScriptLanguage;
  readonly AggInputType = AggInputType;
  readonly AggInputTypes = Object.values(AggInputType) as AggInputType[];
  readonly AggInputTypeTranslations = AggInputTypeTranslations;

  constructor(
    private fb: FormBuilder,
    private popover: TbPopoverComponent<CalculatedFieldMetricsPanelComponent>,
  ) {
    this.observeFilterAllowChange();
    this.observeInputTypeChange();
  }

  ngOnInit(): void {
    this.updatedForm();

    const data: CalculatedFieldAggMetricValuePanel = {
      ...this.metric,
      allowFilter: !!this.metric.filter,
    }
    this.metricForm.patchValue(data, {emitEvent: false});

    this.validateFilter(data.allowFilter);
    this.validateInputTypeFilter(data.input?.type ?? AggInputType.key);
    this.validateInputKey();

    this.functionArgs = ['ctx', ...this.arguments];

    if (this.simpleMode) {
      this.AggFunctions = this.AggFunctions.filter(aggFunc => aggFunc !== AggFunction.COUNT_UNIQUE);
    }
  }

  saveMetric(): void {
    const value = this.metricForm.value as CalculatedFieldAggMetricValuePanel;
    if (!value.allowFilter) {
      delete value.filter;
    }
    delete value.allowFilter;
    this.metricDataApplied.emit(value);
  }

  cancel(): void {
    this.popover.hide();
  }

  private updatedForm(): void {
    this.metricForm.get('name').addValidators(uniqueNameValidator(this.usedNames));
    this.metricForm.get('name').updateValueAndValidity({emitEvent: false});

    if (!this.simpleMode) {
      this.metricForm.removeControl('defaultValue', {emitEvent: false});
    }
  }

  private observeFilterAllowChange(): void {
    this.metricForm.get('allowFilter').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.validateFilter(value));
  }

  private observeInputTypeChange(): void {
    this.metricForm.get('input.type').valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(value => this.validateInputTypeFilter(value));
  }

  private validateFilter(allowFilter = false): void {
    if (allowFilter) {
      this.metricForm.get('filter').enable({emitEvent: false});
    } else {
      this.metricForm.get('filter').disable({emitEvent: false});
    }
    this.filterExpanded = allowFilter;
  }

  private validateInputTypeFilter(value: AggInputType): void {
    const inputForm = this.metricForm.get('input');
    if (value === AggInputType.key) {
      inputForm.get('key').enable({emitEvent: false});
      inputForm.get('function').disable({emitEvent: false});
    } else {
      inputForm.get('key').disable({emitEvent: false});
      inputForm.get('function').enable({emitEvent: false});
    }
  }

  private validateInputKey() {
    if (this.metric.input?.type === AggInputType.key && !this.arguments.includes(this.metric.input.key)) {
      this.metricForm.get('input.key').setValue(null);
      this.metricForm.get('input.key').markAsTouched();
    }
  }

  onTestScript(scriptFunc: 'filter' | 'input.function') {
    this.testScript(this.metricForm.get(scriptFunc).value).subscribe(expression => {
      this.metricForm.get(scriptFunc).setValue(expression);
      this.metricForm.get(scriptFunc).markAsDirty();
    });
  }
}

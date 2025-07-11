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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { EntityType } from '@shared/models/entity-type.models';
import { MatDialog } from '@angular/material/dialog';
import { AIModelDialogComponent, AIModelDialogData } from '@home/components/ai-model/ai-model-dialog.component';
import { AiModel, AiRuleNodeResponseFormatTypeOnlyText, ResponseFormat } from '@shared/models/ai-model.models';
import { deepTrim } from '@core/utils';
import { Operation, Resource } from '@shared/models/security.models';

@Component({
  selector: 'tb-external-node-ai-config',
  templateUrl: './ai-config.component.html',
  styleUrls: []
})
export class AiConfigComponent extends RuleNodeConfigurationComponent {

  aiConfigForm: UntypedFormGroup;

  entityType = EntityType;

  responseFormat = ResponseFormat;

  readonly operation = Operation;
  readonly resource = Resource;

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.aiConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.aiConfigForm = this.fb.group({
      modelId: [configuration?.modelId ?? null, [Validators.required]],
      systemPrompt: [configuration?.systemPrompt ?? '', [Validators.maxLength(10000), Validators.pattern(/.*\S.*/)]],
      userPrompt: [configuration?.userPrompt ?? '', [Validators.required, Validators.maxLength(10000), Validators.pattern(/.*\S.*/)]],
      responseFormat: this.fb.group({
        type: [configuration?.responseFormat?.type ?? ResponseFormat.JSON, []],
        schema: [configuration?.responseFormat?.schema ?? null, [Validators.required]],
      }),
      timeoutSeconds: [configuration?.timeoutSeconds ?? 60, []],
      forceAck: [configuration?.forceAck ?? true, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['responseFormat.type'];
  }

  protected updateValidators(emitEvent: boolean) {
    if (this.aiConfigForm.get('responseFormat.type').value === ResponseFormat.JSON_SCHEMA) {
      this.aiConfigForm.get('responseFormat.schema').enable({emitEvent: false});
    } else {
      this.aiConfigForm.get('responseFormat.schema').disable({emitEvent: false});
    }
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return deepTrim(configuration);
  }

  onEntityChange($event: AiModel) {
    if ($event) {
      if (AiRuleNodeResponseFormatTypeOnlyText.includes($event.configuration.provider)) {
        this.aiConfigForm.get('responseFormat.type').patchValue(ResponseFormat.TEXT, {emitEvent: false});
        this.aiConfigForm.get('responseFormat.type').disable({emitEvent: false});
      }
    } else {
      this.aiConfigForm.get('responseFormat.type').enable({emitEvent: false});
    }
  }

  createModelAi(formControl: string) {
    this.dialog.open<AIModelDialogComponent, AIModelDialogData, AiModel>(AIModelDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd: true
      }
    }).afterClosed()
      .subscribe((model) => {
        if (model) {
          this.aiConfigForm.get(formControl).patchValue(model.id);
          this.aiConfigForm.get(formControl).markAsDirty();
        }
      });
  }
}

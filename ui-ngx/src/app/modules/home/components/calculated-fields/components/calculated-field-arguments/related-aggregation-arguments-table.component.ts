///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
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
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  Renderer2,
  ViewContainerRef,
} from '@angular/core';
import { FormBuilder, NG_VALIDATORS, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { TbPopoverService } from '@shared/components/popover.service';
import { EntityService } from '@core/http/entity.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  CalculatedFieldArgumentsTableComponent
} from '@home/components/calculated-fields/components/calculated-field-arguments/calculated-field-arguments-table.component';
import { ArgumentEntityType, RelationPathLevel } from '@shared/models/calculated-field.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-related-aggregation-arguments-table',
  templateUrl: './calculated-field-arguments-table.component.html',
  styleUrls: [`calculated-field-arguments-table.component.scss`],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RelatedAggregationArgumentsTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RelatedAggregationArgumentsTableComponent),
      multi: true
    }
  ],
})
export class RelatedAggregationArgumentsTableComponent extends CalculatedFieldArgumentsTableComponent {

  @Input({required: true})
  set relation(value: RelationPathLevel) {
    this.panelAdditionalCtx.predefinedEntityFilter = {
      type: AliasFilterType.relationsQuery,
      rootStateEntity: false,
      rootEntity: this.entityId,
      direction: value.direction,
      filters: [{
        relationType: value.relationType,
        entityTypes: [EntityType.DEVICE, EntityType.ASSET, EntityType.CUSTOMER, EntityType.TENANT]
      }],
      maxLevel: 1,
    };
  }

  constructor(
    protected fb: FormBuilder,
    protected popoverService: TbPopoverService,
    protected viewContainerRef: ViewContainerRef,
    protected cd: ChangeDetectorRef,
    protected renderer: Renderer2,
    protected entityService: EntityService,
    protected destroyRef: DestroyRef,
    protected store: Store<AppState>
  ) {
    super(fb, popoverService, viewContainerRef, cd, renderer, entityService, destroyRef, store);

    this.argumentNameColumn = 'calculated-fields.argument-name';
    this.displayColumns = ['name', 'type', 'key', 'actions'];
    this.panelAdditionalCtx = {
      hiddenEntityTypes: true,
      defaultValueRequired: true,
      argumentEntityTypes: [ArgumentEntityType.Current],
      hint: 'calculated-fields.hint.setting-arguments-aggregation',
      watchKeyChange: true,
    };

    this.isScript = false;
  }
}

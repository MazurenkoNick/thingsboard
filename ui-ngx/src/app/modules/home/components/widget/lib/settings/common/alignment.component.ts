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

import { Component, forwardRef, Input, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  alignment, alignmentIcons, alignmentTranslations
} from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  AlignmentPanelComponent
} from '@home/components/widget/lib/settings/common/alignment-panel.component';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-alignment',
    templateUrl: './alignment.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AlignmentComponent),
            multi: true
        }
    ],
    standalone: false
})
export class AlignmentComponent implements ControlValueAccessor {

  alignmentTranslations = alignmentTranslations;
  alignmentIcons = alignmentIcons;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  horizontal = true;

  @Input()
  allowedAlignments: alignment[];

  modelValue: alignment;

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: alignment): void {
    this.modelValue = value;
    if (!this.modelValue) {
      this.modelValue = this.horizontal ? 'left' : 'top';
    }
  }

  openAlignmentPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        alignment: this.modelValue,
        horizontal: this.horizontal,
        allowedAlignments: this.allowedAlignments
      };
      const alignmentPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: AlignmentPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['top', 'topLeft', 'topRight'],
        context: ctx,
        showCloseButton: false,
        isModal: false,
        popoverContentStyle: {padding: '6px'}
      });
      alignmentPanelPopover.tbComponentRef.instance.alignmentSelected.subscribe((alignment) => {
        alignmentPanelPopover.hide();
        this.modelValue = alignment;
        this.propagateChange(this.modelValue);
      });
    }
  }

}

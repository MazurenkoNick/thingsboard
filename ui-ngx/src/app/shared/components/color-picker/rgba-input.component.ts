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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Color } from '@iplab/ngx-color-picker';
import { coerceBoolean } from '@shared/decorators/coercion';

type Channel = 'R' | 'G' | 'B';

@Component({
  selector: 'tb-rgba-input',
  templateUrl: './rgba-input.component.html',
  styleUrl: './color-input.base.scss',
  standalone: false
})
export class RgbaInputComponent {

  @Input()
  public color: Color;

  @Output()
  public colorChange = new EventEmitter<Color>(false);

  @Input()
  @coerceBoolean()
  public labelVisible = false;

  @Input()
  public suffixValue = '%';

  @Input()
  @coerceBoolean()
  alpha: boolean;

  public get value() {
    return this.color.getRgba();
  }

  public get alphaValue(): string {
    return this.color ? Math.round(this.color.getRgba().getAlpha() * 100).toString() : '';
  }

  public onAlphaInputChange(inputValue: number): void {
    if (!this.color) return;
    const color = this.color.getRgba();
    const alpha = +inputValue / 100;
    if (color.getAlpha() !== alpha) {
      const newColor = new Color().setRgba(color.getRed(), color.getGreen(), color.getBlue(), alpha).toRgbaString();
      this.colorChange.emit(new Color(newColor));
    }
  }

  onInputChange(newValue: number, channel: Channel) {
    if (!this.color) return;
    const rgba = this.value;
    const red   = channel === 'R' ? newValue : rgba.getRed();
    const green = channel === 'G' ? newValue : rgba.getGreen();
    const blue  = channel === 'B' ? newValue : rgba.getBlue();
    if (red === rgba.getRed() && green === rgba.getGreen() && blue === rgba.getBlue()) return;
    this.colorChange.emit(new Color().setRgba(red, green, blue, rgba.alpha));
  }
}

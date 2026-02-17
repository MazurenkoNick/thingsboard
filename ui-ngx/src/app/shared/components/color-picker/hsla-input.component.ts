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

type Channel = 'H' | 'S' | 'L';

@Component({
  selector: 'tb-hsla-input',
  templateUrl: './hsla-input.component.html',
  styleUrl: './color-input.base.scss',
  standalone: false
})
export class HslaInputComponent {

  @Input()
  public color: Color;

  @Output()
  public colorChange = new EventEmitter<Color>(false);

  @Input()
  @coerceBoolean()
  public labelVisible = false;

  @Input()
  public suffixValue = '%';

  public get value() {
    return this.color.getHsla();
  }

  public get alphaValue(): number {
    return this.color ? Math.round(this.color.getHsla().getAlpha() * 100) : 0;
  }

  public onAlphaInputChange(inputValue: number): void {
    if (!this.color) return;
    const hsla = this.color.getHsla();
    const alpha = +inputValue / 100;
    if (hsla.alpha !== alpha) {
      const newColor = new Color().setHsla(hsla.getHue(), hsla.getSaturation(), hsla.getLightness(), alpha);
      this.colorChange.emit(newColor);
    }
  }

  public onInputChange(newValue: number, channel: Channel): void {
    if (!this.color) return;
    const hsla = this.value;
    const hue = channel === 'H' ? +newValue : hsla.getHue();
    const saturation = channel === 'S' ? +newValue : hsla.getSaturation();
    const lightness = channel === 'L' ? +newValue : hsla.getLightness();
    if (hue === hsla.getHue() && saturation === hsla.getSaturation() && lightness === hsla.getLightness()) return;
    const newColor = new Color().setHsla(hue, saturation, lightness, hsla.getAlpha());
    this.colorChange.emit(newColor);
  }
}

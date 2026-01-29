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

import { Component, HostBinding, Inject, ViewEncapsulation } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { WINDOW } from '@core/services/window.service';

// @dynamic
@Component({
    selector: 'tb-white-labeling-feature-dialog',
    templateUrl: './white-labeling-feature-dialog.component.html',
    styleUrls: ['./white-labeling-feature-dialog.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class WhiteLabelingFeatureDialogComponent extends DialogComponent<WhiteLabelingFeatureDialogComponent> {

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  whiteLabelingSvg = 'assets/white-labeling.svg';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(WINDOW) private window: Window,
              public dialogRef: MatDialogRef<WhiteLabelingFeatureDialogComponent>) {
    super(store, router, dialogRef);
  }

  public gotoWhiteLabelingDoc() {
    this.window.open('https://thingsboard.io/docs/user-guide/white-labeling/', '_blank');
  }

  public upgrade() {
    this.dialogRef.close();
    this.window.open('https://thingsboard.io/pricing/', '_blank');
  }

  public close() {
    this.dialogRef.close();
  }
}

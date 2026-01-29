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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { WINDOW } from '@core/services/window.service';
import { SubscriptionEntry, SubscriptionErrorCode, subscriptionErrorsMap } from '@shared/models/subscription.models';
import { TranslateService } from '@ngx-translate/core';

export interface EntityLimitDialogData {
  subscriptionErrorCode: SubscriptionErrorCode;
  subscriptionEntry: SubscriptionEntry;
  value: any;
}

// @dynamic
@Component({
    selector: 'tb-entity-limit-dialog',
    templateUrl: './entity-limit-dialog.component.html',
    styleUrls: ['./entity-limit-dialog.component.scss'],
    standalone: false
})
export class EntityLimitDialogComponent extends DialogComponent<EntityLimitDialogComponent> {

  limitReachedSvg = 'assets/limit-reached.svg';

  errorContent: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: EntityLimitDialogData,
              @Inject(WINDOW) private window: Window,
              public dialogRef: MatDialogRef<EntityLimitDialogComponent>) {
    super(store, router, dialogRef);

    const subscriptionErrorText = subscriptionErrorsMap.get(data.subscriptionErrorCode).get(data.subscriptionEntry);
    this.errorContent = this.translate.instant(subscriptionErrorText, {value: data.value.value});
  }

  public upgrade() {
    this.dialogRef.close();
    this.window.open('https://thingsboard.io/pricing/', '_blank');
  }

  public close() {
    this.dialogRef.close();
  }
}

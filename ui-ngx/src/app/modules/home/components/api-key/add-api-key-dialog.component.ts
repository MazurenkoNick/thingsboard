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


import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, Validators } from '@angular/forms';
import { deepTrim } from '@core/utils';
import { ApiKeyService } from '@core/http/api-key.service';
import { ApiKeyInfo } from '@shared/models/api-key.models';
import { ApiKeysTableDialogData } from '@home/components/api-key/api-keys-table-dialog.component';
import { DAY } from '@shared/models/time/time.models';

@Component({
  selector: 'tb-add-api-key-dialog',
  templateUrl: './add-api-key-dialog.component.html',
  styleUrls: ['./add-api-key-dialog.component.scss']
})
export class AddApiKeyDialogComponent extends DialogComponent<AddApiKeyDialogComponent, ApiKeyInfo | string> {

  readonly startDate = new Date();
  readonly expirationTimeOptions: Array<number> = [7, 30, 60, 90].map(days => days * DAY);
  readonly apiKeyForm = this.fb.group({
    description: [{value: null, disabled: false}, [Validators.required]],
    enabled: [{value: true, disabled: false}, []],
    expirationTime: [{value: this.expirationTimeOptions[1] as string | number, disabled: false}, [Validators.required]],
    customExpirationTime: [{value: null, disabled: true}, []],
  });

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    public dialogRef: MatDialogRef<AddApiKeyDialogComponent, ApiKeyInfo | string>,
    private fb: FormBuilder,
    private apiKeyService: ApiKeyService,
    @Inject(MAT_DIALOG_DATA) public data: ApiKeysTableDialogData,
  ) {
    super(store, router, dialogRef);
  }

  close(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    const formValue = this.apiKeyForm.value;
    const userId = this.data.userId;
    const expirationTime = this.calcExpirationTime();
    const apiKey = {
      ...deepTrim(formValue),
      expirationTime,
      userId,
    } as ApiKeyInfo;
    this.apiKeyService.saveApiKey(apiKey).subscribe(
      (res) => {
        this.dialogRef.close(res);
      }
    );
  }

  isCustomExpirationTime() {
    return this.apiKeyForm.value?.expirationTime === 'custom';
  }

  onExpirationDateChange() {
    const customExpirationTimeControl = this.apiKeyForm.get('customExpirationTime');
    if (this.isCustomExpirationTime()) {
      customExpirationTimeControl.enable({emitEvent: false});
    } else {
      customExpirationTimeControl.disable({emitEvent: false});
    }
  }

  private calcExpirationTime(): number {
    const expirationTimeValue = this.apiKeyForm.get('expirationTime').value;
    let value: number;
    if (this.isCustomExpirationTime()) {
      value = this.apiKeyForm.get('customExpirationTime').value.getTime();
    } else if (expirationTimeValue === 'never') {
      value = 0;
    } else {
      value = expirationTimeValue as number + Date.now();
    }
    return value;
  }
}

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
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { userInfoCommand, ApiKey } from '@shared/models/api-key.models';
import { getOS } from '@core/utils';

export interface ApiKeyGeneratedDialogData {
  apiKey: ApiKey;
}

@Component({
  selector: 'tb-api-key-generated-dialog',
  templateUrl: './api-key-generated-dialog.component.html',
  styleUrls: ['api-key-generated-dialog.component.scss']
})
export class ApiKeyGeneratedDialogComponent extends DialogComponent<ApiKeyGeneratedDialogComponent, void> {

  private baseUrl = window.location.origin;

  apiKeyCommand = userInfoCommand(this.baseUrl, this.data.apiKey.value);
  secureUrl = this.baseUrl.startsWith('https');
  selectedTab: number;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<ApiKeyGeneratedDialogComponent, void>,
              @Inject(MAT_DIALOG_DATA) public data: ApiKeyGeneratedDialogData) {
    super(store, router, dialogRef);
    this.selectTabIndexForUserOS();
  }

  close(): void {
    this.dialogRef.close(null);
  }

  createMarkDownCommand(command: string): string {
    return '```bash\n' +
      command +
      '{:copy-code}\n' +
      '```';
  }

  private selectTabIndexForUserOS() {
    const currentOS = getOS();
    switch (currentOS) {
      case 'linux':
      case 'android':
        this.selectedTab = 2;
        break;
      case 'macos':
      case 'ios':
        this.selectedTab = 1;
        break;
      case 'windows':
        this.selectedTab = 0;
        break;
      default:
        this.selectedTab = 2;
    }
  }
}

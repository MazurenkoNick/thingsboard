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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { WINDOW } from '@core/services/window.service';
import { SolutionTemplateLevel, solutionTemplateLevelToPlanNameMap } from '@shared/models/solution-template.models';
import { TranslateService } from '@ngx-translate/core';
import { NotificationService } from '@core/http/notification.service';
import { DialogService } from '@core/services/dialog.service';

export interface RequestPlanUpgradeDialogData {
  solutionTemplateName: string;
  solutionTemplateLevel: SolutionTemplateLevel;
}

@Component({
  selector: 'tb-request-plan-upgrade-dialog',
  templateUrl: './request-plan-upgrade-dialog.component.html',
  styleUrls: ['./request-plan-upgrade-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class RequestPlanUpgradeDialogComponent extends DialogComponent<RequestPlanUpgradeDialogComponent> {

  planName: string;
  dialogMessage: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private translate: TranslateService,
              @Inject(WINDOW) private window: Window,
              @Inject(MAT_DIALOG_DATA) public data: RequestPlanUpgradeDialogData,
              public dialogRef: MatDialogRef<RequestPlanUpgradeDialogComponent>,
              private dialogs: DialogService,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);
    this.planName = solutionTemplateLevelToPlanNameMap.get(this.data.solutionTemplateLevel);
    this.dialogMessage = this.translate.instant('subscription.unsupported-solution-template-request-plan-upgrade-text', {
      solutionTemplateName: this.data.solutionTemplateName, planName: this.planName
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  requestPlanUpgrade($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.notificationService.sendPlanUpgradeRequest(this.planName).subscribe(
      () => {
        this.dialogRef.close();
        this.dialogs.alert(
          this.translate.instant('subscription.plan-upgrade-request-sent-title'),
          this.translate.instant('subscription.plan-upgrade-request-sent-text'),
          this.translate.instant('action.close')
        );
      }
    );
  }
}

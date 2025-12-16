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

import { Component, ViewEncapsulation } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AuthService } from '@core/auth/auth.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { NotificationService } from '@core/http/notification.service';
import { AddonType, addonTypeTranslationMap } from '@shared/models/subscription.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
  selector: 'tb-request-white-labeling-dialog',
  templateUrl: './request-white-labeling-dialog.component.html',
  styleUrls: ['./request-feature-dialog-styles.scss', './request-white-labeling-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class RequestWhiteLabelingDialogComponent extends DialogComponent<RequestWhiteLabelingDialogComponent>{

  authUser = getCurrentAuthUser(this.store);

  isSysAdmin = this.authUser.authority === Authority.SYS_ADMIN;
  isTenantAdmin = this.authUser.authority === Authority.TENANT_ADMIN;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RequestWhiteLabelingDialogComponent>,
              private authService: AuthService,
              private dialogs: DialogService,
              private translate: TranslateService,
              private notificationService: NotificationService,
              private wl: WhiteLabelingService) {
    super(store,  router, dialogRef);
  }

  cancel(): void {
    this.dialogRef.close();
  }

  requestAccess($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.notificationService.sendAddonAccessRequest(AddonType.WHITE_LABELING).subscribe(
      () => {
        this.dialogRef.close();
        this.dialogs.alert(
          this.translate.instant('subscription.feature-request-sent-title', {addonName: this.translate.instant(addonTypeTranslationMap.get(AddonType.WHITE_LABELING))}),
          this.translate.instant('subscription.feature-request-sent-text'),
          this.translate.instant('action.close')
        );
      }
    );
  }

  upgradePlan($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl('license', { state: {skipConfirmOnExit: true} }).then(() => {});
  }

  learnMore($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    window.open(`${this.wl.getHelpLinkBaseUrl()}/docs/user-guide/white-labeling/`, '_blank');
  }

  loginAsSysAdmin($event: Event) {
    if ($event) {
      $event.preventDefault();
      $event.stopPropagation();
    }
    this.authService.redirectUrl = '/license';
    this.authService.logout();
  }

}

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

import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Component, Inject, ViewEncapsulation } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { AuthService } from '@core/auth/auth.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { NotificationService } from '@core/http/notification.service';
import { Authority } from '@shared/models/authority.enum';

export interface EntityLimitExceededDialogData {
  entityType: EntityType;
  limit: number;
  subscriptionViolation: boolean;
}

// @dynamic
@Component({
    selector: 'tb-entity-limit-exceeded-dialog',
    templateUrl: './entity-limit-exceeded-dialog.component.html',
    styleUrls: ['./entity-limit-exceeded-dialog.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EntityLimitExceededDialogComponent extends DialogComponent<EntityLimitExceededDialogComponent> {

  limitReachedText: string;

  isCustomerUser = getCurrentAuthUser(this.store).authority === Authority.CUSTOMER_USER;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EntityLimitExceededDialogData,
              public dialogRef: MatDialogRef<EntityLimitExceededDialogComponent>,
              private authService: AuthService,
              private dialogs: DialogService,
              private translate: TranslateService,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    const entitiesPlural = (this.translate.instant(entityTypeTranslations.get(data.entityType).typePlural) as string).toLowerCase();
    const entity = (this.translate.instant(entityTypeTranslations.get(data.entityType).type) as string).toLowerCase();
    let entitiesText: string;
    if (this.isCustomerUser || this.data.subscriptionViolation) {
      entitiesText = entitiesPlural;
    } else {
      if (data.limit > 1) {
        entitiesText = data.limit + ' ' + entitiesPlural;
      } else {
        entitiesText = '1 ' + entity;
      }
    }
    this.limitReachedText = this.translate.instant('entity.limit-reached-text', {
      entities: entitiesText,
      entity
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  requestLimitIncrease($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.notificationService.sendEntitiesLimitIncreaseRequest(this.data.entityType, this.data.subscriptionViolation).subscribe(
      () => {
        this.dialogRef.close();
        this.dialogs.alert(
          this.translate.instant('entity.increase-limit-request-sent-title'),
          this.translate.instant('entity.increase-limit-request-sent-text'),
          this.translate.instant('action.close')
        );
      }
    );
  }

  loginAsSysAdmin($event: Event) {
    if ($event) {
      $event.preventDefault();
      $event.stopPropagation();
    }
    this.authService.redirectUrl = `/tenants/${getCurrentAuthUser(this.store).tenantId}`;
    this.authService.logout();
  }

}

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

import { inject, NgModule } from "@angular/core";
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from "@angular/router";
import { TrendzSettingsComponent } from "@home/pages/trendz-settings/trendz-settings.component";
import { Authority } from "@app/shared/models/authority.enum";
import { MenuId } from "@app/core/services/menu.models";
import { map, of, switchMap } from "rxjs";
import { TrendzStatus, TrendzSynchronizationStatus } from "@app/shared/models/trendz-analytics.models";
import { TrendzService } from "@app/core/http/trendz.service";

export const TrendzSyncResolver: ResolveFn<TrendzStatus> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  trendzService = inject(TrendzService)) => {
    return trendzService.getTrendzSyncResult().pipe(
      switchMap(result => {
        const trendzStatus: TrendzStatus = {
          type: result.type,
          syncStatus: result.status,
          healthcheckStatus: result.status,
        }
        if (result.status === TrendzSynchronizationStatus.SYNCED) {
          return trendzService.performTrendzHealthcheck().pipe(
            map(healthcheckResult => {
              trendzStatus.healthcheckStatus = healthcheckResult.status;
              trendzStatus.type = healthcheckResult.type;
              return trendzStatus;
            })
          );
        }
        return of(trendzStatus);
      }),
    )
}

const routes: Routes = [
  {
    path: 'trendzSettings',
    component: TrendzSettingsComponent,
    data: {
      auth: [Authority.SYS_ADMIN],
      title: 'trendz-analytics.trendz-settings',
      breadcrumb: {
        menuId: MenuId.trendz_settings
      }
    },
    resolve: {
      trendzSyncInfo: TrendzSyncResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TrendzSettingsRoutingModule { }

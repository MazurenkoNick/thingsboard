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

import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { TrendzAnalyticsComponent } from '@home/pages/trendz-analytics/trendz-analytics.component';
import { MenuId } from '@core/services/menu.models';
import { map, of } from 'rxjs';
import { TrendzSynchronizationStatus } from '@app/shared/models/trendz-analytics.models';
import { TrendzService } from '@core/http/trendz.service';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';

export const TrendzSyncInfoResolver: ResolveFn<boolean> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  store: Store<AppState> = inject(Store<AppState>),
  trendzService = inject(TrendzService)) => {
    const authState = getCurrentAuthState(store);
    if (authState.licenseVersion > 1 && authState.trendzEnabled) {
      return trendzService.performTrendzHealthcheck()
      .pipe(map(result => result.status === TrendzSynchronizationStatus.SYNCED));
    } else {
      return of(false);
    }
  }

const routes: Routes = [
  {
    path: 'analytics',
    component: TrendzAnalyticsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'trendz-analytics.trendz-analytics',
      breadcrumb: {
        menuId: MenuId.trendz_analytics
      }
    },
    resolve: {
      trendzSynced: TrendzSyncInfoResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TrendzAnalyticsRoutingModule { }

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

import { DestroyRef, inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, Router, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { CalculatedFieldsTableComponent } from '@home/components/calculated-fields/calculated-fields-table.component';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { CalculatedFieldsTableConfig } from '@home/components/calculated-fields/calculated-fields-table-config';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DatePipe } from '@angular/common';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';

export const CalculatedFieldsTableConfigResolver: ResolveFn<CalculatedFieldsTableConfig> =
  (_route: ActivatedRouteSnapshot,
   _state: RouterStateSnapshot,
   calculatedFieldsService = inject(CalculatedFieldsService),
   translate = inject(TranslateService),
   dialog = inject(MatDialog),
   store = inject(Store<AppState>),
   datePipe = inject(DatePipe),
   destroyRef = inject(DestroyRef),
   importExportService = inject(ImportExportService),
   entityDebugSettingsService = inject(EntityDebugSettingsService),
   utilsService = inject(UtilsService),
   userPermissionsService = inject(UserPermissionsService),
   router = inject(Router),
  ) => {
    return new CalculatedFieldsTableConfig(
      calculatedFieldsService,
      translate,
      dialog,
      datePipe,
      null,
      store,
      destroyRef,
      null,
      null,
      null,
      importExportService,
      entityDebugSettingsService,
      utilsService,
      router,
      false,
      false,
      null,
      userPermissionsService,
      true,
    );
  };

const routes: Routes = [
  {
    path: 'calculatedFields',
    data: {
      breadcrumb: {
        menuId: MenuId.calculated_fields
      }
    },
    children: [
      {
        path: '',
        component: CalculatedFieldsTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'entity.type-calculated-fields',
          isPage: true,
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'mdi:function-variant'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN],
          title: 'entity.type-calculated-fields',
        },
        resolve: {
          entitiesTableConfig: CalculatedFieldsTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class CalculatedFieldsRoutingModule { }

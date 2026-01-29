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

import { Component, inject, ViewEncapsulation } from '@angular/core';
import { DashboardReportComponentConfig } from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { Observable, of } from 'rxjs';
import { catchError, share } from 'rxjs/operators';
import { DashboardInfo } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { getEntityDetailsPageURL } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import { Router } from '@angular/router';

@Component({
    selector: 'tb-dashboard-preview',
    templateUrl: './dashboard-preview.component.html',
    styleUrls: ['./dashboard-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DashboardPreviewComponent extends AbstractReportComponentPreview<DashboardReportComponentConfig> {

  dashboard$: Observable<DashboardInfo>;

  imageWidth: string = '100%';

  imageAlign: string = 'center';

  private dashboardService = inject(DashboardService);
  private router = inject(Router);

  onComponentUpdated() {
    if (this.reportComponent.config?.dashboardId) {
      this.dashboard$ = this.dashboardService
      .getDashboardInfo(this.reportComponent.config.dashboardId, {ignoreLoading: true, ignoreErrors: true}).pipe(
        catchError(() => of(null)),
        share()
      );
    } else {
      this.dashboard$ = of(null);
    }
    this.imageWidth = '100%';
    if (this.reportComponent.widthType === 'original') {
      this.imageWidth = 'auto';
    } else if (this.reportComponent.widthType === 'custom') {
      const customWidth = this.reportComponent.customWidth || 100;
      this.imageWidth = customWidth + 'px';
    }
    this.imageAlign = this.reportComponent.alignment || 'center';
  }

  openDashboardNewTab($event: Event, dashboard: DashboardInfo) {
    $event.stopPropagation();
    const dashboardUrl = getEntityDetailsPageURL(dashboard.id.id, EntityType.DASHBOARD);
    const url = this.router.serializeUrl(this.router.createUrlTree([dashboardUrl]));
    window.open(url, '_blank');
  }

}

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
import { SubReportReportComponentConfig } from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { ReportTemplateService } from '@core/http/report-template.service';
import { Observable, of } from 'rxjs';
import { ReportTemplateInfo } from '@shared/models/report.models';
import { catchError, share } from 'rxjs/operators';
import { getEntityDetailsPageURL } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import { Router } from '@angular/router';

@Component({
    selector: 'tb-sub-report-preview',
    templateUrl: './sub-report-preview.component.html',
    styleUrls: ['./sub-report-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class SubReportPreviewComponent extends AbstractReportComponentPreview<SubReportReportComponentConfig> {

  subReport$: Observable<ReportTemplateInfo>;

  private reportTemplateService = inject(ReportTemplateService);
  private router = inject(Router);

  onComponentUpdated() {
    if (this.reportComponent.templateId !== null) {
      this.subReport$ = this.reportTemplateService
      .getReportTemplateInfo(this.reportComponent.templateId.id, {ignoreLoading: true, ignoreErrors: true}).pipe(
        catchError(() => of(null)),
        share()
      );
    } else {
      this.subReport$ = of(null);
    }
  }

  openSubReportNewTab($event: Event, subReport: ReportTemplateInfo) {
    $event.stopPropagation();
    const subReportUrl = getEntityDetailsPageURL(subReport.id.id, EntityType.REPORT_TEMPLATE);
    const url = this.router.serializeUrl(this.router.createUrlTree([subReportUrl]));
    window.open(url, '_blank');
  }

}

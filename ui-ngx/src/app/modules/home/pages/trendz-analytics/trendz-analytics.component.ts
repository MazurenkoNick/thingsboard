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

import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { getMetricLink, TrendzSummary, TrendzViewType } from '@shared/models/trendz-analytics.models';
import { TrendzService } from '@app/core/http/trendz.service';
import { ActivatedRoute } from '@angular/router';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { RequestTrendzComponent } from '@home/pages/trendz-analytics/request-trendz.component';
import { TrendzAnalyticsUnavailableComponent } from './trendz-analytics-unavailable.component';
import { DynamicMatDialog } from '@app/shared/components/dialog/dynamic/dynamic-dialog';

@Component({
  selector: 'tb-trendz-analytics',
  templateUrl: './trendz-analytics.component.html',
  styleUrls: ['./trendz-analytics.component.scss']
})
export class TrendzAnalyticsComponent extends PageComponent implements OnInit {

  @ViewChild('replaceComponentAnchor', {static: true}) replaceComponentAnchor: TbAnchorComponent;

  authState = getCurrentAuthState(this.store);
  trendzEnabled = this.authState.licenseVersion > 1 && this.authState.trendzEnabled;

  trendzSummary: TrendzSummary;
  trendzSynced = this.route.snapshot.data.trendzSynced;
  trendzViewTypes = Object.entries(TrendzViewType).map(([key, value]) => ({ key, value }));
  getMetricLink = getMetricLink;

  constructor(protected store: Store<AppState>,
              private trendzService: TrendzService,
              private route: ActivatedRoute,
              private dialog: DynamicMatDialog,
              private elementRef: ElementRef) {
    super();
  }

  ngOnInit(): void {
    if (!this.trendzEnabled) {
      const viewContainerRef = this.replaceComponentAnchor.viewContainerRef;
      viewContainerRef.clear();
      viewContainerRef.createComponent(RequestTrendzComponent);
    } else if(!this.trendzSynced) {
      this.dialog.open(TrendzAnalyticsUnavailableComponent, {
        containerElement: this.elementRef.nativeElement,
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-lg'],
      });
    } else {
      this.trendzService.getTrendzSummary().subscribe(trendzSummary => {
        if (trendzSummary) {
          this.trendzSummary = trendzSummary;
        }
      });
    }
  }
}

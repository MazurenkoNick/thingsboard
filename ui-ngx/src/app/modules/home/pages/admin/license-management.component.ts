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

import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { createManageSubscriptionUrl, PlanUiType, SubscriptionInfo } from '@shared/models/subscription.models';
import { ActivatedRoute } from '@angular/router';
import { AdminService } from '@core/http/admin.service';
import { MatDialog } from '@angular/material/dialog';
import {
  AddLicenseItemDialogComponent,
  AddLicenseItemDialogData
} from '@home/pages/admin/add-license-item-dialog.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-license-management',
  templateUrl: './license-management.component.html',
  styleUrls: ['./license-management.component.scss', './settings-card.scss']
})
export class LicenseManagementComponent extends PageComponent implements OnInit {

  get planWithoutWhiteLabeling(): boolean {
    return this.subscriptionInfo?.planUiType === PlanUiType.TbMaker || this.subscriptionInfo?.planUiType === PlanUiType.TbPrototype
  }

  PlanUiType = PlanUiType;

  subscriptionInfo: SubscriptionInfo;

  constructor(private route: ActivatedRoute,
              private adminService: AdminService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super();
  }

  ngOnInit() {
    this.subscriptionInfo = this.route.snapshot.data.subscriptionInfo;
  }

  manageSubscription($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    window.open(createManageSubscriptionUrl(this.subscriptionInfo), '_blank');
  }

  refreshLicenseInfo($event?: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.adminService.refreshLicense().subscribe(subscriptionInfo => {
      this.subscriptionInfo = subscriptionInfo;
    });
  }

  addDevices($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.addLicenseItem(this.translate.instant('subscription.devices'), true, { extraDeviceCount: 100 });
  }

  addEdges($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.addLicenseItem(this.translate.instant('subscription.edges'), true, { extraEdgeCount: 1 });
  }

  choosePilotPlan($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = `${this.subscriptionInfo.licenseServerEndpoint}/?changeSubscriptionPlan=true&subscriptionId=${this.subscriptionInfo.subscriptionId}&planUiType=${PlanUiType.TbPilot}`;
    window.open(url, '_blank');
  }

  addEdge($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.addLicenseItem(this.translate.instant('subscription.edge'), false, { edgeEnabled: true });
  }

  addTrendz($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.addLicenseItem(this.translate.instant('subscription.trendz'), false, { trendzEnabled: true });
  }

  isItemCritical(value: number, max: number): boolean {
    if (max && value) {
      return (value / max) >= 0.85;
    } else {
      return false;
    }
  }

  private addLicenseItem(itemName: string, add: boolean, items: any) {
    this.dialog.open<AddLicenseItemDialogComponent, AddLicenseItemDialogData, boolean>(AddLicenseItemDialogComponent,
      {
        disableClose: true,
        autoFocus: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          itemName,
          add,
          isPerpetual: this.subscriptionInfo.perpetual,
          licensePortalUrl: createManageSubscriptionUrl(this.subscriptionInfo, items)
        }
      }).afterClosed().subscribe(
        () => this.refreshLicenseInfo()
      );
  }
}

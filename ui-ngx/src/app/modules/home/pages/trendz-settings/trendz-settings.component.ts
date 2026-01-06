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

import { Component, ElementRef, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { TrendzService } from '@core/http/trendz.service';
import {
  TrendzConfiguration,
  TrendzStatus,
  TrendzSynchronization,
  TrendzSynchronizationResultTypeTranslationMap,
  TrendzSynchronizationStatus
} from '@shared/models/trendz-analytics.models';
import { ActivatedRoute } from '@angular/router';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AddLicenseItemDialogComponent,
  AddLicenseItemDialogData
} from '@home/pages/admin/add-license-item-dialog.component';
import { DynamicMatDialog } from '@shared/components/dialog/dynamic/dynamic-dialog';
import { TranslateService } from '@ngx-translate/core';
import { createManageSubscriptionUrl, SubscriptionInfo } from '@shared/models/subscription.models';
import { AdminService } from '@core/http/admin.service';
import { map, Observable, of, switchMap } from 'rxjs';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-trendz-settings',
  templateUrl: './trendz-settings.component.html',
  styleUrls: ['./trendz-settings.component.scss', '../admin/settings-card.scss']
})
export class TrendzSettingsComponent extends PageComponent implements OnInit {
  trendzSettingsForm: FormGroup;
  trendzSyncInfo: TrendzStatus = this.route.snapshot.data.trendzSyncInfo;
  TrendzSynchronizationStatus = TrendzSynchronizationStatus;
  TrendzSynchronizationResultTypeTranslationMap = TrendzSynchronizationResultTypeTranslationMap;

  authState = getCurrentAuthState(this.store);
  trendzEnabled = this.authState.licenseVersion < 2 || this.authState.trendzEnabled;

  private subscriptionInfo: SubscriptionInfo;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder,
              private trendzService: TrendzService,
              private route: ActivatedRoute,
              private dialogService: DialogService,
              private translate: TranslateService,
              private adminService: AdminService,
              private dialog: DynamicMatDialog,
              private elementRef: ElementRef) {
    super()
  }

  ngOnInit(): void {
    this.subscriptionInfo = this.route.snapshot.data.subscriptionInfo;
    this.trendzSettingsForm = this.fb.group({
      trendzUrl: [null, [Validators.required, Validators.pattern(/^(https?:\/\/)[^\s/$.?#].[^\s]*$/i)]],
      tbUrl: [null, [Validators.required, Validators.pattern(/^(https?:\/\/)[^\s/$.?#].[^\s]*$/i)]]
    });
    this.initTrendzSettings();
  }

  save(): void {
    this.saveAndSync(this.trendzService.getTrendzSyncResult());
  }

  retryDiscovery(): void {
    if(this.trendzSettingsForm.dirty) {
      this.dialogService.confirm(
        this.translate.instant('confirm-on-exit.title'),
        this.translate.instant('trendz-analytics.trendz-configuration-unsaved-message'),
        this.translate.instant('trendz-analytics.trendz-configuration-unsaved-continue-without-saving'),
        this.translate.instant('action.save')
      ).subscribe(result => {
        if(result) {
          this.saveAndSync(this.trendzService.connectToTrendz());
        } else {
          this.updateTrendzStatus(this.trendzService.connectToTrendz()).subscribe(trendzStatus => {
            this.trendzSyncInfo = trendzStatus;
          });
        }
      });
    } else {
      this.updateTrendzStatus(this.trendzService.connectToTrendz()).subscribe(trendzStatus => {
        this.trendzSyncInfo = trendzStatus;
      });
    }
  }

  private saveAndSync(syncResult$: Observable<TrendzSynchronization>): void {
    const trendzConfig: TrendzConfiguration = this.trendzSettingsForm.value;

    this.trendzService.saveTrendzConfig(trendzConfig).pipe(
      switchMap((savedConfig) => {
        this.trendzSettingsForm.patchValue(savedConfig);
        this.trendzSettingsForm.markAsPristine();
        return this.updateTrendzStatus(syncResult$);
      })
    ).subscribe(trendzStatus => {
      this.trendzSyncInfo = trendzStatus;
    });
  }

  private updateTrendzStatus(syncResult$: Observable<TrendzSynchronization>): Observable<TrendzStatus> {
    return syncResult$.pipe(
      switchMap(result => {
        const trendzStatus: TrendzStatus = {
          type: result.type,
          syncStatus: result.status,
          healthcheckStatus: TrendzSynchronizationStatus.NOT_AVAILABLE,
        };
        if (result.status === TrendzSynchronizationStatus.SYNCED) {
          return this.trendzService.performTrendzHealthcheck().pipe(
            map(healthcheckResult => {
              trendzStatus.healthcheckStatus = healthcheckResult.status;
              trendzStatus.type = healthcheckResult.type;
              return trendzStatus;
            })
          );
        }
        return of(trendzStatus);
      })
    );
  }

  retryHealthcheck(): void {
    this.trendzService.performTrendzHealthcheck().subscribe(result => {
      this.trendzSyncInfo.healthcheckStatus = result.status;
      this.trendzSyncInfo.type = result.type;
    });
  }

  private initTrendzSettings() {
    if (!this.trendzEnabled) {
      this.addTrendzLicense().subscribe(() => {
        this.adminService.refreshLicense().subscribe(subscriptionInfo => {
          this.subscriptionInfo = subscriptionInfo;
          this.trendzEnabled = this.subscriptionInfo.trendzEnabled;
          this.initTrendzSettings();
        });
      })
    } else {
      this.trendzService.getTrendzConfig().subscribe((settings) => {
        this.trendzSettingsForm.patchValue(settings);
      });
    }
  }

  private addTrendzLicense(): Observable<boolean> {
    return this.dialog.open<AddLicenseItemDialogComponent, AddLicenseItemDialogData, boolean>(AddLicenseItemDialogComponent,
      {
        disableClose: true,
        autoFocus: false,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        containerElement: this.elementRef.nativeElement,
        data: {
          itemName: this.translate.instant('subscription.trendz'),
          add: false,
          isPerpetual: this.subscriptionInfo.perpetual,
          licensePortalUrl: createManageSubscriptionUrl(this.subscriptionInfo, { trendzEnabled: true }),
          disabledClose: true
        }
      }).afterClosed()
  }
}

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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { TrendzService } from '@core/http/trendz.service';
import { TrendzConfiguration, TrendzSynchronizationResultType, TrendzSynchronizationStatus, TrendzSynchronizationResultTypeTranslationMap } from '@shared/models/trendz-analytics.models';
import { ActivatedRoute } from '@angular/router';
import { of, switchMap } from 'rxjs';

@Component({
  selector: 'tb-trendz-settings',
  templateUrl: './trendz-settings.component.html',
  styleUrls: ['./trendz-settings.component.scss', '../admin/settings-card.scss']
})
export class TrendzSettingsComponent extends PageComponent implements OnInit{
  trendzSettingsForm: FormGroup;
  trendzSyncInfo = this.route.snapshot.data.trendzSyncInfo;
  TrendzSynchronizationStatus = TrendzSynchronizationStatus;
  TrendzSynchronizationResultType = TrendzSynchronizationResultType;
  TrendzSynchronizationResultTypeTranslationMap = TrendzSynchronizationResultTypeTranslationMap;

  constructor(private fb: FormBuilder,
              private trendzService: TrendzService,
              private route: ActivatedRoute){
    super()
  }

  ngOnInit(): void {
    this.trendzSettingsForm = this.fb.group({
      trendzUrl: [null, [Validators.required, Validators.pattern(/^(https?:\/\/)[^\s/$.?#].[^\s]*$/i)]],
      tbUrl: [null, [Validators.required, Validators.pattern(/^(https?:\/\/)[^\s/$.?#].[^\s]*$/i)]]
    });

    this.trendzService.getTrendzConfig().subscribe((settings) => {
      this.trendzSettingsForm.patchValue(settings);
    });
  }

  save(): void {
    const trendzConfig: TrendzConfiguration = this.trendzSettingsForm.value;

    this.trendzService.saveTrendzConfig(trendzConfig).subscribe((savedConfig) => {
      if (savedConfig) {
        this.trendzSettingsForm.patchValue(savedConfig);
        this.trendzSettingsForm.markAsPristine();
      }
    });
  }

  retryDiscovery(): void {
    this.trendzService.connectToTrendz().pipe(
      switchMap(result => {
        if (result.status === TrendzSynchronizationStatus.SYNCED) {
          return this.trendzService.performTrendzHealthcheck();
        }
        return of(result);
      })).subscribe(result => {
        this.trendzSyncInfo = result;
    });
  }

  retryHealthcheck(): void {
    this.trendzService.performTrendzHealthcheck().subscribe(result => {
      this.trendzSyncInfo = result;
    });
  }
}

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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';
import { MobileApp, MobileAppBundleInfo } from '@shared/models/mobile-app.models';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { isNotEmptyStr } from '@core/utils';

export interface MobileAppConfigurationDialogData {
  afterAdd: boolean;
  androidApp: MobileApp;
  iosApp: MobileApp;
  bundle: MobileAppBundleInfo;
}

@Component({
  selector: 'tb-mobile-app-configuration-dialog',
  templateUrl: './mobile-app-configuration-dialog.component.html',
  styleUrls: ['./mobile-app-configuration-dialog.component.scss']
})
export class MobileAppConfigurationDialogComponent extends DialogComponent<MobileAppConfigurationDialogComponent> {

  private fileName = 'configs';

  notShowAgain = false;
  showDontShowAgain: boolean;

  gitRepositoryLink = 'git clone -b master https://github.com/thingsboard/flutter_thingsboard_pe_app.git';
  flutterRunCommand = `flutter run --dart-define-from-file ${this.fileName}.json`;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: MobileAppConfigurationDialogData,
              protected dialogRef: MatDialogRef<MobileAppConfigurationDialogComponent>,
              private importExportService: ImportExportService,
              ) {
    super(store, router, dialogRef);
    this.showDontShowAgain = this.data.afterAdd;
  }

  close(): void {
    if (this.notShowAgain && this.showDontShowAgain) {
      this.store.dispatch(new ActionPreferencesPutUserSettings({ notDisplayConfigurationAfterAddMobileBundle: true }));
      this.dialogRef.close(null);
    } else {
      this.dialogRef.close(null);
    }
  }

  createMarkDownCommand(commands: string): string {
    return this.createMarkDownSingleCommand(commands);
  }

  downloadSettings(): void {
    const settings: any = {
      thingsboardApiEndpoint: window.location.origin,
      appLinksUrlHost: window.location.host,
      appLinksUrlScheme: window.location.protocol.slice(0, -1),
      registrationRedirectUrlScheme: this.data.bundle.selfRegistrationParams?.redirect?.scheme ?? '',
      registrationRedirectUrlHost: this.data.bundle.selfRegistrationParams?.redirect?.host ?? '',
    };
    if (!!this.data.androidApp) {
      settings.androidApplicationId = this.data.androidApp.pkgName;
      settings.androidApplicationName = isNotEmptyStr(this.data.androidApp.title) ? this.data.androidApp.title : this.data.bundle.title;
      settings.thingsboardOAuth2CallbackUrlScheme = this.data.androidApp.pkgName + '.auth';
      settings.thingsboardAndroidAppSecret = this.data.androidApp.appSecret;
    }
    if (!!this.data.iosApp) {
      settings.iosApplicationId = this.data.iosApp.pkgName;
      settings.iosApplicationName = isNotEmptyStr(this.data.iosApp.title) ? this.data.iosApp.title : this.data.bundle.title;
      settings.thingsboardIosAppSecret = this.data.iosApp.appSecret;
    }
    this.importExportService.exportJson(settings, this.fileName);
  }

  private createMarkDownSingleCommand(command: string): string {
    return '```bash\n' +
      command +
      '{:copy-code}\n' +
      '```';
  }
}

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

import { Component, HostBinding } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserPasswordPolicy } from '@shared/models/settings.models';
import {
  passwordsMatchValidator,
  passwordStrengthValidator
} from '@shared/models/password.models';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

@Component({
  selector: 'tb-create-password',
  templateUrl: './create-password.component.html',
  styleUrls: ['./create-password.component.scss']
})
export class CreatePasswordComponent extends PageComponent {

  passwordPolicy: UserPasswordPolicy;
  createPassword: FormGroup;

  private activateToken: string;

  @HostBinding('class') class = 'tb-custom-css';

  constructor(private route: ActivatedRoute,
              private authService: AuthService,
              public wl: WhiteLabelingService,
              private fb: FormBuilder) {
    super();

    this.activateToken = this.route.snapshot.queryParams['activateToken'] || '';

    this.route.data
      .pipe(takeUntilDestroyed())
      .subscribe((data) => this.passwordPolicy = data['passwordPolicy']);

    this.buildCreatePasswordForm();
  }

  private buildCreatePasswordForm() {
    this.createPassword = this.fb.group({
      newPassword: ['', [Validators.required, passwordStrengthValidator(this.passwordPolicy)]],
      newPassword2: ['']
    }, {
      validators: [
        passwordsMatchValidator('newPassword', 'newPassword2'),
      ]
    });
  }

  onCreatePassword() {
    if (this.createPassword.invalid) {
      this.createPassword.markAllAsTouched();
    } else {
      this.authService.activate(
        this.activateToken,
        this.createPassword.get('newPassword').value, true).subscribe();
    }
  }
}

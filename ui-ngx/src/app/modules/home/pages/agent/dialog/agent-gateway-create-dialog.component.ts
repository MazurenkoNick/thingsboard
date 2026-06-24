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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { DeviceService } from '@core/http/device.service';
import { Device } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';

// Mirrors the 'Add Gateway' dialog of the gateways dashboard (gateways_dashboard.json).
@Component({
  selector: 'tb-agent-gateway-create-dialog',
  templateUrl: './agent-gateway-create-dialog.component.html',
  styleUrls: [],
  standalone: false
})
export class AgentGatewayCreateDialogComponent
  extends DialogComponent<AgentGatewayCreateDialogComponent, Device> {

  createFormGroup: UntypedFormGroup;
  entityType = EntityType;
  submitting = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private deviceService: DeviceService,
              private fb: UntypedFormBuilder,
              public dialogRef: MatDialogRef<AgentGatewayCreateDialogComponent, Device>) {
    super(store, router, dialogRef);
    this.createFormGroup = this.fb.group({
      name: ['', [Validators.required, Validators.pattern(/.*\S.*/)]],
      type: ['', [Validators.required]]
    });
  }

  cancel() {
    this.dialogRef.close(undefined);
  }

  create() {
    if (this.createFormGroup.invalid || this.submitting) {
      return;
    }
    this.submitting = true;
    const formValues = this.createFormGroup.value;
    const device = {
      name: formValues.name.trim(),
      type: formValues.type,
      additionalInfo: { gateway: true }
    } as Device;
    this.deviceService.saveDevice(device).subscribe({
      next: saved => this.dialogRef.close(saved),
      error: () => {
        this.submitting = false;
      }
    });
  }
}

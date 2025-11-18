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

import { AfterViewInit, Component, Inject, SkipSelf, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { map } from 'rxjs/operators';
import { ResourcesLibraryComponent } from "@home/components/resources/resources-library.component";
import { ErrorStateMatcher } from "@angular/material/core";
import { Resource, ResourceType } from "@shared/models/resource.models";
import { ResourceService } from "@core/http/resource.service";

export interface ResourcesDialogData {
  resources?: Resource;
  isAdd?: boolean;
}

@Component({
  selector: 'tb-resources-dialog',
  templateUrl: './resources-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ResourcesDialogComponent}],
  styleUrls: ['./resources-dialog.component.scss']
})
export class ResourcesDialogComponent extends DialogComponent<ResourcesDialogComponent, Resource> implements ErrorStateMatcher, AfterViewInit {

  readonly entityType = EntityType;

  ResourceType = ResourceType;

  isAdd = false;

  submitted = false;

  resources: Resource;

  @ViewChild('resourcesComponent', {static: true}) resourcesComponent: ResourcesLibraryComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<ResourcesDialogComponent, Resource>,
              @Inject(MAT_DIALOG_DATA) public data: ResourcesDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private resourceService: ResourceService) {
    super(store, router, dialogRef);

    if (this.data.isAdd) {
      this.isAdd = true;
    }

    if (this.data.resources) {
      this.resources = this.data.resources;
    }
  }

  ngAfterViewInit(): void {
    if (this.isAdd) {
      setTimeout(() => {
        this.resourcesComponent.entityForm.markAsDirty();
      }, 0);
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.resourcesComponent.entityForm.valid) {
      const resource = {...this.resourcesComponent.entityFormValue()};
      if (Array.isArray(resource.data)) {
        const resources = [];
        resource.data.forEach((data, index) => {
          resources.push({
            resourceType: resource.resourceType,
            data,
            fileName: resource.fileName[index],
            title: resource.title
          });
        });
        this.resourceService.saveResources(resources, {resendRequest: true}).pipe(
          map((response) => response[0])
        ).subscribe(result => this.dialogRef.close(result));
      } else {
        if (resource.resourceType !== ResourceType.GENERAL) {
          delete resource.descriptor;
        }
        this.resourceService.saveResource(resource).subscribe(result => this.dialogRef.close(result));
      }
    }
  }
}

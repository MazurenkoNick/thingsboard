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

import {
  AfterViewChecked,
  AfterViewInit,
  Component,
  HostBinding,
  OnDestroy,
  OnInit,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { Operation, Resource } from '@shared/models/security.models';
import { ReportTemplate } from '@shared/models/report.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { takeUntil } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-report-template-page',
  templateUrl: './report-template-page.component.html',
  styleUrls: ['./report-template-page.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportTemplatePageComponent extends PageComponent
  implements AfterViewInit, OnInit, OnDestroy, HasDirtyFlag, AfterViewChecked {

  get isDirty(): boolean {
    return this.isDirtyValue;
  }

  set isDirty(value: boolean) {
    this.isDirtyValue = value;
  }

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.REPORT_TEMPLATE, Operation.WRITE);

  isDirtyValue: boolean;

  isFullscreen = false;

  reportTemplate: ReportTemplate;

  private destroy$ = new Subject<void>();

  constructor(private route: ActivatedRoute,
              private userPermissionsService: UserPermissionsService) {
    super();
    this.route.data.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      () => {
        this.reset();
        this.init();
      }
    );
  }

  ngOnInit() {

  }

  ngAfterViewChecked(){

  }

  ngAfterViewInit() {

  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private init() {
    this.reportTemplate = this.route.snapshot.data.reportTemplate;
  }

  private reset(): void {

  }

}

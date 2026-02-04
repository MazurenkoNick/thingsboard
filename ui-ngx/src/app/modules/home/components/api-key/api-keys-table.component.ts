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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  effect,
  input,
  Renderer2,
  ViewChild,
  ViewContainerRef,
} from '@angular/core';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DatePipe } from '@angular/common';
import { ApiKeysTableConfig } from '@home/components/api-key/api-keys-table-config';
import { ApiKeyService } from '@core/http/api-key.service';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { TbPopoverService } from '@shared/components/popover.service';
import { UserId } from '@shared/models/id/user-id';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
    selector: 'tb-api-keys-table',
    templateUrl: './api-keys-table.component.html',
    styleUrls: ['./api-keys-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ApiKeysTableComponent {

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  active = input<boolean>();
  userId = input<UserId>();

  apiKeysTableConfig: ApiKeysTableConfig;

  constructor(
    private apiKeyService: ApiKeyService,
    private translate: TranslateService,
    private customTranslate: CustomTranslatePipe,
    private dialog: MatDialog,
    private datePipe: DatePipe,
    private cd: ChangeDetectorRef,
    private popoverService: TbPopoverService,
    private renderer: Renderer2,
    private viewContainerRef: ViewContainerRef,
    private userPermissionsService: UserPermissionsService,
  ) {
    effect(() => {
      if (this.active()) {
        this.apiKeysTableConfig = new ApiKeysTableConfig(
          this.apiKeyService,
          this.translate,
          this.customTranslate,
          this.dialog,
          this.datePipe,
          this.popoverService,
          this.renderer,
          this.viewContainerRef,
          this.userId(),
          this.userPermissionsService,
        );
        this.cd.markForCheck();
      }
    });
  }
}

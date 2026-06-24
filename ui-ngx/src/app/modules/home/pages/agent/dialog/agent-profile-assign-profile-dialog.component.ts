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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { AgentAppProfile, AgentApplicationType } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { EntityType } from '@shared/models/entity-type.models';
import { emptyPageData } from '@shared/models/page/page-data';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentAppProfileWizardComponent,
  AgentAppProfileWizardData
} from '@home/pages/agent/wizard/agent-app-profile-wizard.component';

export interface AgentProfileAssignProfileDialogData {
  selectedIds?: string[];
  lockedAppType?: AgentApplicationType;
}

@Component({
  selector: 'tb-agent-profile-assign-profile-dialog',
  templateUrl: './agent-profile-assign-profile-dialog.component.html',
  styleUrls: ['./agent-profile-assign-profile-dialog.component.scss'],
  standalone: false
})
export class AgentProfileAssignProfileDialogComponent
  extends DialogComponent<AgentProfileAssignProfileDialogComponent, string[]> {

  readonly EntityType = EntityType;

  formGroup: UntypedFormGroup;

  fetchAppProfiles = (searchText?: string): Observable<Array<BaseData<EntityId>>> => {
    const pageLink = new PageLink(20, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    const selectedIds: string[] = this.formGroup?.value?.appProfileIds ?? [];
    return this.agentService.getTenantAgentAppProfiles(pageLink, { ignoreLoading: true } as any).pipe(
      catchError(() => of(emptyPageData<AgentAppProfile>())),
      map(page => page.data.filter(p => !selectedIds.includes(p.id.id)))
    );
  };

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private agentService: AgentService,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder,
              @Inject(MAT_DIALOG_DATA) public data: AgentProfileAssignProfileDialogData,
              public dialogRef: MatDialogRef<AgentProfileAssignProfileDialogComponent, string[]>) {
    super(store, router, dialogRef);
    this.formGroup = this.fb.group({
      appProfileIds: [data?.selectedIds ?? []]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    const ids: string[] = this.formGroup.value?.appProfileIds ?? [];
    this.dialogRef.close(ids);
  }

  createAppProfile() {
    this.dismissAutocomplete();
    setTimeout(() => this.openAppProfileWizard(), 0);
  }

  private dismissAutocomplete() {
    const active = document.activeElement as HTMLElement | null;
    active?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    active?.blur();
  }

  private openAppProfileWizard() {
    this.dialog.open<AgentAppProfileWizardComponent, AgentAppProfileWizardData, AgentAppProfile>(
      AgentAppProfileWizardComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: { lockedAppType: this.data?.lockedAppType }
      }
    ).afterClosed().subscribe(saved => {
      if (saved) {
        const currentIds: string[] = this.formGroup.value?.appProfileIds ?? [];
        if (!currentIds.includes(saved.id.id)) {
          this.formGroup.get('appProfileIds').setValue([...currentIds, saved.id.id]);
        }
      }
    });
  }
}

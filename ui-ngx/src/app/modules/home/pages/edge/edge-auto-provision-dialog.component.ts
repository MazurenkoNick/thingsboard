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

import { Component, Inject, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { MatStepper } from '@angular/material/stepper';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import {
  AgentApplicationType,
  AgentProfile,
  AgentProfileInfo,
  AgentProvisionType
} from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  AgentProfileAddWizardComponent,
  AgentProfileAddWizardData
} from '@home/pages/agent/wizard/agent-profile-add-wizard.component';
import {
  AgentProfileAssignProfileDialogComponent,
  AgentProfileAssignProfileDialogData
} from '@home/pages/agent/dialog/agent-profile-assign-profile-dialog.component';

type ValidationState = 'idle' | 'validating' | 'valid' | 'invalid-strategy' | 'invalid-no-edge-app';

export interface AgentAutoProvisionDialogData {
  appType?: AgentApplicationType;
}

@Component({
  selector: 'tb-edge-auto-provision-dialog',
  templateUrl: './edge-auto-provision-dialog.component.html',
  styleUrls: ['./edge-auto-provision-dialog.component.scss'],
  standalone: false
})
export class EdgeAutoProvisionDialogComponent
  extends DialogComponent<EdgeAutoProvisionDialogComponent, boolean> {

  selectForm: UntypedFormGroup;

  filteredProfiles$: Observable<AgentProfileInfo[]>;

  selectedProfile: AgentProfile | null = null;

  validation: ValidationState = 'idle';

  searchText = '';
  loading = false;

  @ViewChild('stepper', { static: false }) stepper: MatStepper;

  stepperLabelPosition: Observable<'bottom' | 'end'>;

  get appType(): AgentApplicationType | null {
    return this.data?.appType ?? null;
  }

  private get i18nPrefix(): string {
    if (!this.appType) {
      return 'agent';
    }
    return this.appType === AgentApplicationType.GATEWAY ? 'gateway' : 'edge';
  }

  get titleKey(): string { return `${this.i18nPrefix}.auto-provision-title`; }
  get infoTitleKey(): string { return `${this.i18nPrefix}.auto-provision-info-title`; }
  get infoBodyKey(): string { return `${this.i18nPrefix}.auto-provision-info-body`; }
  get stepSelectKey(): string { return `${this.i18nPrefix}.auto-provision-step-select`; }
  get stepScriptKey(): string { return `${this.i18nPrefix}.auto-provision-step-script`; }
  get validKey(): string { return `${this.i18nPrefix}.auto-provision-valid`; }
  get invalidNoAppKey(): string { return `${this.i18nPrefix}.auto-provision-invalid-no-edge-app-profile`; }
  get successMessageKey(): string { return `${this.i18nPrefix}.auto-provision-success-message`; }
  get scriptHintKey(): string { return `${this.i18nPrefix}.auto-provision-script-hint`; }

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected translate: TranslateService,
              private agentService: AgentService,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private breakpointObserver: BreakpointObserver,
              @Inject(MAT_DIALOG_DATA) public data: AgentAutoProvisionDialogData,
              public dialogRef: MatDialogRef<EdgeAutoProvisionDialogComponent, boolean>) {
    super(store, router, dialogRef);

    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'end' : 'bottom'));

    this.selectForm = this.fb.group({
      agentProfile: [null]
    });

    this.filteredProfiles$ = this.selectForm.get('agentProfile').valueChanges.pipe(
      tap(value => {
        if (!value || typeof value === 'string') {
          this.selectedProfile = null;
          this.validation = 'idle';
        }
      }),
      map(value => (!value ? '' : (typeof value === 'string' ? value : value.name))),
      debounceTime(150),
      distinctUntilChanged(),
      switchMap(name => this.fetchEligibleProfiles(name)),
      share()
    );
  }

  onProfileFocus() {
    this.selectForm.get('agentProfile').updateValueAndValidity({ onlySelf: true, emitEvent: true });
  }

  fetchEligibleProfiles(searchText?: string): Observable<AgentProfileInfo[]> {
    this.searchText = searchText;
    // Fetch a generous page and filter client-side. Eligible = provisionType is set & not DISABLED.
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.agentService.getTenantAgentProfileInfos(pageLink, { ignoreLoading: true }).pipe(
      catchError(() => of(emptyPageData<AgentProfileInfo>())),
      map(page => (page.data || []).filter(p =>
        p.provisionType && p.provisionType !== AgentProvisionType.DISABLED
      ))
    );
  }

  displayProfile(profile?: AgentProfileInfo): string {
    return profile ? profile.name : '';
  }

  onProfileSelected(profile: AgentProfileInfo) {
    if (!profile) {
      return;
    }
    this.validateProfile(profile);
  }

  private validateProfile(profile: AgentProfileInfo) {
    this.selectedProfile = profile;

    if (!profile.provisionType || profile.provisionType === AgentProvisionType.DISABLED) {
      this.validation = 'invalid-strategy';
      return;
    }

    if (!this.appType) {
      this.validation = 'valid';
      return;
    }

    this.validation = 'validating';

    this.agentService.getAgentProfileAppProfileInfos(profile.id.id, { ignoreLoading: true } as any).pipe(
      map(profiles => (profiles || []).some(p => p?.appType === this.appType)),
      catchError(() => of(false))
    ).subscribe(hasEdge => {
      if (this.selectedProfile?.id?.id !== profile.id.id) {
        return; // stale response, user has moved on
      }
      this.validation = hasEdge ? 'valid' : 'invalid-no-edge-app';
    });
  }

  canProceed(): boolean {
    return this.validation === 'valid' && !!this.selectedProfile;
  }

  createNewProfile($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dismissAutocomplete();
    setTimeout(() => {
      this.dialog.open<AgentProfileAddWizardComponent, AgentProfileAddWizardData, AgentProfile>(
        AgentProfileAddWizardComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            defaults: { provisionType: AgentProvisionType.ALLOW_CREATE_NEW_AGENTS },
            lockedAppType: this.appType
          }
        }
      ).afterClosed().subscribe(saved => {
        if (saved) {
          this.agentService.getAgentProfileInfoById(saved.id.id).subscribe(profile => {
            this.selectForm.get('agentProfile').setValue(profile, { emitEvent: false });
            this.validateProfile(profile);
          });
        }
      });
    }, 0);
  }

  private dismissAutocomplete() {
    const active = document.activeElement as HTMLElement | null;
    active?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    active?.blur();
  }

  openSelectedProfile() {
    if (!this.selectedProfile?.id?.id) {
      return;
    }
    this.router.navigateByUrl(`/edgeManagement/profiles/agent/${this.selectedProfile.id.id}`);
    this.dialogRef.close(true);
  }

  assignProfileToSelected($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const profileId = this.selectedProfile?.id?.id;
    if (!profileId) {
      return;
    }
    this.agentService.getAgentProfileAppProfileInfos(profileId, { ignoreLoading: true } as any).subscribe((profiles) => {
      const selectedIds = (profiles || []).map(p => p.id.id);
      this.dialog.open<AgentProfileAssignProfileDialogComponent, AgentProfileAssignProfileDialogData, string[]>(
        AgentProfileAssignProfileDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog'],
          data: { selectedIds, lockedAppType: this.appType }
        }
      ).afterClosed().subscribe(nextIds => {
        if (nextIds !== null && nextIds !== undefined) {
          this.agentService.assignAppProfilesToAgentProfile(profileId, nextIds).subscribe(() => {
            if (this.selectedProfile) {
              this.validateProfile(this.selectedProfile as AgentProfileInfo);
            }
          });
        }
      });
    });
  }

  dockerCommand = '';

  proceedToScript() {
    if (!this.canProceed()) {
      return;
    }
    // Refetch the full profile (with provisionKey/Secret) and load the resolved install command.
    this.loading = true;
    this.agentService.getAgentProfileById(this.selectedProfile.id.id).subscribe({
      next: full => {
        this.selectedProfile = full;
        this.agentService.getAgentProvisionInstructions(full.id.id).subscribe({
          next: res => {
            this.dockerCommand = res?.instructions || '';
            this.loading = false;
            this.stepper.next();
          },
          error: () => {
            this.dockerCommand = '';
            this.loading = false;
            this.stepper.next();
          }
        });
      },
      error: () => { this.loading = false; }
    });
  }

  hasCredentials(): boolean {
    return !!(this.selectedProfile?.provisionKey && this.selectedProfile?.provisionSecret);
  }

  onCopied() {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('agent.install-command-copied-message'),
      type: 'success',
      duration: 1000,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  cancel() {
    this.dialogRef.close(false);
  }

  goToProfile() {
    this.openSelectedProfile();
  }
}

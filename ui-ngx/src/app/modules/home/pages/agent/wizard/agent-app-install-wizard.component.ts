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
  Component,
  EventEmitter,
  Inject,
  Input,
  Optional,
  Output
} from '@angular/core';
import { Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { StepperOrientation } from '@angular/material/stepper';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MediaBreakpoints } from '@shared/models/constants';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AgentApplication,
  AgentAppEvent,
  AgentApplicationType,
  AgentInfo
} from '@shared/models/agent.models';
import { openAgentAppEventProgress } from '@home/pages/agent/util/agent-app-event-progress';
import { AgentAppWizardLoaderService } from '@home/pages/agent/wizard/agent-app-wizard-loader.service';
import { AgentAppWizardSubmitService } from '@home/pages/agent/wizard/agent-app-wizard-submit.service';
import {
  AGENT_INSTALL_WIZARD_BACK,
  AgentAppInstallWizardData,
  AgentAppUpgradeResult,
  AgentAppWizardFinish
} from '@home/pages/agent/wizard/agent-app-wizard.models';

// Re-exported so existing call sites keep importing these from this module.
export { AGENT_INSTALL_WIZARD_BACK } from '@home/pages/agent/wizard/agent-app-wizard.models';
export type {
  AgentAppInstallWizardData,
  AgentAppUpgradeResult
} from '@home/pages/agent/wizard/agent-app-wizard.models';

// Thin dispatcher: keeps the public contract (selector, inputs, finished /
// cancelled outputs, dialog plumbing) and renders the per-mode flow component.
// All mode-specific steps and logic live in the flow components; this owns the
// dialog/embedded completion (close + optional navigate / progress dialog).
@Component({
  selector: 'tb-agent-app-install-wizard',
  templateUrl: './agent-app-install-wizard.component.html',
  styleUrls: ['./agent-app-install-wizard.component.scss'],
  standalone: false,
  providers: [AgentAppWizardLoaderService, AgentAppWizardSubmitService]
})
export class AgentAppInstallWizardComponent {

  @Input() agentId: string;
  @Input() agent: AgentInfo;
  @Input() mode: 'install' | 'update' | 'upgrade' = 'install';
  @Input() application: AgentApplication | null = null;
  @Input() lockedType: AgentApplicationType | null = null;
  @Input() lockedRelatedEntity: EntityId | null = null;
  @Input() navigateToAgentOnFinish = false;
  @Input() selectAgent = false;
  @Input() embedded = false;
  @Input() showBack = false;

  @Output() finished = new EventEmitter<AgentAppEvent | null>();
  @Output() cancelled = new EventEmitter<void>();

  stepperOrientation: Observable<StepperOrientation>;
  stepperLabelPosition: Observable<'bottom' | 'end'>;

  constructor(private router: Router,
              private dialog: MatDialog,
              private breakpointObserver: BreakpointObserver,
              @Optional() @Inject(MAT_DIALOG_DATA) public data: AgentAppInstallWizardData | null,
              @Optional() public dialogRef: MatDialogRef<AgentAppInstallWizardComponent, AgentAppEvent | null> | null) {
    if (this.data) {
      this.agentId = this.data.agentId;
      this.agent = this.data.agent;
      this.mode = this.data.mode || 'install';
      this.application = this.data.application || null;
      this.lockedType = this.data.lockedType || null;
      this.lockedRelatedEntity = this.data.lockedRelatedEntity || null;
      this.navigateToAgentOnFinish = !!this.data.navigateToAgentOnFinish;
      this.selectAgent = !!this.data.selectAgent;
      this.showBack = !!this.data.showBack;
    }
    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'horizontal' : 'vertical'));
    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({ matches }) => matches ? 'end' : 'bottom'));
  }

  onFinished(f: AgentAppWizardFinish) {
    this.closeOrEmit(f.event);
    if (f.closeOnly) {
      return;
    }
    if (this.navigateToAgentOnFinish) {
      const agentId = f.application?.agentId?.id || this.agentId;
      if (agentId) {
        this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), agentId));
      }
      return;
    }
    if (f.application && f.event) {
      openAgentAppEventProgress(this.dialog, f.application, f.event).subscribe();
    }
  }

  onCancelled(result: AgentAppUpgradeResult | null) {
    if (result) {
      // Profile already committed via the upgrade step: let the caller refresh.
      this.closeOrEmit(result as any);
    } else if (this.dialogRef) {
      this.dialogRef.close(null);
    } else {
      this.cancelled.emit();
    }
  }

  onBack() {
    if (this.dialogRef) {
      this.dialogRef.close(AGENT_INSTALL_WIZARD_BACK as any);
    }
  }

  private closeOrEmit(result: AgentAppEvent | null) {
    if (this.dialogRef) {
      this.dialogRef.close(result);
    } else {
      this.finished.emit(result);
    }
  }
}

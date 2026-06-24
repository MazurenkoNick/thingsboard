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

import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AgentAppEvent, AgentApplication } from '@shared/models/agent.models';
import {
  AgentAppEventProgressDialogComponent,
  AgentAppEventProgressDialogData
} from '@home/pages/agent/dialog/agent-app-event-progress-dialog.component';

/**
 * Opens the event-progress dialog for an application-level action. All
 * action dispatches (install, update, upgrade, restart, delete) route
 * through here so the "monitor progress" UX is consistent.
 *
 * Returns the dialog's afterClosed observable so the caller can trigger a
 * list refresh (or navigate away) once the user dismisses the dialog.
 */
export function openAgentAppEventProgress(
  dialog: MatDialog,
  application: AgentApplication,
  event: AgentAppEvent
): Observable<boolean> {
  return dialog.open<AgentAppEventProgressDialogComponent, AgentAppEventProgressDialogData, boolean>(
    AgentAppEventProgressDialogComponent, {
      disableClose: false,
      panelClass: ['tb-dialog'],
      data: { application, event }
    }
  ).afterClosed();
}

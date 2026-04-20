///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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

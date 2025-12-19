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

import { Component, ElementRef, Inject, OnDestroy, OnInit, Renderer2, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionStatus, AuditLog } from '@shared/models/audit-log.models';
import { Ace } from 'ace-builds';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { getAce, updateEditorSize } from '@shared/models/ace/ace.models';
import { Observable, of } from 'rxjs';
import { isObject } from '@core/utils';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { beautifyJs } from '@shared/models/beautify.models';

export interface AuditLogDetailsDialogData {
  auditLog: AuditLog;
}

@Component({
  selector: 'tb-audit-log-details-dialog',
  templateUrl: './audit-log-details-dialog.component.html',
  styleUrls: ['./audit-log-details-dialog.component.scss']
})
export class AuditLogDetailsDialogComponent extends DialogComponent<AuditLogDetailsDialogComponent> implements OnInit, OnDestroy {

  @ViewChild('actionDataEditor', {static: true})
  actionDataEditorElmRef: ElementRef;

  @ViewChild('failureDetailsEditor', {static: true})
  failureDetailsEditorElmRef: ElementRef;

  displayFailureDetails: boolean;

  private auditLog: AuditLog;
  private aceEditors: Ace.Editor[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AuditLogDetailsDialogData,
              public dialogRef: MatDialogRef<AuditLogDetailsDialogComponent>,
              private renderer: Renderer2) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.auditLog = this.data.auditLog;
    this.displayFailureDetails = this.auditLog.actionStatus === ActionStatus.FAILURE;

    this.createEditor(this.actionDataEditorElmRef, this.auditLog.actionData);
    if (this.displayFailureDetails) {
      this.createEditor(this.failureDetailsEditorElmRef, this.auditLog.actionFailureDetails);
    }
  }

  ngOnDestroy(): void {
    this.aceEditors.forEach(editor => editor.destroy());
    super.ngOnDestroy();
  }

  createEditor(editorElementRef: ElementRef, content: string | object): void {
    const editorElement = editorElementRef.nativeElement;
    let mode = 'java';
    let content$: Observable<string> = null;
    let contentType = ContentType.TEXT;
    if (content && isObject(content)) {
      contentType = ContentType.JSON;
      mode = contentTypesMap.get(contentType).code;
      content$ = beautifyJs(JSON.stringify(content), {indent_size: 2});
    }
    if (!content$) {
      content$ = of(content as string);
    }
    content$.subscribe((processedContent) => {
      const isJSON = contentType === ContentType.JSON
      const editorOptions: Partial<Ace.EditorOptions> = {
        mode: `ace/mode/${mode}`,
        theme: 'ace/theme/github',
        showGutter: isJSON,
        showFoldWidgets: true,
        foldStyle: 'markbeginend',
        showPrintMargin: false,
        readOnly: true,
        enableSnippets: false,
        enableBasicAutocompletion: false,
        enableLiveAutocompletion: false,
      };
      getAce().subscribe(
        (ace) => {
          const editor = ace.edit(editorElement, editorOptions);
          this.aceEditors.push(editor);
          editor.session.setUseWrapMode(false);
          editor.setValue(processedContent, -1);
          updateEditorSize(editorElement, processedContent, editor, this.renderer, {showGutter: isJSON});
        }
      )
    });
  }

}

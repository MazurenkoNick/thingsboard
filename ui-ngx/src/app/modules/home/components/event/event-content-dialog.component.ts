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

import { Ace } from 'ace-builds';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { getAce, updateEditorSize } from '@shared/models/ace/ace.models';
import { Observable } from 'rxjs/internal/Observable';
import { beautifyJs } from '@shared/models/beautify.models';
import { of } from 'rxjs';
import { base64toString, isLiteralObject } from '@core/utils';

export interface EventContentDialogData {
  content: string;
  title: string;
  contentType: ContentType;
}

@Component({
  selector: 'tb-event-content-dialog',
  templateUrl: './event-content-dialog.component.html',
  styleUrls: ['./event-content-dialog.component.scss']
})
export class EventContentDialogComponent extends DialogComponent<EventContentDialogComponent> implements OnInit, OnDestroy {

  @ViewChild('eventContentEditor', {static: true})
  eventContentEditorElmRef: ElementRef;

  title: string;
  showBorder = false;

  private contentType: ContentType;
  private aceEditor: Ace.Editor;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EventContentDialogData,
              protected dialogRef: MatDialogRef<EventContentDialogComponent>,
              private renderer: Renderer2) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.title = this.data.title;
    this.contentType = this.data.contentType;

    this.createEditor(this.eventContentEditorElmRef, this.data.content);
  }

  ngOnDestroy(): void {
    this.aceEditor?.destroy();
    super.ngOnDestroy();
  }

  private isJson(str: string) {
    try {
      return isLiteralObject(JSON.parse(str));
    } catch (e) {
      return false;
    }
  }

  private createEditor(editorElementRef: ElementRef, content: string) {
    const editorElement = editorElementRef.nativeElement;
    let mode = 'java';
    let content$: Observable<string> = null;
    if (this.contentType) {
      mode = contentTypesMap.get(this.contentType).code;
      if (this.contentType === ContentType.JSON && content) {
        content$ = beautifyJs(content, {indent_size: 2});
      } else if (this.contentType === ContentType.TEXT && content) {
        try {
          const decodedData = base64toString(content);
          content$ = of(decodedData);
        } catch (e) {/**/}
      }
    }
    if (!content$) {
      content$ = of(content);
    }

    content$.subscribe((processedContent) => {
      const isJSON = mode === 'json' && processedContent !== '{}';
      this.showBorder = isJSON;
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
          this.aceEditor = ace.edit(editorElement, editorOptions);
          this.aceEditor.session.setUseWrapMode(false);
          this.aceEditor.setValue(processedContent, -1);
          updateEditorSize(editorElement, processedContent, this.aceEditor, this.renderer, {showGutter: isJSON});
        }
      )
    });
  }
}

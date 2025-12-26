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

import { Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, Renderer2, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Ace } from 'ace-builds';
import { isDefinedAndNotNull, isUndefined } from '@core/utils';
import { getAce, updateEditorSize } from '@shared/models/ace/ace.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-json-object-view',
  templateUrl: './json-object-view.component.html',
  styleUrls: ['./json-object-view.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsonObjectViewComponent),
      multi: true
    }
  ]
})
export class JsonObjectViewComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @ViewChild('jsonViewer', {static: true})
  jsonViewerElmRef: ElementRef;

  private jsonViewer: Ace.Editor;
  private viewerElement: Ace.Editor;
  private propagateChange = null;
  private modelValue: any;
  private contentValue: string;

  @Input() label: string;

  @Input() fillHeight: boolean;

  @Input() editorStyle: { [klass: string]: any };

  @Input() sort: (key: string, value: any) => any;

  @Input()
  @coerceBoolean()
  autoWidth: boolean

  @Input()
  @coerceBoolean()
  autoHeight: boolean

  constructor(private renderer: Renderer2) {
  }

  ngOnInit(): void {
    this.viewerElement = this.jsonViewerElmRef.nativeElement;
    const editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/java',
      theme: 'ace/theme/github',
      showGutter: false,
      showPrintMargin: false,
      readOnly: true,
      enableSnippets: false,
      enableBasicAutocompletion: false,
      enableLiveAutocompletion: false
    };

    getAce().subscribe(
      (ace) => {
        this.jsonViewer = ace.edit(this.viewerElement, editorOptions);
        this.jsonViewer.session.setUseWrapMode(false);
        this.jsonViewer.setValue(this.contentValue ? this.contentValue : '', -1);
        if (this.contentValue && (this.autoWidth || this.autoHeight)) {
          updateEditorSize(this.viewerElement, this.contentValue, this.jsonViewer, this.renderer, {
            ignoreHeight: !this.autoHeight,
            ignoreWidth: !this.autoWidth
          });
        }
      }
    );
  }

  ngOnDestroy(): void {
    this.jsonViewer?.destroy();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  writeValue(value: any): void {
    this.modelValue = value;
    this.contentValue = '';
    try {
      if (isDefinedAndNotNull(this.modelValue)) {
        this.contentValue = JSON.stringify(this.modelValue, isUndefined(this.sort) ? undefined :
          (key, objectValue) => {
            return this.sort(key, objectValue);
          }, 2);
      }
    } catch (e) {
      console.error(e);
    }
    if (this.jsonViewer) {
      this.jsonViewer.setValue(this.contentValue ? this.contentValue : '', -1);
      if (this.contentValue && (this.autoWidth || this.autoHeight)) {
        updateEditorSize(this.viewerElement, this.contentValue, this.jsonViewer, this.renderer, {
          ignoreHeight: !this.autoHeight,
          ignoreWidth: !this.autoWidth
        });
      }
    }
  }

}

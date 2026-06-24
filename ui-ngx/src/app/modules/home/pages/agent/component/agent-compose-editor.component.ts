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
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';
import { confineWheelToAceEditor } from '@home/pages/agent/util/ace-wheel-confine';

@Component({
  selector: 'tb-agent-compose-editor',
  template: '<div class="yaml-editor yaml-editor-ace" #host></div>',
  styleUrls: ['./agent-compose-editor.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class AgentComposeEditorComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() value = '';
  @Input() readOnly = false;

  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('host', { static: true }) hostRef: ElementRef<HTMLElement>;

  private editor: Ace.Editor | null = null;
  private settingValue = false;

  ngAfterViewInit(): void {
    getAce().subscribe((ace) => {
      const editor: Ace.Editor = ace.edit(this.hostRef.nativeElement);
      editor.setTheme('ace/theme/textmate');
      editor.session.setMode('ace/mode/yaml');
      editor.session.setUseWrapMode(false);
      editor.setShowPrintMargin(false);
      (editor as any).setOption('scrollPastEnd', false);
      editor.renderer.setScrollMargin(0, 0, 0, 0);
      this.forceFontSize(editor, 12);
      editor.setOption('tabSize', 2);
      editor.setOption('useSoftTabs', true);
      editor.setOption('showLineNumbers', true);
      editor.setOption('highlightActiveLine', false);
      editor.setValue(this.value || '', -1);
      editor.getSession().on('change', () => {
        this.settingValue = true;
        this.valueChange.emit(editor.getValue());
        this.settingValue = false;
      });
      this.editor = editor;
      this.applyReadOnly();
      confineWheelToAceEditor((editor as any).container, editor,
        () => editor.isFocused());
      setTimeout(() => editor.resize(true), 0);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.editor) {
      return;
    }
    if (changes.value && !this.settingValue) {
      const current = this.editor.getValue();
      if (current !== (this.value || '')) {
        this.editor.setValue(this.value || '', -1);
      }
    }
    if (changes.readOnly) {
      this.applyReadOnly();
    }
  }

  ngOnDestroy(): void {
    if (this.editor) {
      try { this.editor.destroy(); } catch (e) { /* no-op */ }
      this.editor = null;
    }
  }

  private applyReadOnly(): void {
    if (!this.editor) {
      return;
    }
    this.editor.setReadOnly(this.readOnly);
    const cursorLayer = (this.editor.renderer as any).$cursorLayer;
    if (cursorLayer?.element?.style) {
      cursorLayer.element.style.display = this.readOnly ? 'none' : '';
    }
  }

  private forceFontSize(editor: Ace.Editor, px: number): void {
    const container = (editor as any).container as HTMLElement | undefined;
    if (container?.style) {
      container.style.setProperty('font-size', `${px}px`, 'important');
    }
    editor.setFontSize(px);
    const renderer: any = editor.renderer;
    if (typeof renderer.updateFontSize === 'function') {
      renderer.updateFontSize();
    }
    if (typeof renderer.onResize === 'function') {
      renderer.onResize(true);
    }
  }
}

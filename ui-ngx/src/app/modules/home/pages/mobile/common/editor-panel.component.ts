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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormControl } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { EditorOptions } from 'tinymce';

@Component({
    selector: 'tb-release-notes-panel',
    templateUrl: './editor-panel.component.html',
    styleUrls: ['./editor-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EditorPanelComponent implements OnInit {

  @Input()
  disabled: boolean;

  @Input()
  content: string;

  @Input()
  title: string;

  @Input()
  popover: TbPopoverComponent<EditorPanelComponent>;

  @Output()
  editorContentApplied = new EventEmitter<string>();

  editorControl: FormControl<string>;

  tinyMceOptions: Partial<EditorOptions> = {
    base_url: '/assets/tinymce',
    suffix: '.min',
    plugins: ['link', 'table', 'image', 'lists', 'fullscreen'],
    menubar: 'edit insert view format',
    toolbar: ['fontfamily fontsize | bold italic underline strikethrough forecolor backcolor',
      'alignleft aligncenter alignright alignjustify | bullist | link table image | fullscreen'],
    toolbar_mode: 'sliding',
    height: 400,
    autofocus: false,
    branding: false,
    promotion: false,
    resize: false,
    setup: (editor) => {
      editor.on('PostRender', function() {
        const container = document.querySelector('.tox.tox-tinymce-aux');
        const styleSheet = document.createElement('style');
        styleSheet.innerText = `
          .tox-tiered-menu .tox-menu {
            width: fit-content;
            max-width: min(80%, 440px);
            @media screen and (max-width: 510px) {
              max-width: calc(100% - 64px);
            }
            media screen and (min-width: 511px) and (max-width: 548px) {
              max-width: calc(100% - 84px);
            }
            media screen and (min-width: 549px) and (max-width: 599px) {
              max-width: calc(100% - 104px);
            }
          }
          .tox-tiered-menu .tox-menu .tox-collection__item-label {
            word-break: normal;
          }
          @media screen and (max-width: 890px) {
            .tox-tiered-menu > .tox-collection--list:not(:first-child) {
              left: auto !important;
              right: 0 !important;
            }
          }
        `;
        container.prepend(styleSheet);
      });
    },
    relative_urls: false,
    urlconverter_callback: (url) => url
  };

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.editorControl = this.fb.control(this.content);
    if (this.disabled) {
      this.editorControl.disable({emitEvent: false});
      this.tinyMceOptions.toolbar = false;
      this.tinyMceOptions.menubar = false;
      this.tinyMceOptions.statusbar = false;
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    if (this.editorControl.valid) {
      this.editorContentApplied.emit(this.editorControl.value);
    }
  }
}

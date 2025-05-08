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

import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { RichTextReportComponentConfig } from '@app/shared/public-api';
import { AbstractReportComponentConfig } from '@home/pages/report/components/report-component-config.component';
import { Editor, EditorOptions } from 'tinymce';

@Component({
  selector: 'tb-report-rich-text-config',
  templateUrl: './rich-text-config.component.html',
  styleUrls: ['./report-component-config.scss'],
  encapsulation: ViewEncapsulation.None
})
export class RichTextConfigComponent extends AbstractReportComponentConfig<RichTextReportComponentConfig> {

  settingsTab: 'content' | 'data' | 'layout' = 'content';

  tinyMceOptions: Partial<EditorOptions> = {
    base_url: '/assets/tinymce',
    suffix: '.min',
    plugins: ['link', 'table', 'image', 'lists', 'code', 'fullscreen'],
    menubar: 'edit insert tools view format table',
    toolbar: 'undo redo | fontfamily fontsize blocks | bold italic  strikethrough | forecolor backcolor ' +
      '| link table image | alignleft aligncenter alignright alignjustify  ' +
      '| numlist bullist | outdent indent  | removeformat | code | fullscreen',
    toolbar_mode: 'sliding',
    height: 400,
    autofocus: false,
    branding: false,
    promotion: false,
    relative_urls: false,
    contextmenu: ['link', 'variables'],
    urlconverter_callback: (url) => url,
    setup: (editor) => this.setupEditor(editor)
  };

  private setupEditor(editor: Editor) {
    editor.ui.registry.addAutocompleter('variables', {
      trigger: '$',
      minChars: 0,
      columns: 'auto',
      onAction: (autocompleteApi, rng, value) => {
        editor.selection.setRng(rng);
        editor.insertContent(value);
        autocompleteApi.hide();
      },
      fetch: (pattern) => {
        return new Promise((resolve) => {
          const results= ['active'].map((val) => ({
            type: 'cardmenuitem',
            value: '${'+val+'}',
            label: val,
            items: [
              {
                type: 'cardtext',
                text: val,
                name: 'char_name'
              }
            ]
          } as any));
          resolve(results);
        });
      }
    });
    editor.ui.registry.addNestedMenuItem('variables', {
      text: 'Variable...',
      getSubmenuItems: () => {
        return [
          {
            text: 'active',
            type: 'menuitem',
            onAction: () => {
              editor.insertContent('${active}');
            }
          }
        ];
      }
    });

    editor.ui.registry.addContextMenu('variables', {
      update: element => {
        return [
          {
            text: 'Variable...',
            type: 'submenu',
            getSubmenuItems: () => {
              return [
                {
                  text: 'active',
                  type: 'item',
                  onAction: () => {
                    editor.insertContent('${active}');
                  }
                }
              ];
            }
          }
        ];
      }
    });
  }

  protected buildForm(reportComponentConfig: RichTextReportComponentConfig): FormGroup {
    return this.fb.group({
      value: [reportComponentConfig.value, []],
      dataSources: [reportComponentConfig.dataSources, []]
    });
  }

}

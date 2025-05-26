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

import { Component, inject, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { RichTextReportComponentConfig } from '@app/shared/public-api';
import { AbstractReportComponentConfig } from '@home/pages/report/components/report-component-config.component';
import { Editor, EditorOptions } from 'tinymce';
import { GetImageSrcCallback, HtmlWithImagePipe, SetImageSrcCallback } from '@shared/pipe/html-with-image.pipe';
import { MatDialog } from '@angular/material/dialog';
import {
  ReportImageDialogComponent,
  ReportImageData
} from '@home/pages/report/components/report-image-dialog.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-report-rich-text-config',
  templateUrl: './rich-text-config.component.html',
  styleUrls: ['./report-component-config.scss'],
  encapsulation: ViewEncapsulation.None
})
export class RichTextConfigComponent extends AbstractReportComponentConfig<RichTextReportComponentConfig> {

  translate = inject(TranslateService);
  htmlWithImagePipe = inject(HtmlWithImagePipe);
  dialog =  inject(MatDialog);

  settingsTab: 'content' | 'data' | 'layout' = 'content';

  tinyMceOptions: Partial<EditorOptions> = {
    base_url: '/assets/tinymce',

    body_class: 'tb-report-component',
    content_css: ['/report-component.css'],
    suffix: '.min',
    plugins: ['link', 'table', 'lists', 'code', 'fullscreen'],
    menubar: 'edit customInsert tools view format table',
    menu: {
     customInsert: {
        title: 'Insert',
        items: 'tb-image link inserttable | hr'
      }
    },
    font_family_formats: 'Roboto=Roboto; Monospaced=monospace; Sans Serif=sans-serif; Serif=serif;',
    toolbar: 'undo redo | fontfamily fontsize blocks | bold italic  strikethrough | forecolor backcolor ' +
      '| link table tb-image | alignleft aligncenter alignright alignjustify  ' +
      '| numlist bullist | outdent indent  | removeformat | code | fullscreen',
    toolbar_mode: 'sliding',
    height: 400,
    autofocus: false,
    branding: false,
    promotion: false,
    relative_urls: false,
    automatic_uploads: false,
    images_replace_blob_uris: false,
    contextmenu: ['link', 'variables'],
    urlconverter_callback: (url) => url,
    setup: (editor) => this.setupEditor(editor)
  };

  private setImageSrcCallback: SetImageSrcCallback = (image, origUrl, newUrl) => {
    if (origUrl !== newUrl) {
      image.setAttribute('data-mce-src', origUrl);
    }
  }

  private getImageSrcCallback: GetImageSrcCallback = (image) => {
    if (image.hasAttribute('data-mce-src')) {
      return image.getAttribute('data-mce-src');
    } else {
      return image.getAttribute('src');
    }
  }

  private setupEditor(editor: Editor) {

    editor.ui.registry.addToggleButton('tb-image', {
      icon: 'image',
      tooltip: this.translate.instant('report-template.insert-update-image'),
      onSetup: (api)  => {
        const imgElm = editor.selection.getNode() as HTMLElement;
        api.setActive(imgElm && imgElm.nodeName === 'IMG');
        const editorEventCallback = (eventApi) => {
          api.setActive(eventApi.element.nodeName === 'IMG');
        };
        editor.on('NodeChange', editorEventCallback);
        return () => editor.off('NodeChange', editorEventCallback);
      },
      onAction: () => {
        this.editorImageAction(editor);
      }
    });

    editor.ui.registry.addMenuItem('tb-image', {
      icon: 'image',
      text: this.translate.instant('report-template.image') + '...',
      onAction: () => {
        this.editorImageAction(editor);
      }
    });

    editor.on('SetContent', (event) => {
      this.htmlWithImagePipe.transform(editor.getBody(), {
        getImageSrcCallback: this.getImageSrcCallback,
        setImageSrcCallback: this.setImageSrcCallback
      }).subscribe();
    });
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

  private editorImageAction(editor: Editor) {
    const imageData = this.extractImageData(editor);
    this.dialog.open<ReportImageDialogComponent, ReportImageData,
      ReportImageData>(ReportImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: imageData
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.insertOrUpdateImage(editor, result);
      }
    });
  }

  private extractImageData(editor: Editor): ReportImageData {
    const elm = editor.selection.getNode() as HTMLElement;
    const imgElm = elm.nodeName === 'IMG' ? elm as HTMLImageElement : null;
    let imageUrl: string = null;
    let width: number = null;
    let height: number = null;
    if (imgElm) {
      imageUrl = this.getImageSrcCallback(imgElm);
      if (imgElm.hasAttribute('width')) {
        const value = imgElm.getAttribute('width').replace(/px$/, '');
        width = Number.parseInt(value);
      }
      if (imgElm.hasAttribute('height')) {
        const value = imgElm.getAttribute('height').replace(/px$/, '');
        height = Number.parseInt(value);
      }
    }
    return {
      imageUrl,
      width,
      height
    }
  }

  private insertOrUpdateImage(editor: Editor, data: ReportImageData) {
    editor.undoManager.transact(() => {
      const elm = editor.selection.getNode() as HTMLElement;
      const imgElm = elm.nodeName === 'IMG' ? elm : null;
      if (imgElm) {
        if (data.imageUrl) {
          imgElm.setAttribute('src', data.imageUrl);
          imgElm.setAttribute('data-mce-src', data.imageUrl);
          imgElm.setAttribute('width', data.width ? (data.width + 'px') : null);
          imgElm.setAttribute('height', data.height ? (data.height + 'px') : null);
          editor.dom.setAttrib(imgElm, 'data-mce-id', '__mceupd');
          editor.focus();
          editor.selection.setContent(imgElm.outerHTML);
          const updatedElm = editor.dom.select('*[data-mce-id="__mceupd"]')[0];
          editor.dom.setAttrib(updatedElm, 'data-mce-id', null);
          editor.selection.select(updatedElm);
        } else {
          editor.dom.remove(imgElm);
          editor.focus();
          editor.nodeChanged();
          if (editor.dom.isEmpty(editor.getBody())) {
            editor.setContent('');
            editor.selection.setCursorLocation();
          }
        }
      } else if (data.imageUrl) {
        const image = document.createElement('img');
        image.setAttribute('src', data.imageUrl);
        if (data.width) {
          image.setAttribute('width', data.width + 'px');
        }
        if (data.height) {
          image.setAttribute('height', data.height + 'px');
        }
        editor.dom.setAttrib(image, 'data-mce-id', '__mcenew');
        editor.focus();
        editor.selection.setContent(image.outerHTML);
        const insertedElm = editor.dom.select('*[data-mce-id="__mcenew"]')[0];
        editor.dom.setAttrib(insertedElm, 'data-mce-id', null);
        editor.selection.select(insertedElm);
      }
    });
  }

}

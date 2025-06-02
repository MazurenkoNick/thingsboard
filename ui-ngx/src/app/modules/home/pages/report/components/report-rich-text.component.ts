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

import { Component, DestroyRef, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Editor, EditorOptions } from 'tinymce';
import { GetImageSrcCallback, HtmlWithImagePipe, SetImageSrcCallback } from '@shared/pipe/html-with-image.pipe';
import { TranslateService } from '@ngx-translate/core';
import {
  ReportImageData,
  ReportImageDialogComponent
} from '@home/pages/report/components/report-image-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import {
  extractKeyFromVariable,
  isKeyVariable, keyImage,
  ReportVariable
} from '@home/pages/report/components/report-component.models';
import { CustomImageUrlCallback } from '@shared/pipe/image.pipe';
import { of } from 'rxjs';

const TB_SRC_ATTRIBUTE = 'data-tb-src';

@Component({
  selector: 'tb-report-rich-text',
  templateUrl: './report-rich-text.component.html',
  styleUrls: ['./report-component-config.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ReportRichTextComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ReportRichTextComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  variables: ReportVariable[] = [];

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
        items: 'tb-image link inserttable variables | hr'
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
    contextmenu: ['tb-image', 'variables'],
    urlconverter_callback: (url) => url,
    setup: (editor) => this.setupEditor(editor)
  };

  richTextFormControl: UntypedFormControl;

  private modelValue: string;

  private propagateChange = null;

  private domParser = new DOMParser();

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private dialog: MatDialog,
              private htmlWithImagePipe: HtmlWithImagePipe,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    this.richTextFormControl = this.fb.control(null);
    this.richTextFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.richTextFormControl.disable({emitEvent: false});
    } else {
      this.richTextFormControl.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.modelValue = value;
    this.richTextFormControl.setValue(value, {emitEvent: false});
  }

  private updateModel() {
    this.modelValue = this.richTextFormControl.getRawValue();
    this.propagateChange(this.modelValue);
  }

  private setupEditor(editor: Editor) {
    this.setupTbImagePlugin(editor);
    this.setupVariables(editor);
  }

  private setupTbImagePlugin(editor: Editor) {
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

    editor.ui.registry.addContextMenu('tb-image', {
      update: (element) => {
        return [
          {
            icon: 'image',
            text: this.translate.instant('report-template.image') + '...',
            onAction: () => {
              this.editorImageAction(editor);
            }
          }
        ];
      }
    });

    editor.on('BeforeSetContent', (event) => {
      event.content = this.processImages(event.content, (image) => {
        const src = image.getAttribute('src');
        if (!image.hasAttribute(TB_SRC_ATTRIBUTE) || src !== '#') {
          image.setAttribute(TB_SRC_ATTRIBUTE, src);
        }
        image.setAttribute('src', '#');
      });
    });

    editor.on('SetContent', (_event) => {
      this.htmlWithImagePipe.transform(editor.getBody(), {
        getImageSrcCallback: this.getImageSrcCallback.bind(this),
        setImageSrcCallback: this.setImageSrcCallback.bind(this),
        customImageUrlCallback: this.keyImageUrlCallback.bind(this)
      }).subscribe();
    });

    editor.on('GetContent', (event) => {
      event.content = this.processImages(event.content, (image) => {
        const src = this.getImageSrcCallback(image);
        image.setAttribute('src', src);
        image.removeAttribute(TB_SRC_ATTRIBUTE);
      });
    });
  }

  private setupVariables(editor: Editor) {
    editor.ui.registry.addAutocompleter('variables', {
      trigger: '$',
      minChars: 0,
      columns: 'auto',
      onAction: (autocompleteApi, rng, value) => {
        editor.selection.setRng(rng);
        editor.insertContent(value);
        autocompleteApi.hide();
      },
      fetch: (_pattern) => {
        return new Promise((resolve) => {
          const results= this.variables.map((val) => ({
            type: 'cardmenuitem',
            value: `\${${val.name}}`,
            label: val.name,
            items: [
              {
                type: 'cardtext',
                text: val.name,
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
        return this.variables.map(variable => ({
            text: variable.name,
            type: 'menuitem',
            onAction: () => {
            editor.insertContent(`\${${variable.name}}`);
          }
        }));
      }
    });

    editor.ui.registry.addContextMenu('variables', {
      update: (element) => {
        if (!element || element.nodeName !== 'IMG') {
          return [
            {
              text: 'Variable...',
              type: 'submenu',
              getSubmenuItems: () => {
                return this.variables.map(variable => ({
                  text: variable.name,
                  type: 'item',
                  onAction: () => {
                    editor.insertContent(`\${${variable.name}}`);
                  }
                }));
              }
            }
          ];
        }
      }
    });
  }

  private setImageSrcCallback: SetImageSrcCallback = (image, origUrl, newUrl) => {
    if (origUrl !== newUrl) {
      image.setAttribute(TB_SRC_ATTRIBUTE, origUrl);
    }
  }

  private getImageSrcCallback: GetImageSrcCallback = (image) => {
    if (image.hasAttribute(TB_SRC_ATTRIBUTE)) {
      return image.getAttribute(TB_SRC_ATTRIBUTE);
    } else {
      return image.getAttribute('src');
    }
  }

  private keyImageUrlCallback: CustomImageUrlCallback = (url) => {
    if (isKeyVariable(url)) {
      const key = extractKeyFromVariable(url);
      return of(keyImage(key));
    } else {
      return null;
    }
  }

  private processImages(content: string, imageElementCallback: (image: HTMLImageElement) => void): string {
    const document = this.domParser.parseFromString(content, "text/html");
    const images = document.images;
    for (let i= 0; i < images.length; i++) {
      const image = images.item(i);
      imageElementCallback(image);
    }
    return document.body.innerHTML;
  }

  private editorImageAction(editor: Editor) {
    const imageData = this.extractImageData(editor);
    this.dialog.open<ReportImageDialogComponent, ReportImageData,
      ReportImageData>(ReportImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {...imageData, entityKeys: this.variables.filter(variable => variable.type === 'entityKey')}
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
          imgElm.setAttribute('src', '#');
          imgElm.setAttribute(TB_SRC_ATTRIBUTE, data.imageUrl);
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
        image.setAttribute('src', '#');
        image.setAttribute(TB_SRC_ATTRIBUTE, data.imageUrl);
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

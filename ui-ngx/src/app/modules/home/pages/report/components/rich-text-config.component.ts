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

import { Component, DestroyRef, ViewEncapsulation } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ReportComponentConfig, RichTextReportComponentConfig } from '@app/shared/public-api';
import { AbstractReportComponentConfig } from '@home/pages/report/components/report-component-config.component';
import { EditorOptions } from 'tinymce';

@Component({
  selector: 'tb-report-rich-text-config',
  templateUrl: './rich-text-config.component.html',
  styleUrls: [],
  encapsulation: ViewEncapsulation.None
})
export class RichTextConfigComponent extends AbstractReportComponentConfig {

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
    urlconverter_callback: (url) => url
  };

  constructor(destroyRef: DestroyRef,
              private fb: FormBuilder) {
    super(destroyRef);
  }

  protected buildForm(reportComponentConfig: ReportComponentConfig): FormGroup {
    const richTextConfig: RichTextReportComponentConfig = reportComponentConfig as RichTextReportComponentConfig;
    return this.fb.group({
      value: [richTextConfig.value, []]
    });
  }

}

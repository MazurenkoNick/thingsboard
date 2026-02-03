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

import { AfterViewInit, Component, ElementRef, OnDestroy, viewChild, ViewEncapsulation } from '@angular/core';
import { RichTextReportComponentConfig } from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import {
  extractKeyFromVariable,
  imagePlaceholder,
  isKeyVariable,
  keyImage
} from '@home/pages/reporting/template/components/report-component.models';
import { of } from 'rxjs';

@Component({
    selector: 'tb-rich-text-preview',
    templateUrl: './rich-text-preview.component.html',
    styleUrls: ['./rich-text-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class RichTextPreviewComponent extends AbstractReportComponentPreview<RichTextReportComponentConfig> implements AfterViewInit, OnDestroy {

  richTextEl = viewChild('richText', {
    read: ElementRef<HTMLElement>,
  });

  private richTextResize$: ResizeObserver;

  html: string;

  htmlWithImageOptions =  {
    customImageUrlCallback: (url: string)=> {
      if (!url) {
        return of(imagePlaceholder);
      } else if (isKeyVariable(url)) {
        const key = extractKeyFromVariable(url);
        return of(keyImage(key));
      } else {
        return null;
      }
    }
  };

  onComponentUpdated() {
    if (this.reportComponent.value && this.reportComponent.value.trim().length) {
      this.html = this.reportComponent.value;
    } else {
      this.html = '<p>&nbsp;</p>';
    }
  }

  ngAfterViewInit() {
    this.richTextResize$ = new ResizeObserver(() => {
      this.contentResized.emit();
    });
    this.richTextResize$.observe(this.richTextEl().nativeElement);
  }

  ngOnDestroy() {
    if (this.richTextResize$) {
      this.richTextResize$.disconnect();
    }
  }

}

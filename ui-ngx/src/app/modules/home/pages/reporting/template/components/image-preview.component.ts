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
import { ImageReportComponentConfig } from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { getDataKey } from '@shared/models/widget-settings.models';
import { imagePlaceholder, keyImage } from '@home/pages/reporting/template/components/report-component.models';
import { isNotEmptyStr } from '@core/utils';

@Component({
    selector: 'tb-image-preview',
    templateUrl: './image-preview.component.html',
    styleUrls: ['./image-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ImagePreviewComponent extends AbstractReportComponentPreview<ImageReportComponentConfig> implements AfterViewInit, OnDestroy {

  imageEl = viewChild('image', {
    read: ElementRef<HTMLElement>,
  });

  private imageResize$: ResizeObserver;

  imageUrl: string;

  imageWidth: string = '100%';

  imageAlign: string = 'center';

  imagePlaceholder = imagePlaceholder;

  triggerUpdate: number = 0;

  onComponentUpdated() {
    if (this.reportComponent.sourceType === 'entityKey') {
      const key = getDataKey(this.reportComponent.dataSources);
      if (key) {
        this.imageUrl = keyImage(key.name);
      } else {
        this.imageUrl = this.imagePlaceholder;
      }
    } else {
      if (isNotEmptyStr(this.reportComponent.imageUrl)) {
        if (this.imageUrl === this.reportComponent.imageUrl) {
          this.triggerUpdate +=1;
        } else {
          this.imageUrl = this.reportComponent.imageUrl;
        }
      } else {
        this.imageUrl = this.imagePlaceholder;
      }
    }
    this.imageWidth = '100%';
    if (this.reportComponent.widthType === 'original') {
      this.imageWidth = 'auto';
    } else if (this.reportComponent.widthType === 'custom') {
      const customWidth = this.reportComponent.customWidth || 100;
      this.imageWidth = customWidth + 'px';
    }
    this.imageAlign = this.reportComponent.alignment || 'center';
  }

  ngAfterViewInit() {
    this.imageResize$ = new ResizeObserver(() => {
      this.contentResized.emit();
    });
    this.imageResize$.observe(this.imageEl().nativeElement);
  }

  ngOnDestroy() {
    if (this.imageResize$) {
      this.imageResize$.disconnect();
    }
  }

}

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

import { AfterViewInit, Component, ElementRef, OnDestroy, viewChild, ViewEncapsulation } from '@angular/core';
import {
  AbstractReportComponentPreviewContainer,
  IReportComponent
} from '@home/pages/reporting/template/components/report-component.component';
import { ReportComponentConfig, TwoBlocksReportComponentConfig } from '@shared/models/report-component.models';
import { ReportDropBlockComponent } from '@home/pages/reporting/template/components/report-drop-block.component';
import { alignment } from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-two-blocks-preview',
  templateUrl: './two-blocks-preview.component.html',
  styleUrls: ['./two-blocks-preview.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TwoBlocksPreviewComponent extends AbstractReportComponentPreviewContainer<TwoBlocksReportComponentConfig> implements AfterViewInit, OnDestroy {

  blocksContainerEl = viewChild('blocksContainer', {
    read: ElementRef<HTMLElement>,
  });

  leftBlock = viewChild('leftBlock', {
    read: ReportDropBlockComponent,
  });

  rightBlock = viewChild('rightBlock', {
    read: ReportDropBlockComponent,
  });

  private blocksContainerResize$: ResizeObserver;

  leftWidth = '50%';
  centerWidth = 8;
  rightWidth= '50%';

  leftVerticalAlignment: alignment;
  rightVerticalAlignment: alignment;

  onComponentUpdated() {
    const splitPosition = this.reportComponent.splitPosition;
    this.centerWidth = this.reportComponent.splitGap;
    this.leftWidth = splitPosition + '%';
    this.rightWidth = (100 - splitPosition) + '%';
    this.leftVerticalAlignment = this.reportComponent.leftVerticalAlignment;
    this.rightVerticalAlignment = this.reportComponent.rightVerticalAlignment;
  }

  ngAfterViewInit() {
    this.blocksContainerResize$ = new ResizeObserver(() => {
      this.contentResized.emit();
    });
    this.blocksContainerResize$.observe(this.blocksContainerEl().nativeElement);
  }

  ngOnDestroy() {
    if (this.blocksContainerResize$) {
      this.blocksContainerResize$.disconnect();
    }
  }

  childComponentEdit(leftElseRight: boolean): void {
    this.componentEdit.emit(leftElseRight ? this.reportComponent.leftBlock : this.reportComponent.rightBlock);
  }

  childComponentRemoved(component: ReportComponentConfig, leftElseRight: boolean) {
    if (leftElseRight) {
      this.reportComponent.leftBlock = null;
    } else {
      this.reportComponent.rightBlock = null;
    }
    if (component) {
      this.componentRemoved.emit(component);
    }
    this.componentsChanged.emit();
  }

  childComponentAdded(component: ReportComponentConfig, leftElseRight: boolean) {
    if (leftElseRight) {
      this.reportComponent.leftBlock = component;
    } else {
      this.reportComponent.rightBlock = component;
    }
    this.componentsChanged.emit();
  }

  protected getAllChildReportComponents(): IReportComponent[] {
    const reportComponents: IReportComponent[] = [];
    let comp = this.leftBlock();
    if (comp) {
      reportComponents.push(comp);
    }
    comp = this.rightBlock();
    if (comp) {
      reportComponents.push(comp);
    }
    return reportComponents;
  }

  protected findChildReportComponent(reportComponent: ReportComponentConfig): IReportComponent {
    if (this.reportComponent.leftBlock === reportComponent) {
      return this.leftBlock();
    } else if (this.reportComponent.rightBlock === reportComponent) {
      return this.rightBlock();
    }
    return null;
  }

}

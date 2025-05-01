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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter, HostBinding,
  Input,
  Output,
  viewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  defaultReportComponentConfig,
  ReportComponentConfig,
  ReportComponentType
} from '@shared/models/report-component.models';
import {
  CdkDrag,
  CdkDragDrop, CdkDragEnd,
  CdkDragEnter,
  CdkDragExit,
  CdkDragStart, CdkDropList,
  moveItemInArray,
  transferArrayItem
} from '@angular/cdk/drag-drop';

@Component({
  selector: 'tb-report-components',
  templateUrl: './report-components.component.html',
  styleUrls: ['./report-components.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportComponentsComponent {

  @HostBinding('style.position')
  position = 'relative';

  @Input()
  reportComponents: ReportComponentConfig[];

  @Output()
  componentsChanged = new EventEmitter();

  @Output()
  componentEdit = new EventEmitter<ReportComponentConfig>();

  reportsComponentHeight = 100;

  constructor(private cd: ChangeDetectorRef,
              public element: ElementRef<HTMLElement>) {}

  dropListEnter(event: CdkDragEnter) {
    if (!this.reportComponents?.length) {
      this.reportsComponentHeight = event.item.getPlaceholderElement().offsetHeight;
      //event.item.getPlaceholderElement().style.height = '100px';
    }/* else {
      event.item.getPlaceholderElement().style.height = event.item.element.nativeElement.offsetHeight + 'px';
    }
    this.cd.detectChanges();*/
  }

  dropListExit(event: CdkDragExit) {
    this.reportsComponentHeight = 100;
  }

  componentDrop(event: CdkDragDrop<any[]>) {
    const item = event.item;
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      if (item.data) {
        if (typeof item.data === 'string') {
          const reportComponent = defaultReportComponentConfig(item.data as ReportComponentType);
          if (reportComponent) {
            this.reportComponents.splice(event.currentIndex, 0, reportComponent);
          }
        } else if (typeof item.data === 'object') {
          transferArrayItem(
            event.previousContainer.data,
            event.container.data,
            event.previousIndex,
            event.currentIndex,
          );
        }
      }
    }
    this.reportsComponentHeight = 100;
    this.componentsChanged.emit();
  }

  onComponentEdit(reportComponent: ReportComponentConfig): void {
    this.componentEdit.emit(reportComponent);
  }

  componentRemove(reportComponent: ReportComponentConfig): void {
    const index = this.reportComponents.indexOf(reportComponent);
    if (index > -1) {
      this.reportComponents.splice(index, 1);
      this.cd.detectChanges();
      this.componentsChanged.emit();
    }
  }

  componentDragStarted(event: CdkDragStart){
    event.source.getPlaceholderElement().style.height = event.source.element.nativeElement.offsetHeight + 'px';
    document.body.style.cursor = 'grabbing';
    this.cd.detectChanges();
  }

  componentDragEnded() {
    document.body.style.cursor = 'auto';
  }

}

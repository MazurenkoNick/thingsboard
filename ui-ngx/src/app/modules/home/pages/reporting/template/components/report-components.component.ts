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
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  Input,
  OnChanges, OnDestroy,
  OnInit,
  Output,
  QueryList,
  SimpleChanges, ViewChild,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import { ReportComponentConfig } from '@shared/models/report-component.models';
import {
  CdkDrag,
  CdkDragDrop,
  CdkDragEnter,
  CdkDragExit, CdkDragMove, CdkDragRelease,
  CdkDragStart, CdkDropList,
  moveItemInArray,
  transferArrayItem
} from '@angular/cdk/drag-drop';
import { deepClone } from '@core/utils';
import { ReportComponentComponent } from '@home/pages/reporting/template/components/report-component.component';
import {
  ReportComponentContext,
  reportComponentsLibrary,
  reportComponentTypesData
} from '@home/pages/reporting/template/components/report-component.models';
import { TbReportFormat } from '@shared/models/report.models';

@Component({
  selector: 'tb-report-components',
  templateUrl: './report-components.component.html',
  styleUrls: ['./report-components.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportComponentsComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {

  @ViewChild(CdkDropList) dropList?: CdkDropList;

  @HostBinding('style.position')
  position = 'relative';

  @Input()
  format: TbReportFormat;

  @HostBinding('style.background')
  @Input()
  background: string;

  @HostBinding('style.width.pt')
  @Input()
  width: number;

  @Input()
  scale = 1;

  @HostBinding('style.padding-left.pt')
  @Input()
  marginLeft: number;

  @HostBinding('style.padding-right.pt')
  @Input()
  marginRight: number;

  @HostBinding('style.padding-top.pt')
  @Input()
  marginTop: number;

  @HostBinding('style.padding-bottom.pt')
  @Input()
  marginBottom: number;

  @Input()
  reportComponents: ReportComponentConfig[];

  @Input()
  context: ReportComponentContext;

  @Output()
  componentsChanged = new EventEmitter();

  @Output()
  componentRemoved = new EventEmitter<ReportComponentConfig>();

  @Output()
  componentEdit = new EventEmitter<ReportComponentConfig>();

  @ViewChildren(ReportComponentComponent)
  reportComponentComponents: QueryList<ReportComponentComponent>;

  reportsComponentHeight = 100;

  showNoReportComponents = false;

  allowDropPredicate = (drag: CdkDrag, drop: CdkDropList) => {
    return this.isDropAllowed(drag, drop);
  };

  constructor(public element: ElementRef<HTMLElement>) {}

  ngOnInit() {
    this.updateListHeight();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'reportComponents') {
          this.updateListHeight();
        }
      }
    }
  }

  ngAfterViewInit() {
    if (this.dropList) {
      (this.dropList as any).reportComponentContainer = true;
      (this.dropList as any).reportComponentsUpdated = () => {
        this.updateListHeight();
      }
      this.context.dragDropCtx.register(this.dropList);
    }
  }

  ngOnDestroy() {
    if (this.dropList) {
      this.context.dragDropCtx.deregister(this.dropList);
    }
  }


  dropListEnter(event: CdkDragEnter) {
    if (!this.reportComponents?.length || (this.reportComponents.length === 1 && this.reportComponents[0] === event.item.data)) {
      this.reportsComponentHeight = event.item.getPlaceholderElement().offsetHeight;
    }
  }

  dropListExit(event: CdkDragExit) {
    this.updateListHeight(event.item.data);
  }

  componentDrop(event: CdkDragDrop<any[]>) {
    const item = event.item;
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      if (item.data) {
        if (typeof item.data === 'string') {
          const libraryItem = reportComponentsLibrary.get(item.data);
          if (libraryItem) {
            const reportComponent = deepClone(libraryItem.defaultConfig);
            this.reportComponents.splice(event.currentIndex, 0, reportComponent);
            const componentData = reportComponentTypesData.getReportComponentTypeData(reportComponent.type, reportComponent.subType);
            if (componentData.editable && !componentData.container) {
              setTimeout(() => {
                this.componentEdit.emit(reportComponent);
              }, 0);
            }
          }
        } else if (typeof item.data === 'object') {
          transferArrayItem(
            event.previousContainer.data,
            event.container.data,
            event.previousIndex,
            event.currentIndex,
          );
          const prevContainer = event.previousContainer as any;
          if (prevContainer.nestedReportComponentContainer) {
            prevContainer.reportComponentRemoved();
          }
        }
      }
    }
    this.updateListHeight();
    this.componentsChanged.emit();
  }

  isDropAllowed(drag: CdkDrag, drop: CdkDropList) {

    if (this.context.dragDropCtx.currentHoverDropListId == null) {
      return !drop.element.nativeElement.contains(drag.dropContainer.element.nativeElement);
    }

    return drop.id === this.context.dragDropCtx.currentHoverDropListId;
  }

  dragMoved(event: CdkDragMove) {
    this.context.dragDropCtx.dragMoved(event);
  }

  dragReleased(event: CdkDragRelease) {
    this.context.dragDropCtx.dragReleased(event);
  }

  onComponentEdit(reportComponent: ReportComponentConfig): void {
    this.componentEdit.emit(reportComponent);
  }

  duplicateComponent(reportComponent: ReportComponentConfig): void {
    const duplicate = deepClone(reportComponent);
    const index = this.reportComponents.indexOf(reportComponent);
    this.reportComponents.splice(index + 1, 0, duplicate);
    this.componentsChanged.emit();
  }

  componentRemove(reportComponent: ReportComponentConfig): void {
    const index = this.reportComponents.indexOf(reportComponent);
    if (index > -1) {
      this.reportComponents.splice(index, 1);
      this.updateListHeight();
      this.componentRemoved.emit(reportComponent);
      this.componentsChanged.emit();
    }
  }

  childComponentRemove(reportComponent: ReportComponentConfig): void {
    this.componentRemoved.emit(reportComponent);
  }

  childrenComponentsChanged(): void {
    this.componentsChanged.emit();
  }

  componentUpdated(reportComponent: ReportComponentConfig): boolean {
    if (this.reportComponents) {
      const index = this.reportComponents.indexOf(reportComponent);
      if (index > -1) {
        const component = this.reportComponentComponents.get(index);
        if (component) {
          component.componentUpdated();
          return true;
        }
      }
    }
    const containers = this.reportComponentComponents.
          filter(component => component.reportComponentsContainer);
    for (const container of containers) {
      if (container.childComponentUpdated(reportComponent)) {
        return true;
      }
    }
    return false;
  }

  componentSelected(reportComponent: ReportComponentConfig) {
    this.reportComponentComponents.forEach(component => component.deselect());
    if (reportComponent && this.reportComponents) {
      const index = this.reportComponents.indexOf(reportComponent);
      if (index > -1) {
        const component = this.reportComponentComponents.get(index);
        if (component) {
          component.selected = true;
          return;
        }
      }
    }
    const containers = this.reportComponentComponents.
          filter(component => component.reportComponentsContainer);
    for (const container of containers) {
      if (container.childComponentSelected(reportComponent)) {
        return;
      }
    }
  }

  componentDragStarted(_event: CdkDragStart){
    //event.source.getPlaceholderElement().style.height = Math.max(60, event.source.element.nativeElement.offsetHeight) + 'px';
    document.body.style.cursor = 'grabbing';
  }

  componentDragEnded() {
    document.body.style.cursor = 'auto';
  }

  private updateListHeight(reportComponent?: ReportComponentConfig) {
    if (!this.reportComponents?.length || (this.reportComponents.length === 1 && this.reportComponents[0] === reportComponent)) {
      this.reportsComponentHeight = 100;
      this.showNoReportComponents = true;
    } else {
      this.reportsComponentHeight = undefined;
      this.showNoReportComponents = false;
    }
  }
}

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
  Component, ElementRef,
  EventEmitter,
  Input,
  OnChanges, OnDestroy,
  OnInit,
  Output, QueryList, Renderer2,
  SimpleChanges, ViewChild, ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import { ReportComponentConfig } from '@shared/models/report-component.models';
import {
  CdkDrag,
  CdkDragDrop,
  CdkDragEnter,
  CdkDragExit, CdkDragMove, CdkDragRelease, CdkDragStart,
  CdkDropList,
  moveItemInArray,
  transferArrayItem
} from '@angular/cdk/drag-drop';
import {
  ReportComponentContext,
  reportComponentsLibrary,
  reportComponentTypesData
} from '@home/pages/reporting/template/components/report-component.models';
import { deepClone } from '@core/utils';
import { TbReportFormat } from '@shared/models/report.models';
import { ReportComponentComponent } from '@home/pages/reporting/template/components/report-component.component';

@Component({
  selector: 'tb-report-drop-block',
  templateUrl: './report-drop-block.component.html',
  styleUrls: ['./report-drop-block.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportDropBlockComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {

  @ViewChild(CdkDropList) dropList?: CdkDropList;

  @ViewChildren(ReportComponentComponent)
  reportComponentComponents: QueryList<ReportComponentComponent>;

  @Input()
  context: ReportComponentContext;

  @Input()
  component: ReportComponentConfig;

  @Input()
  format: TbReportFormat;

  @Output()
  componentAdded = new EventEmitter<ReportComponentConfig>();

  @Output()
  componentRemoved = new EventEmitter();

  @Output()
  componentEdit = new EventEmitter<ReportComponentConfig>();

  components: ReportComponentConfig[] = [];

  reportComponentHeight = undefined;

  showNoReportComponent = false;

  fillHeight = true;

  constructor(private elementRef: ElementRef<HTMLElement>,
              private renderer: Renderer2) {
  }

  allowDropPredicate = (drag: CdkDrag, drop: CdkDropList) => {
    return this.isDropAllowed(drag, drop);
  };

  ngOnInit(): void {
    this.updateComponents();
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'component') {
          this.updateComponents();
        }
      }
    }
  }

  ngAfterViewInit() {
    if (this.dropList) {
      (this.dropList as any).nestedReportComponentContainer = true;
      (this.dropList as any).reportComponentRemoved = () => {
        this.componentRemove(null);
      };
      this.context.dragDropCtx.register(this.dropList);
    }
  }

  ngOnDestroy() {
    if (this.dropList) {
      this.context.dragDropCtx.deregister(this.dropList);
    }
  }

  componentDrop(event: CdkDragDrop<any[]>) {
    const item = event.item;
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else if (!this.components.length) {
      if (item.data) {
        if (typeof item.data === 'string') {
          const libraryItem = reportComponentsLibrary.get(item.data);
          if (libraryItem) {
            const reportComponent = deepClone(libraryItem.defaultConfig);
            this.components.push(reportComponent);
            this.componentAdded.emit(reportComponent);
            if (reportComponentTypesData.getReportComponentTypeData(reportComponent.type, reportComponent.subType).editable) {
              setTimeout(() => {
                this.componentEdit.emit(reportComponent);
              }, 0);
            }
          }
        } else if (typeof item.data === 'object') {
          event.previousContainer.data.splice(event.previousIndex, 1);
          const reportComponent: ReportComponentConfig = item.data;
          this.components.push(reportComponent);
          this.componentAdded.emit(reportComponent);
          const prevContainer = event.previousContainer as any;
          if (prevContainer.nestedReportComponentContainer) {
            prevContainer.reportComponentRemoved();
          }

        }
      }
    }
    this.updateHeight();
    //this.componentsChanged.emit();
  }

  isDropAllowed(drag: CdkDrag, drop: CdkDropList) {
    if (this.components.length) {
      return false;
    }
    if (this.context.dragDropCtx.currentHoverDropListId == null) {
      return true;
    }

    return drop.id === this.context.dragDropCtx.currentHoverDropListId;
  }

  dragMoved(event: CdkDragMove) {
    this.context.dragDropCtx.dragMoved(event);
  }

  dragReleased(event: CdkDragRelease) {
    this.context.dragDropCtx.dragReleased(event);
  }

  dropListEnter(event: CdkDragEnter) {
    if (!this.components?.length || (this.components.length === 1 && this.components[0] === event.item.data)) {
//      this.placeHolderHeight = event.item.getPlaceholderElement().offsetHeight;
      const placeholder = event.item.getPlaceholderElement();
      this.updatePlaceholderDimensions(placeholder, event.item);
      // this.reportComponentHeight = event.item.getPlaceholderElement().offsetHeight;
      // this.fillHeight = false;
    }
  }

  private updatePlaceholderDimensions(placeholder: HTMLElement, item: CdkDrag) {
    const targetHeight = this.elementRef.nativeElement.getBoundingClientRect().height;
    const targetWidth = this.elementRef.nativeElement.getBoundingClientRect().width;
    let els = placeholder.getElementsByTagName('tb-report-component');
    let placeholderHeight: number;
    let placeholderWidth: number;
    if (els.length) {
      const repComp = els.item(0);
      placeholderHeight = repComp.getBoundingClientRect().height;
      this.renderer.setStyle(repComp, 'height', targetHeight + 'px');
      els = repComp.getElementsByClassName('tb-report-component');
      if (els.length) {
        const repEl = els.item(0);
        placeholderWidth = repEl.getBoundingClientRect().width;
        this.renderer.setStyle(repEl, 'width', targetWidth + 'px');
      }
    } else {
      // simple placeholder
      placeholderHeight = placeholder.getBoundingClientRect().height;
      this.renderer.setStyle(placeholder, 'height', targetHeight + 'px');
    }
    (item as any).placeholderHeight = placeholderHeight ? placeholderHeight : undefined;
    (item as any).placeholderWidth = placeholderWidth ? placeholderWidth : undefined;
  }

  private restorePlaceholderDimensions(placeholder: HTMLElement, item: CdkDrag) {
    const placeholderHeight = (item as any).placeHolderHeight;
    const placeholderWidth = (item as any).placeholderWidth;
    if (placeholderHeight) {
      let els = placeholder.getElementsByTagName('tb-report-component');
      if (els.length) {
        const repComp = els.item(0);
        this.renderer.setStyle(repComp, 'height', placeholderHeight + 'px');
        if (placeholderWidth) {
          els = repComp.getElementsByClassName('tb-report-component');
          if (els.length) {
            const repEl = els.item(0);
            this.renderer.setStyle(repEl, 'width', placeholderWidth + 'px');
          }
        }
      } else {
        // simple placeholder
        this.renderer.setStyle(placeholder, 'height', placeholderHeight + 'px');
      }
    }
  }

  dropListExit(event: CdkDragExit) {
    this.restorePlaceholderDimensions(event.item.getPlaceholderElement(), event.item);
    this.updateHeight(event.item.data, event.item.getPlaceholderElement().getBoundingClientRect().height);
  }

  onComponentEdit(reportComponent: ReportComponentConfig): void {
    this.componentEdit.emit(reportComponent);
  }

  duplicateComponent(reportComponent: ReportComponentConfig): void {
    // TODO:
    /*const duplicate = deepClone(reportComponent);
    const index = this.reportComponents.indexOf(reportComponent);
    this.reportComponents.splice(index + 1, 0, duplicate);
    this.componentsChanged.emit();*/
  }

  componentRemove(reportComponent: ReportComponentConfig): void {
    this.components.length = 0;
    this.componentRemoved.emit();
    this.updateHeight();
    // TODO:
    /*const index = this.reportComponents.indexOf(reportComponent);
    if (index > -1) {
      this.reportComponents.splice(index, 1);
      this.updateListHeight();
      this.componentRemoved.emit(reportComponent);
      this.componentsChanged.emit();
    }*/
  }

  componentDragStarted(_event: CdkDragStart){
    //event.source.getPlaceholderElement().style.height = Math.max(60, event.source.element.nativeElement.offsetHeight) + 'px';
    document.body.style.cursor = 'grabbing';
  }

  componentDragEnded() {
    document.body.style.cursor = 'auto';
  }

  private updateComponents() {
    if (this.component) {
      this.components.length = 0;
      this.components.push(this.component);
    } else {
      this.components.length = 0;
    }
    this.updateHeight();
  }

  private updateHeight(reportComponent?: ReportComponentConfig, targetHeight?: number) {
    if (!this.components?.length || (this.components.length === 1 && this.components[0] === reportComponent)) {
      this.reportComponentHeight = targetHeight || 100;
      this.showNoReportComponent = true;
      this.fillHeight = true;
    } else {
      this.reportComponentHeight = undefined;
      this.showNoReportComponent = false;
      this.fillHeight = false;
    }
  }

}

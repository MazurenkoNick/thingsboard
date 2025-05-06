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
  ChangeDetectorRef,
  Component,
  ComponentRef, Directive,
  ElementRef,
  EventEmitter, HostBinding,
  Input, OnChanges,
  OnDestroy,
  OnInit,
  Output, SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ReportComponentConfig } from '@shared/models/report-component.models';
import { ReportComponentTypeData, reportComponentTypeMap } from '@home/pages/report/components/report-component.models';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { from } from 'rxjs';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;
import { ReportComponentsComponent } from '@home/pages/report/components/report-components.component';

@Component({
  selector: 'tb-report-component',
  templateUrl: './report-component.component.html',
  styleUrls: ['./report-component.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportComponentComponent implements OnInit, OnDestroy {

  @HostBinding('style.background')
  background: string;

  @Input()
  reportComponent: ReportComponentConfig;

  @Input()
  dragging = false;

  @Output()
  edit = new EventEmitter<() => void>();

  @Output()
  makeCopy = new EventEmitter();

  @Output()
  remove = new EventEmitter();

  @ViewChild('reportPreviewContainer', {static: true}) reportPreviewContainer: TbAnchorComponent;

  typeData: ReportComponentTypeData;

  hovered = false;

  private editReportComponentTooltip: ITooltipsterInstance;

  private reportComponentPreview: AbstractReportComponentPreview;

  private componentUpdated = this._componentUpdated.bind(this);

  constructor(private reportComponents: ReportComponentsComponent,
              private elementRef: ElementRef,
              private container: ViewContainerRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit() {
    const type = this.reportComponent.type;
    this.typeData = reportComponentTypeMap.get(type);
    if (this.typeData) {
      const compRef = this.reportPreviewContainer.viewContainerRef.createComponent(this.typeData.previewComponent);
      this.reportComponentPreview = compRef.instance;
      this.reportComponentPreview.reportComponent = this.reportComponent;
    }
    this.initEditReportComponentTooltip();
    this.updateComponentLayout();
  }

  /*ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'pageBackground') {
          this.updateComponentLayout();
        }
      }
    }
  }*/

  ngOnDestroy(): void {
    if (this.editReportComponentTooltip && !this.editReportComponentTooltip.status().destroyed) {
      this.editReportComponentTooltip.destroy();
    }
  }

  onEdit(event: MouseEvent) {
    if (event) {
      event.stopPropagation();
    }
    this.edit.emit(this.componentUpdated);
  }

  onCopy(event: MouseEvent) {
    if (event) {
      event.stopPropagation();
    }
    this.makeCopy.emit();
  }

  onRemove(event: MouseEvent) {
    if (event) {
      event.stopPropagation();
    }
    this.remove.emit();
  }

  mouseEnter(event: MouseEvent) {
    if (event.buttons === 0) {
      this.hovered = true;
    }
  }

  mouseLeave(_event: MouseEvent) {
    this.hovered = false;
  }

  private updateComponentLayout() {
    this.background = this.reportComponent.background;// || this.pageBackground;
  }

  private _componentUpdated() {
    if (this.reportComponentPreview) {
      this.reportComponentPreview.componentUpdated();
    }
    this.updateComponentLayout();
  }

  private initEditReportComponentTooltip() {
    let componentRef: ComponentRef<EditReportComponentTooltipComponent>;
    const parent = this.reportComponents.element.nativeElement;
    from(import('tooltipster')).subscribe(() => {
      $(this.elementRef.nativeElement).tooltipster({
        parent: $(parent),
        delay: [0, 50],
        distance: 0,
        zIndex: 151,
        arrow: false,
        theme: ['tb-report-component-edit-tooltip'],
        interactive: true,
        trigger: 'hover',
        ignoreCloseOnScroll: true,
        side: ['top'],
        trackOrigin: true,
        trackerInterval: 25,
        content: '',
        functionPosition: (instance, helper, position) => {
          const clientRect = helper.origin.getBoundingClientRect();
          const container = parent.getBoundingClientRect();
          position.coord.left = Math.max(0,clientRect.right - position.size.width - container.left);
          position.coord.top = position.coord.top - container.top;
          position.target = clientRect.right;
          return position;
        },
        functionReady: (_instance, helper) => {
          this.editReportComponentTooltip.__scrollHandler({});
          const tooltipEl = $(helper.tooltip);
          tooltipEl.on('mouseenter', () => {
            this.hovered = true;
            this.cd.markForCheck();
          });
          tooltipEl.on('mouseleave', () => {
            this.hovered = false;
            this.cd.markForCheck();
          });
        },
        functionBefore: (_instance, helper) => {
          return (helper.event as any).buttons === 0;
        },
        functionAfter: () => {
          this.hovered = false;
          this.cd.markForCheck();
        }
      });
      this.editReportComponentTooltip = $(this.elementRef.nativeElement).tooltipster('instance');
      componentRef = this.container.createComponent(EditReportComponentTooltipComponent);
      componentRef.instance.container = this;
      componentRef.instance.viewInited.subscribe(() => {
        if (this.editReportComponentTooltip.status().open) {
          this.editReportComponentTooltip.reposition();
        }
      });
      this.editReportComponentTooltip.on('destroyed', () => {
        componentRef.destroy();
      });
      const parentElement = componentRef.instance.element.nativeElement;
      const content = parentElement.firstChild;
      parentElement.removeChild(content);
      parentElement.style.display = 'none';
      this.editReportComponentTooltip.content(content);
    });
  }
}

@Component({
  template: `
    <div class="tb-report-component-action-container">
      <div class="tb-report-component-actions-panel">
        <button mat-icon-button class="tb-mat-20"
                (click)="container.onEdit($event)"
                matTooltip="{{ 'action.edit' | translate }}"
                matTooltipPosition="above">
          <tb-icon>edit</tb-icon>
        </button>
        <button mat-icon-button class="tb-mat-20"
                (click)="container.onCopy($event)"
                matTooltip="{{ 'action.duplicate' | translate }}"
                matTooltipPosition="above">
          <tb-icon>content_copy</tb-icon>
        </button>
        <button mat-icon-button class="tb-mat-20"
                (click)="container.onRemove($event);"
                matTooltip="{{ 'action.remove' | translate }}"
                matTooltipPosition="above">
          <tb-icon>close</tb-icon>
        </button>
      </div>
    </div>`,
  styles: [],
  encapsulation: ViewEncapsulation.None
})
export class EditReportComponentTooltipComponent implements AfterViewInit {

  @Input()
  container: ReportComponentComponent;

  @Output()
  viewInited = new EventEmitter();

  constructor(public element: ElementRef<HTMLElement>,
              public cd: ChangeDetectorRef) {
  }

  ngAfterViewInit() {
    this.viewInited.emit();
  }
}

@Directive()
export abstract class AbstractReportComponentPreview<C extends ReportComponentConfig = ReportComponentConfig> implements OnInit {

  @Input()
  reportComponent: C;

  ngOnInit() {
    this.componentUpdated();
  }

  componentUpdated() {}

}

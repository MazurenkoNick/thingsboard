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
  ComponentRef, DestroyRef,
  Directive,
  ElementRef,
  EventEmitter,
  HostBinding, HostListener,
  inject,
  Input,
  OnDestroy,
  OnInit,
  Output,
  Renderer2,
  SimpleChanges,
  viewChild,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { isLayoutReportComponentConfig, ReportComponentConfig } from '@shared/models/report-component.models';
import {
  pointsToPixels,
  ReportComponentTypeData,
  reportComponentTypeMap
} from '@home/pages/report/components/report-component.models';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { from } from 'rxjs';
import { ReportComponentsComponent } from '@home/pages/report/components/report-components.component';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import ITooltipsterGeoHelper = JQueryTooltipster.ITooltipsterGeoHelper;
import { TbReportFormat } from '@shared/models/report.models';

@Component({
  selector: 'tb-report-component',
  templateUrl: './report-component.component.html',
  styleUrls: ['./report-component.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportComponentComponent implements OnInit, AfterViewInit, OnDestroy {

  reportComponentElement = viewChild('reportComponentElement', {
    read: ElementRef<HTMLElement>,
  });

  @HostBinding('class')
  class = 'tb-report-component-host';

  @HostBinding('style.background')
  background: string;

  @HostBinding('style.margin-left.pt')
  marginLeft: number;

  @HostBinding('style.margin-right.pt')
  marginRight: number;

  @HostBinding('style.margin-top.pt')
  marginTop: number;

  @HostBinding('style.margin-bottom.pt')
  marginBottom: number;

  @HostBinding('style.padding-left.pt')
  paddingLeft: number;

  @HostBinding('style.padding-right.pt')
  paddingRight: number;

  @HostBinding('style.padding-top.pt')
  paddingTop: number;

  @HostBinding('style.padding-bottom.pt')
  paddingBottom: number;

  @HostBinding('style.display')
  display = 'block';

  @HostBinding('style.position')
  position = 'relative';

  @Input()
  reportComponent: ReportComponentConfig;

  @Input()
  format: TbReportFormat;

  @Input()
  dragging = false;

  @Input()
  scale = 1;

  @Input()
  width: number;

  @Input()
  pageMarginLeft: number;

  @Input()
  pageMarginRight: number;

  @Output()
  edit = new EventEmitter();

  @Output()
  makeCopy = new EventEmitter();

  @Output()
  remove = new EventEmitter();

  @ViewChild('reportPreviewContainer', {static: true}) reportPreviewContainer: TbAnchorComponent;

  typeData: ReportComponentTypeData;

  @HostBinding('class.tb-hover')
  hovered = false;

  private editReportComponentTooltip: ITooltipsterInstance;

  private reportComponentPreview: AbstractReportComponentPreview;

  private reportComponentHeight = 0;

  constructor(private reportComponents: ReportComponentsComponent,
              private elementRef: ElementRef<HTMLElement>,
              private container: ViewContainerRef,
              private renderer: Renderer2,
              private destroyRef: DestroyRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit() {
    const type = this.reportComponent.type;
    this.typeData = reportComponentTypeMap.get(type);
    if (this.typeData) {
      const compRef = this.reportPreviewContainer.viewContainerRef.createComponent(this.typeData.previewComponent);
      this.reportComponentPreview = compRef.instance;
      this.reportComponentPreview.reportComponent = this.reportComponent;
      this.reportComponentPreview.format = this.format;
      this.reportComponentPreview.contentResized.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateComponentLayout();
      });
    }
    this.initEditReportComponentTooltip();
    this.updateComponentLayout();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['scale', 'width'].includes(propName)) {
          this.updateComponentLayout();
        }
        if (['pageMarginLeft', 'pageMarginRight'].includes(propName)) {
          if (this.typeData.pageBreak) {
            this.updateComponentLayout();
          }
        }
      }
    }
  }

  ngAfterViewInit() {
    this.updateComponentLayout();
  }

  ngOnDestroy(): void {
    if (this.editReportComponentTooltip && !this.editReportComponentTooltip.status().destroyed) {
      this.editReportComponentTooltip.destroy();
    }
  }

  @HostListener('click', ['$event'])
  onEdit(event: MouseEvent) {
    if (event) {
      event.stopPropagation();
    }
    this.edit.emit();
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

  @HostListener('mouseenter', ['$event'])
  mouseEnter(event: MouseEvent) {
    if (event.buttons === 0) {
      this.hovered = true;
    }
  }

  @HostListener('mouseleave', ['$event'])
  mouseLeave(_event: MouseEvent) {
    this.hovered = false;
  }

  private updateComponentSize() {
    const parentWidth = this.elementRef.nativeElement.getBoundingClientRect().width;
    const leftRightPaddings = pointsToPixels(this.paddingLeft) + pointsToPixels(this.paddingRight);
    this.renderer.setStyle(this.reportComponentElement().nativeElement, 'width', ((parentWidth - leftRightPaddings) / this.scale) + 'px');
    this.renderer.setStyle(this.reportComponentElement().nativeElement, 'transform', `scale(${this.scale})`);
    const rect = this.reportComponentElement().nativeElement.getBoundingClientRect();
    const targetHeight = rect.height > 0 ? rect.height : this.reportComponentHeight;
    this.reportComponentHeight = targetHeight;
    const topBottomPaddings = pointsToPixels(this.paddingTop) + pointsToPixels(this.paddingBottom);
    this.renderer.setStyle(this.elementRef.nativeElement, 'height', (targetHeight + topBottomPaddings) + 'px');
  }

  private updateComponentLayout() {
    if (isLayoutReportComponentConfig(this.reportComponent)) {
      this.background = this.reportComponent.background;
      this.marginLeft = (this.reportComponent.margins?.left || 0) * this.scale;
      this.marginRight = (this.reportComponent.margins?.right || 0) * this.scale;
      this.marginTop = (this.reportComponent.margins?.top || 0) * this.scale;
      this.marginBottom = (this.reportComponent.margins?.bottom || 0) * this.scale;
      this.paddingLeft = (this.reportComponent.paddings?.left || 0) * this.scale;
      this.paddingRight = (this.reportComponent.paddings?.right || 0) * this.scale;
      this.paddingTop = (this.reportComponent.paddings?.top || 0) * this.scale;
      this.paddingBottom = (this.reportComponent.paddings?.bottom || 0) * this.scale;
    } else {
      this.paddingLeft = this.paddingRight = this.paddingTop = this.paddingBottom =
        this.marginLeft = this.marginRight = this.marginTop = this.marginBottom = 0;
      if (this.typeData.pageBreak) {
        this.marginLeft = -this.pageMarginLeft / this.scale;
        this.marginRight = -this.pageMarginRight / this.scale;
      }
    }
    this.updateComponentSize();
  }

  public componentUpdated() {
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
        checkOverflowY: (geo: ITooltipsterGeoHelper, bcr: DOMRect) => {
          return geo.origin.windowOffset.top < bcr.top || geo.origin.windowOffset.bottom < bcr.bottom;
        },
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

  @HostBinding('style.width')
  width = '100%';

  @Input()
  reportComponent: C;

  @Input()
  format: TbReportFormat;

  @Output()
  contentResized = new EventEmitter();

  public get canHaveLayout(): boolean {
    return this.format === TbReportFormat.PDF;
  }

  protected cd = inject(ChangeDetectorRef);

  ngOnInit() {
    this.componentUpdated();
  }

  componentUpdated() {
    this.onComponentUpdated();
    this.cd.detectChanges();
  }

  protected onComponentUpdated() {}

}

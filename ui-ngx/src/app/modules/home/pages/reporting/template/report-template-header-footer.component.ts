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
  Component, ElementRef, EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  Renderer2, viewChild,
  ViewEncapsulation
} from '@angular/core';
import { HeaderFooter, TbReportFormat } from '@shared/models/report.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ReportComponentConfig } from '@shared/models/report-component.models';

@Component({
  selector: 'tb-report-template-header-footer',
  templateUrl: './report-template-header-footer.component.html',
  styleUrls: ['./report-template-header-footer.component.scss', './report-components-container.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportTemplateHeaderFooterComponent {

  get currentHeader(): HeaderFooter {
    return this.headerToggleValue === 'header' ? this.headerFooter : this.headerFooter.firstPage;
  }

  @Input()
  @coerceBoolean()
  header = true;

  @Input()
  headerFooter: HeaderFooter;

  @Input()
  headerToggleValue: 'header' | 'firstPageHeader' = 'header';

  @Input()
  format: TbReportFormat;

  @Input()
  background: string;

  @Input()
  width: number;

  @Input()
  scale = 1;

  @Input()
  marginTop: number;

  @Input()
  marginLeft: number;

  @Input()
  marginRight: number;

  @Input()
  marginBottom: number;

  @Output()
  componentsChanged = new EventEmitter();

  @Output()
  componentEdit = new EventEmitter<ReportComponentConfig>();

  @Output()
  componentRemoved = new EventEmitter<ReportComponentConfig>();

  @Output()
  currentHeaderChanged = new EventEmitter<'header' | 'firstPageHeader'>();

  @Output()
  enabledHeaderChanged = new EventEmitter();

  @Output()
  expandAnimationStart = new EventEmitter();

  @Output()
  expandAnimationFinish = new EventEmitter();

  reportHeaderToolbarButtonsEl = viewChild('reportHeaderToolbarButtons', {
    read: ElementRef<HTMLElement>,
  });

  disableHeaderButtonEl = viewChild('disableHeaderButton', {
    read: ElementRef<HTMLElement>,
  });

  reportHeaderComponentsEl = viewChild('reportHeaderComponents', {
    read: ElementRef<HTMLElement>,
  });

  expanded = true;

  constructor(private renderer: Renderer2) {

  }

  public toggleCurrentHeader() {
    this.currentHeaderChanged.emit(this.headerToggleValue);
  }

  public disableHeader(): void {
    this.currentHeader.enabled = false;
    this.enabledHeaderChanged.emit();
  }

  public enableHeader(): void {
    this.currentHeader.enabled = true;
    this.enabledHeaderChanged.emit();
  }

  public reportComponentsChanged() {
    this.componentsChanged.emit();
  }

  public editReportComponent(reportComponent: ReportComponentConfig): void {
    this.componentEdit.emit(reportComponent);
  }

  public reportComponentRemoved(reportComponent: ReportComponentConfig) {
    this.componentRemoved.emit(reportComponent);
  }

  public toggleExpanded() {
    const expand = !this.expanded;
    this.expandAnimationStart.emit();
    if (expand) {
      const reportHeaderComponents = this.reportHeaderComponentsEl();
      this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', null);
      const height = reportHeaderComponents.nativeElement.offsetHeight;
      this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', '0');
      setTimeout(() => {
        const reportHeaderToolbarButtons = this.reportHeaderToolbarButtonsEl();
        this.renderer.setStyle(reportHeaderToolbarButtons.nativeElement, 'right', '0px');
        this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', height + 'px');
        const onTransitionEnd = (ev: TransitionEvent) => {
          if (ev.propertyName === 'max-height') {
            this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', null);
            this.renderer.setStyle(reportHeaderComponents.nativeElement, 'overflow', null);
            reportHeaderComponents.nativeElement.removeEventListener('transitionend', onTransitionEnd);
            this.expandAnimationFinish.emit();
          }
        };
        reportHeaderComponents.nativeElement.addEventListener('transitionend', onTransitionEnd);
        this.expanded = expand;
      });
    } else {
      const reportHeaderComponents = this.reportHeaderComponentsEl();
      const height = reportHeaderComponents.nativeElement.offsetHeight;
      this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', height + 'px');
      setTimeout(() => {
        const disableHeaderButton = this.disableHeaderButtonEl();
        const disabledButtonWidth: number = disableHeaderButton ? disableHeaderButton.nativeElement.offsetWidth : 0;
        const toolbarButtonsRight = -(disabledButtonWidth + 8);
        const reportHeaderToolbarButtons = this.reportHeaderToolbarButtonsEl();
        this.renderer.setStyle(reportHeaderToolbarButtons.nativeElement, 'right', toolbarButtonsRight + 'px');
        this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', '0');
        this.renderer.setStyle(reportHeaderComponents.nativeElement, 'overflow', 'hidden');
        const onTransitionEnd = (ev: TransitionEvent) => {
          if (ev.propertyName === 'max-height') {
            reportHeaderComponents.nativeElement.removeEventListener('transitionend', onTransitionEnd);
            this.expandAnimationFinish.emit();
          }
        };
        reportHeaderComponents.nativeElement.addEventListener('transitionend', onTransitionEnd);
        this.expanded = expand;
      });
    }
  }

}

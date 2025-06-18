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
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  viewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  csvReportComponentTypes,
  reportComponentTypeMap,
  reportComponentTypes
} from '@home/pages/reporting/template/components/report-component.models';
import { CdkDragStart } from '@angular/cdk/drag-drop';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ReportComponentType } from '@shared/models/report-component.models';
import { TbReportFormat } from '@shared/models/report.models';

@Component({
  selector: 'tb-report-component-library',
  templateUrl: './report-component-library.component.html',
  styleUrls: ['./report-component-library.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportComponentLibraryComponent implements OnInit, OnChanges {

  @Input()
  @coerceBoolean()
  subReport = false;

  @Input()
  format: TbReportFormat = TbReportFormat.PDF;

  libraryDragOriginList = viewChild('libraryDragOriginList', {
    read: ElementRef,
  });

  libraryDragActiveList = viewChild('libraryDragActiveList', {
    read: ElementRef,
  });

  reportComponentTypes: ReportComponentType[];
  reportComponentTypeMap = reportComponentTypeMap;

  private itemDragEntered = false;

  constructor() {}

  ngOnInit() {
    this.updateReportComponentTypes();
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['subReport', 'format'].includes(propName)) {
          this.updateReportComponentTypes();
        }
      }
    }
  }

  dropListEnterPredicate(): boolean {
    return false;
  }

  dragStarted(event: CdkDragStart) {
    //event.source.getPlaceholderElement().style.height = Math.max(60, event.source.element.nativeElement.offsetHeight) + 'px';
    event.source.getPlaceholderElement().style.height = event.source.element.nativeElement.offsetHeight + 'px';
    document.body.style.cursor = 'grabbing';
    this.copyExistingLibItemsToActiveList();
    this.setActiveListVisibility(true);
  }

  dragEnded() {
    document.body.style.cursor = 'auto';
    this.setActiveListVisibility(false);
  }

  dragEntered() {
    this.itemDragEntered = true;
  }

  dragReleased() {
    if (!this.itemDragEntered) {
      const origin = this.libraryDragOriginList();
      $('.tb-report-component-placeholder', origin.nativeElement).hide();
      $('.tb-report-component-library-drag-item', origin.nativeElement).show();
      this.setActiveListVisibility(false);
    }
    this.itemDragEntered = false;
  }

  private updateReportComponentTypes() {
    const componentTypes = this.format === TbReportFormat.CSV ? csvReportComponentTypes : reportComponentTypes;
    if (this.subReport) {
      this.reportComponentTypes = componentTypes.filter((type) => type !== ReportComponentType.SUB_REPORT );
    } else {
      this.reportComponentTypes = componentTypes;
    }
  }

  private copyExistingLibItemsToActiveList() {
    const overlay = this.libraryDragActiveList();
    const origin = this.libraryDragOriginList();
    if (!overlay || !origin) {
      return;
    }
    overlay.nativeElement.innerHTML = origin.nativeElement.innerHTML;

    $('.tb-report-component-placeholder', overlay.nativeElement).hide();
    $('.tb-report-component-library-drag-item', overlay.nativeElement).show();
  }

  private setActiveListVisibility(visible: boolean) {
    const overlay = this.libraryDragActiveList();
    const origin = this.libraryDragOriginList();
    if (!overlay || !origin) {
      return;
    }
    overlay.nativeElement.style.display = visible ? 'flex' : 'none';
    origin.nativeElement.style.display = !visible ? 'flex' : 'none';
  }
}

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

import { Component, ElementRef, HostBinding, Input, OnChanges, SimpleChanges,
  viewChild,
  ViewEncapsulation
} from '@angular/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TbReportFormat } from '@shared/models/report.models';
import {
  ReportComponentContext,
  ReportComponentLibraryGroup,
  reportComponentLibraryGroupTranslations
} from '@home/pages/reporting/template/components/report-component.models';
import { MatExpansionPanel } from '@angular/material/expansion';

@Component({
    selector: 'tb-report-component-library-group',
    templateUrl: './report-component-library-group.component.html',
    styleUrls: ['./report-component-library-group.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportComponentLibraryGroupComponent implements OnChanges {

  reportComponentLibraryGroupTranslations = reportComponentLibraryGroupTranslations;

  expansionPanel = viewChild('expansionPanel', {
    read: MatExpansionPanel,
  });

  @Input()
  context: ReportComponentContext;

  @Input()
  @coerceBoolean()
  subReport = false;

  @Input()
  format: TbReportFormat = TbReportFormat.PDF;

  @Input()
  group: ReportComponentLibraryGroup;

  @Input()
  filter: string;

  constructor() {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if ('filter' === propName) {
          this.expansionPanel()?.open();
        }
      }
    }
  }

}

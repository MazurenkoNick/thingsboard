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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-generate-dashboard-report-config',
    templateUrl: './generate-dashboard-report-config.component.html',
    styleUrls: [],
    standalone: false
})
export class GenerateDashboardReportConfigComponent extends RuleNodeConfigurationComponent {

  generateDashboardReportConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.generateDashboardReportConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.generateDashboardReportConfigForm = this.fb.group({
      useSystemReportsServer: [configuration ? configuration.useSystemReportsServer : false, []],
      reportsServerEndpointUrl: [configuration ? configuration.reportsServerEndpointUrl : null, []],
      useReportConfigFromMessage: [configuration ? configuration.useReportConfigFromMessage : false, []],
      reportConfig: [configuration ? configuration.reportConfig : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSystemReportsServer', 'useReportConfigFromMessage'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSystemReportsServer: boolean = this.generateDashboardReportConfigForm.get('useSystemReportsServer').value;
    const useReportConfigFromMessage: boolean = this.generateDashboardReportConfigForm.get('useReportConfigFromMessage').value;
    if (emitEvent) {
      const reportsServerEndpointUrl: string = this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').value;
      if (useSystemReportsServer) {
        this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').reset(null, {emitEvent: false});
      } else {
        if (!reportsServerEndpointUrl || !reportsServerEndpointUrl.length) {
          this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').reset('http://localhost:8383',
            {emitEvent: false});
        }
      }
    }
    if (useSystemReportsServer) {
      this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').setValidators([]);
    } else {
      this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').setValidators([Validators.required]);
    }
    if (useReportConfigFromMessage) {
      this.generateDashboardReportConfigForm.get('reportConfig').setValidators([]);
    } else {
      this.generateDashboardReportConfigForm.get('reportConfig').setValidators([Validators.required]);
    }
    this.generateDashboardReportConfigForm.get('reportsServerEndpointUrl').updateValueAndValidity({emitEvent});
    this.generateDashboardReportConfigForm.get('reportConfig').updateValueAndValidity({emitEvent});
  }

}

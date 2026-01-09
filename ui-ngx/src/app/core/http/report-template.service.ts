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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import {
  ReportTemplate,
  ReportTemplateConfig,
  ReportTemplateInfo, ReportTemplateQuery,
  ReportTemplateType
} from '@shared/models/report.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class ReportTemplateService {

  constructor(
    private http: HttpClient,
  ) {
  }

  public getReportTemplate<Config extends ReportTemplateConfig>(reportTemplateId: string, config?: RequestConfig): Observable<ReportTemplate<Config>> {
    return this.http.get<ReportTemplate<Config>>(`/api/reportTemplate/${reportTemplateId}`, defaultHttpOptionsFromConfig(config));
  }

  public getReportTemplateInfo(reportTemplateId: string, config?: RequestConfig): Observable<ReportTemplateInfo> {
    return this.http.get<ReportTemplateInfo>(`/api/reportTemplate/info/${reportTemplateId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveReportTemplate<Config extends ReportTemplateConfig>(reportTemplate: ReportTemplate<Config>, config?: RequestConfig): Observable<ReportTemplate<Config>> {
    return this.http.post<ReportTemplate<Config>>('/api/reportTemplate', reportTemplate, defaultHttpOptionsFromConfig(config));
  }

  public deleteReportTemplate(reportTemplateId: string, config?: RequestConfig) {
    return this.http.delete(`/api/reportTemplate/${reportTemplateId}`, defaultHttpOptionsFromConfig(config));
  }

  public getAllReportTemplateInfos(query: ReportTemplateQuery, config?: RequestConfig): Observable<PageData<ReportTemplateInfo>> {
    return this.http.get<PageData<ReportTemplateInfo>>(`/api/reportTemplateInfos/all${query.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getReportTemplatesByIds(reportTemplateIds: string[], config?: RequestConfig): Observable<Array<ReportTemplateInfo>> {
    return this.http.get<Array<ReportTemplateInfo>>(`/api/reportTemplates?reportTemplateIds=${reportTemplateIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((reportTemplates) => sortEntitiesByIds(reportTemplates, reportTemplateIds))
    );
  }

}

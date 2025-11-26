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

import { Inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Report, ReportInfo, ReportQuery, ReportRequest } from '@shared/models/report.models';
import { map } from 'rxjs/operators';
import { WINDOW } from '@core/services/window.service';
import { DOCUMENT } from '@angular/common';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { PageData } from '@shared/models/page/page-data';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { getFilenameFromHttpHeader } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  constructor(
    @Inject(WINDOW) private window: Window,
    @Inject(DOCUMENT) private document: Document,
    private http: HttpClient,
  ) {
  }

  public getReport(reportId: string, config?: RequestConfig): Observable<Report> {
    return this.http.get<Report>(`/api/v2/report/${reportId}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteReport(reportId: string, config?: RequestConfig) {
    return this.http.delete(`/api/v2/report/${reportId}`, defaultHttpOptionsFromConfig(config));
  }

  public getReports(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Report>> {
    return this.http.get<PageData<Report>>(`/api/v2/reports${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getReportInfos(query: ReportQuery, config?: RequestConfig): Observable<PageData<ReportInfo>> {
    return this.http.get<PageData<ReportInfo>>(`/api/v2/reportInfos/all${query.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public downloadReport(reportId: string): Observable<any> {
    const url = `/api/v2/report/${reportId}/download`;
    const response = this.http.get(url, {
      responseType: 'arraybuffer',
      observe: 'response'
    });
    return this.processDownloadReportResponse(response);
  }

  public downloadTestReport(reportRequest: ReportRequest, downloadElseOpen = true): Observable<any> {
    const url = '/api/v2/report/test';
    const response = this.http.post(url, reportRequest, {
      responseType: 'arraybuffer',
      observe: 'response'
    });
    return this.processDownloadReportResponse(response, downloadElseOpen);
  }

  public getReportsInfosByIds(reportIds: string[], config?: RequestConfig): Observable<Array<ReportInfo>> {
    return this.http.get<Array<ReportInfo>>(`/api/v2/reportInfos?reportIds=${reportIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((reportTemplates) => sortEntitiesByIds(reportTemplates, reportIds))
    );
  }

  private processDownloadReportResponse(response: Observable<HttpResponse<ArrayBuffer>>, downloadElseOpen = true): Observable<any> {

    return response.pipe(
      map((response) => {
        const headers = response.headers;
        const contentType = headers.get('content-type');
        const blob = new Blob([response.body], { type: contentType });
        const href = URL.createObjectURL(blob);
        if (downloadElseOpen) {
          const filename = getFilenameFromHttpHeader(headers);
          const linkElement = this.document.createElement('a');
          linkElement.setAttribute('href', href);
          linkElement.setAttribute('download', filename);
          linkElement.click();
          setTimeout(() => URL.revokeObjectURL(href), 0);
        } else {
          this.window.open(href, '_blank');
        }
        return null;
      })
    );
  }

}

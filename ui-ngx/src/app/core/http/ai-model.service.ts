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
import { AiModel, AiModelWithUserMsg, CheckConnectivityResult } from '@shared/models/ai-model.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
export class AiModelService {

  constructor(
    private http: HttpClient
  ) {}

  public saveAiModel(aiModel: AiModel, config?: RequestConfig): Observable<AiModel> {
    return this.http.post<AiModel>('/api/ai/model', aiModel, defaultHttpOptionsFromConfig(config));
  }

  public getAiModels(pageLink: PageLink, config?: RequestConfig): Observable<PageData<AiModel>> {
    return this.http.get<PageData<AiModel>>(`/api/ai/model${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getAiModelById(aiModelId: string, config?: RequestConfig): Observable<AiModel> {
    return this.http.get<AiModel>(`/api/ai/model/${aiModelId}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteAiModel(aiModelId: string, config?: RequestConfig) {
    return this.http.delete(`/api/ai/model/${aiModelId}`, defaultHttpOptionsFromConfig(config));
  }

  public checkConnectivity(aiModelWithUserMsg: AiModelWithUserMsg, config?: RequestConfig): Observable<CheckConnectivityResult> {
    return this.http.post<CheckConnectivityResult>('/api/ai/model/chat', aiModelWithUserMsg, defaultHttpOptionsFromConfig(config));
  }

}

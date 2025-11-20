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

import { InterceptorHttpParams } from '../interceptors/interceptor-http-params';
import { HttpHeaders } from '@angular/common/http';
import { InterceptorConfig } from '../interceptors/interceptor-config';

export type QueryParams = { [param:string]: any };

export interface RequestConfig {
  ignoreLoading?: boolean;
  ignoreErrors?: boolean;
  resendRequest?: boolean;
  queryParams?: QueryParams;
  loadEntityDetails?: boolean;
}

export function hasRequestConfig(config?: any): boolean {
  if (!config) {
    return false;
  }
  return config.hasOwnProperty('ignoreLoading') || config.hasOwnProperty('ignoreErrors') || config.hasOwnProperty('resendRequest') || config.hasOwnProperty('queryParams');
}

export function createDefaultHttpOptions(queryParamsOrConfig?: QueryParams | RequestConfig, config?: RequestConfig) {
  if (hasRequestConfig(queryParamsOrConfig)) {
    return defaultHttpOptionsFromConfig(queryParamsOrConfig as RequestConfig);
  }
  return defaultHttpOptionsFromParams(queryParamsOrConfig as QueryParams, config);
}

export function defaultHttpOptionsFromParams(queryParams?: QueryParams, config?: RequestConfig) {
  const finalConfig = {
    ...config,
    ...(queryParams && {queryParams}),
  };
  return defaultHttpOptionsFromConfig(finalConfig);
}

export function defaultHttpOptionsFromConfig(config?: RequestConfig) {
  if (!config) {
    config = {};
  }
  return defaultHttpOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest, config.queryParams);
}

export function defaultHttpOptions(ignoreLoading: boolean = false,
                                   ignoreErrors: boolean = false,
                                   resendRequest: boolean = false,
                                   queryParams?: QueryParams) {
  const cleanedParams = cleanQueryParams(queryParams);

  return {
    headers: new HttpHeaders({'Content-Type': 'application/json'}),
    params: new InterceptorHttpParams(new InterceptorConfig(ignoreLoading, ignoreErrors, resendRequest), cleanedParams)
  };
}

export function defaultHttpUploadOptions(ignoreLoading: boolean = false,
                                         ignoreErrors: boolean = false,
                                         resendRequest: boolean = false,
                                         queryParams?: QueryParams) {
  const cleanedParams = cleanQueryParams(queryParams);

  return {
    params: new InterceptorHttpParams(new InterceptorConfig(ignoreLoading, ignoreErrors, resendRequest), cleanedParams)
  };
}

function cleanQueryParams(params?: QueryParams): QueryParams | undefined {
  if (!params) {
    return undefined;
  }

  const entries = Object.entries(params);

  const cleanedEntries = entries.filter(
    ([_, value]) => value !== null && value !== undefined
  );

  if (!cleanedEntries.length) {
    return undefined;
  }

  return Object.fromEntries(cleanedEntries);
}

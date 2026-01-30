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

import { TranslateLoader, TranslationObject } from '@ngx-translate/core';
import { forkJoin, Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError, map } from 'rxjs/operators';
import { environment as env } from '@env/environment';
import { mergeDeep } from '@core/utils';

@Injectable({ providedIn: 'root' })
export class TranslateDefaultLoader implements TranslateLoader {

  isAuthenticated = false;

  constructor(private http: HttpClient) {

  }

  getTranslation(lang: string): Observable<TranslationObject> {
    let tasks: Array<Observable<TranslationObject>>
    if (this.isAuthenticated) {
      tasks = [this.http.get<TranslationObject>(`/api/translation/full/${lang}`)];
    } else {
      tasks = [this.http.get<TranslationObject>(`/api/noauth/translation/login/${lang}`)];
    }
    if (!env.production && env.supportedLangs && env.supportedLangs.indexOf(lang) !== -1) {
      tasks.push(this.http.get<TranslationObject>(`/assets/locale/locale.constant-${lang}.json`));
    }
    const observe = forkJoin(tasks).pipe(
      map((results) => {
        if (results.length > 1) {
          return mergeDeep({}, results[0], results[1]);
        }
        return results[0];
      })
    );
    return observe.pipe(
      catchError(() => this.loadSystemLang(lang))
    );
  }

  private loadSystemLang(lang: string): Observable<TranslationObject> {
    const tasks = [
      this.http.get<TranslationObject>(`/assets/locale/locale.constant-${env.defaultLang}.json`)
    ];
    if (env.supportedLangs && env.supportedLangs.indexOf(lang) !== -1) {
      tasks.push(this.http.get<TranslationObject>(`/assets/locale/locale.constant-${lang}.json`));
    }
    return forkJoin(tasks).pipe(
      map((results) => {
        if (results.length > 1) {
          return mergeDeep({}, results[0], results[1]);
        }
        return results[0];
      })
    );
  }
}

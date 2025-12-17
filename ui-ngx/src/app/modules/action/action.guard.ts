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

import { Injectable, NgZone } from '@angular/core';
import { Observable, of } from 'rxjs';
import { AuthState } from '@core/auth/auth.models';
import { select, Store } from '@ngrx/store';
import { selectAuth } from '@core/auth/auth.selectors';
import { mergeMap, skipWhile, take } from 'rxjs/operators';
import { enterZone } from '@core/operator/enterZone';
import { AppState } from '@core/core.state';
import { ActivatedRoute, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { NotificationService } from '@core/http/notification.service';
import { AuthService } from '@core/auth/auth.service';

@Injectable()
export class ActionGuard {

  constructor(private store: Store<AppState>,
              private zone: NgZone,
              private authService: AuthService,
              private dialogs: DialogService,
              private translate: TranslateService,
              private route: ActivatedRoute,
              private notificationService: NotificationService) {

  }

  getAuthState(): Observable<AuthState> {
    return this.store.pipe(
      select(selectAuth),
      skipWhile((authState) => !authState || !authState.isUserLoaded),
      take(1),
      enterZone(this.zone)
    );
  }

  canActivate(next: ActivatedRouteSnapshot,
              state: RouterStateSnapshot) {
    return this.getAuthState().pipe(
      mergeMap((authState) => {
        const url: string = state.url;
        if (authState.isAuthenticated) {
          const lastChild = this.getLastChild(state.root);
          const path = this.extractPath(state.root);
          const prevPath = this.extractPath(this.route.snapshot);
          const prevParams = this.getLastChild(this.route.snapshot).params || {};
          let actionObservable: Observable<any> = of(null);
          if (path === 'action.entitiesLimitIncreaseRequest') {
            actionObservable = this.performEntitiesLimitIncreaseRequest(authState, lastChild);
          }
          return actionObservable.pipe(
            mergeMap(() => {
              const defaultUrl = this.authService.defaultUrl(true, authState, prevPath, prevParams);
              if (defaultUrl) {
                return of(defaultUrl);
              } else {
                return of(false);
              }
            })
          );
        } else {
          this.authService.redirectUrl = url;
          return of(this.authService.defaultUrl(false));
        }
      })
    );
  }

  performEntitiesLimitIncreaseRequest(authState: AuthState, route: ActivatedRouteSnapshot): Observable<any> {
    if (authState.authUser.authority === Authority.TENANT_ADMIN) {
      const entityType = route.queryParams.entityType;
      if (entityType) {
        return this.notificationService.sendEntitiesLimitIncreaseRequest(entityType).pipe(
          mergeMap(() => this.dialogs.alert(
            this.translate.instant('entity.increase-limit-request-sent-title'),
            this.translate.instant('entity.increase-limit-request-sent-text'),
            this.translate.instant('action.close')
          ))
        );
      }
    }
    return of(null);
  }

  private extractPath(snapshot: ActivatedRouteSnapshot): string {
    snapshot = snapshot.root;
    const urlSegments: string[] = [];
    if (snapshot.url) {
      urlSegments.push(...snapshot.url.map(segment => segment.path));
    }
    while (snapshot.children.length) {
      snapshot = snapshot.children[0];
      if (snapshot.url) {
        urlSegments.push(...snapshot.url.map(segment => segment.path));
      }
    }
    return urlSegments.join('.');
  }

  private getLastChild(snapshot: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
    let lastChild = snapshot.root;
    while (lastChild.children.length) {
      lastChild = lastChild.children[0];
    }
    return lastChild;
  }
}

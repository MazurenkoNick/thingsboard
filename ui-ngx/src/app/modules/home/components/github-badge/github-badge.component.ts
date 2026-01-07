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

import { Component, OnDestroy } from '@angular/core';
import { GitHubService } from '@core/http/git-hub.service';
import { Store } from '@ngrx/store';
import { selectAuthUser, selectIsAuthenticated } from '@core/auth/auth.selectors';
import { distinctUntilChanged, filter, map, switchMap, take, takeUntil } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { AppState } from '@core/core.state';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { Subject } from 'rxjs';

const SETTINGS_KEY = 'HIDE_GITHUB_STAR_BUTTON';

@Component({
  selector: 'tb-github-badge',
  templateUrl: './github-badge.component.html',
  styleUrl: './github-badge.component.scss'
})
export class GithubBadgeComponent implements OnDestroy {

  githubStar = 0;

  private stopWatch$ = new Subject<void>();

  constructor(private gitHubService: GitHubService,
              private localStorageService: LocalStorageService,
              private store: Store<AppState>,) {
    const hide = this.localStorageService.getItem(SETTINGS_KEY) ?? false;

    if (!hide) {
      this.store.select(selectIsAuthenticated).pipe(
        filter((data) => data),
        switchMap(() => this.store.select(selectAuthUser).pipe(take(1))),
        map((authUser) => {
          return [Authority.TENANT_ADMIN, Authority.SYS_ADMIN].includes(authUser?.authority ?? Authority.ANONYMOUS)
        }),
        distinctUntilChanged(),
        takeUntil(this.stopWatch$),
      ).subscribe(value => {
        if (value) {
          this.gitHubService.getGitHubStar().subscribe(star => {
            this.githubStar = star;
          });
        } else {
          this.githubStar = 0
        }
      });
    }
  }

  hideGithubStar($event: Event) {
    $event?.stopPropagation();
    this.localStorageService.setItem(SETTINGS_KEY, true);
    this.githubStar = 0;

    this.stopWatch$.next();
    this.stopWatch$.complete();
  }

  ngOnDestroy() {
    this.stopWatch$.next();
    this.stopWatch$.complete();
  }
}

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

import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { resolveGroupParams } from '@shared/models/entity-group.models';

// Walk the activated route's ancestor chain to find the `agentId` param.
// Angular's default `paramsInheritanceStrategy` is `emptyOnly`, so nested
// routes with their own path segments do not auto-inherit params from
// ancestors like `:agentId/applications`.
export function resolveAgentIdParam(route: ActivatedRouteSnapshot): string | undefined {
  let current: ActivatedRouteSnapshot | null = route;
  while (current) {
    const id = current.params?.agentId;
    if (id) {
      return id;
    }
    current = current.parent;
  }
  return undefined;
}

// Build the absolute URL to an agent within its current entity-group scope,
// preserving the tenant/customer prefix and all / groups/:gid / shared/:gid scope
// — mirroring the edge feature's navigateToChildEdgePage (edge-group-config.factory).
// Scope is derived from the canonical resolveGroupParams() rather than parsing the
// URL, so it stays correct under the customer hierarchy too.
// `tail` are trailing segments, e.g. 'applications', appId, 'units', unitId, 'logs'.
export function agentEntityUrl(route: ActivatedRouteSnapshot, agentId: string, ...tail: Array<string>): string {
  const params = resolveGroupParams(route);
  const groups = params?.shared ? 'shared' : 'groups';
  let url: string;
  if (params?.customerId) {
    if (params.childEntityGroupId) {
      url = `customers/${groups}/${params.entityGroupId}/${params.customerId}` +
        `/edgeManagement/agents/groups/${params.childEntityGroupId}/${agentId}`;
    } else {
      const scope = params.entityGroupId ? `groups/${params.entityGroupId}` : 'all';
      url = `customers/all/${params.customerId}/edgeManagement/agents/${scope}/${agentId}`;
    }
  } else if (params?.entityGroupId) {
    url = `edgeManagement/agents/${groups}/${params.entityGroupId}/${agentId}`;
  } else {
    url = `edgeManagement/agents/all/${agentId}`;
  }
  return '/' + [url, ...tail].join('/');
}

// Deepest activated route snapshot — for callers (dialogs, table configs) that
// only have a Router and need the scope of the page currently displayed.
export function currentAgentRouteSnapshot(router: Router): ActivatedRouteSnapshot {
  let snapshot = router.routerState.snapshot.root;
  while (snapshot.firstChild) {
    snapshot = snapshot.firstChild;
  }
  return snapshot;
}

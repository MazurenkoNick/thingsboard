///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { ActivatedRouteSnapshot } from '@angular/router';

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

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

import { AgentAppTemplate } from '@shared/models/agent.models';

// Extracts the leading dotted numeric segments of a version, ignoring any
// trailing suffix (e.g. "4.3.1.2EDGEPE" -> [4, 3, 1, 2]).
function versionSegments(version: string | undefined | null): number[] {
  const match = (version || '').match(/^\d+(?:\.\d+)*/);
  return match ? match[0].split('.').map(seg => parseInt(seg, 10)) : [];
}

// Compares two version strings numerically, newest-first. Missing trailing
// segments count as 0 (so 4.3.1 < 4.3.1.1), and versions with equal numeric
// parts fall back to a descending string compare on the whole label to keep
// the order deterministic for differing suffixes.
export function compareVersionsDesc(a: string | undefined | null, b: string | undefined | null): number {
  const sa = versionSegments(a);
  const sb = versionSegments(b);
  const len = Math.max(sa.length, sb.length);
  for (let i = 0; i < len; i++) {
    const da = sa[i] ?? 0;
    const db = sb[i] ?? 0;
    if (da !== db) {
      return db - da;
    }
  }
  return (b || '').localeCompare(a || '');
}

// Orders templates newest-first by semantic version. The stored nextVersion
// chain is intentionally not used for ordering: it does not follow version
// order (it can link across minor versions), so walking it can put e.g. 4.2.x
// ahead of 4.3.x. Sorting the parsed numeric segments guarantees the whole
// 4.3.* group precedes 4.2.*, etc.
export function orderTemplatesNewestFirst(templates: AgentAppTemplate[]): AgentAppTemplate[] {
  if (!templates?.length) {
    return templates ? [...templates] : [];
  }
  return [...templates].sort((a, b) => compareVersionsDesc(a.currentVersion, b.currentVersion));
}

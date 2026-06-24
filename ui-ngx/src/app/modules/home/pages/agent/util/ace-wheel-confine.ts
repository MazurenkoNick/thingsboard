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

import { Ace } from 'ace-builds';

export function confineWheelToAceEditor(
  host: HTMLElement | null | undefined,
  editor: Ace.Editor | null,
  shouldCaptureVertical: () => boolean = () => true,
  capture = true
): void {
  if (!host || !editor) { return; }
  host.addEventListener('wheel', (ev: WheelEvent) => {
    const session = editor.getSession();
    const renderer: any = editor.renderer;
    const lineHeight = renderer.lineHeight || 16;
    let deltaX = ev.deltaX;
    let deltaY = ev.deltaY;
    if (ev.deltaMode === 1) {            // DOM_DELTA_LINE
      deltaX *= lineHeight;
      deltaY *= lineHeight;
    } else if (ev.deltaMode === 2) {     // DOM_DELTA_PAGE
      deltaX *= (renderer.$size?.scrollerWidth || 0);
      deltaY *= (renderer.$size?.scrollerHeight || 0);
    }
    if (Math.abs(deltaX) > Math.abs(deltaY)) {
      const maxLeft = Math.max(0,
        (renderer.layerConfig?.width || 0) - (renderer.$size?.scrollerWidth || 0));
      const curLeft = session.getScrollLeft();
      const nextLeft = Math.max(0, Math.min(maxLeft, curLeft + deltaX));
      if (nextLeft !== curLeft) { session.setScrollLeft(nextLeft); }
      ev.preventDefault();
      ev.stopPropagation();
      return;
    }
    if (!shouldCaptureVertical()) { return; }
    const maxTop = Math.max(0,
      (renderer.layerConfig?.maxHeight || 0) - (renderer.$size?.scrollerHeight || 0));
    const curTop = session.getScrollTop();
    const nextTop = Math.max(0, Math.min(maxTop, curTop + deltaY));
    ev.stopPropagation();
    if (nextTop !== curTop) {
      session.setScrollTop(nextTop);
      ev.preventDefault();
    }
  }, { passive: false, capture });
}

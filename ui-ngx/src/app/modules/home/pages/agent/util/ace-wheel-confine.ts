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

import { Ace } from 'ace-builds';

/**
 * Confine wheel events to an ace editor, preventing browser back/forward
 * navigation on horizontal swipes and parent-page scroll on vertical swipes.
 *
 * Behaviour:
 * - Horizontal swipe always captured (blocks browser back/forward).
 * - Vertical scroll captured only when `shouldCaptureVertical()` returns true.
 * - Single-axis per event (dominant axis wins) for a code-editor feel.
 *
 * Detail-page callers pass `() => isEdit && editor.isFocused()` so the page
 * scrolls past the read-only editor. Wizard/dialog callers pass `() => true`
 * so the editor always scrolls within the dialog.
 */
export function confineWheelToAceEditor(
  host: HTMLElement | null | undefined,
  editor: Ace.Editor | null,
  shouldCaptureVertical: () => boolean = () => true
): void {
  if (!host || !editor) { return; }
  host.addEventListener('wheel', (ev: WheelEvent) => {
    const captureVertical = shouldCaptureVertical();
    if (!ev.deltaX && !captureVertical) { return; }
    ev.preventDefault();
    ev.stopPropagation();
    const session = editor.getSession();
    if (Math.abs(ev.deltaX) > Math.abs(ev.deltaY)) {
      session.setScrollLeft(session.getScrollLeft() + ev.deltaX);
    } else if (captureVertical) {
      session.setScrollTop(session.getScrollTop() + ev.deltaY);
    }
  }, { passive: false });
}

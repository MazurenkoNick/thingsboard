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

import { Location } from "@angular/common";
import { Inject, Injectable, Injector, Optional, SkipSelf, TemplateRef } from '@angular/core';
import {
  MAT_DIALOG_DEFAULT_OPTIONS,
  MAT_DIALOG_SCROLL_STRATEGY,
  MatDialog,
  MatDialogConfig, MatDialogRef
} from '@angular/material/dialog';
import { DynamicOverlay } from "./dynamic-overlay";
import { DynamicOverlayContainer } from '@shared/components/dialog/dynamic/dynamic-overlay-container';
import { ComponentType, ScrollStrategy } from '@angular/cdk/overlay';
import { DEFAULT_DIALOG_CONFIG, Dialog, DialogConfig } from '@angular/cdk/dialog';

export interface DynamicMatDialogConfig<D> extends MatDialogConfig<D> {
  containerElement?: HTMLElement;
}

@Injectable()
export class DynamicMatDialog extends MatDialog {

  private _customOverlay: DynamicOverlay;

  constructor( _overlay: DynamicOverlay,
               _injector: Injector,
               @Optional() location: Location,
               @Inject( MAT_DIALOG_DEFAULT_OPTIONS ) _defaultOptions: MatDialogConfig,
               @Inject( MAT_DIALOG_SCROLL_STRATEGY ) _scrollStrategy: ScrollStrategy,
               @Optional() @SkipSelf() _parentDialog:DynamicMatDialog,
               _overlayContainer: DynamicOverlayContainer) {

    super( _overlay, _injector, location, _defaultOptions, _scrollStrategy, _parentDialog, _overlayContainer );
    this._dialog = _injector.get(DynamicDialog);
    this._customOverlay = _overlay;
  }

  public open<T, D = any, R = any>(component: ComponentType<T> | TemplateRef<T>, config?: DynamicMatDialogConfig<D>): MatDialogRef<T, R> {
    if (config?.containerElement) {
      config.containerElement.style.transform = 'translateZ(0)';
      this._customOverlay.setContainerElement( config.containerElement );
    }
    const ref = super.open(component, config);
    if (config?.containerElement) {
      ref.afterClosed().subscribe(
        {
          next: () => {
            this._customOverlay.setContainerElement(null);
          },
          error: () => {
            this._customOverlay.setContainerElement(null);
          }
        }
      );
    }
    return ref;
  }
}

@Injectable()
export class DynamicDialog extends Dialog {
  constructor( _overlay: DynamicOverlay,
               _injector: Injector,
               @Inject( DEFAULT_DIALOG_CONFIG ) _defaultOptions: DialogConfig,
               @Inject( MAT_DIALOG_SCROLL_STRATEGY ) _scrollStrategy: ScrollStrategy,
               @Optional() @SkipSelf() _parentDialog: DynamicDialog,
               _overlayContainer: DynamicOverlayContainer) {

    super( _overlay, _injector, _defaultOptions, _parentDialog, _overlayContainer, _scrollStrategy  );
  }
}

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

import { Inject, Injectable, Renderer2, RendererFactory2, RendererStyleFlags2 } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptions } from '@core/http/http-utils';
import { DOCUMENT } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class DevelopmentService {

  private renderer: Renderer2;
  private readonly ROOT: HTMLElement;

  constructor(private http: HttpClient,
              private rendererFactory: RendererFactory2,
              @Inject(DOCUMENT) private document: Document) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.ROOT = this.document.body;
  }

  public checkIsDevelopment(): void {
    this.isDevelopmentMode().subscribe((developmentMode) => {
      if (developmentMode) {
        this.createDevelopmentModeComponent();
        setInterval(() => this.createDevelopmentModeComponent(), 10000);
      }
    });
  }

  private isDevelopmentMode(): Observable<boolean> {
    return this.http.get<boolean>('/api/noauth/system/development', defaultHttpOptions(true));
  }

  private createDevelopmentModeComponent(): void {
    const prevDevComponent = this.document.getElementById('dev-mode-component');
    if (prevDevComponent) {
      prevDevComponent.remove();
    }
    const devComponent: HTMLElement = this.renderer.createElement('div');
    this.renderer.setAttribute(devComponent, 'id', 'dev-mode-component');
    this.renderer.setStyle(devComponent, 'position', 'fixed', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'top', '0', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'bottom', '0', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'left', '0', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'right', '0', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'margin', '0', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'visibility', 'visible', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'z-index', '100000', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'pointer-events', 'none', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'display', 'block', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'background-repeat', 'repeat', RendererStyleFlags2.Important);
    this.renderer.setStyle(devComponent, 'background-image',
      'url("data:image/svg+xml;utf8,<svg xmlns=\'http://www.w3.org/2000/svg\' height=\'140px\' width=\'140px\'><text transform=\'translate(20, 130) rotate(-45)\' fill=\'rgba(200,200,200,0.35)\' font-size=\'20\'>Development mode</text></svg>")',
      RendererStyleFlags2.Important);
    const devModeText = this.renderer.createText('');
    this.renderer.appendChild(devComponent, devModeText);
    this.renderer.appendChild(this.ROOT, devComponent);
  }

}

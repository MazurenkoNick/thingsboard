///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import { Inject, Injectable, Renderer2, RendererFactory2 } from '@angular/core';
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
    this.renderer.setStyle(devComponent, 'position', 'fixed');
    this.renderer.setStyle(devComponent, 'top', '0');
    this.renderer.setStyle(devComponent, 'bottom', '0');
    this.renderer.setStyle(devComponent, 'left', '0');
    this.renderer.setStyle(devComponent, 'right', '0');
    this.renderer.setStyle(devComponent, 'z-index', '100000');
    this.renderer.setStyle(devComponent, 'pointer-events', 'none');
    this.renderer.setStyle(devComponent, 'display', 'flex');
    this.renderer.setStyle(devComponent, 'text-align', 'center');
    this.renderer.setStyle(devComponent, 'justify-content', 'center');
    this.renderer.setStyle(devComponent, 'align-items', 'center');
    this.renderer.setStyle(devComponent, 'font-size', '12vmin');
    this.renderer.setStyle(devComponent, 'font-wight', '600');
    this.renderer.setStyle(devComponent, 'color', 'rgba(200,200,200,0.5)');
    this.renderer.setStyle(devComponent, 'transform', 'rotate(315deg)');
    const devModeText = this.renderer.createText('Development mode');
    this.renderer.appendChild(devComponent, devModeText);
    this.renderer.appendChild(this.ROOT, devComponent);
  }

}

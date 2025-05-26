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

import { NgZone, Pipe, PipeTransform } from '@angular/core';
import { ImageService } from '@core/http/image.service';
import { DomSanitizer, SafeHtml, SafeUrl } from '@angular/platform-browser';
import { forkJoin, Observable, of } from 'rxjs';
import { CustomImageUrlCallback, ImagePipe, UrlHolder } from '@shared/pipe/image.pipe';
import { isImageResourceUrl, removeTbImagePrefix } from '@shared/models/resource.models';
import { map, tap } from 'rxjs/operators';

export type GetImageSrcCallback = (image: HTMLImageElement) => string;

export type SetImageSrcCallback = (image: HTMLImageElement,
                                   origUrl: string, newUrl: string) => void;

@Pipe({
  name: 'htmlWithImage'
})
export class HtmlWithImagePipe implements PipeTransform {

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer) {
  }

  transform(html: string | HTMLElement, args?: any): Observable<SafeHtml | null> {
    const convertToSafeHtml = typeof html === 'string';
    const content = convertToSafeHtml ? $(html) : html;
    const images = $<HTMLImageElement>('img', content);
    const imageTasks: Observable<any>[] = [];
    const getImageSrcCallback: GetImageSrcCallback = args?.getImageSrcCallback || ((image) => image.getAttribute('src'));
    const setImageSrcCallback: SetImageSrcCallback = args?.setImageSrcCallback || null;
    for (const image of images) {
      const origImageUrl = getImageSrcCallback(image);
      imageTasks.push(this.imagePipe.transform(origImageUrl,
        {asString: true, ignoreLoadingImage: true, ...(args || {}) }).pipe(
        tap((newUrl) => {
          image.setAttribute('src', newUrl as string);
          if (setImageSrcCallback) {
            setImageSrcCallback(image, origImageUrl, newUrl);
          }
        })
      ));
    }
    let imagesConvert: Observable<any>;
    if (imageTasks.length) {
      imagesConvert = forkJoin(imageTasks);
    } else {
      imagesConvert = of(null);
    }
    return imagesConvert.pipe(
      map(() => {
        if (convertToSafeHtml) {
          const result = $("<div />").append(content).html();
          return this.sanitizer.bypassSecurityTrustHtml(result);
        } else {
          return null;
        }
      })
    )
  }
}

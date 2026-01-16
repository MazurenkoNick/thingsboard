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

import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { forkJoin, Observable, of } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { map, tap } from 'rxjs/operators';

export type GetImageSrcCallback = (image: HTMLImageElement) => string;

export type SetImageSrcCallback = (image: HTMLImageElement,
                                   origUrl: string, newUrl: string) => void;

@Pipe({
  name: 'htmlWithImage'
})
export class HtmlWithImagePipe implements PipeTransform {

  private domParser: DOMParser;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer) {
    this.domParser = new DOMParser();
  }

  transform(html: string | HTMLElement, args?: any): Observable<SafeHtml | string> {
    const imageTasks: Observable<any>[] = [];
    const getImageSrcCallback: GetImageSrcCallback = args?.getImageSrcCallback || ((image) => image.getAttribute('src'));
    const setImageSrcCallback: SetImageSrcCallback = args?.setImageSrcCallback || null;
    let images: HTMLCollectionOf<HTMLImageElement>;
    let document: Document = null;
    if (typeof html === 'string') {
      document = this.domParser.parseFromString(html, "text/html");
      images = document.images;
    } else {
      images = html.getElementsByTagName("img");
    }
    for (let i= 0; i < images.length; i++) {
      const image = images.item(i);
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
        if (document) {
          const result = document.body.innerHTML;
          return this.sanitizer.bypassSecurityTrustHtml(result);
        }
        return null;
      })
    )
  }
}

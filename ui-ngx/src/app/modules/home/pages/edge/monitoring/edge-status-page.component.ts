// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'tb-edge-status-page',
  templateUrl: './edge-status-page.component.html',
  standalone: false
})
export class EdgeStatusPageComponent {

  readonly edgeTitle: string;

  constructor(route: ActivatedRoute) {
    this.edgeTitle = route.snapshot.data.edgeTitle;
  }
}

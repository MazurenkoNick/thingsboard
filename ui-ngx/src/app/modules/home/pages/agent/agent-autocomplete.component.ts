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

import { Component, ElementRef, forwardRef, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { AgentInfo } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { AgentCreateDialogService } from '@home/pages/agent/agent-create-dialog.service';

@Component({
  selector: 'tb-agent-autocomplete',
  templateUrl: './agent-autocomplete.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AgentAutocompleteComponent),
    multi: true
  }],
  standalone: false
})
export class AgentAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectAgentFormGroup: UntypedFormGroup;
  modelValue: AgentInfo | null = null;
  filteredAgents: Observable<Array<AgentInfo>>;
  searchText = '';

  @ViewChild('agentInput', { static: true }) agentInput: ElementRef;
  @ViewChild('agentAutocomplete', { static: true }) agentAutocomplete: MatAutocomplete;

  private dirty = false;
  private propagateChange = (_: any) => {};

  constructor(public translate: TranslateService,
              public truncate: TruncatePipe,
              private agentService: AgentService,
              private fb: UntypedFormBuilder,
              private agentCreateDialog: AgentCreateDialogService) {
    this.selectAgentFormGroup = this.fb.group({ agent: [null] });
  }

  ngOnInit(): void {
    this.filteredAgents = this.selectAgentFormGroup.get('agent').valueChanges.pipe(
      tap((value: AgentInfo | string) => {
        const modelValue = (typeof value === 'string' || !value) ? null : value;
        this.updateView(modelValue);
      }),
      map(value => !value ? '' : (typeof value === 'string' ? value : value.name)),
      debounceTime(150),
      distinctUntilChanged(),
      switchMap(name => this.fetchAgents(name)),
      share()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.selectAgentFormGroup.disable({ emitEvent: false });
    } else {
      this.selectAgentFormGroup.enable({ emitEvent: false });
    }
  }

  writeValue(value: AgentInfo | null): void {
    this.searchText = '';
    this.modelValue = value || null;
    this.selectAgentFormGroup.get('agent').patchValue(value || null, { emitEvent: false });
    this.dirty = true;
  }

  onFocus(): void {
    if (this.dirty) {
      this.selectAgentFormGroup.get('agent').updateValueAndValidity({ onlySelf: true, emitEvent: true });
      this.dirty = false;
    }
  }

  updateView(agent: AgentInfo | null): void {
    if (this.modelValue?.id?.id !== agent?.id?.id) {
      this.modelValue = agent;
      this.propagateChange(agent);
    }
  }

  displayAgentFn(agent?: AgentInfo): string | undefined {
    return agent ? agent.name : undefined;
  }

  fetchAgents(searchText?: string): Observable<Array<AgentInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, { property: 'name', direction: Direction.ASC });
    return this.agentService.getTenantAgentInfos(pageLink, { ignoreLoading: true } as any).pipe(
      catchError(() => of(emptyPageData<AgentInfo>())),
      map(pageData => pageData.data)
    );
  }

  clear(): void {
    this.selectAgentFormGroup.get('agent').patchValue(null, { emitEvent: true });
    setTimeout(() => {
      this.agentInput.nativeElement.blur();
      this.agentInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return !!(text && text.length > 0);
  }

  createAgent($event: Event): void {
    $event.stopPropagation();
    this.agentCreateDialog.create().subscribe(agent => {
      if (agent) {
        this.selectAgentFormGroup.get('agent').patchValue(agent, { emitEvent: true });
      } else {
        setTimeout(() => {
          this.agentInput.nativeElement.blur();
          this.agentInput.nativeElement.focus();
        }, 0);
      }
    });
  }
}

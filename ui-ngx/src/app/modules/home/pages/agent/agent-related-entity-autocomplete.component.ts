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

import { Component, DestroyRef, ElementRef, EventEmitter, forwardRef, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map, share, startWith } from 'rxjs/operators';
import { entityIdEquals } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { EntityService } from '@core/http/entity.service';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { Edge } from '@shared/models/edge.models';
import { Device } from '@shared/models/device.models';
import { AgentApplicationType } from '@shared/models/agent.models';
import { AgentService } from '@core/http/agent.service';
import { coerceBoolean } from '@shared/decorators/coercion';
import { EdgeCreateDialogService } from '@home/pages/edge/edge-create-dialog.service';
import { AgentGatewayCreateDialogComponent } from '@home/pages/agent/dialog/agent-gateway-create-dialog.component';

interface RelatedOption {
  id: string;
  name: string;
  entityType: EntityType;
}

@Component({
  selector: 'tb-agent-related-entity-autocomplete',
  templateUrl: './agent-related-entity-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AgentRelatedEntityAutocompleteComponent),
    multi: true
  }],
  standalone: false
})
export class AgentRelatedEntityAutocompleteComponent implements ControlValueAccessor, OnInit, OnChanges {

  @Input() appType: AgentApplicationType;

  @Input()
  @coerceBoolean()
  disabled = false;

  @Input() labelKey: string;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  @coerceBoolean()
  allowCreate = false;

  // Emits the display name of the selected entity (null when cleared) so callers
  // can derive defaults (e.g. an app name) — the value accessor only carries the id.
  @Output() relatedEntityNameChange = new EventEmitter<string | null>();

  selectFormGroup: UntypedFormGroup;
  filteredOptions: Observable<RelatedOption[]>;

  @ViewChild('relatedEntityInput', { static: true }) relatedEntityInput: ElementRef<HTMLInputElement>;

  private allOptions$ = new BehaviorSubject<RelatedOption[]>([]);
  private propagateChange: (value: EntityId | null) => void = () => {};
  private modelValue: EntityId | null = null;
  private fetchedOptions: RelatedOption[] = [];
  private managedIds = new Set<string>();

  constructor(private entityService: EntityService,
              private agentService: AgentService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private edgeCreateDialogService: EdgeCreateDialogService,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    this.selectFormGroup = this.fb.group({ relatedEntity: [null] });
  }

  ngOnInit(): void {
    this.loadOptions();
    this.selectFormGroup.get('relatedEntity').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(value => {
      const selected = (value && typeof value !== 'string') ? value as RelatedOption : null;
      const next: EntityId | null = selected ? { id: selected.id, entityType: selected.entityType } : null;
      if (!entityIdEquals(this.modelValue, next)) {
        this.modelValue = next;
        this.propagateChange(next);
        this.relatedEntityNameChange.emit(selected ? selected.name : null);
      }
    });
    const text$ = this.selectFormGroup.get('relatedEntity').valueChanges.pipe(
      startWith(this.selectFormGroup.get('relatedEntity').value),
      map(value => !value ? '' : (typeof value === 'string' ? value : (value as RelatedOption).name))
    );
    this.filteredOptions = combineLatest([this.allOptions$, text$]).pipe(
      map(([all, text]) => {
        if (!text || !text.length) {
          return all;
        }
        const lc = text.toLowerCase();
        return all.filter(o => o.name.toLowerCase().includes(lc));
      }),
      share()
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.appType && !changes.appType.firstChange) {
      this.selectFormGroup.get('relatedEntity').patchValue(null, { emitEvent: true });
      this.loadOptions();
    }
    if (changes.required) {
      this.updateRequiredValidator();
    }
  }

  private updateRequiredValidator(): void {
    const ctrl = this.selectFormGroup.get('relatedEntity');
    if (this.required) {
      ctrl.setValidators([Validators.required]);
    } else {
      ctrl.clearValidators();
    }
    ctrl.updateValueAndValidity({ emitEvent: false });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.selectFormGroup.disable({ emitEvent: false });
    } else {
      this.selectFormGroup.enable({ emitEvent: false });
    }
  }

  writeValue(value: EntityId | null): void {
    if (!value?.id || !value?.entityType) {
      this.modelValue = null;
      this.selectFormGroup.get('relatedEntity').patchValue(null, { emitEvent: false });
      return;
    }
    this.modelValue = { id: value.id, entityType: value.entityType };
    this.recomputeOptions();
    this.entityService.getEntity(value.entityType as EntityType, value.id,
      { ignoreLoading: true, ignoreErrors: true } as any).subscribe({
      next: (e: any) => {
        const opt: RelatedOption = {
          id: value.id,
          entityType: value.entityType as EntityType,
          name: e?.name || value.id
        };
        this.selectFormGroup.get('relatedEntity').patchValue(opt, { emitEvent: false });
        this.relatedEntityNameChange.emit(opt.name);
      }
    });
  }

  displayOptionFn(option?: RelatedOption): string {
    return option ? option.name : '';
  }

  clear() {
    this.selectFormGroup.get('relatedEntity').patchValue(null, { emitEvent: true });
    setTimeout(() => {
      this.relatedEntityInput.nativeElement.blur();
      this.relatedEntityInput.nativeElement.focus();
    }, 0);
  }

  createEntity($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.relatedEntityInput.nativeElement.blur();
    if (this.appType === AgentApplicationType.EDGE) {
      this.edgeCreateDialogService.create().subscribe(edge => this.onEntityCreated(edge));
    } else if (this.appType === AgentApplicationType.GATEWAY) {
      this.dialog.open<AgentGatewayCreateDialogComponent, any, Device>(
        AgentGatewayCreateDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog']
        }).afterClosed().subscribe(device => this.onEntityCreated(device));
    }
  }

  private onEntityCreated(entity: Edge | Device | undefined) {
    const targetType = this.targetEntityType();
    if (!entity || !targetType) {
      return;
    }
    const option: RelatedOption = { id: entity.id.id, entityType: targetType, name: entity.name };
    this.fetchedOptions = [...this.fetchedOptions, option];
    this.recomputeOptions();
    this.selectFormGroup.get('relatedEntity').patchValue(option, { emitEvent: true });
  }

  private loadOptions(): void {
    const targetType = this.targetEntityType();
    if (!targetType) {
      this.fetchedOptions = [];
      this.allOptions$.next([]);
      return;
    }
    combineLatest([
      this.entityService.getEntitiesByNameFilter(targetType, '', -1, '', { ignoreLoading: true } as any).pipe(
        map((entities: Array<Edge | Device>) => (entities || [])
          .filter(e => targetType !== EntityType.DEVICE
            || (e as Device)?.additionalInfo?.gateway === true)
          .map(e => ({ id: e.id.id, entityType: targetType, name: e.name }) as RelatedOption))
      ),
      this.agentService.getManagedRelatedEntityIds(targetType, { ignoreLoading: true } as any)
    ]).subscribe(([options, managed]) => {
      this.fetchedOptions = options;
      this.managedIds = new Set((managed || []).map(id => id.id));
      this.recomputeOptions();
    });
  }

  // Excludes entities already managed by another app, but always keeps the
  // currently-assigned one so it stays selectable while editing.
  private recomputeOptions(): void {
    const currentId = this.modelValue?.id;
    this.allOptions$.next(this.fetchedOptions.filter(o => !this.managedIds.has(o.id) || o.id === currentId));
  }

  private targetEntityType(): EntityType | null {
    if (this.appType === AgentApplicationType.EDGE) { return EntityType.EDGE; }
    if (this.appType === AgentApplicationType.GATEWAY) { return EntityType.DEVICE; }
    return null;
  }
}

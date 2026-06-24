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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  Renderer2,
  ViewContainerRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EntityService } from '@core/http/entity.service';
import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { MatButton, MatIconButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { TbPopoverComponent } from '@shared/components/popover.component';
import {
  AgentAppArgument,
  AgentAppArgumentFormat,
  agentAppArgumentFormatTranslationMap,
  AgentAppArgumentSource,
  agentAppArgumentSourceTranslationMap,
  AgentAppArgumentValueType
} from '@shared/models/agent.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import {
  AgentAppArgumentPanelComponent
} from '@home/pages/agent/component/agent-app-argument-panel.component';

@Component({
  selector: 'tb-agent-app-arguments',
  templateUrl: './agent-app-arguments.component.html',
  styleUrls: ['./agent-app-arguments.component.scss'],
  standalone: false,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AgentAppArgumentsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => AgentAppArgumentsComponent),
      multi: true
    }
  ]
})
export class AgentAppArgumentsComponent implements ControlValueAccessor, Validator {

  @Input()
  set disabled(value: boolean) {
    this.inputDisabled = value;
  }

  get disabled(): boolean {
    return this.inputDisabled || this.cvaDisabled;
  }

  private inputDisabled = false;
  private cvaDisabled = false;

  arguments: AgentAppArgument[] = [];
  expanded = false;

  displayColumns = ['name', 'source', 'target', 'type', 'key', 'format', 'default', 'actions'];

  readonly argumentSourceTranslationMap = agentAppArgumentSourceTranslationMap;
  readonly tokenExample = '${tb.<name>}';

  entityNameMap = new Map<string, string>();

  private popoverComponent: TbPopoverComponent<AgentAppArgumentPanelComponent>;
  private propagateChange: (value: AgentAppArgument[]) => void = () => {};

  constructor(private popoverService: TbPopoverService,
              private viewContainerRef: ViewContainerRef,
              private renderer: Renderer2,
              private entityService: EntityService,
              private destroyRef: DestroyRef,
              private cd: ChangeDetectorRef) {
  }

  getEntityDetailsPageURL(id: string, entityType: EntityType): string {
    return getEntityDetailsPageURL(id, entityType);
  }

  registerOnChange(fn: (value: AgentAppArgument[]) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.cvaDisabled = isDisabled;
  }

  writeValue(value: AgentAppArgument[]): void {
    this.arguments = Array.isArray(value) ? [...value] : [];
    this.updateEntityNameMap();
  }

  private updateEntityNameMap(): void {
    const idsByType = this.arguments.reduce((acc, argument) => {
      const ref = argument.sourceEntityId;
      if (ref?.id && ref?.entityType) {
        acc[ref.entityType] = acc[ref.entityType] ?? [];
        acc[ref.entityType].push(ref.id);
      }
      return acc;
    }, {} as Record<EntityType, string[]>);
    // Drop names whose argument was removed so the map doesn't retain stale ids.
    const referencedIds = new Set(Object.values(idsByType).flat());
    this.entityNameMap.forEach((_name, id) => {
      if (!referencedIds.has(id)) {
        this.entityNameMap.delete(id);
      }
    });
    const tasks = Object.entries(idsByType).map(([entityType, ids]) =>
      this.entityService.getEntities(entityType as EntityType, ids, { ignoreLoading: true })
        // One missing/deleted reference must not blank the whole name map.
        .pipe(catchError(() => of([] as BaseData<EntityId>[]))));
    if (!tasks.length) {
      return;
    }
    forkJoin(tasks as Observable<BaseData<EntityId>[]>[])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(results => {
        results.forEach(entities => entities.forEach(entity => this.entityNameMap.set(entity.id.id, entity.name)));
        this.cd.markForCheck();
      });
  }

  validate(): ValidationErrors | null {
    return null;
  }

  formatLabel(argument: AgentAppArgument): string {
    const format = argument.format ?? AgentAppArgumentFormat.STRING;
    return agentAppArgumentFormatTranslationMap.get(format);
  }

  typeLabel(argument: AgentAppArgument): string {
    if (argument.valueType === AgentAppArgumentValueType.LATEST_TELEMETRY) {
      return 'agent.argument-value-type-latest-telemetry';
    }
    switch (argument.scope) {
      case AttributeScope.SHARED_SCOPE:
        return 'agent.argument-type-shared-attribute';
      case AttributeScope.CLIENT_SCOPE:
        return 'agent.argument-type-client-attribute';
      default:
        return 'agent.argument-type-server-attribute';
    }
  }

  onDelete($event: Event, index: number): void {
    $event?.stopPropagation();
    this.arguments = this.arguments.filter((_, i) => i !== index);
    this.updateModel();
  }

  manageArgument($event: Event, matButton: MatButton | MatIconButton, index: number = -1): void {
    $event?.stopPropagation();
    if (this.disabled) {
      return;
    }
    if (this.popoverComponent && !this.popoverComponent.tbHidden) {
      this.popoverComponent.hide();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
      return;
    }
    const isExisting = index !== -1;
    const ctx = {
      argument: isExisting ? { ...this.arguments[index] } : {} as AgentAppArgument,
      buttonTitle: isExisting ? 'action.apply' : 'action.add',
      usedNames: this.arguments
        .filter((_, i) => i !== index)
        .map(argument => argument.name)
    };
    this.popoverComponent = this.popoverService.displayPopover({
      trigger,
      renderer: this.renderer,
      componentType: AgentAppArgumentPanelComponent,
      hostView: this.viewContainerRef,
      preferredPlacement: isExisting ? ['leftOnly', 'leftTopOnly', 'leftBottomOnly'] : ['rightOnly', 'rightTopOnly', 'rightBottomOnly'],
      context: ctx,
      isModal: true
    });
    this.popoverComponent.tbComponentRef.instance.argumentApplied.subscribe((value: AgentAppArgument) => {
      this.popoverComponent.hide();
      if (isExisting) {
        this.arguments = this.arguments.map((argument, i) => i === index ? value : argument);
      } else {
        this.arguments = [...this.arguments, value];
      }
      this.updateModel();
      this.updateEntityNameMap();
      this.cd.markForCheck();
    });
  }

  private updateModel(): void {
    this.propagateChange(this.arguments);
  }

}

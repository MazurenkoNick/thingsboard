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

import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog } from '@angular/material/dialog';
import { PageLink } from '@shared/models/page/page-link';
import { Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DestroyRef, Renderer2 } from '@angular/core';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { catchError, filter, switchMap } from 'rxjs/operators';
import {
  ArgumentEntityType,
  CalculatedField,
  CalculatedFieldAlarmRule,
  CalculatedFieldType,
} from '@shared/models/calculated-field.models';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import { DatePipe } from '@angular/common';
import {
  AlarmRuleDialogComponent,
  AlarmRuleDialogData
} from "@home/components/alarm-rules/alarm-rule-dialog.component";
import {
  CalculatedFieldDebugDialogComponent,
  CalculatedFieldDebugDialogData
} from "@home/components/calculated-fields/components/debug-dialog/calculated-field-debug-dialog.component";
import { AlarmSeverity, alarmSeverityTranslations } from "@shared/models/alarm.models";
import { UtilsService } from "@core/services/utils.service";

export class AlarmRulesTableConfig extends EntityTableConfig<any> {

  readonly tenantId = getCurrentAuthUser(this.store).tenantId;
  additionalDebugActionConfig = {
    title: this.translate.instant('calculated-fields.see-debug-events'),
    action: (calculatedField: CalculatedField) => this.openDebugEventsDialog.call(this, calculatedField),
  };

  constructor(private calculatedFieldsService: CalculatedFieldsService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe,
              public entityId: EntityId = null,
              private store: Store<AppState>,
              private destroyRef: DestroyRef,
              private renderer: Renderer2,
              public entityName: string,
              private ownerId: EntityId = null,
              private importExportService: ImportExportService,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private utilsService: UtilsService,
              private readonly: boolean = false,
              private hideClearEventAction: boolean = false,
  ) {
    super();
    this.tableTitle = this.translate.instant('alarm-rule.alarm-rules');
    this.detailsPanelEnabled = false;
    this.pageMode = false;
    this.entityType = EntityType.CALCULATED_FIELD;
    this.entityTranslations = {
      type: 'alarm-rule.alarm-rule',
      typePlural: 'alarm-rule.alarm-rules',
      list: 'alarm-rule.list',
      add: 'action.add',
      noEntities: 'alarm-rule.no-found',
      search: 'action.search',
      selectedEntities: 'alarm-rule.selected-fields'
    };

    this.entitiesFetchFunction = (pageLink: PageLink) => this.fetchCalculatedFields(pageLink);
    this.addEntity = this.getCalculatedAlarmDialog.bind(this);
    this.addEnabled = !this.readonly;
    this.entitiesDeleteEnabled = !this.readonly;
    this.deleteEntityTitle = (field: CalculatedField) => this.translate.instant('alarm-rule.delete-title', {title: field.name});
    this.deleteEntityContent = () => this.translate.instant('alarm-rule.delete-text');
    this.deleteEntitiesTitle = count => this.translate.instant('alarm-rule.delete-multiple-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('alarm-rule.delete-multiple-text');
    this.deleteEntity = id => this.calculatedFieldsService.deleteCalculatedField(id.id);
    this.addActionDescriptors = [
      {
        name: this.translate.instant('alarm-rule.create'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('alarm-rule.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: () => this.importCalculatedField()
      }
    ];

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};
    this.columns.push(new DateEntityTableColumn<CalculatedFieldAlarmRule>('createdTime', 'common.created-time', this.datePipe, '150px'));
    this.columns.push(new EntityTableColumn<CalculatedFieldAlarmRule>('name', 'alarm-rule.alarm-type', '33%',
      entity => this.utilsService.customTranslation(entity.name, entity.name)));
    this.columns.push(new EntityTableColumn<CalculatedFieldAlarmRule>('createRule', 'alarm-rule.severities', '67%',
      entity => Object.keys(entity.configuration.createRules).map((severity) => this.translate.instant(alarmSeverityTranslations.get(severity as AlarmSeverity))).join(', '),
      () => ({}), false));
    this.columns.push(new EntityTableColumn<CalculatedFieldAlarmRule>('clearRule', 'alarm-rule.cleared', '70px',
      entity => checkBoxCell(!!entity.configuration.clearRule), ()=> { return {padding: 0, textAlign: 'center'}}, false));

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('action.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: (event$, entity) => this.exportAlarmRule(event$, entity),
      },
      {
        name: this.translate.instant('entity-view.events'),
        icon: 'mdi:clipboard-text-clock',
        isEnabled: () => true,
        onAction: (_, entity) => this.openDebugEventsDialog(entity),
      },
    );
    if (!this.readonly) {
      this.cellActionDescriptors.push({
        name: '',
        nameFunction: entity => this.entityDebugSettingsService.getDebugConfigLabel(entity?.debugSettings),
        icon: 'mdi:bug',
        isEnabled: () => true,
        iconFunction: ({ debugSettings }) => this.entityDebugSettingsService.isDebugActive(debugSettings?.allEnabledUntil) || debugSettings?.failuresEnabled ? 'mdi:bug' : 'mdi:bug-outline',
        onAction: ($event, entity) => this.onOpenDebugConfig($event, entity),
      });
    }
    this.cellActionDescriptors.push({
      name: this.translate.instant('action.edit'),
      nameFunction: () => this.translate.instant(this.readonly ? 'action.view' : 'action.edit'),
      icon: 'edit',
      iconFunction: () => this.readonly ? 'visibility' : 'edit',
      isEnabled: () => true,
      onAction: (_, entity) => this.editCalculatedField(entity),
    });
  }

  fetchCalculatedFields(pageLink: PageLink): Observable<PageData<CalculatedField>> {
    return this.calculatedFieldsService.getCalculatedFields(this.entityId, pageLink, CalculatedFieldType.ALARM);
  }

  onOpenDebugConfig($event: Event, calculatedField: CalculatedField): void {
    const { debugSettings = {}, id } = calculatedField;
    const additionalActionConfig = {
      ...this.additionalDebugActionConfig,
      action: () => this.openDebugEventsDialog(calculatedField)
    };
    if ($event) {
      $event.stopPropagation();
    }

    const { viewContainerRef, renderer } = this.entityDebugSettingsService;
    if (!viewContainerRef || !renderer) {
      this.entityDebugSettingsService.viewContainerRef = this.getTable().viewContainerRef;
      this.entityDebugSettingsService.renderer = this.renderer;
    }

    this.entityDebugSettingsService.openDebugStrategyPanel({
      debugSettings,
      debugConfig: {
        entityType: EntityType.CALCULATED_FIELD,
        entityLabel: 'alarm-rule.alarm-rule',
        additionalActionConfig,
      },
      onSettingsAppliedFn: settings => this.onDebugConfigChanged(id.id, settings)
    }, $event.target as Element);
  }

  private editCalculatedField(calculatedField: CalculatedField, isDirty = false): void {
    this.getCalculatedAlarmDialog(calculatedField, 'action.apply', isDirty)
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }

  private getCalculatedAlarmDialog(value?: CalculatedField, buttonTitle = 'action.add', isDirty = false): Observable<CalculatedField> {
    return this.dialog.open<AlarmRuleDialogComponent, AlarmRuleDialogData, CalculatedField>(AlarmRuleDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        value,
        buttonTitle,
        entityId: this.entityId,
        tenantId: this.tenantId,
        entityName: this.entityName,
        ownerId: this.ownerId,
        additionalDebugActionConfig: this.additionalDebugActionConfig,
        isDirty,
        readonly: this.readonly,
      },
      enterAnimationDuration: isDirty ? 0 : null,
    })
      .afterClosed()
      .pipe(filter(Boolean));
  }

  private openDebugEventsDialog(calculatedField: CalculatedField): void {
    this.dialog.open<CalculatedFieldDebugDialogComponent, CalculatedFieldDebugDialogData, null>(CalculatedFieldDebugDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        tenantId: this.tenantId,
        value: calculatedField,
        getTestScriptDialogFn: null,
        hideClearEventAction: this.hideClearEventAction
      }
    })
      .afterClosed()
      .subscribe();
  }

  private exportAlarmRule($event: Event, calculatedField: CalculatedField): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExportService.exportCalculatedField(calculatedField.id.id);
  }

  private importCalculatedField(): void {
    this.importExportService.openCalculatedFieldImportDialog()
      .pipe(
        filter(Boolean),
        switchMap(calculatedField => this.getCalculatedAlarmDialog(this.updateImportedCalculatedField(calculatedField), 'action.add', true)),
        filter(Boolean),
        switchMap(calculatedField => this.calculatedFieldsService.saveCalculatedField(calculatedField)),
        filter(Boolean),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.updateData());
  }

  private updateImportedCalculatedField(calculatedField: CalculatedField): CalculatedField {
    if (calculatedField.type === CalculatedFieldType.GEOFENCING) {
      calculatedField.configuration.zoneGroups = Object.keys(calculatedField.configuration.zoneGroups).reduce((acc, key) => {
        const arg = calculatedField.configuration.zoneGroups[key];
        acc[key] = arg.refEntityId?.entityType === ArgumentEntityType.Tenant
          ? { ...arg, refEntityId: { id: this.tenantId, entityType: ArgumentEntityType.Tenant } }
          : arg;
        return acc;
      }, {});
    } else {
      calculatedField.configuration.arguments = Object.keys(calculatedField.configuration.arguments).reduce((acc, key) => {
        const arg = calculatedField.configuration.arguments[key];
        acc[key] = arg.refEntityId?.entityType === ArgumentEntityType.Tenant
          ? { ...arg, refEntityId: { id: this.tenantId, entityType: ArgumentEntityType.Tenant } }
          : arg;
        return acc;
      }, {});
    }

    return calculatedField;
  }

  private onDebugConfigChanged(id: string, debugSettings: EntityDebugSettings): void {
    this.calculatedFieldsService.getCalculatedFieldById(id).pipe(
      switchMap(field => this.calculatedFieldsService.saveCalculatedField({ ...field, debugSettings })),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.updateData());
  }
}

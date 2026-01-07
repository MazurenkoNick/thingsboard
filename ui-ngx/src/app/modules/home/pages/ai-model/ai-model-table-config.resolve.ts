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

import { Injectable } from '@angular/core';
import {
  CellActionDescriptor,
  DateEntityTableColumn, defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { ActivatedRouteSnapshot } from '@angular/router';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AiModel, AiProviderTranslations } from '@shared/models/ai-model.models';
import { AiModelService } from '@core/http/ai-model.service';
import { AiModelTableHeaderComponent } from '@home/pages/ai-model/ai-model-table-header.component';
import { AIModelDialogComponent, AIModelDialogData } from '@home/components/ai-model/ai-model-dialog.component';
import { map } from 'rxjs/operators';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class AiModelsTableConfigResolver {

  private readonly config: EntityTableConfig<AiModel> = new EntityTableConfig<AiModel>();

  constructor(
    private datePipe: DatePipe,
    private aiModelService: AiModelService,
    private userPermissionsService: UserPermissionsService,
    private translate : TranslateService,
    private dialog: MatDialog
  ) {
    this.config.selectionEnabled = true;
    this.config.entityType = EntityType.AI_MODEL;
    this.config.addAsTextButton = true;
    this.config.rowPointer = true;
    this.config.detailsPanelEnabled = false;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AI_MODEL);
    this.config.entityResources = entityTypeResources.get(EntityType.AI_MODEL);

    this.config.headerComponent = AiModelTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.addEntity = () => this.addModel(null, true);

    this.config.columns.push(
      new DateEntityTableColumn<AiModel>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<AiModel>('name', 'ai-models.name', '33%'),
      new EntityTableColumn<AiModel>('provider', 'ai-models.provider', '33%',
          entity => this.translate.instant(AiProviderTranslations.get(entity.configuration.provider))
      ),
      new EntityTableColumn<AiModel>('modelId', 'ai-models.model', '33%', entity => entity.configuration.modelId)
    )

    this.config.deleteEntityTitle = model => this.translate.instant('ai-models.delete-model-title', {modelName: model.name});
    this.config.deleteEntityContent = () => this.translate.instant('ai-models.delete-model-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('ai-models.delete-models-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('ai-models.delete-models-text');

    this.config.deleteEntity = id => this.aiModelService.deleteAiModel(id.id);

    this.config.entitiesFetchFunction = pageLink => this.aiModelService.getAiModels(pageLink);

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.handleRowClick = ($event, model) => {
      this.editModel($event, model);
      return true;
    };
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<AiModel> {
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<AiModel>> {
    return [
      {
        name: this.translate.instant('action.edit'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.editModel($event, entity)
      }
    ];
  }

  private editModel($event, AIModel: AiModel): void {
    $event?.stopPropagation();
    this.addModel(AIModel, false).subscribe(res => res ? this.config.updateData() : null);
  }

  private addModel(AIModel: AiModel, isAdd = false): Observable<AiModel> {
    return this.dialog.open<AIModelDialogComponent, AIModelDialogData, AiModel>(AIModelDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        AIModel,
        readonly: !this.userPermissionsService.hasGenericPermission(Resource.AI_MODEL, Operation.WRITE)
      }
    }).afterClosed();
  }
}

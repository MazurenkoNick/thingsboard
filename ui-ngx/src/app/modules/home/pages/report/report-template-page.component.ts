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
  AfterViewChecked,
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  HostBinding,
  OnDestroy,
  OnInit,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { Operation, Resource } from '@shared/models/security.models';
import {
  filterToReportFilter,
  ReportFilter,
  reportFilterToFilter,
  ReportTemplate,
  ReportTemplateSettings
} from '@shared/models/report.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { takeUntil } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { Subject } from 'rxjs';
import { ReportTemplateService } from '@core/http/report-template.service';
import { FiltersDialogComponent, FiltersDialogData } from '@home/components/filter/filters-dialog.component';
import { Filters } from '@shared/models/query/query.models';
import { deepClone } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';
import { EntityAlias, EntityAliases } from '@shared/models/alias.models';
import {
  ReportTemplateSettingsDialogComponent,
  ReportTemplateSettingsDialogData
} from '@home/pages/report/report-template-settings-dialog.component';
import { ReportComponentConfig, ReportComponentType } from '@shared/models/report-component.models';

@Component({
  selector: 'tb-report-template-page',
  templateUrl: './report-template-page.component.html',
  styleUrls: ['./report-template-page.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportTemplatePageComponent extends PageComponent
  implements AfterViewInit, OnInit, OnDestroy, HasDirtyFlag, AfterViewChecked {

  get isDirty(): boolean {
    return this.isDirtyValue;
  }

  set isDirty(value: boolean) {
    this.isDirtyValue = value;
  }

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.REPORT_TEMPLATE, Operation.WRITE);

  isDirtyValue: boolean;

  isFullscreen = false;

  reportTemplate: ReportTemplate;

  updateBreadcrumbs = new EventEmitter();

  selectedReportComponent: ReportComponentConfig;

  private destroy$ = new Subject<void>();

  constructor(private route: ActivatedRoute,
              private userPermissionsService: UserPermissionsService,
              private reportTemplateService: ReportTemplateService,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef) {
    super();
    this.route.data.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      () => {
        this.reset();
        this.init(this.route.snapshot.data.reportTemplate);
      }
    );
  }

  ngOnInit() {

  }

  ngAfterViewChecked(){

  }

  ngAfterViewInit() {

  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  saveReportTemplate() {
    this.reportTemplateService.saveReportTemplate(this.reportTemplate).subscribe(
      (saved) => {
        this.init(saved);
        this.isDirty = false;
        this.cd.markForCheck();
      }
    );
  }

  declineReportTemplate() {
    this.reportTemplateService.getReportTemplate(this.reportTemplate.id.id).subscribe(
      (saved) => {
        this.init(saved);
        this.isDirty = false;
        this.updateBreadcrumbs.emit();
        this.cd.markForCheck();
      }
    );
  }

  public openFilters($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const filters: Filters = {};
    const reportFilters = deepClone(this.reportTemplate.configuration.filters);
    for (const reportFilter of reportFilters) {
      filters[reportFilter.id] = reportFilterToFilter(reportFilter);
    }
    this.dialog.open<FiltersDialogComponent, FiltersDialogData,
      Filters>(FiltersDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        filters,
        disableUserEdit: true,
        widgets: [],
        isSingleFilter: false
      }
    }).afterClosed().subscribe((filters) => {
      if (filters) {
        const reportFilters: ReportFilter[] = [];
        for (const id of Object.keys(filters)) {
          reportFilters.push(filterToReportFilter(filters[id]));
        }
        this.reportTemplate.configuration.filters = reportFilters;
        this.isDirty = true;
        this.cd.markForCheck();
      }
    });
  }

  public openEntityAliases($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const entityAliases: EntityAliases = {};
    const entityAliasesList = deepClone(this.reportTemplate.configuration.entityAliases);
    for (const entityAlias of entityAliasesList) {
      entityAliases[entityAlias.id] = entityAlias;
    }
    this.dialog.open<EntityAliasesDialogComponent, EntityAliasesDialogData,
      EntityAliases>(EntityAliasesDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityAliases,
        widgets: [],
        disableResolveMultiple: true,
        isSingleEntityAlias: false
      }
    }).afterClosed().subscribe((entityAliases) => {
      if (entityAliases) {
        const entityAliasesList: EntityAlias[] = [];
        for (const id of Object.keys(entityAliases)) {
          entityAliasesList.push(entityAliases[id]);
        }
        this.reportTemplate.configuration.entityAliases = entityAliasesList;
        this.isDirty = true;
        this.cd.markForCheck();
      }
    });
  }

  public openReportTemplateSettings($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const settings: ReportTemplateSettings = {
      name: this.reportTemplate.name,
      fileName: this.reportTemplate.configuration.fileName,
      description: this.reportTemplate.description
    };
    this.dialog.open<ReportTemplateSettingsDialogComponent, ReportTemplateSettingsDialogData,
      ReportTemplateSettings>(ReportTemplateSettingsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        settings
      }
    }).afterClosed().subscribe((settings) => {
      if (settings) {
        this.reportTemplate.name = settings.name;
        this.reportTemplate.configuration.fileName = settings.fileName;
        this.reportTemplate.description = settings.description;
        this.isDirty = true;
        this.updateBreadcrumbs.emit();
        this.cd.markForCheck();
      }
    });
  }

  private init(reportTemplate: ReportTemplate) {
    this.reportTemplate = reportTemplate;
    if (!this.reportTemplate.configuration.header) {
      this.reportTemplate.configuration.header = { enabled: true, components: [] };
    }
    if (!this.reportTemplate.configuration.header.components) {
      this.reportTemplate.configuration.header.components = [];
    }
    if (!this.reportTemplate.configuration.footer) {
      this.reportTemplate.configuration.header = { enabled: true, components: [] };
    }
    if (!this.reportTemplate.configuration.footer.components) {
      this.reportTemplate.configuration.footer.components = [];
    }
  }

  private reset(): void {

  }

}

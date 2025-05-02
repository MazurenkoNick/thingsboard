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
  ChangeDetectorRef,
  Component,
  DestroyRef, ElementRef,
  EventEmitter,
  HostBinding,
  OnInit, viewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { Operation, Resource } from '@shared/models/security.models';
import {
  filterToReportFilter, HeaderFooter,
  ReportFilter,
  reportFilterToFilter,
  ReportTemplate,
  ReportTemplateSettings, validateAndUpdateReportTemplate
} from '@shared/models/report.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { ActivatedRoute, Router } from '@angular/router';
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
import { ReportComponentConfig } from '@shared/models/report-component.models';
import { FormBuilder, FormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { reportComponentTypeMap } from '@home/pages/report/components/report-component.models';
import { MatDrawer } from '@angular/material/sidenav';

@Component({
  selector: 'tb-report-template-page',
  templateUrl: './report-template-page.component.html',
  styleUrls: ['./report-template-page.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportTemplatePageComponent extends PageComponent
  implements OnInit, HasDirtyFlag {

  reportComponentTypeMap = reportComponentTypeMap;

  get isDirty(): boolean {
    return this.isDirtyValue;
  }

  set isDirty(value: boolean) {
    this.isDirtyValue = value;
  }

  get currentHeader(): HeaderFooter {
    return this.headerToggleValue === 'header' ? this.reportTemplate.configuration.header :
      this.reportTemplate.configuration.header.firstPage;
  }

  get currentFooter(): HeaderFooter {
    return this.footerToggleValue === 'footer' ? this.reportTemplate.configuration.footer :
      this.reportTemplate.configuration.footer.firstPage;
  }

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  reportComponentsLibrary = viewChild('reportComponentsLibrary', {
    read: MatDrawer,
  });

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.REPORT_TEMPLATE, Operation.WRITE);

  isDirtyValue: boolean;

  isFullscreen = false;

  reportTemplate: ReportTemplate;

  updateBreadcrumbs = new EventEmitter();

  activeReportComponent: ReportComponentConfig;
  editingReportComponent: ReportComponentConfig;

  reportTemplateSettingsFormControl: FormControl;

  headerToggleValue: 'header' | 'firstPageHeader' = 'header';
  footerToggleValue: 'footer' | 'firstPageFooter' = 'footer';

  constructor(private route: ActivatedRoute,
              private userPermissionsService: UserPermissionsService,
              private reportTemplateService: ReportTemplateService,
              private destroyRef: DestroyRef,
              private dialog: MatDialog,
              private fb: FormBuilder,
              private cd: ChangeDetectorRef,
              private router: Router) {
    super();
  }

  ngOnInit() {
    this.reportTemplateSettingsFormControl = this.fb.control(null);
    this.reportTemplateSettingsFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((settings: ReportTemplateSettings) => {
      this.updateReportTemplateSettings(settings);
    });
    this.route.data.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        this.init(this.route.snapshot.data.reportTemplate);
      }
    );
  }

  saveReportTemplate() {
    this.reportTemplateService.saveReportTemplate(this.reportTemplate).subscribe(
      (saved) => {
        this.init(saved);
      }
    );
  }

  declineReportTemplate() {
    this.reportTemplateService.getReportTemplate(this.reportTemplate.id.id).subscribe(
      (saved) => {
        this.init(saved);
      }
    );
  }

  public disableHeader(): void {
    this.currentHeader.enabled = false;
    this.isDirty = true;
  }

  public enableHeader(): void {
    this.currentHeader.enabled = true;
    this.isDirty = true;
  }

  public disableFooter(): void {
    this.currentFooter.enabled = false;
    this.isDirty = true;
  }

  public enableFooter(): void {
    this.currentFooter.enabled = true;
    this.isDirty = true;
  }

  public reportComponentsChanged(): void {
    this.cancelReportComponentEdit();
    this.isDirty = true;
  }

  public editReportComponent(reportComponent: ReportComponentConfig): void {
    if (this.activeReportComponent !== reportComponent) {
      this.activeReportComponent = reportComponent;
      this.editingReportComponent = deepClone(reportComponent);
      const reportComponentsLibrary = this.reportComponentsLibrary()
      if (reportComponentsLibrary) {
        reportComponentsLibrary.close().then();
      }
    }
  }

  public saveReportComponent(): void {
    Object.assign(this.activeReportComponent, this.editingReportComponent);
    this.activeReportComponent = null;
    this.editingReportComponent = null;
    this.isDirty = true;
    const reportComponentsLibrary = this.reportComponentsLibrary()
    if (reportComponentsLibrary) {
      reportComponentsLibrary.open().then();
    }
  }

  public cancelReportComponentEdit(): void {
    this.activeReportComponent = null;
    this.editingReportComponent = null;
    const reportComponentsLibrary = this.reportComponentsLibrary()
    if (reportComponentsLibrary) {
      reportComponentsLibrary.open().then();
    }
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
        this.updateReportTemplateSettings(settings);
        this.reportTemplateSettingsFormControl.patchValue(settings, {emitEvent: false});
      }
    });
  }

  private updateReportTemplateSettings(settings: ReportTemplateSettings): void {
    this.reportTemplate.name = settings.name;
    this.reportTemplate.configuration.fileName = settings.fileName;
    this.reportTemplate.description = settings.description;
    this.isDirty = true;
    this.updateBreadcrumbs.emit();
    this.cd.markForCheck();
  }

  private init(reportTemplate: ReportTemplate) {
    this.cancelReportComponentEdit();
    this.headerToggleValue = 'header';
    this.footerToggleValue = 'footer';
    this.reportTemplate = validateAndUpdateReportTemplate(reportTemplate);
    const settings: ReportTemplateSettings = {
      name: this.reportTemplate.name,
      fileName: this.reportTemplate.configuration.fileName,
      description: this.reportTemplate.description
    };
    this.reportTemplateSettingsFormControl.patchValue(settings, {emitEvent: false});
    this.isDirty = false;
    this.updateBreadcrumbs.emit();
    this.cd.markForCheck();
  }
}

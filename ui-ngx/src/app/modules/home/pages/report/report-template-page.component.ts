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
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef, ElementRef,
  EventEmitter,
  HostBinding, OnDestroy,
  OnInit, QueryList, Renderer2,
  viewChild, ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { Operation, Resource } from '@shared/models/security.models';
import {
  entityAliasesListToAliases,
  entityAliasesToList,
  filtersToReportFilterList,
  HeaderFooter,
  PageOrientation,
  paperSizeToPointsMap,
  PdfReportTemplateConfig,
  PdfReportTemplateSettings,
  reportFilterListToFilters,
  ReportRequest,
  ReportTemplate,
  toPdfReportTemplateSettings,
  updateFromPdfReportTemplateSettings,
  validateAndUpdateReportTemplate
} from '@shared/models/report.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { ActivatedRoute } from '@angular/router';
import { ReportTemplateService } from '@core/http/report-template.service';
import { FiltersDialogComponent, FiltersDialogData } from '@home/components/filter/filters-dialog.component';
import { Filters } from '@shared/models/query/query.models';
import { deepClone } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityAliasesDialogComponent,
  EntityAliasesDialogData
} from '@home/components/alias/entity-aliases-dialog.component';
import { EntityAliases } from '@shared/models/alias.models';
import {
  ReportTemplateSettingsDialogComponent,
  ReportTemplateSettingsDialogData
} from '@home/pages/report/report-template-settings-dialog.component';
import { ReportComponentConfig } from '@shared/models/report-component.models';
import { FormBuilder, FormControl } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  assignReportComponent, pointsToPixels,
  ReportComponentContext,
  reportComponentTypeMap
} from '@home/pages/report/components/report-component.models';
import { EntityService } from '@core/http/entity.service';
import { IStateController, StateParams } from '@core/api/widget-api.models';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { AliasController } from '@core/api/alias-controller';
import { DialogService } from '@core/services/dialog.service';
import { ReportService } from '@core/http/report.service';
import {
  ReportComponentsComponent
} from '@home/pages/report/components/report-components.component';

@Component({
  selector: 'tb-report-template-page',
  templateUrl: './report-template-page.component.html',
  styleUrls: ['./report-template-page.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ReportTemplatePageComponent extends PageComponent
  implements OnInit, AfterViewInit, OnDestroy, HasDirtyFlag {

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

  @ViewChildren(ReportComponentsComponent)
  reportComponentsComponents: QueryList<ReportComponentsComponent>;

  reportTemplateContainerEl = viewChild('reportTemplateContainer', {
    read: ElementRef<HTMLElement>,
  });

  reportTemplateLayoutEl = viewChild('reportTemplateLayout', {
    read: ElementRef<HTMLElement>,
  });

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.REPORT_TEMPLATE, Operation.WRITE);

  isDirtyValue: boolean;

  isFullscreen = false;

  reportTemplate: ReportTemplate<PdfReportTemplateConfig>;

  updateBreadcrumbs = new EventEmitter();

  prevReportComponent: ReportComponentConfig;
  editingReportComponent: ReportComponentConfig;

  reportTemplateSettingsFormControl: FormControl;

  headerToggleValue: 'header' | 'firstPageHeader' = 'header';
  footerToggleValue: 'footer' | 'firstPageFooter' = 'footer';

  reportComponentContext: ReportComponentContext;

  pageWidth: number;

  marginLeft: number;
  marginRight: number;

  contentMarginTop: number;
  contentMarginBottom: number;

  headerMarginTop: number;
  footerMarginBottom: number;

  background: string;

  scale = 1;

  layoutWidth: number;

  private layoutResize$: ResizeObserver;

  // @ts-ignore
  private stateController: IStateController = {
    getStateParams: (): StateParams => ({})
  };

  constructor(private route: ActivatedRoute,
              private userPermissionsService: UserPermissionsService,
              private reportTemplateService: ReportTemplateService,
              private reportService: ReportService,
              private entityService: EntityService,
              private utils: UtilsService,
              private translate: TranslateService,
              private destroyRef: DestroyRef,
              private dialog: MatDialog,
              private dialogService: DialogService,
              private fb: FormBuilder,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef) {
    super();
  }

  ngOnInit() {
    this.reportComponentContext = {
      translate: this.translate,
      utils: this.utils,
      entityService: this.entityService,
      aliasController: null
    };
    this.reportTemplateSettingsFormControl = this.fb.control(null);
    this.reportTemplateSettingsFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((settings: PdfReportTemplateSettings) => {
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

  ngAfterViewInit() {
    this.layoutResize$ = new ResizeObserver(() => {
      this.layoutResize();
    });
    this.layoutResize$.observe(this.reportTemplateLayoutEl().nativeElement);
    setTimeout(() => {
      this.layoutResize();
    });
  }

  ngOnDestroy() {
    if (this.layoutResize$) {
      this.layoutResize$.disconnect();
    }
  }

  saveReportTemplate() {
    this.reportTemplateService.saveReportTemplate(this.reportTemplate).subscribe(
      (saved) => {
        this.init(saved);
      }
    );
  }

  declineReportTemplate() {
    this.reportTemplateService.getReportTemplate<PdfReportTemplateConfig>(this.reportTemplate.id.id).subscribe(
      (saved) => {
        this.init(saved);
      }
    );
  }

  public currentHeaderChanged() {
    this.updatePageLayout();
  }

  public disableHeader(): void {
    this.currentHeader.enabled = false;
    this.updatePageLayout();
    this.isDirty = true;
  }

  public enableHeader(): void {
    this.currentHeader.enabled = true;
    this.updatePageLayout();
    this.isDirty = true;
  }

  public currentFooterChanged() {
    this.updatePageLayout();
  }

  public disableFooter(): void {
    this.currentFooter.enabled = false;
    this.updatePageLayout();
    this.isDirty = true;
  }

  public enableFooter(): void {
    this.currentFooter.enabled = true;
    this.updatePageLayout();
    this.isDirty = true;
  }

  public reportComponentsChanged(): void {
    this.updatePageLayout();
    this.isDirty = true;
  }

  public reportComponentRemoved(reportComponent: ReportComponentConfig) {
    if (this.editingReportComponent === reportComponent) {
      this.cancelReportComponentEdit();
    }
  }

  public editReportComponent(reportComponent: ReportComponentConfig): void {
    if (this.editingReportComponent !== reportComponent) {
      this.editingReportComponent = reportComponent;
      this.prevReportComponent = deepClone(reportComponent);
      this.renderer.addClass(this.reportTemplateContainerEl().nativeElement, 'tb-close-library');
    }
  }

  public reportComponentUpdated() {
    if (this.editingReportComponent) {
      for (let index = 0; index < this.reportComponentsComponents.length; index++) {
        const component = this.reportComponentsComponents.get(index);
        if (component.componentUpdated(this.editingReportComponent)) {
          break;
        }
      }
    }
    this.isDirty = true;
  }

  public saveReportComponent(): void {
    this.prevReportComponent = null;
    this.editingReportComponent = null;
    this.renderer.removeClass(this.reportTemplateContainerEl().nativeElement, 'tb-close-library');
  }

  public cancelReportComponentEdit(): void {
    if (this.editingReportComponent) {
      assignReportComponent(this.editingReportComponent, this.prevReportComponent);
      this.reportComponentUpdated();
      this.prevReportComponent = null;
      this.editingReportComponent = null;
      this.renderer.removeClass(this.reportTemplateContainerEl().nativeElement, 'tb-close-library');
    }
  }

  public openFilters($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const reportFilters = deepClone(this.reportTemplate.configuration.filters);
    const filters = reportFilterListToFilters(reportFilters);
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
        this.reportTemplate.configuration.filters = filtersToReportFilterList(filters);
        this.reportComponentContext.aliasController.updateFilters(filters);
        this.isDirty = true;
        this.cd.markForCheck();
      }
    });
  }

  public openEntityAliases($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const entityAliasesList = deepClone(this.reportTemplate.configuration.entityAliases);
    const entityAliases = entityAliasesListToAliases(entityAliasesList);
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
        this.reportTemplate.configuration.entityAliases = entityAliasesToList(entityAliases);
        this.reportComponentContext.aliasController.updateEntityAliases(entityAliases);
        this.isDirty = true;
        this.cd.markForCheck();
      }
    });
  }

  public openReportTemplateSettings($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const settings = toPdfReportTemplateSettings(this.reportTemplate);
    this.dialog.open<ReportTemplateSettingsDialogComponent, ReportTemplateSettingsDialogData,
      PdfReportTemplateSettings>(ReportTemplateSettingsDialogComponent, {
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

  generateTestReport() {
    const reportRequest: ReportRequest = {
      reportTemplateConfig: this.reportTemplate.configuration
    };
    this.dialogService.progress(
      this.reportService.downloadTestReport(reportRequest, false), this.translate.instant('report.generating-report')).subscribe();
  }

  private updateReportTemplateSettings(settings: PdfReportTemplateSettings): void {
    updateFromPdfReportTemplateSettings(this.reportTemplate, settings);
    this.updatePageLayout();
    this.isDirty = true;
    this.updateBreadcrumbs.emit();
    this.cd.markForCheck();
  }

  private updatePageLayout() {
    const pageSize = this.reportTemplate.configuration.pageSize;
    const orientation = this.reportTemplate.configuration.pageOrientation;
    const pageSizePoints = paperSizeToPointsMap.get(pageSize);

    this.pageWidth = orientation === PageOrientation.PORTRAIT ? pageSizePoints[0] : pageSizePoints[1];
    this.background = this.reportTemplate.configuration.pageBackground;

    this.marginLeft = this.reportTemplate.configuration.pageMargins.left;
    this.marginRight = this.reportTemplate.configuration.pageMargins.right;

    if (this.currentHeader.enabled && this.currentHeader.components?.length) {
      this.headerMarginTop = this.reportTemplate.configuration.pageMargins.top;
      this.contentMarginTop = 0;
    } else {
      this.headerMarginTop = 0;
      this.contentMarginTop = this.reportTemplate.configuration.pageMargins.top;
    }

    if (this.currentFooter.enabled && this.currentFooter.components?.length) {
      this.footerMarginBottom = this.reportTemplate.configuration.pageMargins.bottom;
      this.contentMarginBottom = 0;
    } else {
      this.footerMarginBottom = 0;
      this.contentMarginBottom = this.reportTemplate.configuration.pageMargins.bottom;
    }
    this.updateScale();
  }

  private layoutResize() {
    this.layoutWidth = this.reportTemplateLayoutEl().nativeElement.getBoundingClientRect().width;
    this.reportComponentsComponents.forEach(component => {
      this.renderer.setStyle(component.element.nativeElement, 'maxWidth', this.layoutWidth + 'px');
    });
    this.updateScale();
  }

  private updateScale() {
    if (this.pageWidth && this.layoutWidth) {
      const pageWidthPx = pointsToPixels(this.pageWidth);
      if (pageWidthPx > this.layoutWidth) {
        this.scale = this.layoutWidth / pageWidthPx;
      } else {
        this.scale = 1;
      }
    } else {
      this.scale = 1;
    }
    this.cd.markForCheck();
  }

  private init(reportTemplate: ReportTemplate<PdfReportTemplateConfig>) {
    this.cancelReportComponentEdit();
    this.headerToggleValue = 'header';
    this.footerToggleValue = 'footer';
    this.reportTemplate = validateAndUpdateReportTemplate(reportTemplate);

    this.updatePageLayout();

    const entityAliases = entityAliasesListToAliases(this.reportTemplate.configuration.entityAliases);
    const filters = reportFilterListToFilters(this.reportTemplate.configuration.filters);

    this.reportComponentContext.aliasController = new AliasController(this.utils,
      this.entityService,
      this.translate,
      () => this.stateController,
      entityAliases,
      filters
    );

    const settings = toPdfReportTemplateSettings(this.reportTemplate);

    this.reportTemplateSettingsFormControl.patchValue(settings, {emitEvent: false});
    this.isDirty = false;
    this.updateBreadcrumbs.emit();
    this.cd.markForCheck();
  }

}

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
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Inject,
  NgZone,
  OnDestroy,
  OnInit,
  Renderer2
} from '@angular/core';
import { Router } from '@angular/router';
import { agentEntityUrl, currentAgentRouteSnapshot } from '@home/pages/agent/util/agent-route-params';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EntityType } from '@shared/models/entity-type.models';
import { AgentInfo } from '@shared/models/agent.models';
import { TranslateService } from '@ngx-translate/core';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { generateSecret, guid } from '@core/utils';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { AgentService } from '@core/http/agent.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AttributeScope, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';

@Component({
  selector: 'tb-agent',
  templateUrl: './agent.component.html',
  styleUrls: ['./agent.component.scss'],
  standalone: false
})
export class AgentComponent extends GroupEntityComponent<AgentInfo> implements OnInit, AfterViewInit, OnDestroy {

  entityType = EntityType;
  agentScope: 'tenant' | 'customer' | 'customer_user';

  agentOnline = false;
  agentOnlineKnown = false;

  private activeSub: TelemetrySubscriber | null = null;
  private subscribedAgentId: string | null = null;
  private relocateBadgeTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private agentService: AgentService,
              private router: Router,
              @Inject('entity') protected entityValue: AgentInfo,
              @Inject('entitiesTableConfig')
              protected entitiesTableConfigValue: EntityTableConfig<AgentInfo> | GroupEntityTableConfig<AgentInfo>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef,
              protected userPermissionsService: UserPermissionsService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private elementRef: ElementRef<HTMLElement>,
              private renderer: Renderer2) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, userPermissionsService);
  }

  ngAfterViewInit() {
    // Move the inline status dot (rendered inside the agent's own hidden
    // wrapper) into the front of .tb-details-title so it reads as a status
    // marker on the title ("● profile-based1"). Works for both the full
    // details page and the side details panel — both wrappers use the
    // same .details-toolbar / .tb-details-title classes.
    this.relocateStatusBadge();
  }

  private relocateStatusBadge(retriesLeft = 10) {
    // The badge starts inside our own view (.tb-agent-header-status-host). The
    // toolbar lives in a parent wrapper (mat-card for the full page,
    // tb-details-panel for the side panel) — walk up looking for either.
    const badge = this.elementRef.nativeElement
      .querySelector<HTMLElement>('.tb-agent-header-status');
    let el: HTMLElement | null = this.elementRef.nativeElement;
    let toolbar: HTMLElement | null = null;
    while (el?.parentElement) {
      el = el.parentElement;
      const found = el.querySelector?.('.details-toolbar') as HTMLElement | null;
      if (found) { toolbar = found; break; }
    }
    const title = toolbar?.querySelector<HTMLElement>('.tb-details-title') ?? null;
    if (badge && title) {
      if (badge.parentElement !== title) {
        this.renderer.insertBefore(title, badge, title.firstChild);
      }
      return;
    }
    // The badge has *ngIf="entity?.id?.id" — it may not be in DOM yet on the
    // first pass. Retry a few frames. Track the timer so a pending retry is
    // cancelled on destroy (don't touch a torn-down view).
    if (retriesLeft > 0) {
      if (this.relocateBadgeTimer !== null) {
        clearTimeout(this.relocateBadgeTimer);
      }
      this.relocateBadgeTimer = setTimeout(() => {
        this.relocateBadgeTimer = null;
        this.relocateStatusBadge(retriesLeft - 1);
      }, 32);
    }
  }

  ngOnInit() {
    this.agentScope = this.entitiesTableConfig.componentsData?.agentScope;
    super.ngOnInit();
    this.maybeSubscribeAgentActive();
  }

  ngOnDestroy() {
    if (this.relocateBadgeTimer !== null) {
      clearTimeout(this.relocateBadgeTimer);
      this.relocateBadgeTimer = null;
    }
    this.tearDownActiveSub();
    super.ngOnDestroy();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  isAssignedToCustomer(entity: AgentInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }

  buildForm(entity: AgentInfo): UntypedFormGroup {
    const form = this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      routingKey: this.fb.control({value: entity ? entity.routingKey : null, disabled: true}),
      secret: this.fb.control({value: entity ? entity.secret : null, disabled: true}),
      agentProfileId: [entity?.agentProfileId || null, [Validators.required]],
      description: [entity ? entity.description : '']
    });
    this.generateRoutingKeyAndSecret(entity, form);
    return form;
  }

  updateForm(entity: AgentInfo) {
    this.entityForm.patchValue({
      name: entity.name,
      routingKey: entity.routingKey,
      secret: entity.secret,
      agentProfileId: entity.agentProfileId || null,
      description: entity.description
    });
    this.generateRoutingKeyAndSecret(entity, this.entityForm);
    this.maybeSubscribeAgentActive();
    // The status badge is rendered with *ngIf="entity?.id?.id" — only
    // present after the entity is loaded. Re-run the relocate now that the
    // badge has appeared in the DOM.
    this.relocateStatusBadge();
  }

  updateFormState() {
    super.updateFormState();
    this.entityForm.get('routingKey').disable({emitEvent: false});
    this.entityForm.get('secret').disable({emitEvent: false});
  }

  onManageApplications($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.entity?.id?.id) {
      this.router.navigateByUrl(agentEntityUrl(currentAgentRouteSnapshot(this.router), this.entity.id.id, 'applications'));
    }
  }

  onAgentInfoCopied(type: string) {
    const messageMap: Record<string, string> = {
      id: 'agent.id-copied-message',
      key: 'agent.routing-key-copied-message',
      secret: 'agent.secret-copied-message'
    };
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(messageMap[type]),
      type: 'success',
      duration: 750,
      verticalPosition: 'bottom',
      horizontalPosition: 'right'
    }));
  }

  private generateRoutingKeyAndSecret(entity: AgentInfo, form: UntypedFormGroup) {
    if (!entity?.id?.id) {
      form.get('routingKey').patchValue(guid(), {emitEvent: false});
      form.get('secret').patchValue(generateSecret(20), {emitEvent: false});
    }
  }

  private maybeSubscribeAgentActive() {
    const id = this.entity?.id?.id;
    if (!id) {
      this.tearDownActiveSub();
      this.agentOnline = false;
      this.agentOnlineKnown = false;
      return;
    }
    if (this.subscribedAgentId === id) {
      return;
    }
    this.tearDownActiveSub();
    this.subscribedAgentId = id;
    if (typeof this.entity.active === 'boolean') {
      this.agentOnline = this.entity.active;
      this.agentOnlineKnown = true;
    } else {
      this.agentOnline = false;
      this.agentOnlineKnown = false;
    }
    this.activeSub = TelemetrySubscriber.createEntityAttributesSubscription(
      this.telemetryWsService,
      this.entity.id,
      AttributeScope.SERVER_SCOPE,
      this.zone,
      ['active']
    );
    this.activeSub.data$.subscribe(update => {
      const entries = update?.data?.['active'];
      if (!entries?.length) {
        return;
      }
      const rawValue = entries[0][1];
      const active = rawValue === true || rawValue === 'true';
      this.zone.run(() => {
        this.agentOnline = active;
        this.agentOnlineKnown = true;
        this.cd.markForCheck();
      });
    });
    this.activeSub.subscribe();
  }

  private tearDownActiveSub() {
    if (this.activeSub) {
      this.activeSub.unsubscribe();
      this.activeSub.complete();
      this.activeSub = null;
    }
    this.subscribedAgentId = null;
  }
}

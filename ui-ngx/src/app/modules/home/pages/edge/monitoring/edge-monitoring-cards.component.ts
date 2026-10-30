// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityCountCmd, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import {
  EdgeMonitoringCard,
  EdgeMonitoringCardInfo,
  edgeMonitoringCards,
  EdgeMonitoringContext
} from '@home/pages/edge/monitoring/edge-monitoring.models';

@Component({
  selector: 'tb-edge-monitoring-cards',
  templateUrl: './edge-monitoring-cards.component.html',
  styleUrls: ['./edge-monitoring-cards.component.scss'],
  standalone: false
})
export class EdgeMonitoringCardsComponent extends EntityTableHeaderComponent<any> implements OnInit, OnDestroy {

  ctx: EdgeMonitoringContext;

  cards: EdgeMonitoringCardInfo[] = [];
  counts: {[card: string]: number} = {};

  private subscribers: TelemetrySubscriber[] = [];
  private changedSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit() {
    this.ctx = this.entitiesTableConfig.componentsData.edgeMonitoring;
    this.cards = edgeMonitoringCards.filter(info => info.card !== EdgeMonitoringCard.UPGRADE_AVAILABLE || this.ctx.upgradeEnabled);
    this.subscribeCounts();
    this.changedSubscription = this.ctx.changed$.subscribe(() => this.subscribeCounts());
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.changedSubscription?.unsubscribe();
    this.unsubscribeCounts();
  }

  isSelected(info: EdgeMonitoringCardInfo): boolean {
    return this.ctx.selectedCard === info.card;
  }

  select(info: EdgeMonitoringCardInfo) {
    if (this.ctx.selectedCard !== info.card) {
      this.ctx.selectedCard = info.card;
      this.ctx.reload();
    }
  }

  private subscribeCounts() {
    this.unsubscribeCounts();
    const baseQuery = this.ctx.baseQuery();
    for (const info of this.cards) {
      const cmd = new EntityCountCmd();
      cmd.query = {
        entityFilter: baseQuery.entityFilter,
        keyFilters: [...baseQuery.keyFilters, ...this.ctx.cardKeyFilters(info.card)]
      };
      const subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
      subscriber.subscriptionCommands.push(cmd);
      subscriber.entityCount$.subscribe(update => {
        this.counts[info.card] = update.count;
        this.cd.markForCheck();
      });
      subscriber.subscribe();
      this.subscribers.push(subscriber);
    }
  }

  private unsubscribeCounts() {
    this.subscribers.forEach(subscriber => subscriber.unsubscribe());
    this.subscribers = [];
  }
}

///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';
import { AgentsTableConfigResolver } from '@home/pages/agent/agents-table-config.resolver';
import { AgentGroupsTableConfigResolver } from '@home/pages/agent/agent-groups-table-config.resolver';
import { AgentAppProfilesTableConfigResolver } from '@home/pages/agent/agent-app-profiles-table-config.resolver';
import { AgentApplicationsTableConfigResolver } from '@home/pages/agent/agent-applications-table-config.resolver';
import { AgentEventsPageComponent } from '@home/pages/agent/agent-events-page.component';
import { AgentBulkActionEventsPageComponent } from '@home/pages/agent/agent-bulk-action-events-page.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';

const routes: Routes = [
  {
    path: 'edgeManagement',
    data: {
      breadcrumb: {
        menuId: MenuId.edge_management
      }
    },
    children: [
      {
        path: 'agents',
        data: {
          breadcrumb: {
            menuId: MenuId.agents
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'agent.agents',
              agentsType: 'tenant'
            },
            resolve: {
              entitiesTableConfig: AgentsTableConfigResolver
            }
          },
          {
            path: ':entityId',
            pathMatch: 'full',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'memory'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'agent.agents',
              agentsType: 'tenant'
            },
            resolve: {
              entitiesTableConfig: AgentsTableConfigResolver
            }
          },
          {
            path: ':agentId',
            component: RouterTabsComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              useChildrenRoutesForTabs: true,
              // Skip the parent breadcrumb — the tab host has no meaningful
              // label, and children (applications / events) supply their own.
              breadcrumb: {
                skip: true
              } as BreadCrumbConfig<RouterTabsComponent>
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                redirectTo: 'applications'
              },
              {
                path: 'applications',
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  breadcrumb: {
                    label: 'agent.applications',
                    icon: 'apps'
                  }
                },
                children: [
                  {
                    path: '',
                    component: EntitiesTableComponent,
                    data: {
                      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                      title: 'agent.applications',
                      isPage: true
                    },
                    resolve: {
                      entitiesTableConfig: AgentApplicationsTableConfigResolver
                    }
                  },
                  {
                    path: ':entityId',
                    component: EntityDetailsPageComponent,
                    canDeactivate: [ConfirmOnExitGuard],
                    data: {
                      breadcrumb: {
                        labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                        icon: 'apps'
                      } as BreadCrumbConfig<EntityDetailsPageComponent>,
                      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                      title: 'agent.applications'
                    },
                    resolve: {
                      entitiesTableConfig: AgentApplicationsTableConfigResolver
                    }
                  }
                ]
              },
              {
                path: 'events',
                component: AgentEventsPageComponent,
                data: {
                  auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
                  title: 'agent.app-events',
                  isPage: true,
                  breadcrumb: {
                    label: 'agent.app-events',
                    icon: 'history'
                  }
                }
              }
            ]
          }
        ]
      },
      {
        path: 'agentGroups',
        data: {
          breadcrumb: {
            menuId: MenuId.agent_groups
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'agent.agent-groups'
            },
            resolve: {
              entitiesTableConfig: AgentGroupsTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'group_work'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'agent.agent-groups'
            },
            resolve: {
              entitiesTableConfig: AgentGroupsTableConfigResolver
            }
          },
          {
            path: 'bulk/:bulkActionId',
            component: AgentBulkActionEventsPageComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'agent.bulk-action-details',
              isPage: true,
              breadcrumb: {
                label: 'agent.bulk-action-details',
                icon: 'playlist_play'
              }
            }
          }
        ]
      },
      {
        path: 'agentAppProfiles',
        data: {
          breadcrumb: {
            menuId: MenuId.agent_app_profiles
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'agent.app-profiles'
            },
            resolve: {
              entitiesTableConfig: AgentAppProfilesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'description'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'agent.app-profiles'
            },
            resolve: {
              entitiesTableConfig: AgentAppProfilesTableConfigResolver
            }
          }
        ]
      },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AgentsTableConfigResolver,
    AgentGroupsTableConfigResolver,
    AgentAppProfilesTableConfigResolver,
    AgentApplicationsTableConfigResolver
  ]
})
export class AgentRoutingModule {
}

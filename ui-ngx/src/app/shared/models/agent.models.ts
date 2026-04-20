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

import { BaseData, HasId } from '@shared/models/base-data';
import { AgentId } from '@shared/models/id/agent-id';
import { AgentGroupId } from '@shared/models/id/agent-group-id';
import { AgentApplicationId } from '@shared/models/id/agent-application-id';
import { AgentAppEventId } from '@shared/models/id/agent-app-event-id';
import { AgentAppUnitId } from '@shared/models/id/agent-app-unit-id';
import { AgentAppTemplateId } from '@shared/models/id/agent-app-template-id';
import { AgentAppProfileId } from '@shared/models/id/agent-app-profile-id';
import { AgentBulkActionId } from '@shared/models/id/agent-bulk-action-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityId } from '@shared/models/id/entity-id';

// --- Enums ---

export enum AgentApplicationType {
  GENERIC = 'GENERIC',
  EDGE = 'EDGE',
  GATEWAY = 'GATEWAY'
}

export const agentApplicationTypeTranslationMap = new Map<AgentApplicationType, string>([
  [AgentApplicationType.GENERIC, 'agent.app-type-generic'],
  [AgentApplicationType.EDGE, 'agent.app-type-edge'],
  [AgentApplicationType.GATEWAY, 'agent.app-type-gateway'],
]);

export enum AgentApplicationOrigin {
  INSTALLED = 'INSTALLED',
  DISCOVERED = 'DISCOVERED',
  AUTO_PROVISIONED = 'AUTO_PROVISIONED'
}

export const agentApplicationOriginTranslationMap = new Map<AgentApplicationOrigin, string>([
  [AgentApplicationOrigin.INSTALLED, 'agent.origin-installed'],
  [AgentApplicationOrigin.DISCOVERED, 'agent.origin-discovered'],
  [AgentApplicationOrigin.AUTO_PROVISIONED, 'agent.origin-auto-provisioned'],
]);

export enum AgentAppEventActionType {
  INSTALL = 'INSTALL',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  RESTART = 'RESTART',
  ROLLBACK = 'ROLLBACK',
  UPGRADE = 'UPGRADE'
}

export const agentAppEventActionTypeTranslationMap = new Map<AgentAppEventActionType, string>([
  [AgentAppEventActionType.INSTALL, 'agent.event-action-install'],
  [AgentAppEventActionType.UPDATE, 'agent.event-action-update'],
  [AgentAppEventActionType.DELETE, 'agent.event-action-delete'],
  [AgentAppEventActionType.RESTART, 'agent.event-action-restart'],
  [AgentAppEventActionType.ROLLBACK, 'agent.event-action-rollback'],
  [AgentAppEventActionType.UPGRADE, 'agent.event-action-upgrade'],
]);

export enum AgentAppEventStatus {
  PENDING = 'PENDING',
  QUEUED = 'QUEUED',
  PROCESSING = 'PROCESSING',
  FINISHED = 'FINISHED',
  ERROR = 'ERROR'
}

export const agentAppEventStatusTranslationMap = new Map<AgentAppEventStatus, string>([
  [AgentAppEventStatus.PENDING, 'agent.event-status-pending'],
  [AgentAppEventStatus.QUEUED, 'agent.event-status-queued'],
  [AgentAppEventStatus.PROCESSING, 'agent.event-status-processing'],
  [AgentAppEventStatus.FINISHED, 'agent.event-status-finished'],
  [AgentAppEventStatus.ERROR, 'agent.event-status-error'],
]);

export enum AgentAppEventDeliveryState {
  PENDING = 'PENDING',
  DELIVERED = 'DELIVERED'
}

export enum AgentAppUnitType {
  CONTAINER = 'CONTAINER',
  VOLUME = 'VOLUME',
  NETWORK = 'NETWORK'
}

export const agentAppUnitTypeTranslationMap = new Map<AgentAppUnitType, string>([
  [AgentAppUnitType.CONTAINER, 'agent.unit-type-container'],
  [AgentAppUnitType.VOLUME, 'agent.unit-type-volume'],
  [AgentAppUnitType.NETWORK, 'agent.unit-type-network'],
]);

export enum AgentAppStepType {
  COMPOSE_TEMPLATE = 'COMPOSE_TEMPLATE',
  COMPOSE = 'COMPOSE',
  COMPOSE_START = 'COMPOSE_START',
  COMPOSE_DOWN = 'COMPOSE_DOWN',
  COMPOSE_MIGRATION = 'COMPOSE_MIGRATION',
  ROLLBACK = 'ROLLBACK',
  BACKUP_VOLUME = 'BACKUP_VOLUME',
  BACKUP_VOLUME_REMOVE = 'BACKUP_VOLUME_REMOVE',
  COMPOSE_TYPE_CHOICE = 'COMPOSE_TYPE_CHOICE'
}

export enum AgentAppConfigType {
  DOCKER_COMPOSE = 'DOCKER_COMPOSE'
}

export enum AgentProvisionType {
  DISABLED = 'DISABLED',
  ALLOW_CREATE_NEW_AGENTS = 'ALLOW_CREATE_NEW_AGENTS'
}

export const agentProvisionTypeTranslationMap = new Map<AgentProvisionType, string>([
  [AgentProvisionType.DISABLED, 'agent.provision-type-disabled'],
  [AgentProvisionType.ALLOW_CREATE_NEW_AGENTS, 'agent.provision-type-allow-create'],
]);

export const agentProvisionTypeDescriptionMap = new Map<AgentProvisionType, string>([
  [AgentProvisionType.DISABLED, 'agent.provision-type-disabled-description'],
  [AgentProvisionType.ALLOW_CREATE_NEW_AGENTS, 'agent.provision-type-allow-create-description'],
]);

export enum AgentBulkActionStatus {
  QUEUED = 'QUEUED',
  IN_PROGRESS = 'IN_PROGRESS',
  STARTED = 'STARTED',
  START_FAILED = 'START_FAILED'
}

// --- Entities ---

export interface Agent extends BaseData<AgentId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  description?: string;
  routingKey: string;
  secret: string;
  agentGroupId?: AgentGroupId;
  version?: number;
}

export interface AgentInfo extends Agent {
  customerTitle: string;
  customerIsPublic: boolean;
  groupName?: string;
  // Derived state (backend may populate from ACTIVITY_STATE telemetry).
  active?: boolean;
}

export interface AgentGroup extends BaseData<AgentGroupId> {
  tenantId?: TenantId;
  name: string;
  description?: string;
  provisionKey?: string;
  provisionSecret?: string;
  provisionType?: AgentProvisionType;
  version?: number;
}

export interface AgentGroupInfo extends AgentGroup {
  // Derived counts (backend may populate).
  agentsCount?: number;
  profilesCount?: number;
}

export interface AgentAppConfig {
  type: AgentAppConfigType;
}

export interface DockerComposeConfig extends AgentAppConfig {
  compose: any;
}

export interface AgentApplication extends BaseData<AgentApplicationId> {
  tenantId?: TenantId;
  agentId: AgentId;
  name: string;
  appType: AgentApplicationType;
  templateId?: AgentAppTemplateId;
  config?: AgentAppConfig;
  version?: number;
  projectName?: string;
  origin?: AgentApplicationOrigin;
  applicationProfileId?: AgentAppProfileId;
  profileConfigVersion?: number;
  relatedEntityId?: EntityId;
}

export interface AgentApplicationInfo extends AgentApplication {
  currentVersion?: string;
  nextVersion?: string;
  profileConfigOutdated?: boolean;
  profileName?: string;
}

export interface AgentAppStep {
  id: string;
  title: string;
  type: AgentAppStepType;
  templateOnly?: boolean;
  nextStepId?: string;
  state?: any;
  defaultState?: any;
}

export interface AgentAppStepState {
  [key: string]: any;
}

export interface AgentAppEvent extends BaseData<AgentAppEventId> {
  tenantId?: TenantId;
  applicationId?: AgentApplicationId;
  agentId?: AgentId;
  applicationName?: string;
  actionType: AgentAppEventActionType;
  deliveryState: AgentAppEventDeliveryState;
  status: AgentAppEventStatus;
  currentStepId?: string;
  currentActivity?: string;
  errorMessage?: string;
  updatedTime: number;
  stepStates?: { [stepId: string]: AgentAppStepState };
  bulkActionId?: string;
}

export interface AgentAppEventInfo extends AgentAppEvent {
  applicationName?: string;
}

export interface AgentAppInstallResponse {
  application: AgentApplication;
  event: AgentAppEvent;
}

export interface AgentAppUnit extends BaseData<AgentAppUnitId> {
  agentApplicationId: AgentApplicationId;
  identifier: string;
  type: AgentAppUnitType;
  /** Client-side enrichment: SERVER_SCOPE attribute, populated by the UI. */
  image?: string;
  /** Client-side enrichment: SERVER_SCOPE attribute, populated by the UI. */
  state?: string;
}

export interface AgentAppTemplate extends BaseData<AgentAppTemplateId> {
  tenantId?: TenantId;
  appType: AgentApplicationType;
  config?: AgentAppConfig;
  imageDigest?: string;
  currentVersion: string;
  previousVersion?: string;
  nextVersion?: string;
  startSteps?: AgentAppStep[];
  upgradeSteps?: AgentAppStep[];
  deleteSteps?: AgentAppStep[];
  rollbackSteps?: AgentAppStep[];
  restartSteps?: AgentAppStep[];
  version?: number;
}

export interface AgentAppProfile extends BaseData<AgentAppProfileId> {
  tenantId?: TenantId;
  name: string;
  description?: string;
  appType: AgentApplicationType;
  templateId: AgentAppTemplateId;
  config?: AgentAppConfig;
  version?: number;
}

export interface AgentBulkAction extends BaseData<AgentBulkActionId> {
  tenantId?: TenantId;
  groupId: string;
  profileId: string;
  actionType: AgentAppEventActionType;
  status: AgentBulkActionStatus;
  errorMsg?: string;
  processingStartedTime?: number;
  total: number;
  submitted: number;
  skipCounts?: { [reason: string]: number };
}

// --- Request/Response DTOs ---

export interface AgentAppEventRequest {
  actionType: AgentAppEventActionType;
  application?: AgentApplication;
  stepInputs?: { [stepId: string]: AgentAppStepState };
  bulkActionId?: string;
  skipProfileRefetch?: boolean;
}

export interface BulkOperationRequest {
  actionType: AgentAppEventActionType;
  stepInputs?: { [stepId: string]: AgentAppStepState };
  force?: boolean;
}

export interface BulkOperationPreview {
  total: number;
  eligible: number;
  skippedCountsByReason: { [reason: string]: number };
  skippedSample: SkippedApp[];
}

export interface SkippedApp {
  agentId?: AgentId;
  agentName?: string;
  applicationId: AgentApplicationId;
  applicationName?: string;
  reason: SkipReason;
  msg?: string;
}

export enum SkipReason {
  VERSION_MISMATCH = 'VERSION_MISMATCH',
  ACTIVE_EVENT = 'ACTIVE_EVENT',
  ERROR = 'ERROR'
}

export interface BulkOperationResult {
  total: number;
  submitted: number;
  skipped: SkippedApp[];
}

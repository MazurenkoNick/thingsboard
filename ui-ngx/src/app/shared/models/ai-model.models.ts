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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { HasTenantId } from '@shared/models/entity.models';
import { AiModelId } from '@shared/models/id/ai-model-id';

export interface AiModel extends Omit<BaseData<AiModelId>, 'label'>, HasTenantId, ExportableEntity<AiModelId> {
  modelType: ModelType;
  configuration: {
    provider: AiProvider
    providerConfig: {
      apiKey?: string;
      personalAccessToken?: string;
      endpoint?: string;
      serviceVersion?: string;
      projectId?: string;
      location?: string;
      serviceAccountKey?: string;
      fileName?: string;
      region?: string;
      accessKeyId?: string;
      secretAccessKey?: string;
      baseUrl?: string;
      auth?: {
        type: AuthenticationType;
        username?: string;
        password?: string;
        token?: string
      }
    };
    modelId: string;
    temperature?: number;
    topP?: number;
    topK?: number;
    frequencyPenalty?: number;
    presencePenalty?: number;
    maxOutputTokens?: number;
    contextLength?: number;
  }
}

export enum ModelType {
  CHAT = 'CHAT'
}

export enum AiProvider {
  OPENAI = 'OPENAI',
  AZURE_OPENAI = 'AZURE_OPENAI',
  GOOGLE_AI_GEMINI = 'GOOGLE_AI_GEMINI',
  GOOGLE_VERTEX_AI_GEMINI = 'GOOGLE_VERTEX_AI_GEMINI',
  MISTRAL_AI = 'MISTRAL_AI',
  ANTHROPIC = 'ANTHROPIC',
  AMAZON_BEDROCK = 'AMAZON_BEDROCK',
  GITHUB_MODELS = 'GITHUB_MODELS',
  OLLAMA = 'OLLAMA'
}

export const AiProviderTranslations = new Map<AiProvider, string>(
  [
    [AiProvider.OPENAI , 'ai-models.ai-providers.openai'],
    [AiProvider.AZURE_OPENAI , 'ai-models.ai-providers.azure-openai'],
    [AiProvider.GOOGLE_AI_GEMINI , 'ai-models.ai-providers.google-ai-gemini'],
    [AiProvider.GOOGLE_VERTEX_AI_GEMINI , 'ai-models.ai-providers.google-vertex-ai-gemini'],
    [AiProvider.MISTRAL_AI , 'ai-models.ai-providers.mistral-ai'],
    [AiProvider.ANTHROPIC , 'ai-models.ai-providers.anthropic'],
    [AiProvider.AMAZON_BEDROCK , 'ai-models.ai-providers.amazon-bedrock'],
    [AiProvider.GITHUB_MODELS , 'ai-models.ai-providers.github-models'],
    [AiProvider.OLLAMA , 'ai-models.ai-providers.ollama']
  ]
);

export const ProviderFieldsAllList = [
  'apiKey',
  'personalAccessToken',
  'projectId',
  'location',
  'serviceAccountKey',
  'fileName',
  'endpoint',
  'serviceVersion',
  'region',
  'accessKeyId',
  'secretAccessKey',
  'baseUrl'
];

export const ModelFieldsAllList = ['temperature', 'topP', 'topK', 'frequencyPenalty', 'presencePenalty', 'maxOutputTokens', 'contextLength'];

export const AiModelMap = new Map<AiProvider, { modelList: string[], providerFieldsList: string[], modelFieldsList: string[] }>([
  [
    AiProvider.OPENAI,
    {
      modelList: [
        'o4-mini',
        'o3-pro',
        'o3',
        'o3-mini',
        'o1',
        'gpt-5',
        'gpt-5-mini',
        'gpt-5-nano',
        'gpt-4.1',
        'gpt-4.1-mini',
        'gpt-4.1-nano',
        'gpt-4o',
        'gpt-4o-mini',
      ],
      providerFieldsList: ['baseUrl', 'apiKey'],
      modelFieldsList: ['temperature', 'topP', 'frequencyPenalty', 'presencePenalty', 'maxOutputTokens'],
    },
  ],
  [
    AiProvider.AZURE_OPENAI,
    {
      modelList: [],
      providerFieldsList: ['apiKey', 'endpoint', 'serviceVersion'],
      modelFieldsList: ['temperature', 'topP', 'frequencyPenalty', 'presencePenalty', 'maxOutputTokens'],
    },
  ],
  [
    AiProvider.GOOGLE_AI_GEMINI,
    {
      modelList: [
        'gemini-2.5-pro',
        'gemini-2.5-flash',
        'gemini-2.5-flash-lite',
        'gemini-2.0-flash',
        'gemini-2.0-flash-lite',
      ],
      providerFieldsList: ['apiKey'],
      modelFieldsList: ['temperature', 'topP', 'topK', 'frequencyPenalty', 'presencePenalty', 'maxOutputTokens'],
    },
  ],
  [
    AiProvider.GOOGLE_VERTEX_AI_GEMINI,
    {
      modelList: [
        'gemini-2.5-pro',
        'gemini-2.5-flash',
        'gemini-2.5-flash-lite',
        'gemini-2.0-flash',
        'gemini-2.0-flash-lite',
      ],
      providerFieldsList: ['projectId', 'location', 'serviceAccountKey', 'fileName'],
      modelFieldsList: ['temperature', 'topP', 'topK', 'frequencyPenalty', 'presencePenalty', 'maxOutputTokens'],
    },
  ],
  [
    AiProvider.MISTRAL_AI,
    {
      modelList: [
        'magistral-medium-latest',
        'magistral-small-latest',
        'mistral-large-latest',
        'mistral-medium-latest',
        'mistral-small-latest',
        'pixtral-large-latest',
        'ministral-8b-latest',
        'ministral-3b-latest',
        'open-mistral-nemo',
      ],
      providerFieldsList: ['apiKey'],
      modelFieldsList: ['temperature', 'topP', 'frequencyPenalty', 'presencePenalty', 'maxOutputTokens'],
    },
  ],
  [
    AiProvider.ANTHROPIC,
    {
      modelList: [
        'claude-opus-4-1',
        'claude-opus-4-0',
        'claude-sonnet-4-5',
        'claude-sonnet-4-0',
        'claude-3-7-sonnet-latest',
        'claude-3-5-haiku-latest',
      ],
      providerFieldsList: ['apiKey'],
      modelFieldsList: ['temperature', 'topP', 'topK', 'maxOutputTokens'],
    },
  ],
  [
    AiProvider.AMAZON_BEDROCK,
    {
      modelList: [],
      providerFieldsList: ['region', 'accessKeyId', 'secretAccessKey'],
      modelFieldsList: ['temperature', 'topP', 'maxOutputTokens'],
    },
  ],
  [
    AiProvider.GITHUB_MODELS,
    {
      modelList: [],
      providerFieldsList: ['personalAccessToken'],
      modelFieldsList: ['temperature', 'topP', 'frequencyPenalty', 'presencePenalty', 'maxOutputTokens'],
    },
  ],
  [
    AiProvider.OLLAMA,
    {
      modelList: [],
      providerFieldsList: ['baseUrl'],
      modelFieldsList: ['temperature', 'topP', 'topK', 'maxOutputTokens', 'contextLength'],
    },
  ],
]);

export const AiRuleNodeResponseFormatTypeOnlyText: AiProvider[] = [AiProvider.AMAZON_BEDROCK, AiProvider.ANTHROPIC, AiProvider.GITHUB_MODELS];

export enum ResponseFormat {
  TEXT = 'TEXT',
  JSON = 'JSON',
  JSON_SCHEMA = 'JSON_SCHEMA'
}

export interface AiModelWithUserMsg {
  userMessage: {
    contents: Array<{contentType: string; text: string}>;
  }
  chatModelConfig: {
    modelType: string;
    provider: AiProvider
    providerConfig: {
      apiKey?: string;
      personalAccessToken?: string;
      endpoint?: string;
      serviceVersion?: string;
      projectId?: string;
      location?: string;
      serviceAccountKey?: string;
      fileName?: string;
      baseUrl?: string;
    };
    modelId: string;
    maxRetries: number;
    timeoutSeconds: number;
  }
}

export interface CheckConnectivityResult {
  status: string;
  errorDetails: string;
}
export enum AuthenticationType {
  NONE = 'NONE',
  BASIC = 'BASIC',
  TOKEN = 'TOKEN'
}

/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
/* eslint-disable import/no-unresolved, import/default */

import entityLimitDialogTemplate from './entity-limit.dialog.tpl.html';
import whiteLabelingFeatureDialogTemplate from './white-labeling-feature.dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function SubscriptionDialogs($q, $translate, $mdDialog, $document, types) {


    var service = {
        subscriptionViolation: subscriptionViolation
    };

    return service;

    function subscriptionViolation($event, rejection) {
        var subscriptionErrorCode = rejection.data.subscriptionErrorCode;
        var subscriptionEntry = rejection.data.subscriptionEntry;
        if (subscriptionErrorCode === types.subscriptionErrorCode.LIMIT_REACHED) {
            return showEntityLimitDialog($event, rejection.data);
        } else if (subscriptionErrorCode === types.subscriptionErrorCode.FEATURE_DISABLED &&
            subscriptionEntry === types.subscriptionEntry.WHITE_LABELING) {
            return showWhiteLabelingFeatureDialog($event, rejection.data);
        } else {
            return showSubscriptionAlert($event, rejection.data);
        }
    }

    function showEntityLimitDialog($event, errorData) {
        var subscriptionErrorCode = errorData.subscriptionErrorCode;
        var subscriptionEntry = errorData.subscriptionEntry;
        var value = errorData.subscriptionValue;
        if ($event) {
            $event.stopPropagation();
        }
        return $mdDialog.show({
            controller: 'EntityLimitDialogController',
            controllerAs: 'vm',
            templateUrl: entityLimitDialogTemplate,
            locals: {subscriptionErrorCode: subscriptionErrorCode,
                subscriptionEntry: subscriptionEntry,
                value: value
            },
            parent: angular.element($document[0].body),
            fullscreen: true,
            skipHide: true,
            targetEvent: $event
        });
    }

    function showWhiteLabelingFeatureDialog($event, errorData) {
        var subscriptionErrorCode = errorData.subscriptionErrorCode;
        var subscriptionEntry = errorData.subscriptionEntry;
        var value = errorData.subscriptionValue;
        if ($event) {
            $event.stopPropagation();
        }
        return $mdDialog.show({
            controller: 'WhiteLabelingFeatureDialogController',
            controllerAs: 'vm',
            templateUrl: whiteLabelingFeatureDialogTemplate,
            locals: {subscriptionErrorCode: subscriptionErrorCode,
                subscriptionEntry: subscriptionEntry,
                value: value
            },
            parent: angular.element($document[0].body),
            fullscreen: true,
            skipHide: true,
            targetEvent: $event
        });
    }

    function showSubscriptionAlert($event, errorData) {
        var subscriptionErrorCode = errorData.subscriptionErrorCode;
        var subscriptionEntry = errorData.subscriptionEntry;
        var value = errorData.subscriptionValue;
        var content;
        if (subscriptionEntry && value && types.subscriptionError[subscriptionErrorCode] && types.subscriptionError[subscriptionErrorCode][subscriptionEntry]) {
            var subscriptionErrorText = types.subscriptionError[subscriptionErrorCode][subscriptionEntry];
            content = $translate.instant(subscriptionErrorText, {value: value});
        } else {
            content = errorData.message;
        }
        var alert = $mdDialog.alert().targetEvent($event)
            .title($translate.instant('subscription-error.title'))
            .htmlContent(content)
            .ariaLabel($translate.instant('subscription-error.title'))
            .ok($translate.instant('action.ok'));
        alert._options.skipHide = true;
        alert._options.fullscreen = true;
        return $mdDialog.show(alert);
    }

}
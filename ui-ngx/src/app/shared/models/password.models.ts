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

import { UserPasswordPolicy } from '@shared/models/settings.models';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { isEqual } from '@core/utils';

export enum TooltipPasswordErrorMessageKey {
  minLength = 'security.password-requirement.password-tooltip-min-length',
  maxLength = 'security.password-requirement.password-tooltip-max-length',
  notUpperCase = 'security.password-requirement.password-tooltip-uppercase',
  notLowerCase = 'security.password-requirement.password-tooltip-lowercase',
  notNumeric = 'security.password-requirement.password-tooltip-digit',
  notSpecial = 'security.password-requirement.password-tooltip-special-characters',
  hasWhitespaces = 'security.password-requirement.password-should-not-contain-spaces'
}

export const passwordErrorRules = [
  { key: 'minLength', policyProp: 'minimumLength', translation: TooltipPasswordErrorMessageKey.minLength },
  { key: 'notUpperCase', policyProp: 'minimumUppercaseLetters', translation: TooltipPasswordErrorMessageKey.notUpperCase },
  { key: 'notLowerCase', policyProp: 'minimumLowercaseLetters', translation: TooltipPasswordErrorMessageKey.notLowerCase },
  { key: 'notNumeric', policyProp: 'minimumDigits', translation: TooltipPasswordErrorMessageKey.notNumeric },
  { key: 'notSpecial', policyProp: 'minimumSpecialCharacters', translation: TooltipPasswordErrorMessageKey.notSpecial },
  { key: 'maxLength', policyProp: 'maximumLength', translation: TooltipPasswordErrorMessageKey.maxLength },
  { key: 'hasWhitespaces', policyProp: 'hasWhitespaces', translation: TooltipPasswordErrorMessageKey.hasWhitespaces },
];

export const passwordsMatchValidator = (firstControlName: string, secondControlName: string): ValidatorFn =>{
  return (group: AbstractControl): ValidationErrors | null => {
    const newPassControl = group.get(firstControlName);
    const confirmControl = group.get(secondControlName);

    if (!newPassControl || !confirmControl) {
      return null;
    }

    const newPass = newPassControl.value ?? '';
    const confirm = confirmControl.value ?? '';

    if ((newPass || confirm) && confirm !== newPass) {
      confirmControl.setErrors({ passwordsNotMatch: true });
      return { passwordsNotMatch: true };
    } else {
      const currentErrors = confirmControl?.errors;
      if (currentErrors?.['passwordsNotMatch']) {
        const { passwordsNotMatch, ...rest } = currentErrors;
        confirmControl?.setErrors(Object.keys(rest).length ? rest : null);
      }
      return null;
    }
  };
}

export const passwordStrengthValidator = (passwordPolicy: UserPasswordPolicy): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const value: string = control.value;
    const errors: any = {};

    if (passwordPolicy.minimumUppercaseLetters > 0 &&
      !new RegExp(`(?:.*?[A-Z]){${passwordPolicy.minimumUppercaseLetters}}`).test(value)) {
      errors.notUpperCase = true;
    }

    if (passwordPolicy.minimumLowercaseLetters > 0 &&
      !new RegExp(`(?:.*?[a-z]){${passwordPolicy.minimumLowercaseLetters}}`).test(value)) {
      errors.notLowerCase = true;
    }

    if (passwordPolicy.minimumDigits > 0
      && !new RegExp(`(?:.*?\\d){${passwordPolicy.minimumDigits}}`).test(value)) {
      errors.notNumeric = true;
    }

    if (passwordPolicy.minimumSpecialCharacters > 0 &&
      !new RegExp(`(?:.*?[\\W_]){${passwordPolicy.minimumSpecialCharacters}}`).test(value)) {
      errors.notSpecial = true;
    }

    if (!passwordPolicy.allowWhitespaces && /\s/.test(value)) {
      errors.hasWhitespaces = true;
    }

    if (passwordPolicy.minimumLength > 0 && value.length < passwordPolicy.minimumLength) {
      errors.minLength = true;
    }

    if (!value.length || passwordPolicy.maximumLength > 0 && value.length > passwordPolicy.maximumLength) {
      errors.maxLength = true;
    }

    return isEqual(errors, {}) ? null : errors;
  };
}

/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.security.system;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.settings.SecuritySettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.permission.UserPermissionsService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSystemSecurityServiceTest {

    @Mock
    private AdminSettingsService adminSettingsService;
    @Mock
    private BCryptPasswordEncoder encoder;
    @Mock
    private UserService userService;
    @Mock
    private MailService mailService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private SecuritySettingsService securitySettingsService;
    @Mock
    private JwtTokenFactory tokenFactory;
    @Mock
    private UserPermissionsService userPermissionsService;
    @Mock
    private CustomerService customerService;
    @Mock
    private WhiteLabelingService whiteLabelingService;

    private DefaultSystemSecurityService systemSecurityService;

    private TenantId tenantId;
    private UserId userId;
    private UserCredentials userCredentials;
    private SecuritySettings securitySettings;
    private String username;
    private String password;
    private String encodedPassword;

    @Before
    public void setUp() {
        systemSecurityService = new DefaultSystemSecurityService(adminSettingsService, encoder, tokenFactory, userService, userPermissionsService, customerService, mailService, auditLogService, whiteLabelingService, securitySettingsService);

        tenantId = TenantId.fromUUID(UUID.randomUUID());
        userId = new UserId(UUID.randomUUID());
        username = "tenant@example.com";
        password = "correctPassword";
        encodedPassword = "$2a$10$encodedPasswordHash";

        userCredentials = new UserCredentials();
        userCredentials.setUserId(userId);
        userCredentials.setEnabled(true);
        userCredentials.setPassword(encodedPassword);
        userCredentials.setCreatedTime(System.currentTimeMillis());

        securitySettings = new SecuritySettings();
        securitySettings.setMaxFailedLoginAttempts(5);
        securitySettings.setPasswordPolicy(new UserPasswordPolicy());
    }

    @Test
    public void testValidateUserCredentials_successfulLogin() {
        when(encoder.matches(password, encodedPassword)).thenReturn(true);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password);

        verify(encoder).matches(password, encodedPassword);
        verify(userService).resetFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).increaseFailedLoginAttempts(any(), any());
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_incrementsFailedAttempts() {
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(3);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Authentication Failed");

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).setUserCredentialsEnabled(any(), any(), eq(false));
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_accountLocked() {
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(6);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("locked due to security policy");

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService).setUserCredentialsEnabled(TenantId.SYS_TENANT_ID, userId, false);
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_exactlyAtThreshold_noLock() {
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(5);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Authentication Failed");

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).setUserCredentialsEnabled(any(), any(), eq(false));
    }

    @Test
    public void testValidateUserCredentials_correctPassword_disabledUser_throwsDisabledException() {
        userCredentials.setEnabled(false);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(DisabledException.class)
                .hasMessage("User is not active");

        verify(encoder, never()).matches(any(), any());
        verify(userService, never()).increaseFailedLoginAttempts(any(), any());
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_maxAttemptsDisabled() {
        securitySettings.setMaxFailedLoginAttempts(null);
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(100);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(BadCredentialsException.class);

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).setUserCredentialsEnabled(any(), any(), eq(false));
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_maxAttemptsSetToZero() {
        securitySettings.setMaxFailedLoginAttempts(0);
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(100);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(BadCredentialsException.class);

        verify(userService).increaseFailedLoginAttempts(tenantId, userId);
        verify(userService, never()).setUserCredentialsEnabled(any(), any(), eq(false));
    }

    @Test
    public void testValidateUserCredentials_wrongPassword_withNotificationEmail() throws ThingsboardException {
        String notificationEmail = "admin@example.com";
        securitySettings.setUserLockoutNotificationEmail(notificationEmail);
        when(encoder.matches(password, encodedPassword)).thenReturn(false);
        when(userService.increaseFailedLoginAttempts(tenantId, userId)).thenReturn(6);
        when(securitySettingsService.getSecuritySettings()).thenReturn(securitySettings);

        assertThatThrownBy(() -> systemSecurityService.validateUserCredentials(tenantId, userCredentials, username, password))
                .isInstanceOf(LockedException.class);

        verify(userService).setUserCredentialsEnabled(TenantId.SYS_TENANT_ID, userId, false);
        verify(mailService).sendAccountLockoutEmail(eq(tenantId), eq(username), eq(notificationEmail), eq(5));
    }

}

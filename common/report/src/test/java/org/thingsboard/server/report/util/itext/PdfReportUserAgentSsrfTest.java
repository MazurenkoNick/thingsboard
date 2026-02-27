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
package org.thingsboard.server.report.util.itext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.common.util.SsrfProtectionValidator;
import org.xhtmlrenderer.pdf.ITextOutputDevice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PdfReportUserAgentSsrfTest {

    private PdfReportUserAgent userAgent;

    @BeforeEach
    void setUp() {
        ITextOutputDevice outputDevice = mock(ITextOutputDevice.class);
        userAgent = new PdfReportUserAgent(null, null, outputDevice, 1, 800);
    }

    @AfterEach
    void tearDown() {
        SsrfProtectionValidator.setEnabled(false);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/hosts",
            "file:///etc/passwd",
            "ftp://internal.host/file"
    })
    void testBlockedSchemesReturnNull(String uri) {
        SsrfProtectionValidator.setEnabled(true);
        assertThat(userAgent.resolveAndOpenStream(uri)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://169.254.169.254/latest/meta-data/",
            "http://localhost/secret",
            "http://127.0.0.1/admin",
            "http://10.0.0.1/internal",
            "http://192.168.1.1/config",
            "http://[::1]/path"
    })
    void testBlockedHostsReturnNull(String uri) {
        SsrfProtectionValidator.setEnabled(true);
        assertThat(userAgent.resolveAndOpenStream(uri)).isNull();
    }

    @Test
    void testInvalidUriReturnsNull() {
        SsrfProtectionValidator.setEnabled(true);
        assertThat(userAgent.resolveAndOpenStream("://broken uri{")).isNull();
    }

}

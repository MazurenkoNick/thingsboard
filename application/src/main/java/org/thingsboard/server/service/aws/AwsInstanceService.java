/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.user.UserService;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service("awsInstanceService")
@Slf4j
public class AwsInstanceService {

    private static final String FIRST_LAUNCH_FILE = ".first_launch";

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        try {
            Path firstLaunchPath = Paths.get(FIRST_LAUNCH_FILE);
            if (!firstLaunchPath.toFile().exists()) {
                String awsInstanceId = getAwsInstanceId();
                log.info("Updating SysAdmin password with AWS instanceId [{}].", awsInstanceId);
                User sysadminUser = userService.findUserByEmail(TenantId.SYS_TENANT_ID,"sysadmin@thingsboard.org");
                UserCredentials credentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, sysadminUser.getId());
                if (!passwordEncoder.matches(awsInstanceId, credentials.getPassword())) {
                    credentials.setPassword(passwordEncoder.encode(awsInstanceId));
                    userService.saveUserCredentials(TenantId.SYS_TENANT_ID, credentials);
                    log.info("SysAdmin password successfully updated.");
                } else {
                    log.info("SysAdmin password already set to AWS instanceId.");
                }
                Files.createFile(firstLaunchPath);
            }
        } catch (Exception e) {
            log.error("Failed to init ThingsBoard on AWS instance.", e);
        }
    }

    private String getAwsInstanceId() {
        RestTemplate restClient = new RestTemplate();
        String instanceId = restClient.getForObject("http://169.254.169.254/latest/meta-data/instance-id", String.class);
        return instanceId;
    }

}

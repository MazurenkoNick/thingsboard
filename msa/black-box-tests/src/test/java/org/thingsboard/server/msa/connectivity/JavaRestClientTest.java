/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.msa.connectivity;

import com.google.gson.JsonObject;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.mobile.app.MobileApp;
import org.thingsboard.server.common.data.mobile.app.MobileAppStatus;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundle;
import org.thingsboard.server.common.data.mobile.bundle.MobileAppBundleInfo;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestPreview;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.UserNotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationTarget;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.platform.UserListFilter;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.HasSubject;
import org.thingsboard.server.common.data.notification.template.MobileAppDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.notification.template.SmsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.WebDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.AvailableEntityKeys;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.TestProperties;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.WEB;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantAdmin;

public class JavaRestClientTest extends AbstractContainerTest {

    public static final String DEFAULT_NOTIFICATION_SUBJECT = "Just a test";
    public static final NotificationType DEFAULT_NOTIFICATION_TYPE = NotificationType.GENERAL;
    private RestClient restClient;
    private Tenant tenant;
    private User user;

    @BeforeClass
    public void beforeClass() throws Exception {
        SSLContext ssl = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();

        var tls = new DefaultClientTlsStrategy(
                ssl,
                HostnameVerificationPolicy.CLIENT,
                NoopHostnameVerifier.INSTANCE
        );

        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tls)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();

        RestTemplate rt = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        restClient = new RestClient(rt, TestProperties.getBaseUrl());
    }

    @BeforeMethod
    public void setUp() throws Exception {
        restClient.login("sysadmin@thingsboard.org", "sysadmin");

        // create tenant and tenant admin
        tenant = new Tenant();
        tenant.setTitle("Java Rest Client Test Tenant " + RandomStringUtils.randomAlphabetic(5));
        tenant = restClient.saveTenant(tenant);

        String email = RandomStringUtils.randomAlphabetic(5) + "@gmail.com";
        user = restClient.saveUser(defaultTenantAdmin(tenant.getId(), email), false);
        restClient.activateUser(user.getId(), "password123", false);
        restClient.login(email, "password123");
    }

    @AfterMethod
    public void tearDown() {
        restClient.login("sysadmin@thingsboard.org", "sysadmin");
        if (tenant != null) {
            restClient.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testGetAlarmsV2() {
        Device device = restClient.saveDevice(defaultDevicePrototype(RandomStringUtils.randomAlphabetic(5)));
        assertThat(device).isNotNull();

        String type = "High temp" + RandomStringUtils.randomAlphabetic(5);
        Alarm alarm = Alarm.builder()
                .originator(device.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type(type)
                .build();
        restClient.saveAlarm(alarm);

        // get /api/v2/alarm
        PageData<AlarmInfo> alarmsV2 = restClient.getAlarmsV2(device.getId(), null, null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(alarmsV2.getData()).hasSize(1);

        PageData<AlarmInfo> activeAlarms = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.ACTIVE), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(activeAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> cleared = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.CLEARED), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(cleared.getData()).hasSize(0);

        PageData<AlarmInfo> activeAndClearedAlarms = restClient.getAlarmsV2(device.getId(), List.of(AlarmSearchStatus.CLEARED, AlarmSearchStatus.ACTIVE), null, null, null, new TimePageLink(10, 0));
        assertThat(activeAndClearedAlarms.getData()).hasSize(1);

        // get /api/v2/alarms
        PageData<AlarmInfo> allAlarmsV2 = restClient.getAllAlarmsV2(List.of(AlarmSearchStatus.ACTIVE), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(allAlarmsV2.getData()).hasSize(1);

        PageData<AlarmInfo> allClearedAlarmsV2 = restClient.getAllAlarmsV2(List.of(AlarmSearchStatus.CLEARED), null, List.of(type), null, new TimePageLink(10, 0));
        assertThat(allClearedAlarmsV2.getData()).hasSize(0);

        // get /api/alarms
        PageData<AlarmInfo> allAlarms = restClient.getAllAlarms(AlarmSearchStatus.ACTIVE, null, new TimePageLink(10, 0), null);
        assertThat(allAlarms.getData()).hasSize(1);

        PageData<AlarmInfo> allClearedAlarms = restClient.getAllAlarms(AlarmSearchStatus.CLEARED, null, new TimePageLink(10, 0), null);
        assertThat(allClearedAlarms.getData()).hasSize(0);
    }

    @Test
    public void testTimeSeriesByReadTsKvQueries() {
        Device device = restClient.saveDevice(defaultDevicePrototype(RandomStringUtils.randomAlphabetic(5)));
        assertThat(device).isNotNull();

        DeviceCredentials deviceCredentials = restClient.getDeviceCredentialsByDeviceId(device.getId()).get();
        for (int i = 0; i < 3; i++) {
            JsonObject values = new JsonObject();
            values.addProperty("temperature", i + 25);
            testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), JacksonUtil.toJsonNode(createPayload().toString()));
        }

        restClient.saveEntityTelemetry(device.getId(), "ts", JacksonUtil.toJsonNode("{\"temperature\": 25, \"humidity\": 60}"));
        restClient.saveEntityTelemetry(device.getId(), "ts", JacksonUtil.toJsonNode("{\"temperature\": 27, \"humidity\": 59}"));
        restClient.saveEntityTelemetry(device.getId(), "ts", JacksonUtil.toJsonNode("{\"temperature\": 33, \"humidity\": 62}"));

        List<BaseReadTsKvQuery> queries = new ArrayList<>();
        BaseReadTsKvQuery tempQuery = new BaseReadTsKvQuery("temperature", System.currentTimeMillis() - 5000, System.currentTimeMillis(), 5000, 3, Aggregation.AVG);
        BaseReadTsKvQuery humQuery = new BaseReadTsKvQuery("humidity", System.currentTimeMillis() - 5000, System.currentTimeMillis(), 5000, 3, Aggregation.MAX);
        queries.add(tempQuery);
        queries.add(humQuery);
        List<ReadTsKvQueryResult> results = restClient.getTimeseriesByQueries(device.getId(), queries);
        assertThat(results).isNotNull().hasSize(2);

        ReadTsKvQueryResult tempQueryResult = results.get(0);
        assertThat(tempQueryResult.getData()).hasSize(1);
        TsKvEntry tempTsKv = tempQueryResult.getData().get(0);
        assertThat(tempTsKv.getKey()).isEqualTo("temperature");
        assertThat(tempTsKv.getValue()).isEqualTo((25 + 27 + 33) / 3d);

        ReadTsKvQueryResult humQueryResult = results.get(1);
        assertThat(humQueryResult.getData()).hasSize(1);
        TsKvEntry humTsKv = humQueryResult.getData().get(0);
        assertThat(humTsKv.getKey()).isEqualTo("humidity");
        assertThat(humTsKv.getValue()).isEqualTo(62L);

        EntityTypeFilter filter = new EntityTypeFilter();
        filter.setEntityType(EntityType.DEVICE);
        var pageLink = new EntityDataPageLink(20, 0, null, new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC), false);

        var entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));

        EntityDataQuery entityDataQuery = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        AvailableEntityKeys availableEntityKeys = restClient.findAvailableEntityKeysByQuery(entityDataQuery, true, true, null);
        assertThat(availableEntityKeys).isNotNull();
        assertThat(availableEntityKeys.timeseries()).contains("temperature", "humidity");
    }

    @Test
    public void testFindNotifications() {
        NotificationTarget notificationTarget = createNotificationTarget(user.getId());
        String notificationText1 = "Notification 1";
        NotificationTemplate notificationTemplate = createNotificationTemplate(DEFAULT_NOTIFICATION_TYPE, DEFAULT_NOTIFICATION_SUBJECT, notificationText1, new NotificationDeliveryMethod[]{WEB});
        NotificationRequest notificationRequest = submitNotificationRequest(notificationTarget.getId(), notificationTemplate.getId());

        String notificationText2 = "Notification 2";
        NotificationTemplate notificationTemplate2 = createNotificationTemplate(DEFAULT_NOTIFICATION_TYPE, DEFAULT_NOTIFICATION_SUBJECT, notificationText2, new NotificationDeliveryMethod[]{WEB});
        NotificationRequest notificationRequest2 = submitNotificationRequest(notificationTarget.getId(), notificationTemplate2.getId());

        PageData<NotificationRequestInfo> initialRequests = restClient.getNotificationRequests(new PageLink(30));
        assertThat(initialRequests.getTotalElements()).isGreaterThanOrEqualTo(2);

        NotificationRequestInfo notificationRequestInfo = restClient.getNotificationRequestById(notificationRequest.getId()).get();
        assertThat(notificationRequestInfo.getName()).isEqualTo(notificationRequest.getName());
        assertThat(notificationRequestInfo.getTemplateName()).isEqualTo(notificationTemplate.getName());

        NotificationRequestPreview requestPreview = restClient.getNotificationRequestPreview(notificationRequest, 10);
        assertThat(requestPreview.getTotalRecipientsCount()).isEqualTo(1);
        assertThat(requestPreview.getRecipientsPreview()).isEqualTo(List.of(user.getEmail()));

        PageData<Notification> notifications = restClient.getNotifications(false, WEB, new PageLink(30));
        assertThat(notifications.getTotalElements()).isEqualTo(2);

        Integer unreadCount = restClient.getUnreadNotificationsCount(WEB);
        assertThat(unreadCount).isEqualTo(2);

        restClient.markNotificationAsRead(notifications.getData().get(0).getId());

        Integer unreadCountAfterRead = restClient.getUnreadNotificationsCount(WEB);
        assertThat(unreadCountAfterRead).isEqualTo(1);

        restClient.markAllNotificationsAsRead(WEB);

    }

    @Test
    public void testSaveNotificationSettings() {
        NotificationSettings settings = new NotificationSettings();
        SlackNotificationDeliveryMethodConfig slackConfig = new SlackNotificationDeliveryMethodConfig();
        String slackToken = "xoxb-123123123";
        slackConfig.setBotToken(slackToken);
        settings.setDeliveryMethodsConfigs(Map.of(
                NotificationDeliveryMethod.SLACK, slackConfig
        ));

        restClient.saveNotificationSettings(settings);

        NotificationSettings savedSettings = restClient.getNotificationSettings().get();
        assertThat(savedSettings.getDeliveryMethodsConfigs()).hasSize(1);
        assertThat(savedSettings.getDeliveryMethodsConfigs().get(slackConfig.getMethod())).isEqualTo(slackConfig);

        // save user notification settings
        var entityActionNotificationPref = new UserNotificationSettings.NotificationPref();
        entityActionNotificationPref.setEnabled(true);
        entityActionNotificationPref.setEnabledDeliveryMethods(Map.of(
                NotificationDeliveryMethod.WEB, true,
                NotificationDeliveryMethod.SMS, false,
                NotificationDeliveryMethod.EMAIL, false
        ));

        UserNotificationSettings userNotificationSettings = new UserNotificationSettings(Map.of(
                NotificationType.ENTITY_ACTION, entityActionNotificationPref
        ));
        UserNotificationSettings saved = restClient.saveUserNotificationSettings(userNotificationSettings);
        UserNotificationSettings retrieved = restClient.getUserNotificationSettings().get();
        assertThat(retrieved).isEqualTo(saved);
    }

    @Test
    public void testSaveDomain() {
        restClient.login("sysadmin@thingsboard.org", "sysadmin");

        Domain domain = new Domain();
        String prefix = RandomStringUtils.randomAlphabetic(5).toLowerCase();
        domain.setName(prefix + ".test.com");
        Domain savedDomain = restClient.saveDomain(domain);
        assertThat(savedDomain.getName()).isEqualTo(domain.getName());

        PageData<DomainInfo> tenantDomainInfos = restClient.getTenantDomainInfos(new PageLink(10));
        List<DomainInfo> domainInfos = tenantDomainInfos.getData().stream().filter(domainInfo -> domainInfo.getName().startsWith(prefix)).toList();
        assertThat(domainInfos).hasSize(1);
    }

    @Test
    public void testSaveMobileApp() {
        restClient.login("sysadmin@thingsboard.org", "sysadmin");

        MobileApp mobileApp = new MobileApp();
        String prefix = RandomStringUtils.randomAlphabetic(5).toLowerCase();
        mobileApp.setPkgName(prefix + "test.app.apple");
        mobileApp.setPlatformType(PlatformType.ANDROID);
        mobileApp.setAppSecret(RandomStringUtils.randomAlphabetic(20));
        mobileApp.setStatus(MobileAppStatus.DRAFT);

        MobileApp savedMobileApp = restClient.saveMobileApp(mobileApp);
        assertThat(savedMobileApp.getName()).isEqualTo(mobileApp.getName());

        PageData<MobileApp> mobileApps = restClient.getTenantMobileApps(new PageLink(10));
        List<MobileApp> retrieved = mobileApps.getData().stream().filter(app -> app.getPkgName().startsWith(prefix)).toList();
        assertThat(retrieved).hasSize(1);

        MobileAppBundle mobileAppBundle = new MobileAppBundle();
        String bundlePrefix = RandomStringUtils.randomAlphabetic(5).toLowerCase();
        mobileAppBundle.setTitle(bundlePrefix + "Test Bundle");
        mobileAppBundle.setAndroidAppId(savedMobileApp.getId());

        MobileAppBundle savedMobileAppBundle = restClient.saveMobileBundle(mobileAppBundle);
        PageData<MobileAppBundleInfo> mobileBundleInfos = restClient.getTenantMobileBundleInfos(new PageLink(10));
        List<MobileAppBundleInfo> bundleInfos = mobileBundleInfos.getData().stream().filter(mobileAppBundleInfo -> mobileAppBundleInfo.getTitle().startsWith(bundlePrefix)).toList();
        assertThat(bundleInfos).hasSize(1);
    }

    private NotificationTarget createNotificationTarget(UserId... usersIds) {
        UserListFilter filter = new UserListFilter();
        filter.setUsersIds(Arrays.stream(usersIds).map(UUIDBased::getId).toList());

        NotificationTarget notificationTarget = new NotificationTarget();
        notificationTarget.setName(filter.toString() + org.apache.commons.lang3.RandomStringUtils.randomNumeric(5));
        PlatformUsersNotificationTargetConfig targetConfig = new PlatformUsersNotificationTargetConfig();
        targetConfig.setUsersFilter(filter);
        notificationTarget.setConfiguration(targetConfig);
        return restClient.createNotificationTarget(notificationTarget);
    }

    private NotificationTemplate createNotificationTemplate(NotificationType notificationType, String subject,
                                                              String text, NotificationDeliveryMethod... deliveryMethods) {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setName("Notification template: " + RandomStringUtils.randomAlphabetic(5));
        notificationTemplate.setNotificationType(notificationType);
        NotificationTemplateConfig config = new NotificationTemplateConfig();
        config.setDeliveryMethodsTemplates(new HashMap<>());
        for (NotificationDeliveryMethod deliveryMethod : deliveryMethods) {
            DeliveryMethodNotificationTemplate deliveryMethodNotificationTemplate;
            switch (deliveryMethod) {
                case WEB: {
                    deliveryMethodNotificationTemplate = new WebDeliveryMethodNotificationTemplate();
                    break;
                }
                case EMAIL: {
                    deliveryMethodNotificationTemplate = new EmailDeliveryMethodNotificationTemplate();
                    break;
                }
                case SMS: {
                    deliveryMethodNotificationTemplate = new SmsDeliveryMethodNotificationTemplate();
                    break;
                }
                case MOBILE_APP:
                    deliveryMethodNotificationTemplate = new MobileAppDeliveryMethodNotificationTemplate();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported delivery method " + deliveryMethod);
            }
            deliveryMethodNotificationTemplate.setEnabled(true);
            deliveryMethodNotificationTemplate.setBody(text);
            if (deliveryMethodNotificationTemplate instanceof HasSubject) {
                ((HasSubject) deliveryMethodNotificationTemplate).setSubject(subject);
            }
            config.getDeliveryMethodsTemplates().put(deliveryMethod, deliveryMethodNotificationTemplate);
        }
        notificationTemplate.setConfiguration(config);
        return restClient.createNotificationTemplate(notificationTemplate);
    }

    private NotificationRequest submitNotificationRequest(NotificationTargetId targetId, NotificationTemplateId notificationTemplateId) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        config.setSendingDelayInSec(0);
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .targets(List.of(targetId).stream().map(UUIDBased::getId).collect(Collectors.toList()))
                .templateId(notificationTemplateId)
                .additionalConfig(config)
                .build();
        return restClient.createNotificationRequest(notificationRequest);
    }
}

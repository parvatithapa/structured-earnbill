/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sapienter.jbilling.server.csv.export.event.ReportExportNotificationEvent;
import com.sapienter.jbilling.server.customer.event.NewCustomerEvent;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.AutoRenewalEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.event.UsagePoolConsumptionNotificationEvent;
import com.sapienter.jbilling.server.user.event.NewContactEvent;
import com.sapienter.jbilling.server.util.Context;

/**
 * Event custom notification task.
 *
 * @author: Panche.Isajeski
 * @since: 12/07/12
 */
public class SpcNotificationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final ParameterDescription PARAMETER_EXTERNAL_API_URL =
            new ParameterDescription("external_api_url", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_USERNAME =
            new ParameterDescription("username", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_PASSWORD =
            new ParameterDescription("password", false, ParameterDescription.Type.STR, true);
    private static final ParameterDescription PARAMETER_NEW_CUSTOMER_NOTIFICATION_ID =
            new ParameterDescription("new_customer_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("new_contact_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("new_order_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAMETER_AUTO_RENEWAL_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("auto_renewal_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAMETER_AUTO_RENEWAL_CONFIRMATION_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("auto_renewal_confirmation_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription REPORT_EXPORT_SUCCESS_NOTIFICATION_ID =
            new ParameterDescription("report_export_success_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription REPORT_EXPORT_FAILURE_NOTIFICATION_ID =
            new ParameterDescription("report_export_failed_notification_id", false, ParameterDescription.Type.INT);
    public static final ParameterDescription PARAM_SEND_EMAIL_NOTIFICATION =
            new ParameterDescription("send_email_notification", true, ParameterDescription.Type.BOOLEAN);
    public static final ParameterDescription PARAM_SEND_NOTIFICATION_METHOD_NAME =
            new ParameterDescription("send_notification_method_name", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_ACCESS_TOKEN_URL =
            new ParameterDescription("wookie_access_token_url", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_ENABLE_ACCESS_TOKEN =
            new ParameterDescription("use_access_token_endpoint", true, ParameterDescription.Type.BOOLEAN);
    public static final ParameterDescription PARAMETER_CRM_PAYLOAD_PREFIX =
            new ParameterDescription("crm_payload_prefixes", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAM_TIME_OUT =
            new ParameterDescription("timeout", false, ParameterDescription.Type.STR);



    private static final LoadingCache<TokenKey, TokenResponse> tokenCache =
            CacheBuilder.newBuilder()
            .concurrencyLevel(10)
            .maximumSize(100)
            .refreshAfterWrite(10, TimeUnit.HOURS) // all token will be refreshed after 10 hours.
            .removalListener(entry -> logger.debug("token {} removed", entry))
            .expireAfterAccess(2, TimeUnit.DAYS) // all token will be removed after 2 days.
            .build(new CacheLoader<TokenKey, TokenResponse>() {
                @Override
                public TokenResponse load(TokenKey key) {
                    // generate token.
                    return generateToken(key);
                }
            });


    static LoadingCache<TokenKey, TokenResponse> tokenCache() {
        return tokenCache;
    }

    public SpcNotificationTask() {
        descriptions.add(PARAMETER_EXTERNAL_API_URL);
        descriptions.add(PARAMETER_USERNAME);
        descriptions.add(PARAMETER_PASSWORD);
        descriptions.add(PARAMETER_NEW_CUSTOMER_NOTIFICATION_ID);
        descriptions.add(PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID);
        descriptions.add(PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID);
        descriptions.add(PARAMETER_AUTO_RENEWAL_CUSTOM_NOTIFICATION_ID);
        descriptions.add(PARAMETER_AUTO_RENEWAL_CONFIRMATION_CUSTOM_NOTIFICATION_ID);
        descriptions.add(REPORT_EXPORT_SUCCESS_NOTIFICATION_ID);
        descriptions.add(REPORT_EXPORT_FAILURE_NOTIFICATION_ID);
        descriptions.add(PARAM_SEND_EMAIL_NOTIFICATION);
        descriptions.add(PARAM_SEND_NOTIFICATION_METHOD_NAME);
        descriptions.add(PARAMETER_ACCESS_TOKEN_URL);
        descriptions.add(PARAMETER_ENABLE_ACCESS_TOKEN);
        descriptions.add(PARAMETER_CRM_PAYLOAD_PREFIX);
        descriptions.add(PARAM_TIME_OUT);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
        SpcCreditPoolNotificationEvent.class,
        OptusMurNotificationEvent.class,
        NewCustomerEvent.class,
        NewContactEvent.class,
        UsagePoolConsumptionNotificationEvent.class,
        NewOrderEvent.class,
        AutoRenewalEvent.class,
        ReportExportNotificationEvent.class,
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        // registering call back which executes after current transaction commits.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(final int status) {
                // only successfully committed transactions will send notification to user.
                if (TransactionSynchronization.STATUS_COMMITTED == status && Boolean.parseBoolean(parameters.get(SpcNotificationTask.PARAM_SEND_EMAIL_NOTIFICATION.getName()))) {
                    logger.debug("executing event {} for entity {} after commiting transaction", event.getName(), event.getEntityId());
                    SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
                    spcHelperService.notifyToUser(getEntityId(), event, parameters);
                    return;
                }
                logger.debug("skipping notification event {}, since transaction is roll back for entity {}",
                        event.getName(), event.getEntityId());
            }
        });
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private static ClientHttpRequestFactory getClientHttpRequestFactory(final int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        return new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().setDefaultRequestConfig(config).build());
    }

    private static RestTemplate restTemplate() {
        return new RestTemplate(getClientHttpRequestFactory(DEFAULT_TIME_OUT));
    }

    private static final int DEFAULT_TIME_OUT = 10000;

    private static TokenResponse generateToken(TokenKey key) {
        try {
            logger.debug("generating token for key {}", key);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("username", key.getUserName());
            map.add("password", key.getPassword());
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
            ResponseEntity<JsonNode> response = restTemplate().postForEntity(key.getTokenUrl(), entity, JsonNode.class);
            JsonNode tokenResponse = response.getBody();
            List<String> errorMessages = new ArrayList<>();
            String accessToken = tokenResponse.findPath("access_token").asText();
            String tokenExpires = tokenResponse.findPath("token_expires").asText();
            if (StringUtils.isEmpty(accessToken)) {
                errorMessages.add("access_token not found in response!");
            }
            if (StringUtils.isEmpty(tokenExpires)) {
                errorMessages.add("token_expires not found in response!");
            }
            if(CollectionUtils.isNotEmpty(errorMessages)) {
                throw new RestClientException(String.join(",", errorMessages));
            }
            TokenResponse token = new TokenResponse(accessToken, tokenExpires);
            logger.debug("token response {} for key {}", token, key);
            return token;
        } catch (HttpStatusCodeException ex) {
            throw new RestClientException("Failed To get Wookie Access Token " + ex.getResponseBodyAsString());
        }
    }
}


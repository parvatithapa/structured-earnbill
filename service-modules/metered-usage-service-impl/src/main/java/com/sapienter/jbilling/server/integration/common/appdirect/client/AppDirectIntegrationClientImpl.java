package com.sapienter.jbilling.server.integration.common.appdirect.client;


import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sapienter.jbilling.appdirect.vo.BillingAPIResult;
import com.sapienter.jbilling.appdirect.vo.UsageBean;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.FreeSubscriptionExpiredException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.NetworkTimeoutException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.SubscriptionNotFoundException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.SubscriptionUsageNotAllowed;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.UnAuthorizedTransientException;

public class AppDirectIntegrationClientImpl implements IntegrationClient {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private RestTemplate restTemplate;
    private String endpointUrl;

    public AppDirectIntegrationClientImpl(String endpointUrl, String consumerKey, String consumerSecret, int connectTimeout, int readTimeout) {
        this.endpointUrl =  endpointUrl;
        this.restTemplate = new RestTemplate(new TwoLeggedOAuthClientHttpRequestFactory(consumerKey, consumerSecret, connectTimeout, readTimeout));
        this.restTemplate.setErrorHandler(new ClientResponseErrorHandler());
    }

    @Override
    public boolean send(UsageBean usageBean) throws
                                                UnAuthorizedTransientException,
                                                FreeSubscriptionExpiredException,
                                                SubscriptionNotFoundException,
                                                SubscriptionUsageNotAllowed,
                                                NetworkTimeoutException {
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(usageBean);

        logger.debug("Payload: {}", json);

        HttpEntity<String> entity = getHttpEntity(json);
        boolean success = false;

        try {
            restTemplate.exchange(endpointUrl, HttpMethod.POST, entity, BillingAPIResult.class);
            success = true;
        } catch (RestClientException e) {
            success = false;
            Throwable cause  = e.getCause();
            if(cause == null) {
                logger.error(e.getLocalizedMessage(), e);
            } else if (cause instanceof UnAuthorizedTransientException) {
                throw (UnAuthorizedTransientException) cause;
            } else if (cause instanceof FreeSubscriptionExpiredException) {
                throw (FreeSubscriptionExpiredException)cause;
            } else if (cause instanceof  SubscriptionNotFoundException) {
                throw (SubscriptionNotFoundException)cause;
            } else if (cause instanceof SubscriptionUsageNotAllowed) {
                throw (SubscriptionUsageNotAllowed)cause;
            } else if (cause instanceof NetworkTimeoutException) {
                throw (NetworkTimeoutException)cause;
            } else {
                logger.error("Fatal Rest client exception", e);
            }
        }
        return success;
    }

    private HttpEntity<String> getHttpEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        final Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("charset", "utf-8");
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON,parameterMap));
        List<MediaType> accepts = new ArrayList<>();
        accepts.add(MediaType.APPLICATION_JSON);
        headers.setAccept(accepts );
        return  new HttpEntity<>(json, headers);

    }
}

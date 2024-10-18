package com.sapienter.jbilling.saml.web.api;

import com.sapienter.jbilling.saml.integration.APIResult;
import com.sapienter.jbilling.saml.model.ApplicationProfile;
import org.springframework.web.client.RestTemplate;

public class AppDirectIntegrationApiImpl implements AppDirectIntegrationApi {
    private final RestTemplate restTemplate;

    public AppDirectIntegrationApiImpl(ApplicationProfile applicationProfile) {
        restTemplate = new RestTemplate(new TwoLeggedOAuthClientHttpRequestFactory(applicationProfile));
    }

    @Override
    public void registerResult(String eventUrl, APIResult apiResult) {
        restTemplate.postForObject(eventUrl + "/result", apiResult, Void.class);
    }
}

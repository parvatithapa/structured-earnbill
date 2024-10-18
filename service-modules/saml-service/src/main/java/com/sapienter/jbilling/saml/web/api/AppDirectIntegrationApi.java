package com.sapienter.jbilling.saml.web.api;


import com.sapienter.jbilling.saml.integration.APIResult;

@FunctionalInterface
public interface AppDirectIntegrationApi {
    void registerResult(String eventToken, APIResult apiResult);
}

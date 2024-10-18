package com.sapienter.jbilling.saml.integration.service;

import com.sapienter.jbilling.saml.integration.APIResult;
import com.sapienter.jbilling.saml.model.ApplicationProfile;

public interface IntegrationService {
    String SAML_IDP_LINK = "samlIdp";

    AppDirectIntegrationAPI getAppDirectIntegrationApi(String basePath, ApplicationProfile applicationProfile);

    public APIResult processEvent(ApplicationProfile applicationProfile, String eventUrl, String token);
}

package com.sapienter.jbilling.saml.web.api;

import com.sapienter.jbilling.saml.model.ApplicationProfile;
import org.springframework.stereotype.Component;

@Component
public class AppDirectIntegrationApiFactory {
    public AppDirectIntegrationApi get(ApplicationProfile applicationProfile) {
        return new AppDirectIntegrationApiImpl(applicationProfile);
    }
}

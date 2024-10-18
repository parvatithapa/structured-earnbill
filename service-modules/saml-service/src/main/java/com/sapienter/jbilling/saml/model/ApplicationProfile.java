package com.sapienter.jbilling.saml.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class ApplicationProfile implements Serializable {

    private String uuid;
    private String url;
    private String oauthConsumerKey;
    private String oauthConsumerSecret;
    private AuthenticationMethod authenticationMethod = AuthenticationMethod.SAML;
    private boolean legacy = false;
    private String legacyMarketplaceBaseUrl;
}

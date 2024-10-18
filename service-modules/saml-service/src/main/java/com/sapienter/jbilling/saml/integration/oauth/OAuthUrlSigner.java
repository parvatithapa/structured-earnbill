package com.sapienter.jbilling.saml.integration.oauth;

public interface OAuthUrlSigner {
    public String sign(String urlString);
}

package com.sapienter.jbilling.saml.integration.oauth;

import lombok.extern.slf4j.Slf4j;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.signature.QueryStringSigningStrategy;

import java.io.Serializable;

@Slf4j
public class OAuthUrlSignerImpl implements OAuthUrlSigner, Serializable {
    private static final long serialVersionUID = -805012415547114918L;

    private final OAuthConsumer consumer;

    public OAuthUrlSignerImpl(String consumerKey, String consumerSecret) {
        this.consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
        this.consumer.setSigningStrategy(new QueryStringSigningStrategy());
    }

    @Override
    public String sign(String urlString) {
        log.debug("Signing URL: {}.", urlString);
        try {
            String signedUrl = consumer.sign(urlString);
            log.debug("Signed URL: {}.", signedUrl);
            return signedUrl;
        } catch (OAuthException e) {
            log.error("Error when signing URL", e);
            return urlString;
        }
    }
}

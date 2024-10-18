package com.sapienter.jbilling.appdirect.subscription.oauth;

import lombok.Getter;
import lombok.Setter;


public class OAuthWS {

	@Getter @Setter
	private String baseApiUrl;

	@Getter @Setter
	private String consumerKey;

	@Getter @Setter
	private String consumerSecret;

    @Override
    public String toString() {
        return "OAuthWS{" +
                "baseApiUrl='" + baseApiUrl + '\'' +
                ", consumerKey='" + consumerKey + "'}";
    }
}
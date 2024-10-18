package com.sapienter.jbilling.server.spc;

class TokenResponse {

    private String accessToken;
    private String tokenExpires;

    TokenResponse(String accessToken, String tokenExpires) {
        this.accessToken = accessToken;
        this.tokenExpires = tokenExpires;
    }

    String getAccessToken() {
        return accessToken;
    }

    String getTokenExpires() {
        return tokenExpires;
    }
}

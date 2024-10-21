package com.sapienter.jbilling.server.spc;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(exclude = {"password"})
@EqualsAndHashCode
class TokenKey {
    private String userName;
    private String password;
    private String tokenUrl;

    static TokenKey of(String userName, String password, String tokenUrl) {
        return new TokenKey(userName, password, tokenUrl);
    }

    private TokenKey(String userName, String password, String tokenUrl) {
        this.userName = userName;
        this.password = password;
        this.tokenUrl = tokenUrl;
    }

    String getUserName() {
        return userName;
    }

    String getPassword() {
        return password;
    }

    String getTokenUrl() {
        return tokenUrl;
    }
}
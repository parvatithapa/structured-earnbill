package com.sapienter.jbilling.server.user.balance;

import lombok.ToString;

/**
 * Created by pablo_galera on 25/04/17.
 */
@ToString
public class CustomerProperties {

    private String userName;
    private String languageCode;
    private String oldPassword;
    private String newPassword;

    public CustomerProperties(String userName, String languageCode) {
        this.userName = userName;
        this.languageCode = languageCode;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getUserName() {
        return userName;
    }

}


package com.sapienter.jbilling.server.user;

import java.io.Serializable;

@SuppressWarnings("serial")
public class PortalCredential implements Serializable {

    private Integer userId;
    private String username;
    private String currentPassword;
    private String newPassword;

    public Integer getUserId() {
        return userId;
    }
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getCurrentPassword() {
        return currentPassword;
    }
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
    public String getNewPassword() {
        return newPassword;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PortalCredential [userId=");
        builder.append(userId);
        builder.append(", username=");
        builder.append(username);
        builder.append(", currentPassword=");
        builder.append(currentPassword);
        builder.append(", newPassword=");
        builder.append(newPassword);
        builder.append("]");
        return builder.toString();
    }

}

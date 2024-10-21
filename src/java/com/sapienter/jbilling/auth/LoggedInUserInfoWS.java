package com.sapienter.jbilling.auth;

import com.sapienter.jbilling.client.authentication.model.User;

import java.util.List;

public class LoggedInUserInfoWS {

    private final User user;
    private final List<String> permissions;

    public LoggedInUserInfoWS(User user, List<String> permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    public User getUser() {
        return user;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
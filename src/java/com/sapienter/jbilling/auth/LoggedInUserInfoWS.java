package com.sapienter.jbilling.auth;

import java.util.List;

import com.sapienter.jbilling.client.authentication.model.User;

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

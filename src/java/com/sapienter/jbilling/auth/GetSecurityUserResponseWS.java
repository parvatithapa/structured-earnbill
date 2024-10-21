package com.sapienter.jbilling.auth;

import java.util.List;

import lombok.ToString;

import com.sapienter.jbilling.client.authentication.model.User;

@ToString
public class GetSecurityUserResponseWS {

	private final User user;
	private final List<String> permissions;


	public GetSecurityUserResponseWS(User user, List<String> permissions) {
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
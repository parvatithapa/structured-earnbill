package com.sapienter.jbilling.auth;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthRequestWS {
	@NotNull(message = "validation.error.notnull")
	private String username;
	@NotNull(message = "validation.error.notnull")
	private String password;
	@NotNull(message = "validation.error.notnull")
	private Integer entityId;

	@JsonCreator
	public AuthRequestWS(@JsonProperty("username") String username,
			@JsonProperty("password") String password,
			@JsonProperty("entityId") Integer entityId) {
		this.username = username;
		this.password = password;
		this.entityId = entityId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}
}
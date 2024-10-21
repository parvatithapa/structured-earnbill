package com.sapienter.jbilling.auth;

import javax.validation.constraints.NotNull;

import lombok.ToString;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@ToString
public class GetSecurityUserRequestWS {

	@NotEmpty(message = "validation.error.notnull")
	private final String username;
	@NotNull(message = "validation.error.notnull")
	private Integer entityId;

	@JsonCreator
	public GetSecurityUserRequestWS(
			@JsonProperty("username") String username,
			@JsonProperty("entityId") Integer entityId) {
		this.username = username;
		this.entityId = entityId;
	}

	public String getUsername() {
		return username;
	}


	public Integer getEntityId() {
		return entityId;
	}
}
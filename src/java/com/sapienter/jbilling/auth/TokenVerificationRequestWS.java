package com.sapienter.jbilling.auth;

import lombok.ToString;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@ToString
public class TokenVerificationRequestWS {

	@NotEmpty(message = "validation.error.notnull")
	private String token;

	@JsonCreator
	public TokenVerificationRequestWS(@JsonProperty("token") String token) {
		this.token = token;
	}

	public String getToken() {
		return token;
	}
}
package com.sapienter.jbilling.auth;

import java.util.Map;

import lombok.ToString;

@ToString
public final class JwtDecodedTokenInfoWS {
	private final String jwt;
	private StatusWS status = StatusWS.VALID;
	private Map<String, Object> claims;
	private String errorMessage;
	private int httpStatusCode = 200;

	public JwtDecodedTokenInfoWS(String jwt) {
		this.jwt = jwt;
	}

	public Map<String, Object> getClaims() {
		return claims;
	}

	public void setClaims(Map<String, Object> claims) {
		this.claims = claims;
	}

	public String getJwt() {
		return jwt;
	}

	public StatusWS getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}

	public void setHttpStatusCode(int httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}

	public void setStatus(StatusWS status) {
		this.status = status;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		this.status = StatusWS.INVALID;
	}
}

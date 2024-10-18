package com.sapienter.jbilling.auth;

import lombok.ToString;

@ToString
public class RefreshTokenResponseWS {
	private String accessToken;
	private String refreshToken;
	private String tokenType = "Bearer";
	private StatusWS status;

	public RefreshTokenResponseWS(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public StatusWS getStatus() {
		return status;
	}

	public void setStatus(StatusWS status) {
		this.status = status;
	}

}
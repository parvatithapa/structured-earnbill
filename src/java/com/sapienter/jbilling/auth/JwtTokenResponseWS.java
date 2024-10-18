package com.sapienter.jbilling.auth;

import lombok.ToString;


@ToString
public final class JwtTokenResponseWS {
	private final String token;
	private final String refreshToken;
	private final long createTime;
	private final String tokenType;

	public JwtTokenResponseWS(String token, String refreshToken, long createTime) {
		this.token = token;
		this.refreshToken = refreshToken;
		this.createTime = createTime;
		this.tokenType = "Bearer";
	}

	public String getToken() {
		return token;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public long getCreateTime() {
		return createTime;
	}

	public String getTokenType() {
		return tokenType;
	}
}
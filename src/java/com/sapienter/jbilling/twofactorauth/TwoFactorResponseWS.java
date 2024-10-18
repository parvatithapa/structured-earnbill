package com.sapienter.jbilling.twofactorauth;

public class TwoFactorResponseWS {
	private final String otp;
	private final String responseJson;

	public TwoFactorResponseWS(String otp, String responseJson) {
		this.otp = otp;
		this.responseJson = responseJson;
	}

	public String getOtp() {
		return otp;
	}

	public String getResponseJson() {
		return responseJson;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TwoFactorResponseWS [otp=");
		builder.append(otp);
		builder.append(", jsonResponseBody=");
		builder.append(responseJson);
		builder.append("]");
		return builder.toString();
	}
}

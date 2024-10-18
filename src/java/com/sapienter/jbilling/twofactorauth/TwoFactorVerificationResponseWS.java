package com.sapienter.jbilling.twofactorauth;

public class TwoFactorVerificationResponseWS {

	private final boolean otpMatched;
	private final String responseJson;

	public TwoFactorVerificationResponseWS(boolean otpMatched, String responseJson) {
		this.otpMatched = otpMatched;
		this.responseJson = responseJson;
	}

	public boolean isOtpMatched() {
		return otpMatched;
	}

	public String getResponseJson() {
		return responseJson;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TwoFactorVerificationResponseWS [otpMatched=");
		builder.append(otpMatched);
		builder.append(", responseJson=");
		builder.append(responseJson);
		builder.append("]");
		return builder.toString();
	}
}

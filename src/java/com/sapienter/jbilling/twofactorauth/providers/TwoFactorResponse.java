package com.sapienter.jbilling.twofactorauth.providers;

public class TwoFactorResponse {
	private final String otp;
	private final String jsonResponseBody;

	public TwoFactorResponse(String otp, String jsonResponseBody) {
		this.otp = otp;
		this.jsonResponseBody = jsonResponseBody;
	}

	public String getOtp() {
		return otp;
	}

	public String getJsonResponseBody() {
		return jsonResponseBody;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TwoFactorResponse [otp=");
		builder.append(otp);
		builder.append(", jsonResponseBody=");
		builder.append(jsonResponseBody);
		builder.append("]");
		return builder.toString();
	}
}

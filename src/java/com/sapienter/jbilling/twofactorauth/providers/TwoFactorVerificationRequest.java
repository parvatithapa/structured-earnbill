package com.sapienter.jbilling.twofactorauth.providers;

import com.sapienter.jbilling.twofactorauth.TwoFactorMethod;


public class TwoFactorVerificationRequest {
	private String id;
	private String otp;
	private TwoFactorMethod twoFactorMethod;

	public TwoFactorVerificationRequest(String id, String otp, TwoFactorMethod twoFactorMethod) {
		this.id = id;
		this.otp = otp;
		this.twoFactorMethod = twoFactorMethod;
	}

	public String getId() {
		return id;
	}

	public String getOtp() {
		return otp;
	}

	public TwoFactorMethod getTwoFactorMethod() {
		return twoFactorMethod;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TwoFactorVerificationRequest [id=");
		builder.append(id);
		builder.append(", otp=");
		builder.append(otp);
		builder.append(", twoFactorMethod=");
		builder.append(twoFactorMethod);
		builder.append("]");
		return builder.toString();
	}
}

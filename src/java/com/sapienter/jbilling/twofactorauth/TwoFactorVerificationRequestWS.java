package com.sapienter.jbilling.twofactorauth;

import javax.validation.constraints.NotNull;

public class TwoFactorVerificationRequestWS {
	@NotNull
	private String id;
	@NotNull
	private String otp;
	@NotNull
	private TwoFactorMethod twoFactorMethod;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}

	public TwoFactorMethod getTwoFactorMethod() {
		return twoFactorMethod;
	}

	public void setTwoFactorMethod(TwoFactorMethod twoFactorMethod) {
		this.twoFactorMethod = twoFactorMethod;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TwoFactorVerificationRequestWS [id=");
		builder.append(id);
		builder.append(", otp=");
		builder.append(otp);
		builder.append(", twoFactorMethod=");
		builder.append(twoFactorMethod);
		builder.append("]");
		return builder.toString();
	}
}

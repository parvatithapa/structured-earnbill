package com.sapienter.jbilling.twofactorauth.providers;

import com.sapienter.jbilling.twofactorauth.TwoFactorMethod;

public class TwoFactorRequest {
	private TwoFactorMethod twoFactorMethod;
	private String id;

	public TwoFactorRequest(TwoFactorMethod twoFactorMethod, String id) {
		this.twoFactorMethod = twoFactorMethod;
		this.id = id;
	}

	public TwoFactorMethod getTwoFactorMethod() {
		return twoFactorMethod;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TwoFactorRequest [twoFactorMethod=");
		builder.append(twoFactorMethod);
		builder.append(", id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}
}

package com.sapienter.jbilling.twofactorauth;

import javax.validation.constraints.NotNull;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "TwoFactorRequestWS", description = "TwoFactorRequest payload")
public class TwoFactorRequestWS {
	@NotNull
	private TwoFactorMethod twoFactorMethod;
	@NotNull
	private String id;

	@ApiModelProperty(value = "twoFactorMethod", required = true)
	public TwoFactorMethod getTwoFactorMethod() {
		return twoFactorMethod;
	}

	public void setTwoFactorMethod(TwoFactorMethod twoFactorMethod) {
		this.twoFactorMethod = twoFactorMethod;
	}

	@ApiModelProperty(value = "id can be phoneNumber/email id depends upon twoFactorMethod", required = true)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TwoFactorRequestWS [twoFactorMethod=");
		builder.append(twoFactorMethod);
		builder.append(", id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}
}

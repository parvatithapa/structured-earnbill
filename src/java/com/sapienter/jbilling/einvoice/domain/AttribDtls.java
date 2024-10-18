package com.sapienter.jbilling.einvoice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AttribDtls {
	@JsonProperty("Nm")
	private String nm;
	@JsonProperty("Val")
	private String Val;

	public String getNm() {
		return nm;
	}

	public void setNm(String nm) {
		this.nm = nm;
	}

	public String getVal() {
		return Val;
	}

	public void setVal(String val) {
		Val = val;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AttribDtls [nm=");
		builder.append(nm);
		builder.append(", Val=");
		builder.append(Val);
		builder.append("]");
		return builder.toString();
	}
}

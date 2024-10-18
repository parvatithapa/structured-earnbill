package com.sapienter.jbilling.einvoice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BchDtls {

	@JsonProperty("Nm")
	private String nm;
	@JsonProperty("ExpDt")
	private String ExpDt;
	@JsonProperty("WrDt")
	private String WrDt;

	public String getNm() {
		return nm;
	}

	public void setNm(String nm) {
		this.nm = nm;
	}

	public String getExpDt() {
		return ExpDt;
	}

	public void setExpDt(String expDt) {
		ExpDt = expDt;
	}

	public String getWrDt() {
		return WrDt;
	}

	public void setWrDt(String wrDt) {
		WrDt = wrDt;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BchDtls [nm=");
		builder.append(nm);
		builder.append(", ExpDt=");
		builder.append(ExpDt);
		builder.append(", WrDt=");
		builder.append(WrDt);
		builder.append("]");
		return builder.toString();
	}
}

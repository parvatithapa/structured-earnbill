package com.sapienter.jbilling.einvoice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DocDtls {
	@JsonProperty("Typ")
	private Typ typ;
	@JsonProperty("No")
	private String no;
	@JsonProperty("Dt")
	private String dt;

	public Typ getTyp() {
		return typ;
	}

	public void setTyp(Typ typ) {
		this.typ = typ;
	}

	public String getNo() {
		return no;
	}

	public void setNo(String no) {
		this.no = no;
	}

	public String getDt() {
		return dt;
	}

	public void setDt(String dt) {
		this.dt = dt;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DocDtls [typ=");
		builder.append(typ);
		builder.append(", no=");
		builder.append(no);
		builder.append(", dt=");
		builder.append(dt);
		builder.append("]");
		return builder.toString();
	}
}

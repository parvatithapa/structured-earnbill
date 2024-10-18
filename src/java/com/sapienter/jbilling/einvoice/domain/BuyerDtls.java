package com.sapienter.jbilling.einvoice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BuyerDtls {
	@JsonProperty("Gstin")
	private String gstin;
	@JsonProperty("LglNm")
	private String lglNm;
	@JsonProperty("TrdNm")
	private String trdNm;
	@JsonProperty("Addr1")
	private String addr1;
	@JsonProperty("Addr2")
	private String addr2;
	@JsonProperty("Loc")
	private String loc;
	@JsonProperty("Pin")
	private int pin;
	@JsonProperty("Stcd")
	private String stcd;
	@JsonProperty("Ph")
	private String ph;
	@JsonProperty("Em")
	private String em;
	@JsonProperty("Pos")
	private String pos;

	public String getGstin() {
		return gstin;
	}

	public void setGstin(String gstin) {
		this.gstin = gstin;
	}

	public String getLglNm() {
		return lglNm;
	}

	public void setLglNm(String lglNm) {
		this.lglNm = lglNm;
	}

	public String getTrdNm() {
		return trdNm;
	}

	public void setTrdNm(String trdNm) {
		this.trdNm = trdNm;
	}

	public String getAddr1() {
		return addr1;
	}

	public void setAddr1(String addr1) {
		this.addr1 = addr1;
	}

	public String getAddr2() {
		return addr2;
	}

	public void setAddr2(String addr2) {
		this.addr2 = addr2;
	}

	public String getLoc() {
		return loc;
	}

	public void setLoc(String loc) {
		this.loc = loc;
	}

	public int getPin() {
		return pin;
	}

	public void setPin(int pin) {
		this.pin = pin;
	}

	public String getStcd() {
		return stcd;
	}

	public void setStcd(String stcd) {
		this.stcd = stcd;
	}

	public String getPh() {
		return ph;
	}

	public void setPh(String ph) {
		this.ph = ph;
	}

	public String getEm() {
		return em;
	}

	public void setEm(String em) {
		this.em = em;
	}

	public String getPos() {
		return pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BuyerDtls [gstin=");
		builder.append(gstin);
		builder.append(", lglNm=");
		builder.append(lglNm);
		builder.append(", trdNm=");
		builder.append(trdNm);
		builder.append(", addr1=");
		builder.append(addr1);
		builder.append(", addr2=");
		builder.append(addr2);
		builder.append(", loc=");
		builder.append(loc);
		builder.append(", pin=");
		builder.append(pin);
		builder.append(", stcd=");
		builder.append(stcd);
		builder.append(", ph=");
		builder.append(ph);
		builder.append(", em=");
		builder.append(em);
		builder.append(", pos=");
		builder.append(pos);
		builder.append("]");
		return builder.toString();
	}
}

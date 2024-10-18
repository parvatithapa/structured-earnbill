package com.sapienter.jbilling.einvoice.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValDtls {
	@JsonProperty("AssVal")
	private BigDecimal assVal = BigDecimal.ZERO;
	@JsonProperty("CgstVal")
	private BigDecimal cgstVal = BigDecimal.ZERO;
	@JsonProperty("SgstVal")
	private BigDecimal sgstVal = BigDecimal.ZERO;
	@JsonProperty("IgstVal")
	private BigDecimal igstVal = BigDecimal.ZERO;
	@JsonProperty("CesVal")
	private BigDecimal cesVal = BigDecimal.ZERO;
	@JsonProperty("StCesVal")
	private BigDecimal stCesVal = BigDecimal.ZERO;
	@JsonProperty("Discount")
	private BigDecimal discount = BigDecimal.ZERO;
	@JsonProperty("OthChrg")
	private BigDecimal othChrg = BigDecimal.ZERO;
	@JsonProperty("RndOffAmt")
	private BigDecimal rndOffAmt = BigDecimal.ZERO;
	@JsonProperty("TotInvVal")
	private BigDecimal totInvVal = BigDecimal.ZERO;
	@JsonProperty("TotInvValFc")
	private BigDecimal totInvValFc = BigDecimal.ZERO;

	public BigDecimal getAssVal() {
		return assVal;
	}

	public void setAssVal(BigDecimal assVal) {
		this.assVal = assVal;
	}

	public BigDecimal getCgstVal() {
		return cgstVal;
	}

	public void setCgstVal(BigDecimal cgstVal) {
		this.cgstVal = cgstVal;
	}

	public BigDecimal getSgstVal() {
		return sgstVal;
	}

	public void setSgstVal(BigDecimal sgstVal) {
		this.sgstVal = sgstVal;
	}

	public BigDecimal getIgstVal() {
		return igstVal;
	}

	public void setIgstVal(BigDecimal igstVal) {
		this.igstVal = igstVal;
	}

	public BigDecimal getCesVal() {
		return cesVal;
	}

	public void setCesVal(BigDecimal cesVal) {
		this.cesVal = cesVal;
	}

	public BigDecimal getStCesVal() {
		return stCesVal;
	}

	public void setStCesVal(BigDecimal stCesVal) {
		this.stCesVal = stCesVal;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}

	public BigDecimal getOthChrg() {
		return othChrg;
	}

	public void setOthChrg(BigDecimal othChrg) {
		this.othChrg = othChrg;
	}

	public BigDecimal getRndOffAmt() {
		return rndOffAmt;
	}

	public void setRndOffAmt(BigDecimal rndOffAmt) {
		this.rndOffAmt = rndOffAmt;
	}

	public BigDecimal getTotInvVal() {
		return totInvVal;
	}

	public void setTotInvVal(BigDecimal totInvVal) {
		this.totInvVal = totInvVal;
	}

	public BigDecimal getTotInvValFc() {
		return totInvValFc;
	}

	public void setTotInvValFc(BigDecimal totInvValFc) {
		this.totInvValFc = totInvValFc;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ValDtls [assVal=");
		builder.append(assVal);
		builder.append(", cgstVal=");
		builder.append(cgstVal);
		builder.append(", sgstVal=");
		builder.append(sgstVal);
		builder.append(", igstVal=");
		builder.append(igstVal);
		builder.append(", cesVal=");
		builder.append(cesVal);
		builder.append(", stCesVal=");
		builder.append(stCesVal);
		builder.append(", discount=");
		builder.append(discount);
		builder.append(", othChrg=");
		builder.append(othChrg);
		builder.append(", rndOffAmt=");
		builder.append(rndOffAmt);
		builder.append(", totInvVal=");
		builder.append(totInvVal);
		builder.append(", totInvValFc=");
		builder.append(totInvValFc);
		builder.append("]");
		return builder.toString();
	}

}

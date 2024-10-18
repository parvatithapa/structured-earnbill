package com.sapienter.jbilling.einvoice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TranDtls {
	@JsonProperty("TaxSch")
	private TaxSch taxSch = TaxSch.GST;
	@JsonProperty("SupTyp")
	private SupTyp supTyp = SupTyp.EXPWOP;
	@JsonProperty("RegRev")
	private RegRev regRev = RegRev.N;
	@JsonProperty("EcmGstin")
	private String ecmGstin;
	@JsonProperty("IgstOnIntra")
	private IgstOnIntra igstOnIntra = IgstOnIntra.N;

	public TaxSch getTaxSch() {
		return taxSch;
	}

	public void setTaxSch(TaxSch taxSch) {
		this.taxSch = taxSch;
	}

	public SupTyp getSupTyp() {
		return supTyp;
	}

	public void setSupTyp(SupTyp supTyp) {
		this.supTyp = supTyp;
	}

	public RegRev getRegRev() {
		return regRev;
	}

	public void setRegRev(RegRev regRev) {
		this.regRev = regRev;
	}

	public String getEcmGstin() {
		return ecmGstin;
	}

	public void setEcmGstin(String ecmGstin) {
		this.ecmGstin = ecmGstin;
	}

	public IgstOnIntra getIgstOnIntra() {
		return igstOnIntra;
	}

	public void setIgstOnIntra(IgstOnIntra igstOnIntra) {
		this.igstOnIntra = igstOnIntra;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TranDtls [taxSch=");
		builder.append(taxSch);
		builder.append(", supTyp=");
		builder.append(supTyp);
		builder.append(", regRev=");
		builder.append(regRev);
		builder.append(", ecmGstin=");
		builder.append(ecmGstin);
		builder.append(", igstOnIntra=");
		builder.append(igstOnIntra);
		builder.append("]");
		return builder.toString();
	}

}

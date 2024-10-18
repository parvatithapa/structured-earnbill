package com.sapienter.jbilling.einvoice.domain;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemDtls {

	@JsonProperty("SlNo")
	private String slNo;
	@JsonProperty("PrdDesc")
	private String prdDesc;
	@JsonProperty("IsServc")
	private IsServc isServc = IsServc.N;
	@JsonProperty("HsnCd")
	private String hsnCd;
	@JsonProperty("Barcde")
	private String barcde;
	@JsonProperty("Qty")
	private BigDecimal qty = BigDecimal.ZERO;
	@JsonProperty("FreeQty")
	private BigDecimal freeQty = BigDecimal.ZERO;
	@JsonProperty("Unit")
	private String unit;
	@JsonProperty("UnitPrice")
	private BigDecimal unitPrice = BigDecimal.ZERO;
	@JsonProperty("TotAmt")
	private BigDecimal totAmt = BigDecimal.ZERO;
	@JsonProperty("Discount")
	private BigDecimal discount = BigDecimal.ZERO;
	@JsonProperty("PreTaxVal")
	private BigDecimal preTaxVal = BigDecimal.ZERO;
	@JsonProperty("AssAmt")
	private BigDecimal assAmt = BigDecimal.ZERO;
	@JsonProperty("GstRt")
	private BigDecimal gstRt = BigDecimal.ZERO;
	@JsonProperty("IgstAmt")
	private BigDecimal igstAmt = BigDecimal.ZERO;
	@JsonProperty("CgstAmt")
	private BigDecimal cgstAmt = BigDecimal.ZERO;
	@JsonProperty("SgstAmt")
	private BigDecimal sgstAmt = BigDecimal.ZERO;
	@JsonProperty("CesRt")
	private BigDecimal cesRt = BigDecimal.ZERO;
	@JsonProperty("CesAmt")
	private BigDecimal cesAmt = BigDecimal.ZERO;
	@JsonProperty("CesNonAdvlAmt")
	private BigDecimal cesNonAdvlAmt = BigDecimal.ZERO;
	@JsonProperty("StateCesRt")
	private BigDecimal stateCesRt = BigDecimal.ZERO;
	@JsonProperty("StateCesAmt")
	private BigDecimal stateCesAmt = BigDecimal.ZERO;
	@JsonProperty("StateCesNonAdvlAmt")
	private BigDecimal stateCesNonAdvlAmt = BigDecimal.ZERO;
	@JsonProperty("OthChrg")
	private BigDecimal othChrg = BigDecimal.ZERO;
	@JsonProperty("TotItemVal")
	private BigDecimal totItemVal = BigDecimal.ZERO;
	@JsonProperty("OrdLineRef")
	private String ordLineRef;
	@JsonProperty("OrgCntry")
	private String orgCntry;
	@JsonProperty("PrdSlNo")
	private String prdSlNo;
	@JsonProperty("BchDtls")
	private BchDtls bchDtls;
	@JsonProperty("AttribDtls")
	private List<AttribDtls> attribDtls;

	public String getSlNo() {
		return slNo;
	}

	public void setSlNo(String slNo) {
		this.slNo = slNo;
	}

	public String getPrdDesc() {
		return prdDesc;
	}

	public void setPrdDesc(String prdDesc) {
		this.prdDesc = prdDesc;
	}

	public IsServc getIsServc() {
		return isServc;
	}

	public void setIsServc(IsServc isServc) {
		this.isServc = isServc;
	}

	public String getHsnCd() {
		return hsnCd;
	}

	public void setHsnCd(String hsnCd) {
		this.hsnCd = hsnCd;
	}

	public String getBarcde() {
		return barcde;
	}

	public void setBarcde(String barcde) {
		this.barcde = barcde;
	}

	public BigDecimal getQty() {
		return qty;
	}

	public void setQty(BigDecimal qty) {
		this.qty = qty;
	}

	public BigDecimal getFreeQty() {
		return freeQty;
	}

	public void setFreeQty(BigDecimal freeQty) {
		this.freeQty = freeQty;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public BigDecimal getTotAmt() {
		return totAmt;
	}

	public void setTotAmt(BigDecimal totAmt) {
		this.totAmt = totAmt;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}

	public BigDecimal getPreTaxVal() {
		return preTaxVal;
	}

	public void setPreTaxVal(BigDecimal preTaxVal) {
		this.preTaxVal = preTaxVal;
	}

	public BigDecimal getAssAmt() {
		return assAmt;
	}

	public void setAssAmt(BigDecimal assAmt) {
		this.assAmt = assAmt;
	}

	public BigDecimal getGstRt() {
		return gstRt;
	}

	public void setGstRt(BigDecimal gstRt) {
		this.gstRt = gstRt;
	}

	public BigDecimal getIgstAmt() {
		return igstAmt;
	}

	public void setIgstAmt(BigDecimal igstAmt) {
		this.igstAmt = igstAmt;
	}

	public BigDecimal getCgstAmt() {
		return cgstAmt;
	}

	public void setCgstAmt(BigDecimal cgstAmt) {
		this.cgstAmt = cgstAmt;
	}

	public BigDecimal getSgstAmt() {
		return sgstAmt;
	}

	public void setSgstAmt(BigDecimal sgstAmt) {
		this.sgstAmt = sgstAmt;
	}

	public BigDecimal getCesRt() {
		return cesRt;
	}

	public void setCesRt(BigDecimal cesRt) {
		this.cesRt = cesRt;
	}

	public BigDecimal getCesAmt() {
		return cesAmt;
	}

	public void setCesAmt(BigDecimal cesAmt) {
		this.cesAmt = cesAmt;
	}

	public BigDecimal getCesNonAdvlAmt() {
		return cesNonAdvlAmt;
	}

	public void setCesNonAdvlAmt(BigDecimal cesNonAdvlAmt) {
		this.cesNonAdvlAmt = cesNonAdvlAmt;
	}

	public BigDecimal getStateCesRt() {
		return stateCesRt;
	}

	public void setStateCesRt(BigDecimal stateCesRt) {
		this.stateCesRt = stateCesRt;
	}

	public BigDecimal getStateCesAmt() {
		return stateCesAmt;
	}

	public void setStateCesAmt(BigDecimal stateCesAmt) {
		this.stateCesAmt = stateCesAmt;
	}

	public BigDecimal getStateCesNonAdvlAmt() {
		return stateCesNonAdvlAmt;
	}

	public void setStateCesNonAdvlAmt(BigDecimal stateCesNonAdvlAmt) {
		this.stateCesNonAdvlAmt = stateCesNonAdvlAmt;
	}

	public BigDecimal getOthChrg() {
		return othChrg;
	}

	public void setOthChrg(BigDecimal othChrg) {
		this.othChrg = othChrg;
	}

	public BigDecimal getTotItemVal() {
		return totItemVal;
	}

	public void setTotItemVal(BigDecimal totItemVal) {
		this.totItemVal = totItemVal;
	}

	public String getOrdLineRef() {
		return ordLineRef;
	}

	public void setOrdLineRef(String ordLineRef) {
		this.ordLineRef = ordLineRef;
	}

	public String getOrgCntry() {
		return orgCntry;
	}

	public void setOrgCntry(String orgCntry) {
		this.orgCntry = orgCntry;
	}

	public String getPrdSlNo() {
		return prdSlNo;
	}

	public void setPrdSlNo(String prdSlNo) {
		this.prdSlNo = prdSlNo;
	}

	public BchDtls getBchDtls() {
		return bchDtls;
	}

	public void setBchDtls(BchDtls bchDtls) {
		this.bchDtls = bchDtls;
	}

	public List<AttribDtls> getAttribDtls() {
		return attribDtls;
	}

	public void setAttribDtls(List<AttribDtls> attribDtls) {
		this.attribDtls = attribDtls;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ItemDtls [slNo=");
		builder.append(slNo);
		builder.append(", prdDesc=");
		builder.append(prdDesc);
		builder.append(", isServc=");
		builder.append(isServc);
		builder.append(", hsnCd=");
		builder.append(hsnCd);
		builder.append(", barcde=");
		builder.append(barcde);
		builder.append(", qty=");
		builder.append(qty);
		builder.append(", freeQty=");
		builder.append(freeQty);
		builder.append(", unit=");
		builder.append(unit);
		builder.append(", unitPrice=");
		builder.append(unitPrice);
		builder.append(", totAmt=");
		builder.append(totAmt);
		builder.append(", discount=");
		builder.append(discount);
		builder.append(", preTaxVal=");
		builder.append(preTaxVal);
		builder.append(", assAmt=");
		builder.append(assAmt);
		builder.append(", gstRt=");
		builder.append(gstRt);
		builder.append(", igstAmt=");
		builder.append(igstAmt);
		builder.append(", cgstAmt=");
		builder.append(cgstAmt);
		builder.append(", sgstAmt=");
		builder.append(sgstAmt);
		builder.append(", cesRt=");
		builder.append(cesRt);
		builder.append(", cesAmt=");
		builder.append(cesAmt);
		builder.append(", cesNonAdvlAmt=");
		builder.append(cesNonAdvlAmt);
		builder.append(", stateCesRt=");
		builder.append(stateCesRt);
		builder.append(", stateCesAmt=");
		builder.append(stateCesAmt);
		builder.append(", stateCesNonAdvlAmt=");
		builder.append(stateCesNonAdvlAmt);
		builder.append(", othChrg=");
		builder.append(othChrg);
		builder.append(", totItemVal=");
		builder.append(totItemVal);
		builder.append(", ordLineRef=");
		builder.append(ordLineRef);
		builder.append(", orgCntry=");
		builder.append(orgCntry);
		builder.append(", prdSlNo=");
		builder.append(prdSlNo);
		builder.append(", bchDtls=");
		builder.append(bchDtls);
		builder.append(", attribDtls=");
		builder.append(attribDtls);
		builder.append("]");
		return builder.toString();
	}

}

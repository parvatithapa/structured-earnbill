package com.sapienter.jbilling.einvoice.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EInvoiceRequest {
	@JsonProperty("Version")
	private String version = "1.1";
	@JsonProperty("TranDtls")
	private TranDtls tranDtls;
	@JsonProperty("DocDtls")
	private DocDtls docDtls;
	@JsonProperty("SellerDtls")
	private SellerDtls sellerDtls;
	@JsonProperty("BuyerDtls")
	private BuyerDtls buyerDtls;
	@JsonProperty("DispDtls")
	private DispDtls dispDtls;
	@JsonProperty("ExpDtls")
	private ExpDtls expDtls;
	@JsonProperty("ItemList")
	private List<ItemDtls> itemList;
	@JsonProperty("ValDtls")
	private ValDtls valDtls;
	@JsonProperty("AddlDocDtls")
	private List<AddlDocDtls> addlDocDtlsList;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public TranDtls getTranDtls() {
		return tranDtls;
	}

	public void setTranDtls(TranDtls tranDtls) {
		this.tranDtls = tranDtls;
	}

	public DocDtls getDocDtls() {
		return docDtls;
	}

	public void setDocDtls(DocDtls docDtls) {
		this.docDtls = docDtls;
	}

	public SellerDtls getSellerDtls() {
		return sellerDtls;
	}

	public void setSellerDtls(SellerDtls sellerDtls) {
		this.sellerDtls = sellerDtls;
	}

	public BuyerDtls getBuyerDtls() {
		return buyerDtls;
	}

	public void setBuyerDtls(BuyerDtls buyerDtls) {
		this.buyerDtls = buyerDtls;
	}

	public List<ItemDtls> getItemList() {
		return itemList;
	}

	public void setItemList(List<ItemDtls> itemList) {
		this.itemList = itemList;
	}

	public ValDtls getValDtls() {
		return valDtls;
	}

	public void setValDtls(ValDtls valDtls) {
		this.valDtls = valDtls;
	}

	public ExpDtls getExpDtls() { return expDtls; }

	public void setExpDtls(ExpDtls expDtls) { this.expDtls = expDtls; }

	public List<AddlDocDtls> getAddlDocDtlsList() {	return addlDocDtlsList;	}

	public void setAddlDocDtlsList(List<AddlDocDtls> addlDocDtlsList) { this.addlDocDtlsList = addlDocDtlsList;	}

	public DispDtls getDispDtls() {	return dispDtls; }

	public void setDispDtls(DispDtls dispDtls) { this.dispDtls = dispDtls; }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EInvoicePayload [version=");
		builder.append(version);
		builder.append(", tranDtls=");
		builder.append(tranDtls);
		builder.append(", docDtls=");
		builder.append(docDtls);
		builder.append(", sellerDtls=");
		builder.append(sellerDtls);
		builder.append(", buyerDtls=");
		builder.append(buyerDtls);
		builder.append(", dispDtls");
		builder.append(dispDtls);
		builder.append(", itemList=");
		builder.append(itemList);
		builder.append(", valDtls=");
		builder.append(valDtls);
		builder.append(", expDtls=");
		builder.append(expDtls);
		builder.append("addlDocDtls=");
		builder.append(addlDocDtlsList);
		builder.append("]");
		return builder.toString();
	}
}

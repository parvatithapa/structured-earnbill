package com.sapienter.jbilling.einvoice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EInvoiceResponse {
	@JsonProperty("status")
	private int responseStatus;
	@JsonProperty("message")
	private String message;
	@JsonProperty("Irn")
	private String irn;
	@JsonProperty("AckDt")
	private String ackDt;
	@JsonProperty("AckNo")
	private long ackNo;
	@JsonProperty("EwbDt")
	private String ewbDt;
	@JsonProperty("EwbNo")
	private long ewbNo;
	@JsonProperty("Status")
	private String status;
	@JsonProperty("Remarks")
	private String remarks;
	@JsonProperty("AckNoStr")
	private String ackNoStr;
	@JsonProperty("InfoDtls")
	private String infoDtls;
	@JsonProperty("EwbValidTill")
	private String ewbValidTill;
	@JsonProperty("SignedQRCode")
	private String signedQRCode;
	@JsonProperty("SignedInvoice")
	private String signedInvoice;
	@JsonProperty("uuid")
	private String uuid;
	@JsonProperty("SignedQrCodeImgUrl")
	private String signedQrCodeImgUrl;
	@JsonProperty("InvoicePdfUrl")
	private String invoicePdfUrl;
	@JsonProperty("IrnStatus")
	private IrnStatus irnStatus;
	@JsonProperty("EwbStatus")
	private String ewbStatus;
	@JsonProperty("Irp")
	private String irp;

	public int getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(int responseStatus) {
		this.responseStatus = responseStatus;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getIrn() {
		return irn;
	}

	public void setIrn(String irn) {
		this.irn = irn;
	}

	public String getAckDt() {
		return ackDt;
	}

	public void setAckDt(String ackDt) {
		this.ackDt = ackDt;
	}

	public long getAckNo() {
		return ackNo;
	}

	public void setAckNo(long ackNo) {
		this.ackNo = ackNo;
	}

	public String getEwbDt() {
		return ewbDt;
	}

	public void setEwbDt(String ewbDt) {
		this.ewbDt = ewbDt;
	}

	public long getEwbNo() {
		return ewbNo;
	}

	public void setEwbNo(long ewbNo) {
		this.ewbNo = ewbNo;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public String getAckNoStr() {
		return ackNoStr;
	}

	public void setAckNoStr(String ackNoStr) {
		this.ackNoStr = ackNoStr;
	}

	public String getInfoDtls() {
		return infoDtls;
	}

	public void setInfoDtls(String infoDtls) {
		this.infoDtls = infoDtls;
	}

	public String getEwbValidTill() {
		return ewbValidTill;
	}

	public void setEwbValidTill(String ewbValidTill) {
		this.ewbValidTill = ewbValidTill;
	}

	public String getSignedQRCode() {
		return signedQRCode;
	}

	public void setSignedQRCode(String signedQRCode) {
		this.signedQRCode = signedQRCode;
	}

	public String getSignedInvoice() {
		return signedInvoice;
	}

	public void setSignedInvoice(String signedInvoice) {
		this.signedInvoice = signedInvoice;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getSignedQrCodeImgUrl() {
		return signedQrCodeImgUrl;
	}

	public void setSignedQrCodeImgUrl(String signedQrCodeImgUrl) {
		this.signedQrCodeImgUrl = signedQrCodeImgUrl;
	}

	public String getInvoicePdfUrl() {
		return invoicePdfUrl;
	}

	public void setInvoicePdfUrl(String invoicePdfUrl) {
		this.invoicePdfUrl = invoicePdfUrl;
	}

	public IrnStatus getIrnStatus() {
		return irnStatus;
	}

	public void setIrnStatus(IrnStatus irnStatus) {
		this.irnStatus = irnStatus;
	}

	public String getEwbStatus() {
		return ewbStatus;
	}

	public void setEwbStatus(String ewbStatus) {
		this.ewbStatus = ewbStatus;
	}

	public String getIrp() {
		return irp;
	}

	public void setIrp(String irp) {
		this.irp = irp;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EInvoiceResponse [responseStatus=");
		builder.append(responseStatus);
		builder.append(", message=");
		builder.append(message);
		builder.append(", irn=");
		builder.append(irn);
		builder.append(", ackDt=");
		builder.append(ackDt);
		builder.append(", ackNo=");
		builder.append(ackNo);
		builder.append(", ewbDt=");
		builder.append(ewbDt);
		builder.append(", ewbNo=");
		builder.append(ewbNo);
		builder.append(", status=");
		builder.append(status);
		builder.append(", remarks=");
		builder.append(remarks);
		builder.append(", ackNoStr=");
		builder.append(ackNoStr);
		builder.append(", infoDtls=");
		builder.append(infoDtls);
		builder.append(", ewbValidTill=");
		builder.append(ewbValidTill);
		builder.append(", signedQRCode=");
		builder.append(signedQRCode);
		builder.append(", signedInvoice=");
		builder.append(signedInvoice);
		builder.append(", uuid=");
		builder.append(uuid);
		builder.append(", signedQrCodeImgUrl=");
		builder.append(signedQrCodeImgUrl);
		builder.append(", invoicePdfUrl=");
		builder.append(invoicePdfUrl);
		builder.append(", irnStatus=");
		builder.append(irnStatus);
		builder.append(", ewbStatus=");
		builder.append(ewbStatus);
		builder.append(", irp=");
		builder.append(irp);
		builder.append("]");
		return builder.toString();
	}
}

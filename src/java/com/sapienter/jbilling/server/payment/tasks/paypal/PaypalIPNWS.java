package com.sapienter.jbilling.server.payment.tasks.paypal;

import com.sapienter.jbilling.server.security.WSSecured;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by usman on 7/21/14.
 */
public class PaypalIPNWS  {

    private Integer id;
    private String itemNumber;
    private String verifySign;
    private String business;
    private String paymentStatus;
    private String transactionSubject;
    private String protectionEligibilty;
    private String firstName;
    private String payerId;
    private String payerEmail;
    private String mcFee;
    private String txnId;
    private String parentTxnId;
    private String quantity;
    private String recieverEmail;
    private String notifyVersion;
    private String payerStatus;
    private String mcGross;
    private Date paymentDate;
    private String paymentGross;
    private String ipnTrackId;
    private String receiptId;
    private String lastName;
    private String paymentType;
    private Integer verified;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getVerifySign() {
        return verifySign;
    }

    public void setVerifySign(String verifySign) {
        this.verifySign = verifySign;
    }

    public String getBusiness() {
        return business;
    }

    public void setBusiness(String business) {
        this.business = business;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionSubject() {
        return transactionSubject;
    }

    public void setTransactionSubject(String transactionSubject) {
        this.transactionSubject = transactionSubject;
    }

    public String getProtectionEligibilty() {
        return protectionEligibilty;
    }

    public void setProtectionEligibilty(String protectionEligibilty) {
        this.protectionEligibilty = protectionEligibilty;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }

    public String getPayerEmail() { return payerEmail; }

    public void setPayerEmail(String payerEmail) {  this.payerEmail = payerEmail; }

    public String getMcFee() {
        return mcFee;
    }

    public void setMcFee(String mcFee) {
        this.mcFee = mcFee;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getRecieverEmail() {
        return recieverEmail;
    }

    public void setRecieverEmail(String recieverEmail) {
        this.recieverEmail = recieverEmail;
    }

    public String getNotifyVersion() {
        return notifyVersion;
    }

    public void setNotifyVersion(String notifyVersion) {
        this.notifyVersion = notifyVersion;
    }

    public String getPayerStatus() {
        return payerStatus;
    }

    public void setPayerStatus(String payerStatus) {
        this.payerStatus = payerStatus;
    }

    public String getMcGross() {
        return mcGross;
    }

    public void setMcGross(String mcGross) {
        this.mcGross = mcGross;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentGross() {
        return paymentGross;
    }

    public void setPaymentGross(String paymentGross) {
        this.paymentGross = paymentGross;
    }

    public String getIpnTrackId() {
        return ipnTrackId;
    }

    public void setIpnTrackId(String ipnTrackId) {
        this.ipnTrackId = ipnTrackId;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public Integer getVerified() {return verified; }

    public void setVerified(Integer verified) {this.verified = verified; }

    public String getParentTxnId() { return parentTxnId; }

    public void setParentTxnId(String parentTxnId) { this.parentTxnId = parentTxnId; }
}

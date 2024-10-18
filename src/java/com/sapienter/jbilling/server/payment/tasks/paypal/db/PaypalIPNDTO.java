package com.sapienter.jbilling.server.payment.tasks.paypal.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import com.sapienter.jbilling.server.payment.tasks.paypal.PaypalIPNWS;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

/**
 * Created by usman on 7/21/14.
 */

@Entity
@org.hibernate.annotations.Entity(dynamicUpdate = true)
@TableGenerator(
        name = "paypal_ipn_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "paypal_ipn",
        allocationSize = 100)
@Table(name = "paypal_ipn")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class PaypalIPNDTO  implements java.io.Serializable  {


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

    public PaypalIPNDTO(){}

    public PaypalIPNDTO(PaypalIPNWS dto){

        setVerifySign(dto.getVerifySign());
        setBusiness(dto.getBusiness());
        setPaymentStatus(dto.getPaymentStatus());
        setTransactionSubject(dto.getTransactionSubject());
        setProtectionEligibilty(dto.getProtectionEligibilty());
        setFirstName(dto.getFirstName());
        setPayerId(dto.getPayerId());
        setPayerEmail(dto.getPayerEmail());
        setMcFee(dto.getMcFee());
        setTxnId(dto.getTxnId());
        setParentTxnId(dto.getParentTxnId());
        setQuantity(dto.getQuantity());
        setRecieverEmail(dto.getRecieverEmail());
        setNotifyVersion(dto.getNotifyVersion());
        setPayerStatus(dto.getPayerStatus());
        setMcGross(dto.getMcGross());
        setPaymentDate(dto.getPaymentDate());
        setPaymentGross(dto.getPaymentGross());
        setIpnTrackId(dto.getIpnTrackId());
        setReceiptId(dto.getReceiptId());
        setLastName(dto.getLastName());
        setPaymentType(dto.getPaymentType());
        setItemNumber(dto.getItemNumber());
        setVerified(dto.getVerified());

    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "paypal_ipn_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() { return id;}

    public void setId(Integer id) { this.id = id;}

    @Column(name = "item_number")
    public String getItemNumber() {  return itemNumber;}

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }
    @Column(name = "verify_sign")
    public String getVerifySign() {
        return verifySign;
    }

    public void setVerifySign(String verifySign) {
        this.verifySign = verifySign;
    }

    @Column(name = "business")
    public String getBusiness() {
        return business;
    }

    public void setBusiness(String business) {
        this.business = business;
    }

    @Column(name = "payment_status")
    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Column(name = "transaction_subject")
    public String getTransactionSubject() {
        return transactionSubject;
    }

    public void setTransactionSubject(String transactionSubject) {
        this.transactionSubject = transactionSubject;
    }

    @Column(name = "protection_eligibilty")
    public String getProtectionEligibilty() {
        return protectionEligibilty;
    }

    public void setProtectionEligibilty(String protectionEligibilty) {
        this.protectionEligibilty = protectionEligibilty;
    }

    @Column(name = "first_name")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Column(name = "payer_id")
    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }

    @Column(name = "payer_email")
    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    @Column(name = "mc_fee")
    public String getMcFee() {
        return mcFee;
    }

    public void setMcFee(String mcFee) {
        this.mcFee = mcFee;
    }

    @Column(name = "txn_id")
    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    @Column(name = "parent_txn_id")
    public String getParentTxnId() { return parentTxnId; }

    public void setParentTxnId(String parentTxnId) { this.parentTxnId = parentTxnId; }

    @Column(name = "quantity")
    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    @Column(name = "reciever_email")
    public String getRecieverEmail() {
        return recieverEmail;
    }

    public void setRecieverEmail(String recieverEmail) {
        this.recieverEmail = recieverEmail;
    }

    @Column(name = "notify_version")
    public String getNotifyVersion() {
        return notifyVersion;
    }

    public void setNotifyVersion(String notifyVersion) {
        this.notifyVersion = notifyVersion;
    }

    @Column(name = "payer_status")
    public String getPayerStatus() {
        return payerStatus;
    }

    public void setPayerStatus(String payerStatus) {
        this.payerStatus = payerStatus;
    }

    @Column(name = "mc_gross")
    public String getMcGross() {
        return mcGross;
    }

    public void setMcGross(String mcGross) {
        this.mcGross = mcGross;
    }

    @Column(name = "payment_date")
    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    @Column(name = "payment_gross")
    public String getPaymentGross() {
        return paymentGross;
    }

    public void setPaymentGross(String paymentGross) {
        this.paymentGross = paymentGross;
    }

    @Column(name = "ipn_track_id")
    public String getIpnTrackId() {
        return ipnTrackId;
    }

    public void setIpnTrackId(String ipnTrackId) {
        this.ipnTrackId = ipnTrackId;
    }

    @Column(name = "receipt_id")
    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    @Column(name = "last_name")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Column(name = "payment_type")
    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }


    @Column(name = "verified")
    public Integer getVerified() {return  verified;}

    public void setVerified(Integer  verified) {
        this. verified =  verified;
    }
}

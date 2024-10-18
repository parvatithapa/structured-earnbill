/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.payment;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.entity.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.sapienter.jbilling.server.security.HierarchicalEntity;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;


/**
 * @author Emil
 */
@ApiModel(value = "Payment Data", description = "PaymentWS model")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentWS implements WSSecured, Serializable, AutoCloseable, HierarchicalEntity {

    @NotNull(message="validation.error.notnull")
    private Integer userId = null;

    private String method = null;
    private Integer invoiceIds[] = null;

    // refund specific fields
    private Integer paymentId = null; // this is the payment refunded / to refund
    private PaymentAuthorizationDTO authorization = null;

    //missing properties from PaymentDTO
    @NotEmpty(message="validation.error.notnull")
    @Digits(integer=12, fraction=10, message="validation.error.not.a.number")
    @DecimalMin(value = "0.01", message = "validation.error.min,0.01")
    private String amount;
    @NotNull(message="validation.error.notnull")
    private Integer isRefund;
    //@NotNull(message="validation.error.apply.without.method")
    private Integer paymentMethodId;
    @NotNull(message="validation.error.notnull")
    private Date paymentDate;
    @NotNull(message="validation.error.notnull")
    private Integer currencyId;
    private int id;
    private Integer isPreauth;
    private Integer attempt;
    private String balance;
    @ConvertToTimezone
    private Date createDatetime;
    @ConvertToTimezone
    private Date updateDatetime;
    private int deleted;
    private Integer resultId;
    @Size(min = 0, max = 500, message = "validation.error.size,0,500")
    private String paymentNotes = null;
    private Integer paymentPeriod;

    private ProvisioningCommandWS[] provisioningCommands;

    @Valid
    private MetaFieldValueWS[] metaFields;

    private List<PaymentInformationWS> paymentInstruments = new ArrayList<>();

    // those instruments that are not linked to any payment and user user uses them to make payments
    private List<PaymentInformationWS> userPaymentInstruments = new ArrayList<>();

    //for auto payment
    private Integer autoPayment;
    private List<Integer> accessEntities;

    private boolean sendNotification = true;
    @JsonIgnore
    private PaymentInvoiceMapWS[] paymentInvoiceMap;

    //Changes for Stripe 
    
    private String externalIdOfAuthenticatedPayment;
    

    @ApiModelProperty(value = "Identifier of the result of the payment attempt")
    public Integer getResultId() {
        return resultId;
    }

    public void setResultId(Integer resultId) {
        this.resultId = resultId;
    }

    public PaymentWS() {
        super();
    }

    @ApiModelProperty(value = "Identifier of the user this payment record belongs to", required = true)
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @ApiModelProperty(value = "Name of the payment method used, must be unique")
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @ApiModelProperty(value = "Contains the list of invoices this payment is paying")
    public Integer[] getInvoiceIds() {
        return invoiceIds;
    }

    public void setInvoiceIds(Integer[] invoiceIds) {
        this.invoiceIds = invoiceIds;
    }

    @ApiModelProperty(value = "Refund specific field. When a refund is to be issued, this field holds the identifier of the payment that is to be refunded")
    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    @JsonProperty(value = "authorization")
    @ApiModelProperty(value = "Refund specific field. Contains the identifier of the authorization details for the refund")
    public PaymentAuthorizationDTO getAuthorizationId() {
        return authorization;
    }

    @JsonProperty(value = "authorization")
    public void setAuthorization(PaymentAuthorizationDTO authorization) {
        this.authorization = authorization;
    }

    // required by CXF
    @JsonIgnore
    public void setAuthorizationId(PaymentAuthorizationDTO authorization) {
        this.authorization = authorization;
    }

    @JsonIgnore
    public String getAmount() {
        return amount;
    }

    @JsonProperty(value = "amount")
    @ApiModelProperty(value = "The amount of the payment operation", required = true)
    public BigDecimal getAmountAsDecimal() {
        return Util.string2decimal(amount);
    }

    @JsonIgnore
    public void setAmountAsDecimal(BigDecimal amount) {
        setAmount(amount);
    }

    @JsonIgnore
    public void setAmount(String amount) {
        this.amount = amount;
    }

    @JsonProperty(value = "amount")
    public void setAmount(BigDecimal amount) {
        this.amount = (amount != null ? amount.toString() : null);
    }

    @ApiModelProperty(value = "'1' if this payment constitutes a refund operation, '0' otherwise")
    public Integer getIsRefund() {
        return isRefund;
    }

    public void setIsRefund(Integer isRefund) {
        this.isRefund = isRefund;
    }

    @ApiModelProperty(value = "Identifier of the payment method")
    @JsonProperty(value = "paymentMethodId")
    public Integer getMethodId() {
        return paymentMethodId;
    }

    @JsonProperty(value = "paymentMethodId")
    public void setMethodId(Integer paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    @ApiModelProperty(value = "Date of the payment", required = true)
    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    @ApiModelProperty(value = "Identifier of the currency in which the payment is being made", required = true)
    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @ApiModelProperty(value = "Unique identifier of the payment record")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "'1' if this payment is a preauthorization, '0' otherwise", required = true)
    public Integer getIsPreauth() {
        return isPreauth;
    }

    public void setIsPreauth(Integer isPreauth) {
        this.isPreauth = isPreauth;
    }

    @ApiModelProperty(value = "Number of the attempt to process this payment")
    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    @JsonIgnore
    public String getBalance() {
        return balance;
    }

    @ApiModelProperty(value = "Balance of this payment. If greater than 0, this payment could pay part of another invoice. If 0, this payment has already been applied to an invoice, lowering the invoice balance")
    @JsonProperty(value = "balance")
    public BigDecimal getBalanceAsDecimal() {
        return Util.string2decimal(balance);
    }

    @JsonIgnore
    public void setBalanceAsDecimal(BigDecimal balance) {
        setBalance(balance);
    }

    @JsonIgnore
    public void setBalance(String balance) {
        this.balance = balance;
    }

    @JsonProperty(value = "balance")
    public void setBalance(BigDecimal balance) {
        this.balance = (balance != null ? balance.toString() : null);
    }

    @ApiModelProperty(value = "Date this payment record was created")
    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @ApiModelProperty(value = "Date in which this payment record was last updated")
    public Date getUpdateDatetime() {
        return updateDatetime;
    }

    public void setUpdateDatetime(Date updateDatetime) {
        this.updateDatetime = updateDatetime;
    }

    @ApiModelProperty(value = "Delete flag. '1' if this record has been deleted, '0' otherwise")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @ApiModelProperty(value = "Any notes related to this payment for e.g. relevant invoiceId can be added to notes or any relevant information")
    public void setPaymentNotes(String paymentNotes){
        this.paymentNotes = paymentNotes;
    }

    public String getPaymentNotes(){
        return paymentNotes;
    }

    @ApiModelProperty(value = "Optional payment period identifier")
    public void setPaymentPeriod(Integer paymentPeriod){
        this.paymentPeriod = paymentPeriod;
    }

    public Integer getPaymentPeriod(){
        return paymentPeriod;
    }

    @ApiModelProperty(value = "An array of user defined fields")
    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @JsonIgnore
    public ProvisioningCommandWS[] getProvisioningCommands() {
        return provisioningCommands;
    }

    @JsonIgnore
    public void setProvisioningCommands(ProvisioningCommandWS[] provisioningCommands) {
        this.provisioningCommands = provisioningCommands;
    }

    @ApiModelProperty(value = "The payment instruments used in the payment")
    public List<PaymentInformationWS> getPaymentInstruments() {
        return paymentInstruments;
    }

    public void setPaymentInstruments(List<PaymentInformationWS> paymentInstruments) {
        this.paymentInstruments = paymentInstruments;
    }

    @ApiModelProperty(value = "The payment instruments for the user")
    public List<PaymentInformationWS> getUserPaymentInstruments() {
        return userPaymentInstruments;
    }

    public void setUserPaymentInstruments(List<PaymentInformationWS> paymentInstruments) {
        this.userPaymentInstruments = paymentInstruments;
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     * @return null
     */
    @Override
    public Integer getOwningEntityId() {
        return null;
    }

    @Override
    public Integer getOwningUserId() {
        return getUserId();
    }

    @ApiModelProperty(value = "The payment invoice map")
    @JsonProperty("paymentInvoiceMap")
    public PaymentInvoiceMapWS[] getPaymentInvoiceMap() {
        return paymentInvoiceMap;
    }

    @JsonIgnore
    void setPaymentInvoiceMap(PaymentInvoiceMapWS[] paymentInvoiceMap) {
        this.paymentInvoiceMap = paymentInvoiceMap;
    }

    @Override
    public String toString() {
        return "PaymentWS{"
                + "id=" + id
                + ", userId=" + userId
                + ", paymentMethodId=" + paymentMethodId
                + ", method='" + method + '\''
                + ", amount='" + amount + '\''
                + ", balance='" + balance + '\''
                + ", isRefund=" + isRefund
                + ", isPreauth=" + isPreauth
                + ", paymentDate=" + paymentDate
                + ", deleted=" + deleted
                + ", paymentId=" + paymentId
                + ", invoiceIds=" + (invoiceIds == null ? "null" : Arrays.asList(invoiceIds))
                + ", currencyId=" + currencyId
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentWS)) {
            return false;
        }

        PaymentWS paymentWS = (PaymentWS) o;
        return id == paymentWS.id &&
                deleted == paymentWS.deleted &&
                nullSafeEquals(userId, paymentWS.userId) &&
                nullSafeEquals(method, paymentWS.method) &&
                nullSafeEquals(invoiceIds, paymentWS.invoiceIds) &&
                nullSafeEquals(paymentId, paymentWS.paymentId) &&
                nullSafeEquals(authorization, paymentWS.authorization) &&
                Util.decimalEquals(getAmountAsDecimal(), paymentWS.getAmountAsDecimal()) &&
                nullSafeEquals(isRefund, paymentWS.isRefund) &&
                nullSafeEquals(paymentMethodId, paymentWS.paymentMethodId) &&
                nullSafeEquals(paymentDate, paymentWS.paymentDate) &&
                nullSafeEquals(currencyId, paymentWS.currencyId) &&
                nullSafeEquals(isPreauth, paymentWS.isPreauth) &&
                nullSafeEquals(attempt, paymentWS.attempt) &&
                Util.decimalEquals(getBalanceAsDecimal(), paymentWS.getBalanceAsDecimal()) &&
                nullSafeEquals(createDatetime, paymentWS.createDatetime) &&
                nullSafeEquals(resultId, paymentWS.resultId) &&
                nullSafeEquals(paymentNotes, paymentWS.paymentNotes) &&
                nullSafeEquals(paymentPeriod, paymentWS.paymentPeriod) &&
                nullSafeEquals(metaFields, paymentWS.metaFields) &&
                nullSafeEquals(paymentInstruments, paymentWS.paymentInstruments) &&
                nullSafeEquals(userPaymentInstruments, paymentWS.userPaymentInstruments);
    }

    @Override
    public int hashCode() {
        int result = nullSafeHashCode(userId);
        result = 31 * result + nullSafeHashCode(method);
        result = 31 * result + nullSafeHashCode(invoiceIds);
        result = 31 * result + nullSafeHashCode(paymentId);
        result = 31 * result + nullSafeHashCode(authorization);
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getAmountAsDecimal()));
        result = 31 * result + nullSafeHashCode(isRefund);
        result = 31 * result + nullSafeHashCode(paymentMethodId);
        result = 31 * result + nullSafeHashCode(paymentDate);
        result = 31 * result + nullSafeHashCode(currencyId);
        result = 31 * result + id;
        result = 31 * result + nullSafeHashCode(isPreauth);
        result = 31 * result + nullSafeHashCode(attempt);
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getBalanceAsDecimal()));
        result = 31 * result + nullSafeHashCode(createDatetime);
        result = 31 * result + deleted;
        result = 31 * result + nullSafeHashCode(resultId);
        result = 31 * result + nullSafeHashCode(paymentNotes);
        result = 31 * result + nullSafeHashCode(paymentPeriod);
        result = 31 * result + nullSafeHashCode(metaFields);
        result = 31 * result + nullSafeHashCode(paymentInstruments);
        result = 31 * result + nullSafeHashCode(userPaymentInstruments);
        return result;
    }

    @ApiModelProperty(value = "Auto Payment")
    public Integer getAutoPayment() {
        return autoPayment;
    }

    public void setAutoPayment(Integer autoPayment) {
        this.autoPayment = autoPayment;
    }

    @ApiModelProperty(value = "Send Notification")
    public boolean isSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(boolean sendNotification) {
        this.sendNotification = sendNotification;
    }

    @Override
    public void close() throws Exception {
        for(int i = 0; i < paymentInstruments.size(); i++ ){
            paymentInstruments.get(i).close();
            userPaymentInstruments.get(i).close();
        }
    }

    @Override
    public List<Integer> getAccessEntities() {
        return this.accessEntities;
    }


    public void setAccessEntities(List<Integer> accessEntities) {
        this.accessEntities = accessEntities;
    }

    @Override
    public Boolean ifGlobal() {
        return Boolean.FALSE;
    }

    @ApiModelProperty(value = "External reference id to complete authenticated payment.")
	public String getExternalIdOfAuthenticatedPayment() {
		return externalIdOfAuthenticatedPayment;
	}

	public void setExternalIdOfAuthenticatedPayment(String stripePaymentIntentId) {
		this.externalIdOfAuthenticatedPayment = stripePaymentIntentId;
	}
}

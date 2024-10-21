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

package com.sapienter.jbilling.server.invoice;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentInvoiceMapWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.security.HierarchicalEntity;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;


/**
 * @author Emil
 */
@ApiModel(value = "Invoice data", description = "InvoiceWS model")
public class InvoiceWS implements WSSecured, Serializable, HierarchicalEntity {

    private static final long serialVersionUID = 20130704L;

    private Integer delegatedInvoiceId = null;
    private Integer payments[] = null;
    private Integer userId = null;
    private InvoiceLineDTO invoiceLines[] = null;
    private Integer orders[] = null;
    private BillingProcessWS billingProcess;

    // original DTO
    private Integer id;
    private Date createDatetime;
    @ConvertToTimezone
    private Date createTimeStamp;
    private Date lastReminder;
    private Date dueDate;
    private String total;
    private Integer toProcess;
    private Integer statusId;
    private String balance;
    private String carriedBalance;
    private Integer inProcessPayment;
    private Integer deleted;
    private Integer paymentAttempts;
    private Integer isReview;
    private Integer currencyId;
    private String customerNotes;
    private String number;
    private Integer overdueStep;
    @Valid
    private MetaFieldValueWS[] metaFields;
    private List<Integer> accessEntities;

    //additional fields for the new gui
    private String statusDescr;
    private Integer[] creditNoteIds;

    @JsonIgnore
    private PaymentInvoiceMapWS[] paymentInvoiceMap;

    public InvoiceWS() {
        super();
    }

    @ApiModelProperty(value = "Name of the invoice status as determined from the field statusId")
    public String getStatusDescr() {
        return statusDescr;
    }

    public void setStatusDescr(String statusDescr) {
        this.statusDescr = statusDescr;
    }

    @ApiModelProperty(value = "Unique identifier of the invoice")
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The invoice date which is assigned by the billing process when the invoice is generated")
    public Date getCreateDatetime() {
        return this.createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @ApiModelProperty(value = "Timestamp of when this invoice record was created")
    public Date getCreateTimeStamp() {
        return this.createTimeStamp;
    }

    public void setCreateTimeStamp(Date createTimeStamp) {
        this.createTimeStamp = createTimeStamp;
    }

    @ApiModelProperty(value = "Date and time of when the latest reminder was issued for this invoice")
    public Date getLastReminder() {
        return this.lastReminder;
    }

    public void setLastReminder(Date lastReminder) {
        this.lastReminder = lastReminder;
    }

    @ApiModelProperty(value = "Due date of the invoice")
    public Date getDueDate() {
        return this.dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @JsonIgnore
    public String getTotal() {
        return this.total;
    }

    @ApiModelProperty(value = "The total amount of this invoice", dataType = "BigDecimal")
    @JsonProperty(value = "total")
    public BigDecimal getTotalAsDecimal() {
        return Util.string2decimal(total);
    }

    @JsonIgnore
    public void setTotal(String total) {
        this.total = total;
    }

    @JsonProperty(value = "total")
    public void setTotal(BigDecimal total) {
        this.total = (total != null ? total.toString() : null);
    }

    @ApiModelProperty(value = "1 - the invoice will be considered by the billing process as unpaid;" +
            " 0 - the invoice is either paid or carried over to another invoice")
    public Integer getToProcess() {
        return this.toProcess;
    }

    public void setToProcess(Integer toProcess) {
        this.toProcess = toProcess;
    }

    @ApiModelProperty(value = "Flag that indicates the status of the invoice")
    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    @JsonIgnore
    public String getBalance() {
        return this.balance;
    }

    @ApiModelProperty(value = "The amount of this invoice that is yet to be paid", dataType = "BigDecimal")
    @JsonProperty(value = "balance")
    public BigDecimal getBalanceAsDecimal() {
        return Util.string2decimal(balance);
    }

    @JsonIgnore
    public void setBalance(String balance) {
        this.balance = balance;
    }

    @JsonProperty(value = "balance")
    public void setBalance(BigDecimal balance) {
        this.balance = (balance != null ? balance.toString() : null);
    }

    @JsonIgnore
    public String getCarriedBalance() {
        return this.carriedBalance;
    }

    @ApiModelProperty(value = "Part of the total belonging to previous unpaid invoices" +
            " that has been delegated to this one", dataType = "BigDecimal")
    @JsonProperty(value = "carriedBalance")
    public BigDecimal getCarriedBalanceAsDecimal() {
        return Util.string2decimal(carriedBalance);
    }

    @JsonIgnore
    public void setCarriedBalance(String carriedBalance) {
        this.carriedBalance = carriedBalance;
    }

    @JsonProperty(value = "carriedBalance")
    public void setCarriedBalance(BigDecimal carriedBalance) {
        this.carriedBalance = (carriedBalance != null ? carriedBalance.toString() : null);
    }

    @ApiModelProperty(value = "Flag indicating if this invoice will be paid using automated payment" +
            " (through a payment processor), or if it will be paid externally (for example, with a paper check)")
    public Integer getInProcessPayment() {
        return this.inProcessPayment;
    }

    public void setInProcessPayment(Integer inProcessPayment) {
        this.inProcessPayment = inProcessPayment;
    }

    @ApiModelProperty(value = "Flag that indicates if this record is logically deleted in the database." +
            " 0 - not deleted; 1 - deleted")
    public Integer getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @ApiModelProperty(value = "How many payment attempts have been done by the automated payment" +
            " process to get this invoice paid")
    public Integer getPaymentAttempts() {
        return this.paymentAttempts;
    }

    public void setPaymentAttempts(Integer paymentAttempts) {
        this.paymentAttempts = paymentAttempts;
    }

    @ApiModelProperty(value = "Value that indicates if this invoice is a 'real' invoice, or one that belongs" +
            " to a review process. 0 - real invoice; 1 - review invoice")
    public Integer getIsReview() {
        return this.isReview;
    }

    public void setIsReview(Integer isReview) {
        this.isReview = isReview;
    }

    @ApiModelProperty(value = "Unique identifier of the currency in which the invoice amounts are being expressed")
    public Integer getCurrencyId() {
        return this.currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @ApiModelProperty(value = "Customer notes that are entered in a purchase order that was applied to this invoice")
    public String getCustomerNotes() {
        return this.customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    @ApiModelProperty(value = "Invoice number, which is assigned from a 'preference' field")
    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @ApiModelProperty(value = "Unique identifier of the collections step in which the invoice is currently")
    public Integer getOverdueStep() {
        return this.overdueStep;
    }

    public void setOverdueStep(Integer overdueStep) {
        this.overdueStep = overdueStep;
    }

    @ApiModelProperty(value = "Unique identifier of the customer for the invoice")
    public Integer getUserId() {
        return this.userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @ApiModelProperty(value = "Unique identifier of the invoice in which this invoice has been included" +
            " (usually for lack of payment)")
    public Integer getDelegatedInvoiceId() {
        return delegatedInvoiceId;
    }

    public void setDelegatedInvoiceId(Integer delegatedInvoiceId) {
        this.delegatedInvoiceId = delegatedInvoiceId;
    }

    @ApiModelProperty(value = "List of objects representing the invoice lines of the invoice")
    public InvoiceLineDTO[] getInvoiceLines() {
        return invoiceLines;
    }

    public void setInvoiceLines(InvoiceLineDTO[] invoiceLines) {
        this.invoiceLines = invoiceLines;
    }

    @ApiModelProperty(value = "Array of the ids of the purchase orders which have been included in this invoice")
    public Integer[] getOrders() {
        return orders;
    }

    public void setOrders(Integer[] orders) {
        this.orders = orders;
    }

    @ApiModelProperty(value = "List of ids of the payments that have been applied to this invoice")
    public Integer[] getPayments() {
        return payments;
    }

    public void setPayments(Integer[] payments) {
        this.payments = payments;
    }

    @ApiModelProperty(value = "List of user defined meta fields")
    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @ApiModelProperty(value = "The billing process that has generated this invoice")
    public BillingProcessWS getBillingProcess() {
        return billingProcess;
    }

    public void setBillingProcess(BillingProcessWS billingProcess) {
        this.billingProcess = billingProcess;
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     *
     * @return null
     */
    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return null;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return getUserId();
    }

    @Override
    @ApiModelProperty(value = "Companies with access")
    public List<Integer> getAccessEntities() {
        return this.accessEntities;
    }

    public void setAccessEntities(List<Integer> accessEntities) {
        this.accessEntities = accessEntities;
    }

    @Override
    public Boolean ifGlobal() {
        return false;
    }

    @ApiModelProperty(value = "CreditNotes applied to this invoice")
    public Integer[] getCreditNoteIds() {
        return creditNoteIds;
    }

    public void setCreditNoteIds(Integer[] creditNoteIds) {
        this.creditNoteIds = creditNoteIds;
    }

    @ApiModelProperty(value = "The payment invoice map")
    @JsonProperty("paymentInvoiceMap")
    public PaymentInvoiceMapWS[] getPaymentInvoiceMap() {
        return paymentInvoiceMap;
    }

    @JsonIgnore
    public void setPaymentInvoiceMap(PaymentInvoiceMapWS[] paymentInvoiceMap) {
        this.paymentInvoiceMap = paymentInvoiceMap;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("InvoiceWS [balance=");
        builder.append(balance);
        builder.append(", carriedBalance=");
        builder.append(carriedBalance);
        builder.append(", createDatetime=");
        builder.append(createDatetime);
        builder.append(", createTimeStamp=");
        builder.append(createTimeStamp);
        builder.append(", currencyId=");
        builder.append(currencyId);
        builder.append(", customerNotes=");
        builder.append(customerNotes);
        builder.append(", delegatedInvoiceId=");
        builder.append(delegatedInvoiceId);
        builder.append(", deleted=");
        builder.append(deleted);
        builder.append(", dueDate=");
        builder.append(dueDate);
        builder.append(", id=");
        builder.append(id);
        builder.append(", inProcessPayment=");
        builder.append(inProcessPayment);
        builder.append(", invoiceLines=");
        builder.append(Arrays.toString(invoiceLines));
        builder.append(", isReview=");
        builder.append(isReview);
        builder.append(", lastReminder=");
        builder.append(lastReminder);
        builder.append(", number=");
        builder.append(number);
        builder.append(", orders=");
        builder.append(Arrays.toString(orders));
        builder.append(", overdueStep=");
        builder.append(overdueStep);
        builder.append(", paymentAttempts=");
        builder.append(paymentAttempts);
        builder.append(", payments=");
        builder.append(Arrays.toString(payments));
        builder.append(", statusDescr=");
        builder.append(statusDescr);
        builder.append(", statusId=");
        builder.append(statusId);
        builder.append(", toProcess=");
        builder.append(toProcess);
        builder.append(", total=");
        builder.append(total);
        builder.append(", userId=");
        builder.append(userId);
        builder.append(", metaField=");
        builder.append(Arrays.toString(metaFields));
        builder.append(", billingProcess=");
        builder.append(billingProcess);
        builder.append(", creditNoteIds=");
        builder.append(Arrays.toString(creditNoteIds));
        builder.append(']');
        return builder.toString();
    }
}

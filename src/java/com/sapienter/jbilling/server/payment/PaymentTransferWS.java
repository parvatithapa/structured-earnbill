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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.validation.constraints.NotNull;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;


/**
 * @author Javier Rivero
 *
 */
public class PaymentTransferWS implements Serializable {

    @NotNull(message="validation.error.notnull")
    private Integer fromUserId = null;

    @NotNull(message="validation.error.notnull")
    private Integer toUserId = null;

    @NotNull(message="validation.error.notnull")
    private Integer paymentId = null;

    //missing properties from PaymentDTO
    private String amount;
    private int id;
    @ConvertToTimezone
    private Date createDatetime;
    private int deleted;
    private Integer createdBy;
    private String paymentTransferNotes;

    public PaymentTransferWS() {
        super();
    }


    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public String getAmount() {
        return amount;
    }

    public BigDecimal getAmountAsDecimal() {
        return amount != null ? new BigDecimal(amount) : null;
    }

    public void setAmountAsDecimal(BigDecimal amount) {
        setAmount(amount);
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = (amount != null ? amount.toString() : null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public Integer getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Integer fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Integer getToUserId() {
        return toUserId;
    }

    public void setToUserId(Integer toUserId) {
        this.toUserId = toUserId;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getPaymentTransferNotes() {
        return paymentTransferNotes;
    }

    public void setPaymentTransferNotes(String paymentTransferNotes) {
        this.paymentTransferNotes = paymentTransferNotes;
    }

    @Override
    public String toString() {
        return "PaymentWS{"
                + "id=" + id
                + ", fromUserId=" + fromUserId
                + ", toUserId=" + toUserId
                + ", amount='" + amount + '\''
                + ", createDatetime=" + createDatetime
                + ", deleted=" + deleted
                + ", paymentId=" + paymentId
                + ", createdBy=" + createdBy
                + '}';
    }
}

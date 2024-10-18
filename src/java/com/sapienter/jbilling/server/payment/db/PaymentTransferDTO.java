package com.sapienter.jbilling.server.payment.db;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author  Javier Rivero
 * @since 13/01/16.
 */
@Entity
@TableGenerator(
        name="payment_transfer_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="payment_transfer",
        allocationSize = 100)
@Table(name = "payment_transfer")
public class PaymentTransferDTO implements Serializable {

    private int id;
    private PaymentDTO payment;
    private Integer fromUserId;
    private Integer toUserId;
    private BigDecimal amount;
    private Date createDatetime;
    private Integer createdBy;
    private int deleted;
    private String paymentTransferNotes;

    public PaymentTransferDTO() {
    }

    public PaymentTransferDTO(int id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "payment_transfer_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    public PaymentDTO getPayment() {
        return payment;
    }

    public void setPayment(PaymentDTO payment) {
        this.payment = payment;
    }

    @Column(name = "amount", nullable = false, precision = 17, scale = 17)
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Column(name = "fromUserId")
    public Integer getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Integer fromUserId) {
        this.fromUserId = fromUserId;
    }

    @Column(name = "toUserId")
    public Integer getToUserId() {
        return toUserId;
    }

    public void setToUserId(Integer toUserId) {
        this.toUserId = toUserId;
    }

    @Column(name = "create_datetime", nullable = false, length = 29)
    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @Column(name = "createdBy")
    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    @Column(name = "deleted", nullable = false)
    public int getDeleted() {
        return this.deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @Column(name = "payment_transfer_notes", nullable = true)
    public String getPaymentTransferNotes() {
        return paymentTransferNotes;
    }

    public void setPaymentTransferNotes(String paymentTransferNotes) {
        this.paymentTransferNotes = paymentTransferNotes;
    }

}
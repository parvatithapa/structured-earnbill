package com.sapienter.jbilling.server.payment.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import java.io.Serializable;

@Entity
@TableGenerator(
    name = "payment_processor_unavailable_GEN",
    table = "jbilling_seqs",
    pkColumnName = "name",
    valueColumnName = "next_id",
    pkColumnValue = "payment_processor_unavailable")
@Table(name = "payment_processor_unavailable")
public class PaymentProcessorUnavailableDTO implements Serializable {
    
    private int id;
    private Integer entityId;
    private Integer paymentId;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "payment_processor_unavailable_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "entity_id", nullable = false)
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Column(name = "payment_id", nullable = false)
    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }
}

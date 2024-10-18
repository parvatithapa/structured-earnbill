package com.sapienter.jbilling.server.user.partner.db;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@TableGenerator(
        name="customer_commission_def_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="customer_commission_def",
        allocationSize=10
)
@Table(name="customer_commission_def")
public class CustomerCommissionDefinitionDTO {

    private CustomerCommissionDefinitionPK id;
    private BigDecimal rate;

    public CustomerCommissionDefinitionDTO() {
    }

    public CustomerCommissionDefinitionDTO(CustomerCommissionDefinitionPK id, BigDecimal rate) {
        this.id = id;
        this.rate = rate;
    }

    @Id
    public CustomerCommissionDefinitionPK getId() {
        return id;
    }

    public void setId(CustomerCommissionDefinitionPK id) {
        this.id = id;
    }

    @Column(name="rate", nullable=false, precision=17, scale=17)
    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public String toString() {
        return "CustomerCommissionDefinitionDTO{" +
                "id=" + id +
                ", rate=" + rate +
                '}';
    }
}

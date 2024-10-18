package com.sapienter.jbilling.server.user.partner.db;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 *
 */
@SuppressWarnings("serial")
@Entity
@TableGenerator(
        name = "partner_commission_value_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "partner_commission_value",
        allocationSize = 10
)
@Table(name = "partner_commission_value")
public class PartnerCommissionValueDTO {

    private int id;
    private PartnerDTO partner;
    private int days;
    private BigDecimal rate;
    private int versionNum;


    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "partner_commission_value_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="partner_id", nullable=false)
    public PartnerDTO getPartner () {
        return partner;
    }

    public void setPartner (PartnerDTO partner) {
        this.partner = partner;
    }

    @Column(name = "days")
    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    @Column(name = "rate", nullable = false, precision = 17, scale = 17)
    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    @Version
    @Column(name = "optlock")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }
}

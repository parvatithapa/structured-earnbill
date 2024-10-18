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
package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.server.user.partner.CommissionType;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Entity
@TableGenerator(
        name="partner_commission_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="partner_commission",
        allocationSize=10
)
@Table(name="partner_commission")
public class CommissionDTO implements Serializable, Exportable {
    private int id;
    private BigDecimal amount;
    private PartnerDTO partner;
    private CommissionType type;
    private CommissionProcessRunDTO commissionProcessRun;
    private List<PartnerCommissionLineDTO> invoiceCommissions;
    private CurrencyDTO currencyDTO;

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="partner_commission_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @Column(name="amount", nullable=false, precision=17, scale=17)
    public BigDecimal getAmount () {
        return amount;
    }

    public void setAmount (BigDecimal amount) {
        this.amount = amount;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    public CommissionType getType () {
        return type;
    }

    public void setType (CommissionType type) {
        this.type = type;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="partner_id", nullable=false)
    public PartnerDTO getPartner () {
        return partner;
    }

    public void setPartner (PartnerDTO partner) {
        this.partner = partner;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "commission")
    public List<PartnerCommissionLineDTO> getInvoiceCommissions () {
        return invoiceCommissions;
    }

    public void setInvoiceCommissions (List<PartnerCommissionLineDTO> invoiceCommissions) {
        this.invoiceCommissions = invoiceCommissions;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_process_run_id")
    public CommissionProcessRunDTO getCommissionProcessRun () {
        return commissionProcessRun;
    }

    public void setCommissionProcessRun (CommissionProcessRunDTO commissionProcessRun) {
        this.commissionProcessRun = commissionProcessRun;
    }

    @Transient
    public String[] getFieldNames() {
        return new String[] {
                "Agent ID",
                "Agent Name",
                "Currency",
                "Amount",
                "Commission Type"
        };
    }

    @Transient
    public Object[][] getFieldValues() {

        return new Object[][] {
                {
                        partner.getId(),
                        partner.getBaseUser().getUserName(),
                        currencyDTO.getDescription(),
                        amount,
                        type.name()
                }
        };
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    public CurrencyDTO getCurrency() {
        return this.currencyDTO;
    }

    public void setCurrency(CurrencyDTO currencyDTO) {
        this.currencyDTO = currencyDTO;
    }
}

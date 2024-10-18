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

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@TableGenerator(
        name="partner_commission_line_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="partner_commission_line",
        allocationSize=10
)
@Table(name="partner_commission_line")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name="dtype",
        discriminatorType = DiscriminatorType.STRING
)
public abstract class PartnerCommissionLineDTO {
    private int id;
    private CommissionProcessRunDTO commissionProcessRun;
    private PartnerDTO partner;
    private CommissionDTO commission;
    private PartnerCommissionLineDTO reversal;
    private PartnerCommissionLineDTO originalCommissionLine; //opposite of reversal. This object must the reversal

    public enum Type { INVOICE, REFERRAL, CUSTOMER}

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="partner_commission_line_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "reversal_id")
    public PartnerCommissionLineDTO getReversal() {
        return reversal;
    }

    public void setReversal(PartnerCommissionLineDTO reversal) {
        this.reversal = reversal;
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "reversal")
    public PartnerCommissionLineDTO getOriginalCommissionLine() {
        return originalCommissionLine;
    }

    public void setOriginalCommissionLine(PartnerCommissionLineDTO originalCommissionLine) {
        this.originalCommissionLine = originalCommissionLine;
    }

    @ManyToOne(fetch=FetchType.LAZY )
    @JoinColumn(name="partner_id", nullable=false)
    public PartnerDTO getPartner () {
        return partner;
    }

    public void setPartner (PartnerDTO partner) {
        this.partner = partner;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_process_run_id")
    public CommissionProcessRunDTO getCommissionProcessRun () {
        return commissionProcessRun;
    }

    public void setCommissionProcessRun (CommissionProcessRunDTO commissionProcessRun) {
        this.commissionProcessRun = commissionProcessRun;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id")
    public CommissionDTO getCommission () {
        return commission;
    }

    public void setCommission (CommissionDTO commission) {
        this.commission = commission;
    }

    @Transient
    public abstract Type getType();

    public abstract PartnerCommissionLineDTO createReversal();

    public static class Builder {
        int id;
        CommissionDTO commissionDTO;
        CommissionProcessRunDTO commissionProcessRunDTO;
        PartnerDTO partnerDTO;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder commissionDTO(CommissionDTO commissionDTO) {
            this.commissionDTO = commissionDTO;
            return this;
        }

        public Builder commissionProcessRunDTO(CommissionProcessRunDTO commissionProcessRunDTO) {
            this.commissionProcessRunDTO = commissionProcessRunDTO;
            return this;
        }

        public Builder partnerDTO(PartnerDTO partnerDTO) {
            this.partnerDTO = partnerDTO;
            return this;
        }

        protected <T extends PartnerCommissionLineDTO> T build(T line) {
            line.setId(id);
            line.setCommission(commissionDTO);
            line.setCommissionProcessRun(commissionProcessRunDTO);
            line.setPartner(partnerDTO);
            return line;
        }
    }
}

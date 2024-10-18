package com.sapienter.jbilling.server.user.partner.db;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Class hold commission paid for a referral. It holds the sum of commission for a referrer and referee
 */
@Entity
@DiscriminatorValue("REFERRAL")
public class ReferralCommissionDTO extends PartnerCommissionLineDTO {
    private BigDecimal referralAmount = BigDecimal.ZERO;
    private PartnerDTO referralPartner;

    @Column(name="referral_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getReferralAmount () {
        return referralAmount;
    }

    public void setReferralAmount (BigDecimal referralAmount) {
        this.referralAmount = referralAmount;
    }

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="referral_partner_id", nullable=true)
    public PartnerDTO getReferralPartner () {
        return referralPartner;
    }

    public void setReferralPartner (PartnerDTO referralPartner) {
        this.referralPartner = referralPartner;
    }

    @Transient
    public Type getType() {
        return Type.REFERRAL;
    }

    @Override
    public PartnerCommissionLineDTO createReversal() {
        //Reversals from commissions must be calculated from source types (invoice or customer)
        return null;
    }
}

package com.sapienter.jbilling.server.user.partner;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @see com.sapienter.jbilling.server.user.partner.db.PartnerCommissionValueDTO
 */
public class PartnerCommissionValueWS implements Serializable {
    private int days;
    private String rate;

    public PartnerCommissionValueWS() {
    }

    public PartnerCommissionValueWS(int days, BigDecimal rate) {
        this.days = days;
        this.rate = rate.toString();
    }

    public PartnerCommissionValueWS(int days, String rate) {
        this.days = days;
        this.rate = rate;
        if(rate != null && !rate.isEmpty()) {
            new BigDecimal(rate);
        }
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }
}

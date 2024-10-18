package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Embeddable
public class CustomerCommissionDefinitionPK implements Serializable {

    private PartnerDTO partner;
    private UserDTO baseUser;

    public CustomerCommissionDefinitionPK() {
    }

    public CustomerCommissionDefinitionPK(PartnerDTO partner, UserDTO baseUser) {
        this.partner = partner;
        this.baseUser = baseUser;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    public UserDTO getUser() {
        return baseUser;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    public PartnerDTO getPartner() {
        return partner;
    }

    public void setPartner(PartnerDTO partner) {
        this.partner = partner;
    }

    public void setUser(UserDTO baseUser) {
        this.baseUser = baseUser;
    }

    @Override
    public String toString() {
        return "CustomerCommissionDefinitionPK{" +
                "partner=" + partner +
                ", baseUser=" + baseUser +
                '}';
    }
}
package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.server.user.db.UserDTO;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Class hold commission paid for a new customer. Commission is only paid once per customer.
 */
@NamedQueries({
        @NamedQuery(name = "CustomerCommissionDTO.findForUserAndPartner",
                query = "from CustomerCommissionDTO " +
                        "where user.id = :userId and partner.id = :partnerId"),
        @NamedQuery(name = "CustomerCommissionDTO.findForUser",
                query = "from CustomerCommissionDTO " +
                        "where user.id = :userId")
})
@Entity
@DiscriminatorValue("CUSTOMER")
public class CustomerCommissionDTO extends PartnerCommissionLineDTO {
    private UserDTO user;
    private BigDecimal amount;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=true)
    public UserDTO getUser () {
        return user;
    }

    public void setUser (UserDTO baseUser) {
        this.user = baseUser;
    }

    @Column(name="customer_amount", nullable=false, precision=17, scale=17)
    public BigDecimal getAmount () {
        return amount;
    }

    public void setAmount (BigDecimal standardAmount) {
        this.amount = standardAmount;
    }

    @Override
    public PartnerCommissionLineDTO createReversal() {
        CustomerCommissionDTO reversal = new CustomerCommissionDTO();
        reversal.setAmount(amount.negate());
        reversal.setUser(user);
        reversal.setPartner(getPartner());
        return reversal;
    }

    @Transient
    public Type getType() {
        return Type.CUSTOMER;
    }

    public static class Builder extends PartnerCommissionLineDTO.Builder {
        UserDTO user;
        BigDecimal amount;

        public Builder user(UserDTO user) {
            this.user = user;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public CustomerCommissionDTO build() {
            CustomerCommissionDTO customerCommissionDTO = new CustomerCommissionDTO();
            customerCommissionDTO = super.build(customerCommissionDTO);
            customerCommissionDTO.setAmount(amount);
            customerCommissionDTO.setUser(user);
            return customerCommissionDTO;
        }
    }
}

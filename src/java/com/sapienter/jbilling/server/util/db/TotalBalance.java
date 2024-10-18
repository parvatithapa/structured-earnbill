package com.sapienter.jbilling.server.util.db;

import java.math.BigDecimal;

import lombok.ToString;

/**
 * Created by Andres Canevaro 06/06/18.
 *
 * Class to be used for gathering total balance per currency
 */
@ToString
public class TotalBalance {

    private BigDecimal balance;
    private Integer currency;

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Integer getCurrency() {
        return currency;
    }

    public void setCurrency(Integer currencyId) {
        this.currency = currencyId;
    }

}

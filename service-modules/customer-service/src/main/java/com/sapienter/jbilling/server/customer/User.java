package com.sapienter.jbilling.server.customer;

/**
 * Created by marcolin on 09/10/15.
 */
public class User {
    private Integer id;
    private Integer currencyId;
    private boolean deleted;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean getDeleted() {
        return deleted;
    }
}

package com.sapienter.jbilling.server.dt;

import java.util.Date;

/**
 * Created by wajeeha on 1/15/18.
 */
public class Price {
    private Date date;
    private String company;
    private Integer currencyId;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }
}

package com.sapienter.jbilling.server.payment.tasks.stripe.dto;

import java.util.Date;

import lombok.Getter;

import com.sapienter.jbilling.server.util.Util;

@Getter
public class CreditCard implements AutoCloseable{
    private final char[] type;
    private final char[] account;
    private final Date expirationDate;
    private final char[] cvv2;

    public CreditCard(char[] type, char[] account, Date date, char[] cvv2) {
        this.type = type;
        this.account = account;
        this.expirationDate = date;
        this.cvv2 = cvv2;
    }    

    @Override
    public void close() throws Exception {
    	Util.clearArray(account);
    	Util.clearArray(type);
    	Util.clearArray(cvv2);
    }
}
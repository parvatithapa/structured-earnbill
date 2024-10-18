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
package com.sapienter.jbilling.server.payment.tasks.paypal.dto;

import java.util.Arrays;

import com.sapienter.jbilling.server.util.Util;

/**
 * Created by Roman Liberov
 */

public class CreditCard implements AutoCloseable{
    private final char[] type;
    private final char[] account;
    private final char[] expirationDate;
    private final char[] cvv2;

    public CreditCard(char[] type, char[] account, char[] expirationDate, char[] cvv2) {
        this.type = type;
        this.account = account;
        this.expirationDate = expirationDate;
        this.cvv2 = cvv2;
    }

    public char[] getType() {
        return type;
    }

    public char[] getAccount() {
        return account;
    }

    public char[] getExpirationDate() {
        return expirationDate;
    }

    public char[] getCvv2() {
        return cvv2;
    }

    @Override
    public void close() throws Exception {
    	Util.clearArray(account);
    	Util.clearArray(type);
    	Util.clearArray(cvv2);
    	Util.clearArray(expirationDate);
    }
}

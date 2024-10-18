package com.sapienter.jbilling.server.spa;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by pablo_galera on 10/01/17.
 */
public class SpaPaymentResultWS implements Serializable {
    private BigDecimal amount;
    private String result;
    private String transactionToken;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("amount: ").append(amount);
        stringBuilder.append("\nresult: ").append(result);
        stringBuilder.append("\ntransactionToken: ").append(transactionToken);
        return stringBuilder.toString();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getTransactionToken() {
        return transactionToken;
    }

    public void setTransactionToken(String transactionToken) {
        this.transactionToken = transactionToken;
    }

    public boolean hasInvalidFields() {
        return amount == null || StringUtils.isEmpty(result)
                || StringUtils.isEmpty(transactionToken);
    }
}

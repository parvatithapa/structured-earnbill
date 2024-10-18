package com.sapienter.jbilling.server.spa;

import java.math.BigDecimal;

/**
 * Created by pablo_galera on 14/02/17.
 */
public class TaxDistributel {
    private String description;
    private BigDecimal total;
    private BigDecimal percentage;
    private DistributelTaxType type;

    public TaxDistributel(String description, BigDecimal percentage, BigDecimal total, DistributelTaxType type) {
        this.description = description;
        this.total = total;
        this.percentage = percentage;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public DistributelTaxType getType() {
        return type;
    }

    public void setType(DistributelTaxType type) {
        this.type = type;
    }
}

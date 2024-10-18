package com.sapienter.jbilling.server.report.builder;

import java.math.BigDecimal;

/**
 * RowRevenueSummary class
 * 
 * Represents each row for the Deferred Revenue Summary report
 * 
 * @author Leandro Bagur
 * @since 17/01/18.
 */
public class RowRevenueSummary {

    private final Integer invoiceId;
    private final String invoicedEntered;
    private final String revenueType;
    private final String category;
    private final String currency;
    private final BigDecimal amount;
    private final BigDecimal pstHst;
    private final BigDecimal gst;
    private final String province;

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public String getInvoicedEntered() {
        return invoicedEntered;
    }

    public String getRevenueType() {
        return revenueType;
    }

    public String getCategory() {
        return category;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPstHst() {
        return pstHst;
    }

    public BigDecimal getGst() {
        return gst;
    }

    public String getProvince() {
        return province;
    }

    private RowRevenueSummary(RowRevenueSummaryBuilder builder) {
        invoiceId = builder.invoiceId;
        invoicedEntered = builder.invoicedEntered;
        revenueType = builder.revenueType;
        category = builder.category;
        currency = builder.currency;
        amount = builder.amount;
        pstHst = builder.pstHst;
        gst = builder.gst;
        province = builder.province;
    }
    
    public static class RowRevenueSummaryBuilder {
        private int invoiceId;
        private String invoicedEntered;
        private String revenueType;
        private String category;
        private String currency;
        private BigDecimal amount;
        private BigDecimal pstHst;
        private BigDecimal gst;
        private String province;

        public RowRevenueSummaryBuilder invoiceId(int invoiceId) {
            this.invoiceId = invoiceId;
            return this;
        }
        
        public RowRevenueSummaryBuilder invoicedEntered(String invoicedEntered) {
            this.invoicedEntered = invoicedEntered;
            return this;
        }

        public RowRevenueSummaryBuilder revenueType(String revenueType) {
            this.revenueType = revenueType;
            return this;
        }

        public RowRevenueSummaryBuilder category(String category) {
            this.category = category;
            return this;
        }

        public RowRevenueSummaryBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public RowRevenueSummaryBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public RowRevenueSummaryBuilder pstHst(BigDecimal pstHst) {
            this.pstHst = pstHst;
            return this;
        }
        
        public RowRevenueSummaryBuilder gst(BigDecimal gst) {
            this.gst = gst;
            return this;
        }

        public RowRevenueSummaryBuilder province(String province) {
            this.province = province;
            return this;
        }
        
        public RowRevenueSummary build() {
            return new RowRevenueSummary(this);
        }
    }
    
    
}

package com.sapienter.jbilling.server.report.builder;

import org.apache.commons.lang.time.DateUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

/**
 * RowActivityReport class.
 * <p>
 * Class to represent each row for Activity reports.
 *
 * @author Leandro Bagur
 * @since 02/10/17.
 */
public class RowActivityReport {

    private final String staff;
    private final Date createDate;
    private final Integer productId;
    private final String productName;
    private final String productGroup; // report group
    private final int term;
    private final String service; // product class
    private final int customerId;
    private final BigDecimal totalPrice;

    RowActivityReport(RowReportActivityBuilder builder) {
        this.staff = builder.staff;
        this.createDate = DateUtils.truncate(builder.createDate, Calendar.DATE);
        this.productId = builder.productId;
        this.productName = builder.productName;
        this.productGroup = builder.productGroup;
        this.term = builder.term;
        this.service = builder.service;
        this.customerId = builder.customerId;
        this.totalPrice = builder.totalPrice;
    }

    public String getStaff() {
        return staff;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductGroup() {
        return productGroup;
    }

    public int getTerm() {
        return term;
    }

    public String getService() {
        return service;
    }

    public int getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public static class RowReportActivityBuilder {
        private String staff;
        private Date createDate;
        private Integer productId;
        private String productName;
        private String productGroup; // report group
        private int term;
        private String service; // product class
        private int customerId;
        private BigDecimal totalPrice;
        
        RowReportActivityBuilder() { }

        RowReportActivityBuilder staff(String staff) {
            this.staff = staff;
            return this;
        }

        RowReportActivityBuilder createDate(Date createDate) {
            this.createDate = createDate;
            return this;
        }

        RowReportActivityBuilder productId(Integer productId) {
            this.productId = productId;
            return this;
        }

        RowReportActivityBuilder productName(String productName) {
            this.productName = productName;
            return this;
        }

        RowReportActivityBuilder productGroup(String productGroup) {
            this.productGroup = productGroup;
            return this;
        }

        RowReportActivityBuilder term(int term) {
            this.term = term;
            return this;
        }

        RowReportActivityBuilder service(String service) {
            this.service = service;
            return this;
        }

        RowReportActivityBuilder customerId(int customerId) {
            this.customerId = customerId;
            return this;
        }

        RowReportActivityBuilder totalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        RowActivityReport build() {
            return new RowActivityReport(this);
        }
    }
}

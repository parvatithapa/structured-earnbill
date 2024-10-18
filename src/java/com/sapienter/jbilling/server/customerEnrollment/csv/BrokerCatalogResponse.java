package com.sapienter.jbilling.server.customerEnrollment.csv;

import java.math.BigDecimal;
import java.util.Date;

public class BrokerCatalogResponse {

    private String productId;
    private String productName;
    private Date activeStartDate;
    private Date activeEndDate;
    private String division;
    private String ldc;
    private String commodity;
    private String customerType;
    private String billingModel;
    private String productType;
    private BigDecimal rate;
    private BigDecimal etf;
    private Integer term;
    private Integer discountPeriod;
    private Integer blockSize;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Date getActiveStartDate() {
        return activeStartDate;
    }

    public void setActiveStartDate(Date activeStartDate) {
        this.activeStartDate = activeStartDate;
    }

    public Date getActiveEndDate() {
        return activeEndDate;
    }

    public void setActiveEndDate(Date activeEndDate) {
        this.activeEndDate = activeEndDate;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getLdc() {
        return ldc;
    }

    public void setLdc(String ldc) {
        this.ldc = ldc;
    }

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public String getBillingModel() {
        return billingModel;
    }

    public void setBillingModel(String billingModel) {
        this.billingModel = billingModel;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getEtf() {
        return etf;
    }

    public void setEtf(BigDecimal etf) {
        this.etf = etf;
    }

    public Integer getTerm() {
        return term;
    }

    public void setTerm(Integer term) {
        this.term = term;
    }

    public Integer getDiscountPeriod() {
        return discountPeriod;
    }

    public void setDiscountPeriod(Integer discountPeriod) {
        this.discountPeriod = discountPeriod;
    }

    public Integer getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(Integer blockSize) {
        this.blockSize = blockSize;
    }
}
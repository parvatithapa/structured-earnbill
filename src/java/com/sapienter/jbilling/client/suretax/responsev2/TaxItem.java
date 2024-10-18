package com.sapienter.jbilling.client.suretax.responsev2;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TaxItem {
    @JsonProperty("TaxTypeCode")
    public String taxTypeCode;
    @JsonProperty("TaxTypeDesc")
    public String taxTypeDesc;
    @JsonProperty("TaxAmount")
    public String taxAmount;
    @JsonProperty("Revenue")
    public String revenue;
    @JsonProperty("CountyName")
    public String countyName;
    @JsonProperty("CityName")
    public String cityName;
    @JsonProperty("TaxRate")
    public String taxRate;
    @JsonProperty("PercentTaxable")
    public String percentTaxable;
    @JsonProperty("FeeRate")
    public String feeRate;
    @JsonProperty("RevenueBase")
    public String revenueBase;
    @JsonProperty("TaxOnTax")
    public String taxOnTax;



    public void setTaxTypeCode(String taxTypeCode) {
        this.taxTypeCode = taxTypeCode;
    }

    public void setTaxTypeDesc(String taxTypeDesc) {
        this.taxTypeDesc = taxTypeDesc;
    }

    public void setTaxAmount(String taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setRevenue(String revenue) {
        this.revenue = revenue;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public void setTaxRate(String taxRate) {
        this.taxRate = taxRate;
    }

    public void setPercentTaxable(String percentTaxable) {
        this.percentTaxable = percentTaxable;
    }

    public void setFeeRate(String feeRate) {
        this.feeRate = feeRate;
    }

    public void setRevenueBase(String revenueBase) {
        this.revenueBase = revenueBase;
    }

    public void setTaxOnTax(String taxOnTax) {
        this.taxOnTax = taxOnTax;
    }

    @Override
    public String toString() {
        return "TaxItem{" +
                "taxTypeCode='" + taxTypeCode + '\'' +
                ", taxTypeDesc='" + taxTypeDesc + '\'' +
                ", taxAmount='" + taxAmount + '\'' +
                ", revenue='" + revenue + '\'' +
                ", countyName='" + countyName + '\'' +
                ", cityName='" + cityName + '\'' +
                ", taxRate='" + taxRate + '\'' +
                ", percentTaxable='" + percentTaxable + '\'' +
                ", feeRate='" + feeRate + '\'' +
                ", revenueBase='" + revenueBase + '\'' +
                ", taxOnTax='" + taxOnTax + '\'' +
                '}';
    }
}

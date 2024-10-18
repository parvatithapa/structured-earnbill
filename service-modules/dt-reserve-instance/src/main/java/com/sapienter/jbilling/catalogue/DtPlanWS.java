package com.sapienter.jbilling.catalogue;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.util.Map;

import javax.validation.constraints.Size;

@ApiModel(value = "Reserve Instance Plan catalogue Data", description = "DtPlanWS model")
public class DtPlanWS {
    private Integer planId;
    @Size (min=0,max=255, message="validation.error.size,1,255")
    private String enPlanName;
    private String dePlanName;
    private String currencyCode;
    private BigDecimal planPrice;
    private String activeSince;
    private String activeUntil;
    private String enProductName;
    private String deProductName;
    private String productCategory;
    private Integer duration;
    private String paymentMode;
    private BigDecimal productElasticPrice;
    private Map<String,String> productData;



    @ApiModelProperty(value = "Category of the product")
    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    @ApiModelProperty(value = "German description of the plan")
    public String getDePlanName() {
        return dePlanName;
    }

    public void setDePlanName(String dePlanName) {
        this.dePlanName = dePlanName;
    }

    @ApiModelProperty(value = "German description of plan item")
    public String getDeProductName() {
        return deProductName;
    }

    public void setDeProductName(String deProductName) {
        this.deProductName = deProductName;
    }

    @ApiModelProperty(value = "Plan Price")
    public BigDecimal getPlanPrice() {
        return planPrice;
    }

    public void setPlanPrice(BigDecimal planPrice) {
        this.planPrice = planPrice;
    }

    @ApiModelProperty(value = "Price of the plan item after usage exceed from the reserve instance free usage limit")
    public BigDecimal getProductElasticPrice() {
        return productElasticPrice;
    }

    public void setProductElasticPrice(BigDecimal productElasticPrice) {
        this.productElasticPrice = productElasticPrice;
    }

    @ApiModelProperty(value = "Plan Id")
    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer id) {
        this.planId = id;
    }

    @ApiModelProperty(value = "English description of the plan")
    public String getEnPlanName() {
        return enPlanName;
    }

    public void setEnPlanName(String description) {
        this.enPlanName = description;
    }

    @ApiModelProperty(value = "Currency code of plan")
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @ApiModelProperty(value = "Date when this plan/order will stop being active.")
    public String getActiveSince() {
        return activeSince;
    }

    public void setActiveSince(String activeSince) {
        this.activeSince = activeSince;
    }

    @ApiModelProperty(value = "Date when this plan/order will stop being active")
    public String getActiveUntil() {
        return activeUntil;
    }

    public void setActiveUntil(String activeUntil) {
        this.activeUntil = activeUntil;
    }

    @ApiModelProperty(value = "English description of plan item")
    public String getEnProductName() {
        return enProductName;
    }

    public void setEnProductName(String enProductName) {
        this.enProductName = enProductName;
    }

    @ApiModelProperty(value = "Duration in months of the reserve instance.(possible values 12,24,36)")
    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    @ApiModelProperty(value = "Payment mode of the reserve instance.(Monthly/UPFRONT)")
    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    @ApiModelProperty(value = "Product specifications that defines the product")
    public Map<String, String> getProductData() {
        return productData;
    }

    public void setProductData(Map<String, String> prodData) {
        this.productData = prodData;
    }
}

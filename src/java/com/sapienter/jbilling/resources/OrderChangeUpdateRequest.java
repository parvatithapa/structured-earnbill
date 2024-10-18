package com.sapienter.jbilling.resources;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@SuppressWarnings("serial")
public class OrderChangeUpdateRequest implements Serializable {

    private Integer userId; 
    private String productCode;
    private BigDecimal newPrice; 
    private BigDecimal newQuantity;
    private Date changeEffectiveDate;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    public BigDecimal getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(BigDecimal newQuantity) {
        this.newQuantity = newQuantity;
    }

    public Date getChangeEffectiveDate() {
        return changeEffectiveDate;
    }

    public void setChangeEffectiveDate(Date changeEffectiveDate) {
        this.changeEffectiveDate = changeEffectiveDate;
    }

    @Override
    public String toString() {
        return "OrderChangeUpdateRequest [userId=" + userId + ", productCode="
                + productCode + ", newPrice=" + newPrice + ", newQuantity="
                + newQuantity + ", changeEffectiveDate=" + changeEffectiveDate
                + "]";
    }

}

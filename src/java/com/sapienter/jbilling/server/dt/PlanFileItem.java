package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.server.util.InternationalDescriptionWS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Taimoor Choudhary on 3/28/18.
 */
public class PlanFileItem {

    private String planNumber;
    private String planCategory;
    private String paymentOption;
    private String duration;
    private List<InternationalDescriptionWS> descriptions = new ArrayList<>();
    private Integer planPeriodId;
    private Integer currencyId;
    private Integer planId;
    private Date availabilityStartDate;
    private Date availabilityEndDate;
    private BigDecimal rate;
    private List<PlanUsagePool> freeUsagePools;
    private List<PlanProduct> planProducts;

    public PlanFileItem() {
        freeUsagePools = new ArrayList<>();
        planProducts = new ArrayList<>();
    }

    public PlanFileItem(String planNumber, List<InternationalDescriptionWS> descriptions, Integer planPeriodId, Integer currencyId) {
        this.planNumber = planNumber;
        this.descriptions = descriptions;
        this.planPeriodId = planPeriodId;
        this.currencyId = currencyId;

        freeUsagePools = new ArrayList<>();
        planProducts = new ArrayList<>();
    }

    public String getPlanNumber() {
        return planNumber;
    }

    public void setPlanNumber(String planNumber) {
        this.planNumber = planNumber;
    }

    public String getPlanCategory() {
        return planCategory;
    }

    public void setPlanCategory(String planCategory) {
        this.planCategory = planCategory;
    }

    public String getPaymentOption() {
        return paymentOption;
    }

    public void setPaymentOption(String paymentOption) {
        this.paymentOption = paymentOption;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    public Integer getPlanPeriodId() {
        return planPeriodId;
    }

    public void setPlanPeriodId(Integer planPeriodId) {
        this.planPeriodId = planPeriodId;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public Date getAvailabilityStartDate() {
        return availabilityStartDate;
    }

    public void setAvailabilityStartDate(Date availabilityStartDate) {
        this.availabilityStartDate = availabilityStartDate;
    }

    public Date getAvailabilityEndDate() {
        return availabilityEndDate;
    }

    public void setAvailabilityEndDate(Date availabilityEndDate) {
        this.availabilityEndDate = availabilityEndDate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public List<PlanUsagePool> getFreeUsagePools() {
        return freeUsagePools;
    }

    public List<PlanProduct> getPlanProducts() {
        return planProducts;
    }

    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PlanFileItem{");
        sb.append("planNumber='").append(planNumber).append('\'');
        sb.append(", planCategory=").append(planCategory);
        sb.append(", descriptions=").append(descriptions);
        sb.append(", planPeriodId=").append(planPeriodId);
        sb.append(", currencyId=").append(currencyId);
        sb.append(", availabilityStartDate=").append(availabilityStartDate);
        sb.append(", getAvailabilityEndDate=").append(availabilityEndDate);
        sb.append(", rate=").append(rate);
        sb.append(", freeUsagePoolIds=").append(freeUsagePools);
        sb.append(", planProducts=").append(planProducts);
        sb.append(", paymentOption=").append(paymentOption);
        sb.append(", duration=").append(duration);
        sb.append('}');
        return sb.toString();
    }
}



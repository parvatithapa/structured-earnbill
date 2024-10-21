package com.sapienter.jbilling.server.spc.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public class Plan {
    private String planNumber;
    private String planName;
    private String routeRateCard;
    private String planRate;
    private String routeRateCardId;
    private String usagePoolName;
    private Integer[] usageItemIds;
    private String bundleItemRate;
    private String[] assetItemIds;
    private String assetRate;

    private String status;
    private Integer[] categories;
    private String technology;
    private String revenueGLCode;
    private String costGLCode;
    private Integer[] carrier;
    private Double exGST;
    private Double gst;
    private Double total;
    private String usageTotal;
    private Integer[] items;
    private Integer period;
    private String creditUsageAmount;
    private List<Integer> tariffcodeItmeIds;
    private String usagePoolGLCode;
    private String usagePoolCostsGLCode;
    private Integer[] usagePoolIds;

    public Plan() {
        super();
    }

    public Plan(String planNumber, String planName, String planRate, String usagePoolName, Integer[] usageItemIds, String bundleItemRate) {
        super();
        this.planNumber = planNumber;
        this.planName = planName;
        this.planRate = planRate;
        this.usagePoolName = usagePoolName;
        this.usageItemIds = usageItemIds;
        this.bundleItemRate = bundleItemRate;
    }

    public Plan(String planNumber, String planName, String routeRateCard, String planRate, String routeRateCardId) {
        super();
        this.planNumber = planNumber;
        this.planName = planName;
        this.routeRateCard = routeRateCard;
        this.planRate = planRate;
        this.routeRateCardId = routeRateCardId;
    }

    public Plan(String planNumber, String planName, String routeRateCard, String planRate, String routeRateCardId, String usagePoolName,
            Integer[] usageItemIds, String bundleItemRate, String[] assetItemIds, String assetRate) {
        super();
        this.planNumber = planNumber;
        this.planName = planName;
        this.routeRateCard = routeRateCard;
        this.planRate = planRate;
        this.routeRateCardId = routeRateCardId;
        this.usagePoolName = usagePoolName;
        this.usageItemIds = usageItemIds;
        this.bundleItemRate = bundleItemRate;
        this.assetItemIds = assetItemIds;
        this.assetRate = assetRate;
    }

    public Plan(String planNumber, String planName, String routeRateCard, String routeRateCardId, String planRate, String bundleItemRate) {
        super();
        this.planNumber = planNumber;
        this.planName = planName;
        this.routeRateCard = routeRateCard;
        this.planRate = planRate;
        this.routeRateCardId = routeRateCardId;
        this.bundleItemRate = bundleItemRate;
    }

    public String getUsagePoolCostsGLCode() {
        return usagePoolCostsGLCode;
    }

    public void setUsagePoolCostsGLCode(String usagePoolCostsGLCode) {
        this.usagePoolCostsGLCode = usagePoolCostsGLCode;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Integer[] getItems() {
        return items;
    }

    public void setItems(Integer[] items) {
        this.items = items;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer[] getCategories() {
        return categories;
    }

    public void setCategories(Integer[] categories) {
        this.categories = categories;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getRevenueGLCode() {
        return revenueGLCode;
    }

    public void setRevenueGLCode(String revenueGLCode) {
        this.revenueGLCode = revenueGLCode;
    }

    public String getCostGLCode() {
        return costGLCode;
    }

    public void setCostGLCode(String costGLCode) {
        this.costGLCode = costGLCode;
    }

    public Integer[] getCarrier() {
        return carrier;
    }

    public void setCarrier(Integer[] carrier) {
        this.carrier = carrier;
    }

    public Double getExGST() {
        return exGST;
    }

    public void setExGST(Double exGST) {
        this.exGST = exGST;
    }

    public Double getGst() {
        return gst;
    }

    public void setGst(Double gst) {
        this.gst = gst;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getUsageTotal() {
        return usageTotal;
    }

    public void setUsageTotal(String usageTotal) {
        this.usageTotal = usageTotal;
    }

    public String getPlanNumber() {
        return planNumber;
    }

    public void setPlanNumber(String planNumber) {
        this.planNumber = planNumber;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getRouteRateCard() {
        return routeRateCard;
    }

    public void setRouteRateCard(String routeRateCard) {
        this.routeRateCard = routeRateCard;
    }

    public String getPlanRate() {
        return planRate;
    }

    public void setPlanRate(String planRate) {
        this.planRate = planRate;
    }

    public String getRouteRateCardId() {
        return routeRateCardId;
    }

    public void setRouteRateCardId(String routeRateCardId) {
        this.routeRateCardId = routeRateCardId;
    }

    public String getUsagePoolName() {
        return usagePoolName;
    }

    public void setUsagePoolName(String usagePoolName) {
        this.usagePoolName = usagePoolName;
    }

    public Integer[] getUsageItemIds() {
        return usageItemIds;
    }

    public void setUsageItemIds(Integer[] usageItemIds) {
        this.usageItemIds = usageItemIds;
    }

    public String getBundleItemRate() {
        return bundleItemRate;
    }

    public void setBundleItemRate(String bundleItemRate) {
        this.bundleItemRate = bundleItemRate;
    }

    public String[] getAssetItemIds() {
        return assetItemIds;
    }

    public void setAssetItemIds(String[] assetItemIds) {
        this.assetItemIds = assetItemIds;
    }

    public String getAssetRate() {
        return assetRate;
    }

    public void setAssetRate(String assetRate) {
        this.assetRate = assetRate;
    }

    public String getCreditUsageAmount() {
        return creditUsageAmount;
    }

    public void setCreditUsageAmount(String creditUsageAmount) {
        this.creditUsageAmount = creditUsageAmount;
    }

    public List<Integer> getTariffcodeItmeIds() {
        return tariffcodeItmeIds;
    }

    public void setTariffcodeItmeIds(List<Integer> tariffcodeItmeIds) {
        this.tariffcodeItmeIds = tariffcodeItmeIds;
    }

    public String getUsagePoolGLCode() {
        return usagePoolGLCode;
    }

    public void setUsagePoolGLCode(String usagePoolGLCode) {
        this.usagePoolGLCode = usagePoolGLCode;
    }

    public Integer[] getUsagePoolIds() {
        return usagePoolIds;
    }

    public void setUsagePoolIds(Integer[] usagePoolIds) {
        this.usagePoolIds = usagePoolIds;
    }

    public String getExTaxRate() {
        BigDecimal value = null;
        BigDecimal planRate1 = new BigDecimal(planRate);
        value = planRate1.multiply(BigDecimal.TEN).divide(new BigDecimal("11"), MathContext.DECIMAL64).setScale(4, RoundingMode.HALF_UP);
        return value.toString();
    }
}

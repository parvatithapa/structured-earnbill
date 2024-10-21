package com.sapienter.jbilling.server.spc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.ToString;

@ToString
public class SpcCreditPoolInfo {
    private Integer id;
    private Integer planId;
    private String tariffCodes;
    private String creditItemId;
    private String consumptionPercentages;
    private String freeAmount;
    private String creditPoolName;

    public SpcCreditPoolInfo(Integer id, Integer planId, String tariffCodes,
            String consumptionPercentages, String freeAmount, String creditItemId, String creditPoolName) {
        this.id = id;
        this.planId = planId;
        this.tariffCodes = tariffCodes;
        this.consumptionPercentages = consumptionPercentages;
        this.freeAmount = freeAmount;
        this.creditItemId = creditItemId;
        this.creditPoolName = creditPoolName;
    }

    public Integer getId() {
        return id;
    }

    public Integer getPlanId() {
        return planId;
    }

    public String getTariffCodes() {
        return tariffCodes;
    }

    public String getConsumptionPercentages() {
        return consumptionPercentages;
    }

    public String getFreeAmount() {
        return freeAmount;
    }

    public String getCreditItemId() {
        return creditItemId;
    }

    public Integer getCreditItemIdAsInt() {
        return Integer.parseInt(creditItemId);
    }

    public BigDecimal getFreeAmountAsDecimal() {
        return new BigDecimal(freeAmount);
    }

    public String getCreditPoolName() {
        return creditPoolName;
    }

    public void setCreditPoolName(String creditPoolName) {
        this.creditPoolName = creditPoolName;
    }

    public List<String> getTariffCodeList() {
        return Arrays.stream(tariffCodes.split(","))
                .collect(Collectors.toList());
    }

    public List<BigDecimal> getConsumptionPercentageList() {
        return Arrays.stream(consumptionPercentages.split(","))
                .map(BigDecimal::new)
                .collect(Collectors.toList());
    }

}

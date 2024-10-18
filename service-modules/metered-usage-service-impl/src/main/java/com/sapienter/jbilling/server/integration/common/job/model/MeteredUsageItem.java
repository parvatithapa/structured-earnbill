package com.sapienter.jbilling.server.integration.common.job.model;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Data;

@Data
public class MeteredUsageItem {
    // Usage info
    private Integer         itemId;
    private BigDecimal      price;
    private BigDecimal      quantity;

    // Product Info
    private String          productDescription;
    private String          productCode;
    private String          productBillingUnit;

    // Pricing info
    private PriceModelType  priceModelType;
    private Map<String,String> priceModelAttributes;

    // Derived values
    private String          formattedDescription;
    private String          customUnit;
}

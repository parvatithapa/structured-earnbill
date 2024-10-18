package com.sapienter.jbilling.appdirect.subscription;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CostWS {

	private Long id;

	private Long editionPricingItemId;

	private String unit;

	private int unitDependency;

	private BigDecimal minUnits;

	private BigDecimal maxUnits;

	private boolean meteredUsage;

	private BigDecimal increment;

	private boolean pricePerIncrement;

	private boolean blockContractDecrease;

	private boolean blockContractIncrease;

	private boolean blockOriginalContractDecrease;

	private Map<String, BigDecimal> amount = new HashMap<>();

	private String pricingStrategy;

}
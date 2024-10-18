package com.sapienter.jbilling.appdirect.subscription;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TerminationFeeWS implements Serializable {
	private static final long serialVersionUID = -8758738154282462200L;

	/**
	 * Type of termination fee: NONE, PERCENTAGE, FLAT_RATE
	 */
	private String type;

	private String description;

	private BigDecimal percentageFee;

	/**
	 * Flat fee
	 */
	private Map<String, BigDecimal> amount = new HashMap<>();
}

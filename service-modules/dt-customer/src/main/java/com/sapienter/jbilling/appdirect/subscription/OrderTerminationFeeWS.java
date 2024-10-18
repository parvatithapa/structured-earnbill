package com.sapienter.jbilling.appdirect.subscription;


import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderTerminationFeeWS implements Serializable {
	/**
	 * Termination fee type -- can be NONE, PERCENTAGE, or FLAT_RATE
	 */
	private String type;

	/**
	 * Termination fee description
	 */
	private String description;

	/**
	 * Percentage to charge for the termination fee
	 */
	private BigDecimal percentage;

	/**
	 * Flat rate price to charge for the termination fee
	 */
	private BigDecimal price;
}

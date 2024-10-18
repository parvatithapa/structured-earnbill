package com.sapienter.jbilling.appdirect.subscription;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderLineWS {

	private Long id;

	private Long editionPricingItemId;

	private String type;

	private String unit;

	private BigDecimal quantity;

	private BigDecimal basePrice;

	private BigDecimal price;

	private BigDecimal listingPrice;

	private BigDecimal percentage;

	private BigDecimal totalPrice;

	private BigDecimal channelMarkupPrice;

	private BigDecimal resellerMarkupPrice;

	private BigDecimal wholesalePrice;

	private String applicationName;

	private String editionName;

	private String description;
}

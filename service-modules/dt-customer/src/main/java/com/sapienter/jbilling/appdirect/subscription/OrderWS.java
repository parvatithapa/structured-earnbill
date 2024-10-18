package com.sapienter.jbilling.appdirect.subscription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;


@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderWS extends BaseOrderWS{

	private PaymentPlanWS paymentPlan;

	private OrderContractWS contract;

	private LinkWS previousOrder;

	private LinkWS nextOrder;

	private LinkWS discount;

	private Long paymentPlanId;

	private Long discountId;

	private boolean activated;

	private Set<OrderListingWS> oneTimeOrders;

	private Set<OrderLineWS> orderLines;

	private Set<ParameterWS> parameters;

	private Set<CustomAttributeWS> customAttributes;

	private Set<HrefWS> links;

}

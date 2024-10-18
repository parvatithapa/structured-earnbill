package com.sapienter.jbilling.appdirect.subscription;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderContractRenewalWS implements Serializable {
	/**
	 * Link to the order
	 */
	private LinkWS order;

	/**
	 * Contract renewal payment plan
	 */
	private PaymentPlanWS paymentPlan;
}

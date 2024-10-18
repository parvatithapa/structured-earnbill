package com.sapienter.jbilling.appdirect.subscription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by rvaibhav on 19/12/17.
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderListingWS extends BaseOrderWS {

	private LinkWS paymentPlan;

}

package com.sapienter.jbilling.appdirect.subscription.productdetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDetailsWS {

	private String productIdentifier;

	private String consumerKey;

	private String consumerSecret;
}
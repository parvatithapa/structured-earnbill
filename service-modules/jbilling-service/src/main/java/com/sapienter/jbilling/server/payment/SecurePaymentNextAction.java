package com.sapienter.jbilling.server.payment;

import java.io.Serializable;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author amey.pelapkar
 *
 */
@Data
public class SecurePaymentNextAction implements Serializable {

	@ApiModelProperty(value = "The gateway referencekey can be used to complete a payment.", dataType = "java.lang.String")
	@JsonProperty("gatewayReferenceKey")
	private String gatewayReferenceKey;
	
	@ApiModelProperty(value = "The URL you must redirect your customer to in order to authenticate the payment.", dataType = "java.lang.String")
	@JsonProperty("redirectToUrl")
	private String redirectToUrl;
	
	@JsonCreator
	public SecurePaymentNextAction() {
		gatewayReferenceKey = null;
		redirectToUrl = null;
	}

	public SecurePaymentNextAction(String gatewayReferenceKey, String redirectToUrl) {
		this.gatewayReferenceKey = gatewayReferenceKey;
		this.redirectToUrl = redirectToUrl;
	}	
}
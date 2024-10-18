/**
 * 
 */
package com.sapienter.jbilling.server.payment;

import java.io.Serializable;

import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author amey.pelapkar
 *
 */
@ToString
public class SecurePaymentNextAction implements Serializable {

	private String gatewayReferenceKey;
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

	@ApiModelProperty(value = "The gateway referencekey can be used to complete a payment.", dataType = "java.lang.String")
	public String getGatewayReferenceKey() {
		return gatewayReferenceKey;
	}

	@JsonProperty("gatewayReferenceKey")
	public void setGatewayReferenceKey(String gatewayReferenceKey) {
		this.gatewayReferenceKey = gatewayReferenceKey;
	}

	@ApiModelProperty(value = "The URL you must redirect your customer to in order to authenticate the payment.", dataType = "java.lang.String")
	public String getRedirectToUrl() {
		return redirectToUrl;
	}
	
	@JsonProperty("redirectToUrl")
	public void setRedirectToUrl(String redirectToUrl) {
		this.redirectToUrl = redirectToUrl;
	}

}

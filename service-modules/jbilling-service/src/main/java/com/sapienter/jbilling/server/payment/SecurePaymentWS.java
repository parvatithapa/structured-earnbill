package com.sapienter.jbilling.server.payment;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;



/**
 * @author amey.pelapkar
 *
 */

@ApiModel(value = "SecurePaymentWS", description = "SecurePaymentWS, response object, it provides status of request and what need to be done next.")
@Data
@Builder
public final class SecurePaymentWS implements Serializable {
	
	@ApiModelProperty(value = "The user id." , dataType="java.lang.Integer")
	@JsonProperty(value = "userId")
	private Integer userId;
	
	@ApiModelProperty(value = "The BillingHub's reference Id." , dataType="java.lang.Integer")
	@JsonProperty(value = "billingHubRefId")
	private Integer billingHubRefId;
	
	@ApiModelProperty(value = "If present, this property tells you what actions you need to take in order for your customer to fulfill a payment using the provided source." , dataType="java.lang.Object")
	@JsonProperty(value = "nextAction")
	private SecurePaymentNextAction nextAction;
	
	@ApiModelProperty(value = "Status of this request, one of requires_payment_method, requires_confirmation, requires_action, processing, requires_capture, canceled, or succeeded" , dataType="java.lang.String")
	@JsonProperty(value = "status")
	private String status;
	
	@ApiModelProperty(value = "If present, this property tells you why request got failed." , dataType="ErrorWS")
	@JsonProperty(value = "error")
	private ErrorWS error;
	
	// Below constructor is required for StripeRestTest.java
	@JsonCreator
	public SecurePaymentWS(
			@JsonProperty(value = "userId") Integer userId,
			@JsonProperty(value = "billingHubRefId") Integer billingHubRefId,
			@JsonProperty(value = "nextAction") SecurePaymentNextAction nextAction,
			@JsonProperty(value = "status") String status, 
			@JsonProperty(value = "error") ErrorWS error) {
		this.userId = userId;
		this.billingHubRefId = billingHubRefId;
		this.nextAction = nextAction;
		this.status = status;
		this.error = error;
	}	
	
	@JsonIgnore
	public boolean isSucceeded(){
		return this.status!= null ? this.status.equals("succeeded") : false; 
	}
	
	@JsonIgnore
	public boolean isActionRequired(){
		return this.status!= null ? this.status.equals("requires_action") : false; 
	}
	
	@JsonIgnore
	public boolean isFailed(){
		return this.status!= null ? this.status.equals("failed") : false; 
	}
}

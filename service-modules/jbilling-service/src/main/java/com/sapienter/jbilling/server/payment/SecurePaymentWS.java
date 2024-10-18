/**
 * 
 */
package com.sapienter.jbilling.server.payment;

import java.io.Serializable;

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
@ToString
public final class SecurePaymentWS implements Serializable {
	
	private Integer userId;
	private Integer billingHubRefId;
	private SecurePaymentNextAction nextAction;
	private String status;
	private ErrorWS error;
	
	public SecurePaymentWS() {
	}
	
	@JsonCreator
	public SecurePaymentWS(
			@JsonProperty(value = "userId") Integer userId,
			@JsonProperty(value = "billingHubRefId") Integer billingHubRefId,
			@JsonProperty(value = "authenticationRequired") boolean authenticationRequired,
			@JsonProperty(value = "nextAction") SecurePaymentNextAction nextAction,
			@JsonProperty(value = "status") String status, 
			@JsonProperty(value = "error") ErrorWS error) {
		this.userId = userId;
		this.billingHubRefId = billingHubRefId;
		this.nextAction = nextAction;
		this.status = status;
		this.error = error;
	}
	
	@ApiModelProperty(value = "The user id." , dataType="java.lang.Integer")
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	@ApiModelProperty(value = "The BillingHub's reference Id." , dataType="java.lang.Integer")
	public Integer getBillingHubRefId() {
		return billingHubRefId;
	}
	public void setBillingHubRefId(Integer billingHubRefId) {
		this.billingHubRefId = billingHubRefId;
	}
	
	@ApiModelProperty(value = "If present, this property tells you what actions you need to take in order for your customer to fulfill a payment using the provided source." , dataType="java.lang.Object")
	public SecurePaymentNextAction getNextAction() {
		return nextAction;
	}
	public void setNextAction(SecurePaymentNextAction nextAction) {
		this.nextAction = nextAction;
	}
	
	@ApiModelProperty(value = "Status of this request, one of requires_payment_method, requires_confirmation, requires_action, processing, requires_capture, canceled, or succeeded" , dataType="java.lang.String")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@ApiModelProperty(value = "If present, this property tells you why request got failed." , dataType="ErrorWS")
	public ErrorWS getError() {
		return error;
	}
	public void setError(ErrorWS error) {
		this.error = error;
	}
	@JsonIgnore
	public boolean isSucceeded(){
		return status!= null ? status.equals("succeeded") : false; 
	}
	
	@JsonIgnore
	public boolean isActionRequired(){
		return status!= null ? status.equals("requires_action") : false; 
	}
	
	@JsonIgnore
	public boolean isFailed(){
		return status!= null ? status.equals("failed") : false; 
	}
}

package com.sapienter.jbilling.server.payment.tasks.stripe.dto;

import lombok.ToString;

import com.sapienter.jbilling.server.payment.SecurePaymentNextAction;


@ToString
public class StripeResult {

	private String stripeCustomerId;
    private String stripePaymentMethodId;
    private String stripeSetupIntentId;
    
    private String errorCode;
    private String errorMsg;
    private String stripePaymentIntentId;
    private String stripeRefundId;
    private String status;
    
    private SecurePaymentNextAction nextAction;
    
    public enum StripeIntentStatus {
    	REQUIRES_PAYMENT_METHOD("requires_payment_method"), 
    	REQUIRES_CONFIRMATION("requires_confirmation"),
    	REQUIRES_ACTION("requires_action"),
    	PROCESSING("processing"), 
    	REQUIRES_CAPTURE("requires_capture"),
    	CANCELED("canceled"),
    	SUCCEEDED("succeeded");
    	
    	private String value;

        private StripeIntentStatus(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
    
    
    public String getStripePaymentMethodId() {
        return stripePaymentMethodId;
    }

    public void setStripePaymentMethodId(String stripePaymentMethodId) {
        this.stripePaymentMethodId = stripePaymentMethodId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errMsg) {
        this.errorMsg = errMsg;
    }

	public String getStripeCustomerId() {
		return stripeCustomerId;
	}

	public void setStripeCustomerId(String stripeCustomerId) {
		this.stripeCustomerId = stripeCustomerId;
	}

	public String getStripePaymentIntentId() {
		return stripePaymentIntentId;
	}

	public void setStripePaymentIntentId(String stripePaymentIntentId) {
		this.stripePaymentIntentId = stripePaymentIntentId;
	}

	public String getStripeSetupIntentId() {
		return stripeSetupIntentId;
	}

	public void setStripeSetupIntentId(String stripeSetupIntentId) {
		this.stripeSetupIntentId = stripeSetupIntentId;
	}	
	
	public String getStripeRefundId() {
		return stripeRefundId;
	}

	public void setStripeRefundId(String stripeRefundId) {
		this.stripeRefundId = stripeRefundId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isSucceeded(){
		return status.equals(StripeIntentStatus.SUCCEEDED.toString());
	}
	
	public boolean isActionRequired(){
		return status.equals(StripeIntentStatus.REQUIRES_ACTION.toString());
	}
	
	public boolean isConfirmationRequired(){
		return status.equals(StripeIntentStatus.REQUIRES_CONFIRMATION.toString());
	}
	
	
	public boolean isCaptureRequired(){
		return status.equals(StripeIntentStatus.REQUIRES_CAPTURE.toString());
	}
	
	public boolean isCanceled(){
		return status.equals(StripeIntentStatus.CANCELED.toString());
	}

	public SecurePaymentNextAction getNextAction() {
		return nextAction;
	}

	public void setNextAction(SecurePaymentNextAction nextAction) {
		this.nextAction = nextAction;
	}
}

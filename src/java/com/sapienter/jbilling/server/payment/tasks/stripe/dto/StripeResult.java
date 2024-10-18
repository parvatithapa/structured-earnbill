package com.sapienter.jbilling.server.payment.tasks.stripe.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.sapienter.jbilling.server.payment.SecurePaymentNextAction;


@Data
@NoArgsConstructor
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
    	SUCCEEDED("succeeded"),
    	DUPLICATE_REQUEST("duplicate_request");
    	
    	private String value;

        private StripeIntentStatus(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
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
}
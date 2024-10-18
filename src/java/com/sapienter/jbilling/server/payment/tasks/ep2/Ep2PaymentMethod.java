package com.sapienter.jbilling.server.payment.tasks.ep2;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
public enum Ep2PaymentMethod {

	CREDIT_CARD {

		@Override
		public String getPaymentMethodName() {
			return "creditcard";
		}
		 
	}, PAYPAL {

		@Override
		public String getPaymentMethodName() {
			return "paypal";
		}
		
	};
	
	public abstract String getPaymentMethodName();
}

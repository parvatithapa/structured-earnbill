/**
 * 
 */
package com.sapienter.jbilling.server.payment.tasks.stripe;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.payment.SecurePaymentNextAction;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.CreditCard;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.Payer;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.Payment;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.StripeResult;
import com.sapienter.jbilling.server.payment.tasks.stripe.util.StripeHelper;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Refund;
import com.stripe.model.SetupIntent;
import com.stripe.model.SetupIntent.NextAction;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentCreateParams.CaptureMethod;
import com.stripe.param.PaymentIntentCreateParams.ConfirmationMethod;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.PaymentMethodCreateParams.CardDetails;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.SetupIntentCreateParams;



/**
 * @Package: com.sapienter.jbilling.server.payment.tasks
 * @author amey.pelapkar
 *
 */
public final class StripeService {
	
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles
			.lookup().lookupClass());
	
	private String apiKey ;
	private static final String PAYMENT_INTENT_DESCRIPTION = "payment created by BillingHub.";
	private static final String LOG_MESSAGE_STRIPE_CREATE_PAYMENT = "Creating a payment for stripe customer {} using payment method id {}.";
	private static final String LOG_MESSAGE_STRIPE_SIGN_UP="{}, the customer signs up with stripe ";
	 
	
	
	public StripeService(final String apiKey) {
		this. apiKey = apiKey;
	}
	
	/*
	 * For multi-threaded environment apiKey should be set through request option
	*/
	private RequestOptions getRequestOptions(){
		return RequestOptions.builder().setApiKey(this.apiKey).build();
	}
	
	/** To create a stripe customer
	 * @param payer
	 * @return
	 * @throws StripeException
	 */
	public Customer createCustomer(Payer payer, String paymentMethodId) throws StripeException {
		logger.debug("Creating a stripe customer for user {}" , payer.getEmail());
		//Create shipping address
		
		CustomerCreateParams.Address address = CustomerCreateParams.Address
				.builder()
				.setLine1(payer.getStreet())
				.setLine2(payer.getStreet2())
				.setCity(payer.getCity())
				.setPostalCode(payer.getZip())
				.setCountry(payer.getCountryCode())
				.build();
		
		CustomerCreateParams.Shipping.Address shippingAddress = CustomerCreateParams.Shipping.Address
				.builder()
				.setLine1(payer.getStreet())
				.setLine2(payer.getStreet2())
				.setCity(payer.getCity())
				.setPostalCode(payer.getZip())
				.setCountry(payer.getCountryCode())
				.build();
		
		//set shipping address
		CustomerCreateParams.Shipping shipping = CustomerCreateParams.Shipping
				.builder()
				.setName("shipping-address")
				.setAddress(shippingAddress)
				.build();
		
        //create customer parameters
    	CustomerCreateParams customerCreateParams = CustomerCreateParams
	    		.builder()
	    		.setEmail(payer.getEmail())
	    		.setName(payer.getFirstName())
	    		.setMetadata(Collections.singletonMap("user_id", payer.getId().toString()))
	    		.setPaymentMethod(paymentMethodId)
	    		.setAddress(address)
	    		.setShipping(shipping)
	    		.build();
    	
    	// creating customer
    	return Customer.create(customerCreateParams,this.getRequestOptions());
		 		
	}
	
	/** Creates Stripe payment method. 
	 * @param paymentMethodCreateParams 
	 * @param params
	 * @return com.stripe.model.PaymentMethod;
	 * @throws StripeException
	 */
	private PaymentMethod createPaymentMethod(CreditCard creditCard) throws StripeException{
		
		logger.info("Stripe, creating a payment method");
		
		Calendar expiryDate = Calendar.getInstance();
		expiryDate.setTime(creditCard.getExpirationDate());
		
		CardDetails stripeCardDetails = CardDetails
	    		.builder()
	    		.setNumber(new String(creditCard.getAccount()))
	    		.setExpMonth(Long.valueOf(expiryDate.get(Calendar.MONTH) + 1l))
	    		.setExpYear(Long.valueOf(expiryDate.get(Calendar.YEAR)))
	    		.build();
	    
	    PaymentMethodCreateParams paymentMethodCreateParams = PaymentMethodCreateParams
	    		.builder()
	    		.setCard(stripeCardDetails)
	    		.setType(PaymentMethodCreateParams.Type.CARD)
	    		.build();
	    
	    return PaymentMethod.create(paymentMethodCreateParams, this.getRequestOptions());
	}
	
	
	/**The Setup Intents API lets you save a customer’s card without an initial payment. 
	 * This is helpful if you want to onboard customers now, set them up for payments, and charge them in the future—when they’re offline.
	 * 
	 * @param stripeCustomerId
	 * @param stripePaymentMethodId
	 * @return
	 * @throws StripeException
	 */
	public SetupIntent createSetupIntent(String stripeCustomerId, String stripePaymentMethodId, String returnURL) throws StripeException{
		logger.debug("Creating a setupintent to validate card. Stripe customer id {} , payment method id {}, retrun URL {}" ,stripeCustomerId ,  stripePaymentMethodId, returnURL);
		
		SetupIntentCreateParams params = SetupIntentCreateParams
				.builder()
				.setConfirm(true)
				.setCustomer(stripeCustomerId)
				.setPaymentMethod(stripePaymentMethodId)
				.setReturnUrl(returnURL)
				.build();
		
		return SetupIntent.create(params,this.getRequestOptions());		
	}
	
	/** This method helps to retrieve payment method id by setup intent id 
	 * @param setupIntentId
	 * @return
	 * @throws StripeException
	 */
	public SetupIntent retrieveSetupIntent(String setupIntentId) throws StripeException{
		return SetupIntent.retrieve(setupIntentId, getRequestOptions());
	}	

	/** Confirm payment intent
	 * @param paymentIntentId
	 * @return
	 * @throws StripeException
	 */
	private PaymentIntent confirmPayment(PaymentIntent paymentToBeConfirmed) throws StripeException{
		logger.debug("Confirming a payment for {}", paymentToBeConfirmed.getId());
		PaymentIntentConfirmParams params = PaymentIntentConfirmParams
				.builder()
				.setUseStripeSdk(true)
				.build();
		return paymentToBeConfirmed.confirm(params,this.getRequestOptions());
	}
	
	/** Capture payment intent
	 * @param paymentIntentId
	 * @return
	 * @throws StripeException
	 */
	private PaymentIntent capturePayment(PaymentIntent paymentIntent, long amountToCapture) throws StripeException{
		logger.debug("Cancel a payment for {}",  paymentIntent.getId());
		PaymentIntentCaptureParams params = PaymentIntentCaptureParams
				.builder()
				.setAmountToCapture(amountToCapture)
				.build();
		return paymentIntent.capture(params,this.getRequestOptions());
	}
	
	
	public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
		return PaymentIntent.retrieve(paymentIntentId,this.getRequestOptions());
	}

	public StripeResult refundTransaction(String paymentIntentId, long amount) throws StripeException{
		logger.debug("Initiating stripe refund for payment intent id  {}" ,paymentIntentId);
		
		StripeResult stripeResult = new StripeResult();
		stripeResult.setStripePaymentIntentId(paymentIntentId);
		
		RefundCreateParams params = RefundCreateParams
				.builder()
				.setAmount(amount)
				.setPaymentIntent(paymentIntentId)
				.build(); 
		
		Refund refund = Refund.create(params,this.getRequestOptions());
		stripeResult.setStatus(refund.getStatus());
		if(refund.getStatus().equals(StripeResult.StripeIntentStatus.SUCCEEDED.toString())){
    		stripeResult.setStripeRefundId(refund.getId());
		}else{
			stripeResult.setStripeRefundId(StripeHelper.isObjectEmpty(refund) ? StringUtils.EMPTY : refund.getId());
			stripeResult.setErrorCode(refund.getStatus());
    		stripeResult.setErrorMsg(refund.getStatus());
		}
		return stripeResult;
	}

	
	/**
	 * This method is called at very first time when customer is created WITH payment method.
	 * 
	 * @param paymentAction
	 * @param payer
	 * @param creditCard
	 * @return
	 * @throws StripeException
	 */
	public StripeResult doSignUp(Payer payer, CreditCard creditCard, String returnURL)
 throws StripeException {
		logger.debug(LOG_MESSAGE_STRIPE_SIGN_UP, payer.getEmail());

		StripeResult stripeResult = new StripeResult();

		PaymentMethod stripePaymentMethod = createPaymentMethod(creditCard);
		
		if (StripeHelper.isObjectEmpty(payer.getStripeCustomerId())) {
			Customer stripeCustomer = createCustomer(payer, stripePaymentMethod.getId());
			payer.setStripeCustomerId(stripeCustomer.getId());
		}
		
		SetupIntent setupIntent = createSetupIntent(payer.getStripeCustomerId(), stripePaymentMethod.getId(), returnURL);
		stripeResult.setStatus(setupIntent.getStatus());
		stripeResult.setStripeCustomerId(payer.getStripeCustomerId());
		stripeResult.setStripePaymentMethodId(stripePaymentMethod.getId());
		stripeResult.setStripeSetupIntentId(setupIntent.getId());
		
		if(stripeResult.isActionRequired()){
			SecurePaymentNextAction spNextAction = new SecurePaymentNextAction(setupIntent.getClientSecret(), null);
			
			NextAction nextAction = setupIntent.getNextAction();
			if(!StripeHelper.isObjectEmpty(nextAction) && nextAction.getType().equals(StripeHelper.NEXT_ACTION_TYPE_REDIRECT_TO_URL)){
				spNextAction.setRedirectToUrl(nextAction.getRedirectToUrl().getUrl());
			}
			stripeResult.setNextAction(spNextAction);
		}
		return stripeResult;
	}
	
	/**
	 * This method is called at very first time when customer is created WITH payment method.
	 * 
	 * @param paymentAction
	 * @param payer
	 * @param creditCard
	 * @return
	 * @throws StripeException
	 */
	public StripeResult processOnSessionPayment(Payer payer, CreditCard creditCard, Payment payment, String returnURL, boolean doCapture)
 throws StripeException {
		logger.debug(LOG_MESSAGE_STRIPE_SIGN_UP, payer.getEmail());
		PaymentMethod stripePaymentMethod = createPaymentMethod(creditCard);

		if (StripeHelper.isObjectEmpty(payer.getStripeCustomerId())) {
			Customer stripeCustomer = createCustomer(payer, stripePaymentMethod.getId());
			payer.setStripeCustomerId(stripeCustomer.getId());
		}
		
		PaymentIntentCreateParams params = PaymentIntentCreateParams
	    		.builder()
	    		.setAmount(payment.getAmount())
	    		.setCurrency(payment.getCurrencyCode())
	    		.setPaymentMethod(stripePaymentMethod.getId())	    		
	    		.setCustomer(payer.getStripeCustomerId())	  
	    		.setOffSession(false)
	    		.setConfirm(true)
	    		.setConfirmationMethod(ConfirmationMethod.MANUAL)// Server will confirm 3DS transaction manually after client completes user authorization and calls stripe.handleCardAction( client_secret )
	    		.setCaptureMethod(CaptureMethod.MANUAL)
	    		.setReturnUrl(returnURL)
	    		.setDescription("Online ".concat(PAYMENT_INTENT_DESCRIPTION))
	    		.build();
	    	
		PaymentIntent paymentIntent = PaymentIntent.create(params, this.getRequestOptions());
		
		return verifyAndCompleteThePayment(paymentIntent, null, payment.getAmount(), doCapture); // capturing amount later in case of 3DS auth required
	}
	
	
	/** Process recursive/off-session payment
	 *  
	 * @param stripeCustomerId
	 * @param gatewayKey
	 * @param payment
	 * @return
	 * @throws StripeException
	 */
	public StripeResult processOffSessionPayment(
			String stripeCustomerId, String gatewayKey, Payment payment, String returnUrl)
            throws StripeException {
		logger.debug(LOG_MESSAGE_STRIPE_CREATE_PAYMENT, stripeCustomerId,  gatewayKey);
		
		PaymentIntentCreateParams params = PaymentIntentCreateParams
	    		.builder()
	    		.setAmount(payment.getAmount())
	    		.setCurrency(payment.getCurrencyCode())
	    		.setPaymentMethod(gatewayKey)	    		
	    		.setCustomer(stripeCustomerId)	  
	    		.setOffSession(true)
	    		.setConfirm(true)
	    		.setConfirmationMethod(ConfirmationMethod.AUTOMATIC)// Manual - Server will confirm 3DS transaction manually after client completes user authorization and calls stripe.handleCardAction( client_secret )
	    		.setCaptureMethod(CaptureMethod.MANUAL) // capturing the amount later in case of 3DS auth required
	    		.setReturnUrl(returnUrl)
	    		.setDescription("Offline ".concat(PAYMENT_INTENT_DESCRIPTION))
	    		.build();
	    	
		PaymentIntent paymentToBeVerified = PaymentIntent.create(params, this.getRequestOptions());
		
		return verifyAndCompleteThePayment(paymentToBeVerified, null, payment.getAmount(), true);     // capturing amount later in case of 3DS auth required    
    }
	
	/** To confirm the payment(intent) that needs confirmation post 3DS authentication 
	 * @param paymentIntentId
	 * @return
	 * @throws StripeException
	 */
	public StripeResult doCapturePayment(String paymentIntentId, long amountToCapture)
            throws StripeException {
		logger.debug("Confirming stripe payment intent id {}", paymentIntentId);
		return this.verifyAndCompleteThePayment(null, paymentIntentId, amountToCapture, true);
    }
	
	
	/** Retrieve payment method
	 * @param paymentMethodId
	 * @return
	 * @throws StripeException
	 */
	public PaymentMethod retrievePaymentMethod(String paymentMethodId) throws StripeException{
		return PaymentMethod.retrieve(paymentMethodId,this.getRequestOptions());
	}

	/**
	 * verifyAndCompleteThePayment, This method can be called to verify pre-auth payment or to capture normal payment
	 * @param paymentToBeVerified
	 * @param stripePaymentIntentId
	 * @param isPreAuthPayment
	 * @param amountToCapture
	 * @return
	 * @throws StripeException
	 */
	public StripeResult verifyAndCompleteThePayment(PaymentIntent paymentToBeVerified, String stripePaymentIntentId, long amountToCapture, boolean doCapture) throws StripeException{
		StripeResult  stripeResult = new StripeResult();
		
		if(!StripeHelper.isObjectEmpty(stripePaymentIntentId)){
			paymentToBeVerified = this.retrievePaymentIntent(stripePaymentIntentId);
		}
		
		// This would require to attach payment method with customer in stripe
		stripeResult.setStatus(paymentToBeVerified.getStatus());
		stripeResult.setStripeCustomerId(paymentToBeVerified.getCustomer());
		stripeResult.setStripePaymentMethodId(paymentToBeVerified.getPaymentMethod());
		stripeResult.setStripePaymentIntentId(paymentToBeVerified.getId());
		
		if(stripeResult.isActionRequired()){
			SecurePaymentNextAction spNextAction = new SecurePaymentNextAction(paymentToBeVerified.getClientSecret(), null);
			com.stripe.model.PaymentIntent.NextAction nextAction = paymentToBeVerified.getNextAction();
			
			if(!StripeHelper.isObjectEmpty(nextAction) && nextAction.getType().equals(StripeHelper.NEXT_ACTION_TYPE_REDIRECT_TO_URL)){
				spNextAction.setRedirectToUrl(nextAction.getRedirectToUrl().getUrl());
			}
			stripeResult.setNextAction(spNextAction);
		}
		
		if(stripeResult.isConfirmationRequired()){
			paymentToBeVerified = confirmPayment(paymentToBeVerified);
			stripeResult.setStatus(paymentToBeVerified.getStatus());
		}
		
		if(stripeResult.isCaptureRequired() && doCapture){
			paymentToBeVerified = capturePayment(paymentToBeVerified, amountToCapture);// Stripe status should be "succeeded"
		}
		// status need to set after checking above condition
		stripeResult.setStatus(paymentToBeVerified.getStatus());
		return stripeResult;
	}
}

/**
 * 
 */
package com.sapienter.jbilling.server.payment.tasks;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.ErrorWS;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.ISecurePayment;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.SecurePaymentNextAction;
import com.sapienter.jbilling.server.payment.SecurePaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.CreditCard;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.Payer;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.Payment;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.StripeResult;
import com.sapienter.jbilling.server.payment.tasks.stripe.util.StripeHelper;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.model.SetupIntent.NextAction;

/**
 * @Package: com.sapienter.jbilling.server.payment.tasks 
 * @author: Amey Pelapkar   
 * @date: 12-Apr-2021 6:37:53 pm
 *
 */

public class PaymentStripeTask extends PaymentTaskWithTimeout implements
		IExternalCreditCardStorage, ISecurePayment {

	
	private static final String PROCESSOR_NAME = "Stripe";
	
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles
			.lookup().lookupClass());
	private static final PaymentInformationBL piBl = new PaymentInformationBL();
	
	private static final String META_FIELD_CC_GATEWAY_KEY = "cc.gateway.key";
	private static final String META_FIELD_CC_STRIPE_INTENT_ID = "cc.stripe.intent.id";
    public static final String RESULT_STRIPE_INVALID_CUSTOMER_ID = "Invalid stripe customer id.";
    
	/* Plug-in parameters */
    
    public static final ParameterDescription PARAMETER_CONTACT_AIT_ID = new ParameterDescription("Customer contact section id(AIT ID)", true, ParameterDescription.Type.INT);
	
	public static final ParameterDescription PARAMETER_CUSTOMER_METAFIELD_ID = new ParameterDescription("Customer metafield id", true, ParameterDescription.Type.INT);
	
	public static final ParameterDescription PARAMETER_PAYMENT_METHOD_TYPE_ID = new ParameterDescription("Payment method type id", true, ParameterDescription.Type.INT);
	
	public static final ParameterDescription PARAMETER_PAYMENT_METAFILED_ID = new ParameterDescription("Payment metafield id", true, ParameterDescription.Type.INT);
	
	public static final ParameterDescription PARAMETER_STRIPE_API_KEY = new ParameterDescription("Stripe API key", true, ParameterDescription.Type.STR, true);
	
	public static final ParameterDescription PARAMETER_RETURN_URL= new ParameterDescription("URL, Redirect customer post authentication", false, ParameterDescription.Type.STR);
	
	private Integer contactAccInfoTypeId;
	private Integer stripeCustomertMetafieldId;
	private Integer stripePaymentMetafieldId;
	private Integer paymentMethodTypeId;
	private String stripeApiKey;
	private String returnURL;
	
	public PaymentStripeTask() {
		// initialize plug-in parameters
		descriptions.add(PARAMETER_STRIPE_API_KEY);
		descriptions.add(PARAMETER_PAYMENT_METHOD_TYPE_ID);
		descriptions.add(PARAMETER_CONTACT_AIT_ID);
		descriptions.add(PARAMETER_CUSTOMER_METAFIELD_ID);
		descriptions.add(PARAMETER_PAYMENT_METAFILED_ID);
		descriptions.add(PARAMETER_RETURN_URL);
	}
	
	/** Strong Customer Authentication(SCA) - Customer should authenticate transaction against 3D secure authentication.
	 * Transactions that required customer authentication 
	 * 		Process one time payment
	 * 		Store card for future payment
	 * 		Card to be used for recurrence payment 
	 * @param paymentInstrument
	 * @return SecurePaymentWS
	 * @throws PluggableTaskException
	 */
	@Override
	public SecurePaymentWS perform3DSecurityCheck(PaymentInformationDTO paymentInstrument, PaymentDTOEx paymentDTOEx) throws PluggableTaskException{
		SecurePaymentWS securePaymentWS =  null;
		Integer userId = null;
		try  {
			if(!StripeHelper.isObjectEmpty(paymentInstrument)){
				userId = paymentInstrument.getUser().getId();
				securePaymentWS = handleSetupIntent(paymentInstrument);
			}
			else if(!StripeHelper.isObjectEmpty(paymentDTOEx )) {
				userId = paymentDTOEx.getUserId();
				securePaymentWS = handlePaymentIntent(paymentDTOEx);
			}
		}
		catch (StripeException stripeExp) {
			logger.error("perform3DSecurityCheck:StripeException ", stripeExp);
			securePaymentWS = StripeHelper.converToSecurePaymentWS(userId, null, stripeExp);
		} catch (PluggableTaskException plgTaskExp) {
			logger.error("perform3DSecurityCheck:Exception ", plgTaskExp);
			securePaymentWS = StripeHelper.converToSecurePaymentWS(userId, null, plgTaskExp);
		} catch (Exception exp) {
			logger.error("perform3DSecurityCheck:Exception ", exp);
			securePaymentWS = StripeHelper.converToSecurePaymentWS(userId, null, exp );
		}
		return securePaymentWS;
	}

	/**
	 * @param paymentInstrument
	 * @param securePaymentWS
	 * @return
	 * @throws Exception 
	 */
	private SecurePaymentWS handleSetupIntent(PaymentInformationDTO paymentInstrument	) throws Exception {
		SecurePaymentWS securePaymentWS = null;
		
		try (CreditCard creditCard = convertCreditCard(paymentInstrument)) {
				String setupIntentId =   getPaymentInformationMetafieldValue(paymentInstrument.getMetaFields(), META_FIELD_CC_STRIPE_INTENT_ID);
				
				if(!StripeHelper.isObjectEmpty(setupIntentId)){
					securePaymentWS = verifySetupIntentStatus(paymentInstrument, setupIntentId);
				}else{
					UserDTO userDTO = StripeHelper.getUser(null, paymentInstrument, null, 0);
					
					Payer payer = StripeHelper.getPayerInformation(getContactAITid(),  getStripeCustomerMetafield(), null, userDTO);
					
					StripeResult stripeResult = StripeHelper.getStripeServiceInstance(getStripeApiKey()).doSignUp(payer, creditCard, getReturnUrl());
					
					//Save stripe customer Id - Customer is still in session hence just set the value
					if(!StripeHelper.isObjectEmpty(stripeResult.getStripeCustomerId())){
						StripeHelper.setCustomerMetafieldValueByMetafieldId(getStripeCustomerMetafield(), userDTO.getCustomer(), stripeResult.getStripeCustomerId());
					}
					if(stripeResult.isSucceeded()){
						updatePaymentInfoWithGatewayKey(paymentInstrument, stripeResult.getStripePaymentMethodId());
					}
					securePaymentWS = StripeHelper.converToSecurePaymentWS(paymentInstrument.getUser().getId(), stripeResult, null);
				}
		}
		return securePaymentWS;
	}
	
	/**
	 * @param paymentDTOEx
	 * @return SecurePaymentWS
	 * @throws Exception
	 */
	private SecurePaymentWS handlePaymentIntent(PaymentDTOEx paymentDTOEx) throws Exception {
		SecurePaymentWS securePaymentWS = null;
		
		String paymentIntentId = getPaymentMetafieldValue(paymentDTOEx, getMetafiledNameById(getStripePaymentMetafieldId()));
		PaymentInformationDTO instrument = null;
		
		if(!StripeHelper.isObjectEmpty(paymentDTOEx.getPaymentInstruments()) && paymentDTOEx.getPaymentInstruments().size()==1){
			instrument = paymentDTOEx.getPaymentInstruments().stream().findFirst().get();
		}
		
		if (!StripeHelper.isObjectEmpty(paymentIntentId)) {
			securePaymentWS =  verifyPaymentIntentStatus(paymentDTOEx, paymentIntentId);
		} else if (!StripeHelper.isObjectEmpty(instrument) && !useGatewayKey(instrument)) {
			StripeResult stripeResult = null;
			try (CreditCard creditCard = convertCreditCard(instrument)) {
				stripeResult = processOneTimePayment(paymentDTOEx, creditCard, false);
				// If authentication is not reuired then set metafield value with stripe payment intent id 
				if(stripeResult.isSucceeded() || stripeResult.isConfirmationRequired() || stripeResult.isCaptureRequired()){
					setPaymentMetafieldValue(paymentDTOEx, getMetafiledNameById(getStripePaymentMetafieldId()), stripeResult.getStripePaymentIntentId());
				}
				securePaymentWS = StripeHelper.converToSecurePaymentWS(paymentDTOEx.getUserId(), stripeResult, null);
			}
		}
		return securePaymentWS;
	}
	
	
	private boolean useGatewayKey(PaymentInformationDTO instrument) {
		return ArrayUtils.isNotEmpty(piBl.getCharMetaFieldByType(instrument, MetaFieldType.GATEWAY_KEY));
	}

	private SecurePaymentWS verifySetupIntentStatus(PaymentInformationDTO piDTO, String setupIntentId) throws StripeException, PluggableTaskException, ParseException {
		SecurePaymentWS securePaymentWS = new SecurePaymentWS();
		securePaymentWS.setUserId(piDTO.getUser().getId());
		SetupIntent setupIntent =  StripeHelper.getStripeServiceInstance(getStripeApiKey()).retrieveSetupIntent(setupIntentId);
		securePaymentWS.setStatus(setupIntent.getStatus());
		
		if(setupIntent.getStatus().equals(StripeResult.StripeIntentStatus.REQUIRES_ACTION.toString())){
			SecurePaymentNextAction spNextAction = new SecurePaymentNextAction();
			spNextAction.setGatewayReferenceKey(setupIntent.getClientSecret());
			
			NextAction nextAction = setupIntent.getNextAction();
			if(!StripeHelper.isObjectEmpty(nextAction) && nextAction.getType().equals(StripeHelper.NEXT_ACTION_TYPE_REDIRECT_TO_URL)){
				spNextAction.setRedirectToUrl(nextAction.getRedirectToUrl().getUrl());
			}
			securePaymentWS.setNextAction(spNextAction);
		}else if(setupIntent.getStatus().equals(StripeResult.StripeIntentStatus.SUCCEEDED.toString())){
			UserDTO userDTO = StripeHelper.getUser(null, piDTO, null, 0);
			StripeHelper.populatePaymentInfoDtoWithStripeCard(getStripeApiKey() ,piDTO, userDTO, setupIntent.getPaymentMethod(), getStripePaymentMethodId() ,true);
			securePaymentWS.setStatus(StripeResult.StripeIntentStatus.SUCCEEDED.toString());
		}
	
		return securePaymentWS;
	}
	
	
	private SecurePaymentWS verifyPaymentIntentStatus(PaymentDTOEx paymentDTOEx, String paymentIntentId) throws StripeException, PluggableTaskException, ParseException {
		SecurePaymentWS securePaymentWS = new SecurePaymentWS();
		securePaymentWS.setUserId(paymentDTOEx.getUserId());
		PaymentIntent  paymentIntent =  StripeHelper.getStripeServiceInstance(getStripeApiKey()).retrievePaymentIntent(paymentIntentId);
		securePaymentWS.setStatus(paymentIntent.getStatus());
		
		/*
		 * Verifying the amount to be captured 
		*/
		if (StripeHelper.convertDollarAmountToCents(paymentDTOEx.getAmount()) > paymentIntent.getAmount()){
			securePaymentWS.setStatus("failed");
			securePaymentWS.setError(new ErrorWS("amount_too_large", "The payment could not be captured because the requested capture amount is greater than the authorized amount."));
			return securePaymentWS;
		}
		
		if(paymentIntent.getStatus().equals(StripeResult.StripeIntentStatus.REQUIRES_ACTION.toString())){
			SecurePaymentNextAction spNextAction = new SecurePaymentNextAction();
			spNextAction.setGatewayReferenceKey(paymentIntent.getClientSecret());
			
			com.stripe.model.PaymentIntent.NextAction nextAction = paymentIntent.getNextAction();
			if(!StripeHelper.isObjectEmpty(nextAction) && nextAction.getType().equals(StripeHelper.NEXT_ACTION_TYPE_REDIRECT_TO_URL)){
				spNextAction.setRedirectToUrl(nextAction.getRedirectToUrl().getUrl());
			}
			securePaymentWS.setNextAction(spNextAction);
		}else if(paymentIntent.getStatus().equals(StripeResult.StripeIntentStatus.REQUIRES_CONFIRMATION.toString())   ||   paymentIntent.getStatus().equals(StripeResult.StripeIntentStatus.REQUIRES_CAPTURE.toString())){
			securePaymentWS.setStatus(StripeResult.StripeIntentStatus.SUCCEEDED.toString());
			UserDTO userDTO = StripeHelper.getUser(null, null, paymentDTOEx, 0);
			
			MetaField metafield = MetaFieldBL.getFieldByName(userDTO.getEntity().getId(), new EntityType[] {EntityType.PAYMENT}  , getMetafiledNameById(getStripePaymentMetafieldId()));
			paymentDTOEx.setMetaField(metafield, paymentIntent.getId());
			
			if(StripeHelper.isObjectEmpty(paymentDTOEx.getInstrument())){
				List<PaymentInformationDTO> tempPiDto = new ArrayList<PaymentInformationDTO>();
				tempPiDto.add(StripeHelper.populatePaymentInfoDtoWithStripeCard(getStripeApiKey(),null, userDTO, paymentIntent.getPaymentMethod(), getStripePaymentMethodId() ,true));
				paymentDTOEx.setPaymentInstruments(tempPiDto);
			}
		}
	
		return securePaymentWS;
	}

	/** Update payment information with gateway key
	 * @param paymentInstrument
	 * @param stripeCCIntentId
	 * @throws StripeException
	 * @throws PluggableTaskException
	 */
	private void updatePaymentInfoWithGatewayKey(
			PaymentInformationDTO paymentInstrument, String gatewayKey) {
		MetaField metafieldGatewayKey = MetaFieldBL.getFieldByName(paymentInstrument.getUser().getEntity().getId(), new EntityType[] {EntityType.PAYMENT_METHOD_TYPE}  , META_FIELD_CC_GATEWAY_KEY);
		paymentInstrument.setMetaField(metafieldGatewayKey, gatewayKey.toCharArray());
	}

	

	/* (non-Javadoc)
	 * @see com.sapienter.jbilling.server.payment.IExternalCreditCardStorage#storeCreditCard(com.sapienter.jbilling.server.user.contact.db.ContactDTO, com.sapienter.jbilling.server.payment.db.PaymentInformationDTO)
	 */
	@Override
	public String storeCreditCard(ContactDTO contact,
			PaymentInformationDTO paymentInstrument) {
		
		logger.debug("{} payment gateway, store credit card",PROCESSOR_NAME );

		// new contact that has not had a credit card created yet
		if (StripeHelper.isObjectEmpty(paymentInstrument)) {
			logger.warn("No credit card to store externally.");
			return null;
		} else {        	
			UserDTO user = StripeHelper.getUser(contact, paymentInstrument, null, 0);

			if (StripeHelper.isObjectEmpty(user) ) {
				logger.error("Could not determine user id for external credit card storage");
				return null;
			}
			
			/*
			 * Handling rest API call where stripe payment method id is already exist
			*/
			String ccGateWaykey =   getPaymentInformationMetafieldValue(paymentInstrument.getMetaFields(), META_FIELD_CC_GATEWAY_KEY);			
			if(!StripeHelper.isObjectEmpty(ccGateWaykey)){
				logger.info("Rest api call to add payment instrument/credit card is ignored as stripe payment method id is already created by portal.");
				return ccGateWaykey;
			}
			
			try {
				
				SecurePaymentWS securePaymentWS =  perform3DSecurityCheck(paymentInstrument,null);
				
				if(securePaymentWS.isSucceeded()){
					return  String.valueOf(piBl.getCharMetaFieldByType(paymentInstrument, MetaFieldType.GATEWAY_KEY));
				}
				logger.error("Could not save credit card due to {}", securePaymentWS );
				return null;

			} catch (PluggableTaskException e) {
				logger.error("Could not save credit card due to", e);
	            return null;
			}

		}
	}

	@Override
	public boolean process(PaymentDTOEx paymentDTOEX) throws PluggableTaskException {
		logger.debug("Payment processing for {}",PROCESSOR_NAME);
		
        if(!isApplicable(paymentDTOEX)) {
            return NOT_APPLICABLE.shouldCallOtherProcessors();
        }
        return doProcess(paymentDTOEX);
	}

	@Override
	public void failure(Integer userId, Integer retry) {
		logger.debug("-------failure-----");
	}

	@Override
	public boolean preAuth(PaymentDTOEx paymentInfo)
			throws PluggableTaskException {
		logger.debug("-------preAuth-------");
		return false;
	}

	@Override
	public boolean confirmPreAuth(PaymentAuthorizationDTO auth,
			PaymentDTOEx paymentInfo) throws PluggableTaskException {
		logger.debug("-------confirmPreAuth---------");
		return false;
	}	
	
	
	
	/** Extract the metafield valuefrom payment information metafield
	 * @param metaFieldValues
	 * @param metafieldName
	 * @return
	 */
	private String getPaymentInformationMetafieldValue(
			List<MetaFieldValue> metaFieldValues, String metafieldName) {
		// List stream api does not support object of MetaFieldValue to filter the record
		for (MetaFieldValue value : metaFieldValues) {
			if(value.getFieldName().equals(metafieldName)){
				if(!StripeHelper.isObjectEmpty(value.getValue())){
					
					if(value.getValue() instanceof char[]){
						return new String((char[])value.getValue());
					} else if(value.getValue() instanceof String){
						return (String)value.getValue();
					} else if(value.getValue() instanceof Integer){
						return Integer.getInteger(value.getValue().toString()).toString();
					}
				}
				return null;
			}
		}
		return null;
	}	

	/* (non-Javadoc)
	 * @see com.sapienter.jbilling.server.payment.IExternalCreditCardStorage#deleteCreditCard(com.sapienter.jbilling.server.user.contact.db.ContactDTO, com.sapienter.jbilling.server.payment.db.PaymentInformationDTO)
	 */
	@Override
	public char[] deleteCreditCard(ContactDTO contact,
			PaymentInformationDTO instrument) {
		return new char[0];
	}


    /**
     * Utility method to check if a given {@link PaymentDTOEx} payment can be processed
     * by this task.
     *
     * @param payment payment to check
     * @return true if payment can be processed with this task, false if not
     */
    private static boolean isApplicable(PaymentDTOEx payment) {
        logger.debug("Is this payment {} applicable ", payment);
        if (piBl.isBTPayment(payment.getInstrument())) {
            return false;
        }
        if (piBl.isCreditCard(payment.getInstrument())) {
            return true;
        }
        logger.warn("Can't process if Express checkout payment method or without a credit card or ach");
        return false;
    }
    
    
	private boolean doProcess(PaymentDTOEx paymentDTOEX) throws PluggableTaskException {
		
		if(isRefund(paymentDTOEX)) {
            return doRefund(paymentDTOEX).shouldCallOtherProcessors();
        }

        if(isCreditCardStored(paymentDTOEX)) {
            return doPaymentWithStoredCreditCard(paymentDTOEX)
                    .shouldCallOtherProcessors();
        }

        return doPaymentWithoutStoredCreditCard(paymentDTOEX)
                .shouldCallOtherProcessors();
		
	}
	
	private Result doRefund(PaymentDTOEx payment) throws PluggableTaskException {
		StripeResult stripeResult = new StripeResult();

		try {
			// Retrieving original payment and it's payment intent id
			PaymentAuthorizationDTO originalPayAuth = getOriginalPaymentAuthorization(payment);

			String paymentIntentId = null != originalPayAuth ? originalPayAuth
					.getTransactionId() : null;

			if (StripeHelper.isObjectEmpty(paymentIntentId)) {
				throw new PluggableTaskException(
						"Transaction id i.e. payment intent id for original payment is  NULL.");
			}

			stripeResult = StripeHelper.getStripeServiceInstance(
					getStripeApiKey())
					.refundTransaction(
							paymentIntentId,
							StripeHelper.convertDollarAmountToCents(payment
									.getAmount()));

		} catch (StripeException stripeExp) {
			logger.error("Couldn't handle refund request due to error {}",
					stripeExp);
			stripeResult.setErrorCode(stripeExp.getCode());
			stripeResult.setErrorMsg(stripeExp.getMessage());
		} catch (Exception exception) {
			logger.debug("doRefund() {}", exception);
			payment.setPaymentResult(new PaymentResultDAS()
					.find(CommonConstants.RESULT_UNAVAILABLE));
			return NOT_APPLICABLE;
		}

		PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(stripeResult);
		storeStripeResult(piBl, stripeResult, payment,
				paymentAuthorization);

		return new Result(paymentAuthorization, false);
	}
	
	private static PaymentAuthorizationDTO getOriginalPaymentAuthorization(PaymentDTOEx payment) {
		logger.debug("Refund, getOriginalPaymentAuthorization, payment id '{}'", payment.getPayment().getId());
		Set<PaymentAuthorizationDTO>  paymentAuths  = new PaymentDAS().findNow(payment.getPayment().getId()).getPaymentAuthorizations();
		for (PaymentAuthorizationDTO paymentAuthorizationDTO : paymentAuths) {			
			if( StripeHelper.isObjectEmpty(paymentAuthorizationDTO.getResponseMessage()) ||  paymentAuthorizationDTO.getResponseMessage().isEmpty()){
				return paymentAuthorizationDTO;
			}
		}		
		return null;
	}
	
	private static boolean isRefund(PaymentDTOEx payment) {
        return BigDecimal.ZERO.compareTo(payment.getAmount()) > 0 || payment.getIsRefund() != 0;
    }

    private static boolean isCreditCardStored(PaymentDTOEx paymentDTOEx) {
        return piBl.useGatewayKey(paymentDTOEx.getInstrument());
    }

    
    
    private Result doPaymentWithStoredCreditCard(PaymentDTOEx paymentDTOEx) throws PluggableTaskException {
		
    	StripeResult stripeResult = new StripeResult();

		try {
				String stripePaymentIntentId =   getPaymentMetafieldValue(paymentDTOEx, getMetafiledNameById(getStripePaymentMetafieldId()));
				
				if (!StripeHelper.isObjectEmpty(stripePaymentIntentId)) {
					/*
					 * Some cards always require 3D secure authentication even card is authenticated while adding a payment instrument (attaching to customer profile) 
					*/
					stripeResult = StripeHelper.getStripeServiceInstance(getStripeApiKey())
							.doCapturePayment(stripePaymentIntentId, StripeHelper.convertDollarAmountToCents(paymentDTOEx.getAmount())
							);
				} else {				
					String gatewaKey =  String.valueOf(piBl.getCharMetaFieldByType(paymentDTOEx.getInstrument(), MetaFieldType.GATEWAY_KEY));
					
					Payment payment =  convertDTOToPayment(paymentDTOEx);
					
					String stripeCustomerId = StripeHelper.getStripeCustomerId(getStripeCustomerMetafield(), StripeHelper.getUser(null, null, paymentDTOEx, 0));
					
					stripeResult = StripeHelper.getStripeServiceInstance(getStripeApiKey())
							.processOffSessionPayment(stripeCustomerId, gatewaKey, payment, getReturnUrl());
				}
	
				// if authentication (3DS) required then don't create either payment or payment
				if (stripeResult.isActionRequired()) {
					paymentDTOEx.setAuthenticationRequired(true);
					return new Result(null, false);
				}	

		} catch (com.stripe.exception.CardException cardExp){
			stripeResult.setStatus("failed");
			stripeResult.setErrorCode(cardExp.getCode());
			stripeResult.setErrorMsg(cardExp.getMessage());
			
			if(!StripeHelper.isObjectEmpty(cardExp.getCode()) && cardExp.getCode().equals("authentication_required")){
				paymentDTOEx.setAuthenticationRequired(true);
				return new Result(null, false);
			}else if(!StripeHelper.isObjectEmpty(cardExp.getCode()) && cardExp.getCode().equals("card_declined")){
				return new Result(null, false);
			}
		}catch (StripeException stripeExp) {
			logger.error("Couldn't handle payment request due to error {}", stripeExp);
			stripeResult.setStatus("failed");
			stripeResult.setErrorCode(stripeExp.getCode());
			stripeResult.setErrorMsg(stripeExp.getMessage());
		} catch (Exception exception) {
			logger.error("Couldn't handle payment request due to error {}", exception);
			
			paymentDTOEx.setPaymentResult(new PaymentResultDAS()
					.find(CommonConstants.RESULT_UNAVAILABLE));
			return NOT_APPLICABLE;
		}

		logger.debug("doPaymentWithStoredCreditCard(), StripeResult : {}", stripeResult);
		
		PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(stripeResult);
		storeStripeResult(piBl, stripeResult, paymentDTOEx, paymentAuthorization);

		return new Result(paymentAuthorization, false);
	}
    
    
	private Result doPaymentWithoutStoredCreditCard(PaymentDTOEx paymentDTOEx) throws PluggableTaskException {
		
		StripeResult stripeResult = new StripeResult();

		try (CreditCard creditCard = convertCreditCard(paymentDTOEx.getInstrument())) {

			stripeResult = processOneTimePayment(paymentDTOEx, creditCard, true);
			
			// if authentication (3DS) required then don't create either payment or payment
			if (stripeResult.isActionRequired()) {
				paymentDTOEx.setAuthenticationRequired(true);
				return new Result(null, false);
			}
			
		} catch (com.stripe.exception.CardException cardExp){
			stripeResult.setStatus("failed");
			stripeResult.setErrorCode(cardExp.getCode());
			stripeResult.setErrorMsg(cardExp.getMessage());
			
			if(!StripeHelper.isObjectEmpty(cardExp.getCode()) && cardExp.getCode().equals("authentication_required")){
				paymentDTOEx.setAuthenticationRequired(true);
				return new Result(null, false);
			}else if(!StripeHelper.isObjectEmpty(cardExp.getCode()) && cardExp.getCode().equals("card_declined")){
				return new Result(null, false);
			}
		}catch (StripeException stripeExp) {
			logger.error("Couldn't handle payment request due to error {}", stripeExp);
			stripeResult.setStatus("failed");
			stripeResult.setErrorCode(stripeExp.getCode());
			stripeResult.setErrorMsg(stripeExp.getMessage());
		} catch (Exception exception) {
			logger.error("Couldn't handle payment request due to error {}", exception);
			
			paymentDTOEx.setPaymentResult(new PaymentResultDAS()
					.find(CommonConstants.RESULT_UNAVAILABLE));
			return NOT_APPLICABLE;
		}
		// Creating and storing payment authorization
		PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(stripeResult);
		storeStripeResult(piBl, stripeResult, paymentDTOEx, paymentAuthorization);

		return new Result(paymentAuthorization, false);
	}

	/**
	 * @param paymentDTOEx
	 * @param stripeResult
	 * @param creditCard
	 * @return
	 * @throws PluggableTaskException
	 * @throws StripeException
	 */
	private StripeResult processOneTimePayment(PaymentDTOEx paymentDTOEx, CreditCard creditCard, boolean doCapture)
			throws PluggableTaskException, StripeException {
		
		StripeResult stripeResult = new StripeResult();
		
		// Metafield at customer level must be configured with plug-in to
		// store stripeCustomerId
		
		Payer payer = StripeHelper.getPayerInformation(getContactAITid(), getStripeCustomerMetafield(), paymentDTOEx, null);	

		String exsitingStripeCustomerId = payer.getStripeCustomerId();	

		String stripePaymentIntentId =   getPaymentMetafieldValue(paymentDTOEx, getMetafiledNameById(getStripePaymentMetafieldId()));
		if (!StripeHelper.isObjectEmpty(stripePaymentIntentId)) {
			/*
			 * Some cards always require 3D secure authentication even card is authenticated while adding a payment instrument (attaching to customer profile) 
			*/
			stripeResult = StripeHelper.getStripeServiceInstance(getStripeApiKey())
					.doCapturePayment(stripePaymentIntentId, StripeHelper.convertDollarAmountToCents(paymentDTOEx.getAmount())
					);
		} else {
			/*
			 * if card is NOT attached when customer created but while making
			 * payment card detail is provided
			 */
			if (paymentDTOEx.getAmount().compareTo(BigDecimal.ZERO) > 0) {
				
				Payment payment = convertDTOToPayment(paymentDTOEx);
				
				stripeResult = StripeHelper
								.getStripeServiceInstance(getStripeApiKey())
									.processOnSessionPayment(payer, creditCard, payment, getReturnUrl(), doCapture);

				logger.debug("stripeResult : {}", stripeResult);
				
				
				// Save stripe customer Id - Customer is still in session hence just
				if (!StripeHelper.isObjectEmpty(stripeResult.getStripeCustomerId()) && StripeHelper.isObjectEmpty(exsitingStripeCustomerId)) {
					StripeHelper.setCustomerMetafieldValueByMetafieldId(
							getStripeCustomerMetafield()
							, StripeHelper.getUser(null, paymentDTOEx.getInstrument(), paymentDTOEx, 0).getCustomer()
							, stripeResult.getStripeCustomerId()
						);
				}
			}
		}
		return stripeResult;
	}

	/** Convert PaymentDTOEx to custom payment
	 * @param paymentDTOEx
	 * @return
	 * @throws PluggableTaskException 
	 */
	private Payment convertDTOToPayment(PaymentDTOEx paymentDTOEx) throws PluggableTaskException {
		
		CurrencyDTO currencyDTO =  paymentDTOEx.getCurrency();
		
		if(!StripeHelper.isObjectEmpty(currencyDTO) && currencyDTO.getId() > 0 ){
			currencyDTO = new CurrencyDAS().find(currencyDTO.getId());
		}
		
		if(StripeHelper.isObjectEmpty(currencyDTO) || currencyDTO.getId() <=0){
			throw new PluggableTaskException("Currency id or code should not be null.");
		}
		
		return new Payment(StripeHelper
				.convertDollarAmountToCents(paymentDTOEx
						.getAmount()), currencyDTO.getCode());
	}


	private static CreditCard convertCreditCard(PaymentInformationDTO paymentInstrument) {
		char[] accountNumber = piBl.getCharMetaFieldByType(
				paymentInstrument, MetaFieldType.PAYMENT_CARD_NUMBER);
		
		Integer paymentMethodId = StripeHelper.isObjectEmpty(paymentInstrument.getPaymentMethod()) ? paymentInstrument.getPaymentMethodId() : paymentInstrument.getPaymentMethod().getId();
			
			
		return new CreditCard(
				StripeHelper.convertCreditCardType(paymentMethodId).toCharArray()
				, Arrays.copyOf(accountNumber,accountNumber.length)
				, piBl.getDateMetaFieldByType(paymentInstrument, MetaFieldType.DATE), null);
	}
	
	private void storeStripeResult(final PaymentInformationBL piBl, StripeResult stripeResult, PaymentDTOEx paymentDTOEx,
			PaymentAuthorizationDTO paymentAuthorization) {
		
		new PaymentAuthorizationBL().create(paymentAuthorization, paymentDTOEx.getId());
		
		paymentDTOEx.setAuthorization(paymentAuthorization);
		
		if (!piBl.isCreditCardObscurred(paymentDTOEx.getInstrument())) {
			piBl.updatePaymentMethodInPaymentInformation(paymentDTOEx
					.getInstrument());
		}		
		paymentDTOEx.setPaymentResult(new PaymentResultDAS().find(stripeResult.isSucceeded() ? CommonConstants.RESULT_OK : CommonConstants.RESULT_FAIL));
	}

	private PaymentAuthorizationDTO buildPaymentAuthorization(
			StripeResult stripeResult) {
		logger.debug("Payment authorization result of {} gateway parsing....", PROCESSOR_NAME);

		PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
		paymentAuthDTO.setProcessor(PROCESSOR_NAME);
		paymentAuthDTO.setCode1(StringUtils.EMPTY);
		
		//Verify transaction id
		if (stripeResult.isSucceeded()) {
			paymentAuthDTO.setTransactionId(StripeHelper.isObjectEmpty(stripeResult.getStripeRefundId())  ?  stripeResult.getStripePaymentIntentId() : stripeResult.getStripeRefundId() );
			logger.debug("transactionId '{}'", paymentAuthDTO.getTransactionId());
		}else {
			String error = "";
			error.concat(StripeHelper.isObjectEmpty(stripeResult.getErrorCode()) ? "" : stripeResult.getErrorCode()).concat(StripeHelper.isObjectEmpty(stripeResult.getErrorMsg()) ? "" : stripeResult.getErrorMsg());
			paymentAuthDTO.setResponseMessage(error);
			logger.debug("errorMessage '{}'", error);
		}
		
		return paymentAuthDTO;
	}	
	
	private String getPaymentMetafieldValue(PaymentDTOEx paymentDTOEx, String metafieldName){
		MetaFieldValue<String>  metafieldValue =  paymentDTOEx.getMetaFields().stream()
		 .filter(mfv -> mfv.getField().getName().equals(metafieldName))
		 .findFirst().orElse(null);
		
		return StripeHelper.isObjectEmpty(metafieldValue) ? null : metafieldValue.getValue(); 
	}
	
	private String getMetafiledNameById(Integer metafieldId){
		MetaField metaField =  MetaFieldBL.getMetaField(metafieldId);
		return StripeHelper.isObjectEmpty(metaField) ? null : metaField.getName();
	}
	
	private void setPaymentMetafieldValue(PaymentDTOEx paymentDTOEx, String metafieldName, String value) throws PluggableTaskException{
		
		List<MetaFieldValue> pmMetafieldValues = paymentDTOEx.getMetaFields();
		
		if(!StripeHelper.isObjectEmpty(pmMetafieldValues) && pmMetafieldValues.size() > 0){
			MetaFieldValue<String>  metafieldValue =  pmMetafieldValues.stream()
					 .filter(mfv -> mfv.getField().getName().equals(metafieldName))
					 .findFirst().get();
					
					if(metafieldValue !=null){
						metafieldValue.setValue(value);
					}
		}else{
			MetaField metaField =  MetaFieldBL.getMetaField(getStripePaymentMetafieldId());
			paymentDTOEx.setMetaField(metaField, value);
		}
	}
	
	
	/**This parameter is used to compare against AIT id.
	 *     *
	 * @return contact type
	 * @throws PluggableTaskException
	 */
	public Integer getContactAITid() throws PluggableTaskException {
        if (contactAccInfoTypeId == null) {
            try {
            	contactAccInfoTypeId = Integer.parseInt(parameters.get(PARAMETER_CONTACT_AIT_ID.getName()));
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Customer contact section id(Account Information Type ID) must be valid integer id.", e);
            }
        }
        return contactAccInfoTypeId;
    }
	
	/** 
	 * @return Metafield id for stripe customer
	 * @throws PluggableTaskException
	 */
	public Integer getStripeCustomerMetafield() throws PluggableTaskException {
        if (stripeCustomertMetafieldId == null) {
            try {
            	stripeCustomertMetafieldId = Integer.parseInt(parameters.get(PARAMETER_CUSTOMER_METAFIELD_ID.getName()));
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Customer metafield id for stripe must be valid integer id.", e);
            }
        }
        return stripeCustomertMetafieldId;
    }
	
	
    /**  Payment metafield id for stripe 
	 * @return Payment metafield id for stripe
	 * @throws PluggableTaskException
	 */
	public Integer getStripePaymentMetafieldId() throws PluggableTaskException {
        if (stripePaymentMetafieldId == null) {
            try {
            	stripePaymentMetafieldId = Integer.parseInt(parameters.get(PARAMETER_PAYMENT_METAFILED_ID.getName()));
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Payment metafield id for stripe must be valid integer id.", e);
            }
        }
        return stripePaymentMetafieldId;
    }
	
	/**
	 * @return Configured payment method type id for Stripe
	 * @throws PluggableTaskException
	 */
	public Integer getStripePaymentMethodId() throws PluggableTaskException {
        if (paymentMethodTypeId == null) {
            try {
            	paymentMethodTypeId = Integer.parseInt(parameters.get(PARAMETER_PAYMENT_METHOD_TYPE_ID.getName()));
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Payment method id for stripe must be valid integer id.", e);
            }
        }
        return paymentMethodTypeId;
    }
	
	/** 
	 * @return Stripe secret api key
	 * @throws PluggableTaskException
	 */
	public String getStripeApiKey() throws PluggableTaskException {
        if (StripeHelper.isObjectEmpty(stripeApiKey)) {
        	 try {
        		 stripeApiKey = parameters.get(PARAMETER_STRIPE_API_KEY.getName());
        	 } catch (NullPointerException e) {
                 throw new PluggableTaskException("Stripe API Key must be valid string value.", e);
             }
        }
        return stripeApiKey;
    }
	
	/** 
	 * @return Stripe secret api key
	 * @throws PluggableTaskException
	 */
	public String getReturnUrl() throws PluggableTaskException {
        if (StripeHelper.isObjectEmpty(this.returnURL)) {
        	 try {
        		 String tempUrl = parameters.get(PARAMETER_RETURN_URL.getName());
        		 if(!StripeHelper.isObjectEmpty( tempUrl)){
        				 this.returnURL = new URL(tempUrl).toString();
        		 }
        	 } catch (MalformedURLException e) {
                 throw new PluggableTaskException("The return URL should be valid.", e);
             }
        }
        return returnURL;
    }
	
}

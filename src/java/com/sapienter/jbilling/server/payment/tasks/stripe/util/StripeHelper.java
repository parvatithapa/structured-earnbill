/**
 * 
 */
package com.sapienter.jbilling.server.payment.tasks.stripe.util;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldExternalHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.ErrorWS;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.SecurePaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.payment.tasks.stripe.StripeService;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.Payer;
import com.sapienter.jbilling.server.payment.tasks.stripe.dto.StripeResult;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;

/**
 * @Package: com.sapienter.jbilling.server.payment.tasks.stripe.util 
 * @author: Amey Pelapkar   
 * @date: 21-Apr-2021 3:25:07 pm
 *
 */
public class StripeHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles
			.lookup().lookupClass());

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	public static final String KEY_METADATA_REF_PAYMENT_ID = "BILLINGHUB_REF_PAYMENT_ID";
	public static final String KEY_METADATA_PROCESSED = "BILLINGHUB_PROCESSED";
	private static final String OBSCURED_NUMBER_FORMAT = "************"; // + last four digits	
	public static final String ERROR_STRIPE_METAFIELD_MISSING_CUSTOMER_ID = "Metafield value for stripeCustomerId is missing.";
	public static final String ERROR_BILLING_INFORMATION_NOT_FOUND = "Please verify contact details.";
	public static final String NEXT_ACTION_TYPE_REDIRECT_TO_URL = "redirect_to_url";
	
	private StripeHelper() {
		
	}
	/** Get instance of stripe service  
	 * @param apiKey
	 * @return
	 */
	public static StripeService getStripeServiceInstance(String apiKey){
		return new StripeService(apiKey);
	}

	
	/** Common method to evaluate null value
	 * @param obj
	 * @return boolean
	 */
	public static <T> boolean isObjectEmpty(T obj){
		if(obj instanceof String){
			return ((String)obj).isEmpty();
		}		
		return obj == null;
	}
	
	
	/** Fetch the user from either ContactDTO or paymentInstrument 
	 * @param contact
	 * @param paymentInstrument
	 * @return MetaFieldValue
	 */
	public static UserDTO getUser(ContactDTO contact,	PaymentInformationDTO paymentInstrument, PaymentDTOEx paymentDTOEx, int userId){
		UserDTO user = null;
		
		if (!StripeHelper.isObjectEmpty(contact)) {
			UserBL bl = new UserBL(contact.getUserId());
			user = bl.getEntity();
			// can not get credit card from db as there may be many
		}else if (!StripeHelper.isObjectEmpty(paymentInstrument) && !StripeHelper.isObjectEmpty(paymentInstrument.getUser())) {
			user = paymentInstrument.getUser();
		}else if(!StripeHelper.isObjectEmpty(paymentDTOEx) && !StripeHelper.isObjectEmpty(paymentDTOEx.getUserId())){
			UserBL bl = new UserBL(paymentDTOEx.getUserId());
			user = bl.getEntity();
		}else if(userId > 0){
			UserBL bl = new UserBL(userId);
			user = bl.getEntity();
		}
		return user;
	}
	
	
	/** Get customer metafield value by metafield id 
	 * @param customer
	 * @param metaFieldNameId
	 * @return MetaFieldValue
	 */
	public static MetaFieldValue<String> getCustomerMetafieldValueByMetafieldId( Integer metaFieldNameId ,CustomerDTO customer) {
		@SuppressWarnings("unchecked")
		MetaFieldValue<String> metaFieldStripeCustomerId = customer.getMetaField(metaFieldNameId);	
		return metaFieldStripeCustomerId;
	}

	/** Set customer metafield value by metafield id 
	 * @param customer
	 * @param metaFieldNameId
	 * @return MetaFieldValue
	 */
	public static MetaFieldValue<String> setCustomerMetafieldValueByMetafieldId( Integer metaFieldNameId ,CustomerDTO customer, String value) {
		@SuppressWarnings("unchecked")
		MetaFieldValue<String> matafieldValue = getCustomerMetafieldValueByMetafieldId(metaFieldNameId, customer);
		
		if(!StripeHelper.isObjectEmpty(matafieldValue)){
			matafieldValue.setValue(value);
		}else {
			MetaField metaField = MetaFieldBL.getMetaField(metaFieldNameId);
			customer.setMetaField(metaField, value);
		}
		return matafieldValue;
	}	
	
	/**
     * Utility method to format the given dollar float value to a two
     * digit number in compliance with the PayPal gateway API.
     *
     * @param amount dollar float value to format
     * @return formatted amount as a string
     */
    public static long convertDollarAmountToCents(BigDecimal amount) {
    	
    	amount = (amount.setScale(CommonConstants.BIGDECIMAL_SCALE_STR, CommonConstants.BIGDECIMAL_ROUND))
				.multiply(ONE_HUNDRED);
        return amount.longValue();
    }

    
    /** Get stripe reference id for customer
     * @param stripeCustomertMetafieldId
     * @param userDTO
     * @return
     * @throws PluggableTaskException
     */
    public static  String getStripeCustomerId(Integer stripeCustomertMetafieldId, UserDTO userDTO) {
		try {
			MetaFieldValue<String> metaFieldStripeCustomerId = getCustomerMetafieldValueByMetafieldId( stripeCustomertMetafieldId, userDTO.getCustomer());
			
			if(!StripeHelper.isObjectEmpty(metaFieldStripeCustomerId) && metaFieldStripeCustomerId.getValue()!=null){
				return metaFieldStripeCustomerId.getValue().startsWith("DEFAULT_CUST") ? null : metaFieldStripeCustomerId.getValue();
			}
		} catch (NumberFormatException nfexp) {
			logger.error("getStripeCustomerId -> getCustomerMetafieldValueByMetafieldId -> stripeCustomertMetafieldId", nfexp);
		}	
		return null;
	}
    
    public static Payer getPayerInformation(Integer contactAccInfoTypeId, Integer stripeCustomertMetafieldId, PaymentDTOEx payment, UserDTO userDTO) throws PluggableTaskException {
		
    	AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();    	
    	MetaFieldGroup metaFieldContactGroup = accountInformationTypeDAS.find(contactAccInfoTypeId);
    	
		if(isObjectEmpty(metaFieldContactGroup)){
			throw new PluggableTaskException(ERROR_BILLING_INFORMATION_NOT_FOUND);
		}
    	
    	MetaField customerMetafield =  MetaFieldBL.getMetaField(stripeCustomertMetafieldId);
    	
    	if(StripeHelper.isObjectEmpty(customerMetafield)){
    		throw new PluggableTaskException(ERROR_STRIPE_METAFIELD_MISSING_CUSTOMER_ID);
    	}
    	
    	Payer payer = null;
		
		int userId = 0;
		
		if(!StripeHelper.isObjectEmpty(payment) ){
			userId = payment.getUserId();
		}else if(StripeHelper.isObjectEmpty(payment) && !StripeHelper.isObjectEmpty(userDTO)){
			userId = userDTO.getId();
		}
		

		// Entity id would be null if user is not saved in database.
		Integer entityId = new UserDAS().getEntityByUserId(userId);
		
		//entity id
		if(StripeHelper.isObjectEmpty(entityId) || entityId == 0 ){
			if(payment != null){
				entityId = payment.getInstrument().getPaymentMethodType().getEntity().getId();
			}else if(userDTO != null && userDTO.getCompany() != null ){
				entityId = userDTO.getCompany().getId();
			}
		}
		
		try {
        	
        	int customerId = new CustomerDAS().getCustomerId(userId);
    		
    		
    		MetaFieldDAS metaFieldDAS = new MetaFieldDAS();    		
    		Map<String, String> metaFieldMapByMetaFieldType = metaFieldDAS.getCustomerAITMetaFieldValueMapByMetaFieldType(customerId
    											, metaFieldContactGroup.getId()
    											, TimezoneHelper.companyCurrentDate(entityId));
    		
    		payer = Payer.builder()
    					.id(userId)
			    		.firstName(metaFieldMapByMetaFieldType.get(MetaFieldType.FIRST_NAME.toString()))
			    		.lastName(metaFieldMapByMetaFieldType.get(MetaFieldType.LAST_NAME.toString()))
			    		.street(metaFieldMapByMetaFieldType.get(MetaFieldType.ADDRESS1.toString()))
			    		.street2(metaFieldMapByMetaFieldType.get(MetaFieldType.ADDRESS2.toString()))
			    		.city(metaFieldMapByMetaFieldType.get(MetaFieldType.CITY.toString()))
			    		.state(metaFieldMapByMetaFieldType.get(MetaFieldType.STATE_PROVINCE.toString()))
			    		.countryCode(metaFieldMapByMetaFieldType.get(MetaFieldType.COUNTRY_CODE.toString()))
			    		.zip(metaFieldMapByMetaFieldType.get(MetaFieldType.POSTAL_CODE.toString()))
			    		.email(metaFieldMapByMetaFieldType.get(MetaFieldType.BILLING_EMAIL.toString()))
    				.build();
    		
    		
    		
    		if(isObjectEmpty(payer.getEmail())){
    			payer.setEmail(metaFieldMapByMetaFieldType.get(MetaFieldType.EMAIL.toString()));
    		}
    		
    		if (payer.getFirstName() == null) {
    			payer.setFirstName(payer.getEmail());
    		}
    		
    		/*
    		 * Set stripe customer ID
    		*/
    		if(!StripeHelper.isObjectEmpty(payment) && StripeHelper.isObjectEmpty(userDTO)) {
    			userDTO = getUser(null, null, payment, 0);
    		}
    		
    		payer.setStripeCustomerId(getStripeCustomerId(stripeCustomertMetafieldId, userDTO));
        	
        } catch (NumberFormatException e) {
            throw new PluggableTaskException("Configured contactAccInfoTypeId must be an integer!", e);
        }
		
		return payer;
	}
    
    /** Convert stripe result or exception to SecurePaymentWS
	 * @param userId 
	 * @param stripeResult
	 * @param exception
	 * @return SecurePaymentWS
	 * @throws PluggableTaskException 
	 * @throws StripeException 
	 */
	public static SecurePaymentWS converToSecurePaymentWS(Integer userId, StripeResult stripeResult, Exception exception) {
		// To be Deleted
		//SecurePaymentWS securePaymentWS = new SecurePaymentWS();
		
		SecurePaymentWS securePaymentWS = SecurePaymentWS
    			.builder()
	    			.userId(userId)
	    			.billingHubRefId(0)
	    			.nextAction(null)
	    			.status(null)
	    			.error(null)
    			.build();
		
		if(!StripeHelper.isObjectEmpty(stripeResult)){
			securePaymentWS.setStatus(stripeResult.getStatus());
			if(stripeResult.isActionRequired()){
				securePaymentWS.setNextAction(stripeResult.getNextAction());
			}else if(stripeResult.isCaptureRequired() || stripeResult.isConfirmationRequired()){
				securePaymentWS.setStatus(StripeResult.StripeIntentStatus.SUCCEEDED.toString());
			}
		}else if(!StripeHelper.isObjectEmpty(exception)){
			securePaymentWS.setStatus("failed");
			if (exception instanceof StripeException) {
				StripeException stripeExp = (StripeException) exception;
				securePaymentWS.setError(new ErrorWS(stripeExp.getCode(), stripeExp.getMessage()));
			} else if (exception instanceof PluggableTaskException) {
				PluggableTaskException plugExp = (PluggableTaskException) exception;
				securePaymentWS.setError(new ErrorWS("stripe-plugin-issue", plugExp.getMessage()));
			} else {
				securePaymentWS.setError(new ErrorWS("server-exception", exception.getMessage()));
			}
		}		
		return securePaymentWS;
	}
		
	public static PaymentInformationDTO populatePaymentInfoDtoWithStripeCard(String apiKey,PaymentInformationDTO piDTO,  UserDTO userDTO, String stripePaymentMethodId, Integer paymentMethodTypeId, boolean autopaymentAuthorization) throws ParseException, StripeException {
		PaymentMethod  stripePaymentMethod = StripeHelper.getStripeServiceInstance(apiKey).retrievePaymentMethod(stripePaymentMethodId);
		//Retrieve card details
		com.stripe.model.PaymentMethod.Card stripeCard = stripePaymentMethod.getCard();
		 
		if(StripeHelper.isObjectEmpty(piDTO)){
			PaymentMethodTypeDTO paymentMethodTye = new PaymentMethodTypeDAS().find(paymentMethodTypeId);
			piDTO =  new PaymentInformationDTO(1, userDTO, paymentMethodTye, convertToPaymentMethod(stripeCard.getBrand()));
			piDTO.setPaymentMethod(new PaymentMethodDAS().find(piDTO.getPaymentMethodId()));
		}
		
		for (MetaField metaField : getPaymentMethodMetaFields(paymentMethodTypeId)) {
			if(metaField.getFieldUsage() == MetaFieldType.FIRST_NAME){
				piDTO.setMetaField(metaField, userDTO.getUserName().toCharArray());
			}else if(metaField.getFieldUsage() == MetaFieldType.PAYMENT_CARD_NUMBER){
				piDTO.setMetaField(metaField, (OBSCURED_NUMBER_FORMAT + stripeCard.getLast4()).toCharArray());
			}else if(metaField.getFieldUsage() == MetaFieldType.DATE){
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/yyyy");					
				Date expiry = simpleDateFormat.parse(stripeCard.getExpMonth()+"/"+stripeCard.getExpYear());	
				piDTO.setMetaField(metaField, simpleDateFormat.format(expiry).toCharArray());
			}else if(metaField.getFieldUsage() == MetaFieldType.GATEWAY_KEY){
				piDTO.setMetaField(metaField, stripePaymentMethod.getId().toCharArray());
			}else if(metaField.getFieldUsage() == MetaFieldType.CC_TYPE){
				piDTO.setMetaField(metaField, convertToCreditCardType(stripeCard.getBrand()));
			}else if(metaField.getFieldUsage() == MetaFieldType.AUTO_PAYMENT_AUTHORIZATION){
				piDTO.setMetaField(metaField, autopaymentAuthorization);
			}else if(metaField.getFieldUsage() == MetaFieldType.TRANSACTION_ID){
				piDTO.setMetaField(metaField, "");
			}
		}
		return piDTO;
	}
	
	private static Set<MetaField> getPaymentMethodMetaFields(Integer paymentMetohdTypeId) {
        return MetaFieldExternalHelper.getPaymentMethodMetaFields(paymentMetohdTypeId);
    }    
	
	private static String convertToCreditCardType(String ccType) {
		
		if(ccType.contains("visa")){
			return CreditCardType.VISA.toString();
		}
		else if(ccType.contains("master")){ 
			return CreditCardType.MASTER_CARD.toString();
		}
		else if(ccType.contains("amex")){
			return CreditCardType.AMEX.toString();
		}
		else if(ccType.contains("discover")){
			return CreditCardType.DISCOVER.toString();
		}
		else if(ccType.contains("jcb")){
			return CreditCardType.JCB.toString();
		}
		else if(ccType.contains("maestro")){
			return CreditCardType.MAESTRO.toString();
		}
		else if(ccType.contains("visa_electron")){
			return CreditCardType.VISA_ELECTRON.toString();
		}

		return CreditCardType.VISA.toString();
	}
	
	public static int convertToPaymentMethod(String ccType) {
		
		if(ccType.contains("visa")){
			return CommonConstants.PAYMENT_METHOD_VISA.intValue();
		}
		else if(ccType.contains("master")){ 
			return CommonConstants.PAYMENT_METHOD_MASTERCARD.intValue();
		}
		else if(ccType.contains("amex")){
			return CommonConstants.PAYMENT_METHOD_AMEX.intValue();
		}
		else if(ccType.contains("discover")){
			return CommonConstants.PAYMENT_METHOD_DISCOVER.intValue();
		}
		else if(ccType.contains("jcb")){
			return CommonConstants.PAYMENT_METHOD_JCB.intValue();
		}
		else if(ccType.contains("maestro")){
			return CommonConstants.PAYMENT_METHOD_MAESTRO.intValue();
		}
		else if(ccType.contains("visa_electron")){
			return CommonConstants.PAYMENT_METHOD_VISA_ELECTRON.intValue();
		}

		return CommonConstants.PAYMENT_METHOD_VISA.intValue();
	}
	
	public static String convertCreditCardType(int ccType) {
		switch (ccType) {
		case 2:
			return CreditCardType.VISA.toString();
		case 3:
			return CreditCardType.MASTER_CARD.toString();
		case 4:
			return CreditCardType.AMEX.toString();
		case 6:
			return CreditCardType.DISCOVER.toString();
		case 11:
			return CreditCardType.JCB.toString();
		case 13:
			return CreditCardType.MAESTRO.toString();
		case 14:
			return CreditCardType.VISA_ELECTRON.toString();
		default :
			return "";
		}
	}
}
package com.sapienter.jbilling.server.payment.tasks.braintree;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.braintree.dto.BrainTreeResult;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCard;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.Payer;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.PaymentAction;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

public class BrainTreePaymentExternalTask extends PaymentTaskWithTimeout implements IExternalCreditCardStorage{

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    private static final String PAYMENT_UNDER_SETTLEMENT_ERROR = "TRANSACTION_CANNOT_REFUND_UNLESS_SETTLED";
    
    private static final PaymentInformationBL piBl = new PaymentInformationBL();

    /* Plugin parameters */
    public static final ParameterDescription BUSINESS_ID =
    	new ParameterDescription("Bussiness Id", true, ParameterDescription.Type.STR);
    
    public static final ParameterDescription ALLOWED_PAYMENTMETHOD_IDS =
        	new ParameterDescription("Allowed Payment Method Ids", true, ParameterDescription.Type.STR);

    public static final ParameterDescription FC_WEB_SERVICE_URL =
            new ParameterDescription("FC Web Service URL", true, ParameterDescription.Type.STR);

    private static final String AUTHORIZATION_ID_FOR_CAPTURE_PAYMENT = "Authorization Id";


    public String getBusinessId() throws PluggableTaskException {
        return ensureGetParameter(BUSINESS_ID.getName());
    }
    
    public String getAllowedPaymentMethodIds() throws PluggableTaskException {
        return ensureGetParameter(ALLOWED_PAYMENTMETHOD_IDS.getName());
    }

    public String getRemoteAPI() throws PluggableTaskException {
        return ensureGetParameter(FC_WEB_SERVICE_URL.getName());
    }

    //initializer for pluggable params
    {    	
    	descriptions.add(BUSINESS_ID);
    	descriptions.add(ALLOWED_PAYMENTMETHOD_IDS);
        descriptions.add(FC_WEB_SERVICE_URL);
        
    }        
 
    private BrainTreeApi getBTApi() throws PluggableTaskException{
        return new BrainTreeApi(getBusinessId(), getRemoteAPI());
    }

    /**
     * Prepares a given payment to be processed using an external storage gateway key instead of
     * the raw credit card number. If the associated credit card has been obscured it will be
     * replaced with the users stored credit card from the database, which contains all the relevant
     * external storage data.
     *
     * New or un-obscured credit cards will be left as is.
     *
     * @param payment payment to prepare for processing from external storage
     */
    public void prepareExternalPayment(PaymentDTOEx payment) {
        if (piBl.useGatewayKey(payment.getInstrument())) {
            logger.debug("credit card is obscured, retrieving from database to use external store.");
            if(payment.getInstrument().getId() != null) {
            	// load only if its saved in database. Otherwise do not
            	payment.setInstrument(new PaymentInformationDAS().find(payment.getInstrument().getId()));
            }
        } else {
            logger.debug("new credit card or previously un-obscured, using as is.");
        }
    }

    /**
     * Updates the gateway key of the credit card associated with this payment. PayPal
     * returns a TRANSACTIONID which can be used to start new transaction without specifying
     * payer info.
     *
     * @param payment successful payment containing the credit card to update.
     *  */
    public void updateGatewayKey(PaymentDTOEx payment) {
        PaymentAuthorizationDTO auth = payment.getAuthorization();
        // update the gateway key with the returned PayPal TRANSACTIONID
        PaymentInformationDTO card = payment.getInstrument();
        
        piBl.updateCharMetaField(card, auth.getTransactionId().toCharArray(), MetaFieldType.GATEWAY_KEY);

        // obscure new credit card numbers
        if (!com.sapienter.jbilling.common.Constants.PAYMENT_METHOD_GATEWAY_KEY.equals(card.getPaymentMethod().getId()))
            piBl.obscureCreditCardNumber(card);
    }

    /**
     * Utility method to format the given dollar float value to a two
     * digit number in compliance with the PayPal gateway API.
     *
     * @param amount dollar float value to format
     * @return formatted amount as a string
     */
    private static String formatDollarAmount(BigDecimal amount) {
        amount = amount.abs().setScale(2, RoundingMode.HALF_EVEN); // gateway format, do not change!
        return amount.toPlainString();
    }

    /**
     * Utility method to check if a given {@link PaymentDTOEx} payment can be processed
     * by this task.
     *
     * @param payment payment to check
     * @return true if payment can be processed with this task, false if not
     */
    private boolean isApplicable(PaymentDTOEx payment) throws PluggableTaskException{
        logger.debug("Is this payment {} applicable" , payment);
        if (piBl.isBTPayment(payment.getInstrument())) {
            logger.info("processing payment using unified BT payment since 'BT ID' & 'Type' is provided");
            return false;
        }
        if (piBl.isCreditCard(payment.getInstrument())
                && Arrays.asList(getAllowedPaymentMethodIds().split(",")).contains(
                        String.valueOf(payment.getInstrument().getPaymentMethodType().getId()))) {
            return true;
        }
        logger.warn("Can't process if Express checkout payment method or without a credit card or ach");
        return false;
    }

    /**
     * Returns the name of this payment processor.
     * @return payment processor name
     */
    private String getProcessorName() {
        return "BrainTree";
    }

    private static boolean isRefund(PaymentDTOEx payment) {
        return BigDecimal.ZERO.compareTo(payment.getAmount()) > 0 || payment.getIsRefund() != 0;
    }

    private static boolean isCreditCardStored(PaymentDTOEx payment) {
//        return new PaymentInformationBL().useGatewayKey(payment.getInstrument());
    	return (null != piBl.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.GATEWAY_KEY) && piBl.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.GATEWAY_KEY).length > 0);
    }

    private PaymentAuthorizationDTO buildPaymentAuthorization(BrainTreeResult brainTreeResult) {
        logger.debug("Payment authorization result of {} gateway parsing....",getProcessorName());

        PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
        paymentAuthDTO.setProcessor(getProcessorName());

        paymentAuthDTO.setCode1(StringUtils.EMPTY);
        String txID = brainTreeResult.getTransactionId();
        if (txID != null) {
            paymentAuthDTO.setTransactionId(txID);
            paymentAuthDTO.setCode1(txID);
            logger.debug("transactionId/code1 [{}]",txID);
        }
        
        paymentAuthDTO.setCardCode(brainTreeResult.getCardNumber());
        paymentAuthDTO.setCode2(brainTreeResult.getCardType());
        paymentAuthDTO.setCode3(brainTreeResult.getPaymentType());

        String errorCode = brainTreeResult.getErrorCode();
        String errorShortMsg=brainTreeResult.getErrorMessage();
        paymentAuthDTO.setResponseMessage(errorShortMsg);
        logger.debug("errorMessage [{}]", errorCode);
        logger.debug("errorShortMessage [{}]", errorShortMsg);

        String avs = brainTreeResult.getAvs();
        if(avs != null) {
            paymentAuthDTO.setAvs(avs);
            logger.debug("avs [{}]", avs);
        }

        return paymentAuthDTO;
    }

    private static String convertCreditCardType(int ccType) {
        switch(ccType) {
            case 2: return CreditCardType.VISA.toString();
            case 3: return CreditCardType.MASTER_CARD.toString();
            case 4: return CreditCardType.AMEX.toString();
            case 6: return CreditCardType.DISCOVER.toString();
            case 11: return CreditCardType.JCB.toString();
            case 13: return CreditCardType.MAESTRO.toString();
            case 14: return CreditCardType.VISA_ELECTRON.toString();
        }

        return "";
    }

    private static String convertCreditCardExpiration(Date ccExpiry) {
        return new SimpleDateFormat("MM/yyyy").format(ccExpiry);
    }

    private CreditCard convertCreditCard(PaymentDTOEx payment) {
        char[] paymentCardNumber = piBl.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.PAYMENT_CARD_NUMBER);
        return new CreditCard(
                            convertCreditCardType(Util.getPaymentMethod(paymentCardNumber)).toCharArray(),
                            paymentCardNumber,
                            convertCreditCardExpiration(piBl.getDateMetaFieldByType(payment.getInstrument(), MetaFieldType.DATE)).toCharArray(),
                            getMetaFieldValue(payment.getInstrument(), CommonConstants.METAFIELD_NAME_BRAINTREE_CVV).toCharArray());
    }

    private Payer convertPayer(PaymentDTOEx payment) {

        Integer entityId = getEntityId();

        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        String billingGroupNameValue = metaFieldDAS.getComapanyLevelMetaFieldValue(MetaFieldName.PAYPAL_BILLING_GROUP_NAME.getMetaFieldName(), entityId);

        AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
        UserDTO user = new UserDAS().find(payment.getUserId());
        MetaFieldGroup metaFieldGroup=accountInformationTypeDAS.getGroupByNameAndEntityId(entityId
                , EntityType.ACCOUNT_TYPE, billingGroupNameValue, user.getCustomer().getAccountType().getId());
        if(metaFieldGroup == null){
        	return null;
        }
        
        int customerId = new CustomerDAS().getCustomerId(payment.getUserId());
        Payer payer = new Payer();
        Map<String, String> metaFieldMapByMetaFieldType = metaFieldDAS.getCustomerAITMetaFieldValueMapByMetaFieldType(customerId, metaFieldGroup.getId(), TimezoneHelper.companyCurrentDate(entityId));
     	payer.setFirstName(metaFieldMapByMetaFieldType.get(MetaFieldType.FIRST_NAME.toString()));
     	payer.setLastName(metaFieldMapByMetaFieldType.get(MetaFieldType.LAST_NAME.toString()));
     	payer.setStreet(metaFieldMapByMetaFieldType.get(MetaFieldType.ADDRESS1.toString()));
     	payer.setStreet2(metaFieldMapByMetaFieldType.get(MetaFieldType.ADDRESS2.toString()));
     	payer.setCity(metaFieldMapByMetaFieldType.get(MetaFieldType.CITY.toString()));
     	payer.setState(metaFieldMapByMetaFieldType.get(MetaFieldType.STATE_PROVINCE.toString()));
     	payer.setCountryCode(piBl.getStringMetaFieldByType(payment.getInstrument(), MetaFieldType.COUNTRY_CODE));
     	payer.setZip(metaFieldMapByMetaFieldType.get(MetaFieldType.POSTAL_CODE.toString()));
     	payer.setEmail(metaFieldMapByMetaFieldType.get(MetaFieldType.BILLING_EMAIL.toString()));

         if(payer.getFirstName()==null) {
        	 payer.setFirstName(metaFieldMapByMetaFieldType.get(MetaFieldType.ORGANIZATION.toString()));
         }
         
        if (payer.getFirstName() == null || payer.getFirstName().trim().isEmpty()){
            return null;
//        }else if (payer.getLastName() == null || payer.getLastName().trim().isEmpty()){
//            return null;
        }else if (payer.getStreet() == null || payer.getStreet().trim().isEmpty()){
            return null;
        }else if (payer.getZip() == null || payer.getZip().trim().isEmpty()){
            return null;
        }else if (payer.getEmail() == null || payer.getEmail().trim().isEmpty()){
            return null;
        }
        return payer;
    }

    private void storeBrainTreeResult(BrainTreeResult result, PaymentDTOEx payment,
                                   PaymentAuthorizationDTO paymentAuthorization, boolean updateKey) {
        new PaymentAuthorizationBL().create(paymentAuthorization, payment.getId());
        payment.setAuthorization(paymentAuthorization);
        if (!piBl.isCreditCardObscurred(payment.getInstrument())) {
            piBl.updatePaymentMethodInPaymentInformation(payment.getInstrument());
        }
        if(result.isSucceseded()) {
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_OK));
            if(updateKey) {
                updateGatewayKey(payment);
            }
        } else {
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_FAIL));
        }
    }

    private Result doRefund(PaymentDTOEx payment) throws PluggableTaskException {
        try {
        	
            BrainTreeApi api = getBTApi();
            
            PaymentBL bl = new PaymentBL(payment.getPayment().getId());
            payment.setPayment(bl.getDTOEx(new EntityBL(getEntityId()).getEntity().getLanguageId()));
            
            BrainTreeResult result = api.refund(getTimeoutSeconds() * 1000, formatDollarAmount(payment.getAmount()), 
            		payment.getPayment().getAuthorization().getTransactionId());

            // If payment transaction is not settled, do void the complete payment transaction and make the origial transaction amount as refund amount.
            if(result.getErrorCode() != null && result.getErrorCode().equalsIgnoreCase(PAYMENT_UNDER_SETTLEMENT_ERROR)){            	
            	            	
            	
				payment.setAmount(payment.getPayment().getAmount());
				result = doVoid(payment.getPayment());				
				// Setting original payment's amount as refund amount in sessioned payment
				PaymentDAS paymentDAS = new PaymentDAS();
				PaymentDTO tPayment = paymentDAS.find(payment.getId());
				tPayment.setAmount(payment.getPayment().getAmount());
				paymentDAS.save(tPayment);
				
			}
            
            
            PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(result);
            storeBrainTreeResult(result, payment, paymentAuthorization, false);

            return new Result(paymentAuthorization, false);

        } catch (Exception e) {
            logger.error("Couldn't handle payment request due to error", e);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        }
    }

    private Result doPaymentWithStoredCreditCard(PaymentDTOEx payment, PaymentAction paymentAction) throws PluggableTaskException {
        try {
        	Payer payer = convertPayer(payment);
        	if(payer == null){
                payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_BILLING_INFORMATION_NOT_FOUND));
        		return new Result(null, false);
        	}
        	String gatewayKey = null != piBl.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.GATEWAY_KEY) ? new String(piBl.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.GATEWAY_KEY)) : null;
        	BrainTreeResult result = getBTApi().receivePayment(getTimeoutSeconds() * 1000, 
        			gatewayKey, formatDollarAmount(payment.getAmount()));

            PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(result);
            storeBrainTreeResult(result, payment, paymentAuthorization, false);
            
            return new Result(paymentAuthorization, false);

        } catch (Exception e) {
            logger.error("Couldn't handle payment request due to error", e);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        }
    }
    
    private Result doPaymentWithoutStoredCreditCard(PaymentDTOEx payment, PaymentAction paymentAction) throws PluggableTaskException {    	
        try {
        	Payer payer = convertPayer(payment);
        	if(payer == null){
                payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_BILLING_INFORMATION_NOT_FOUND));
        		return new Result(null, false);
        	}

        	BrainTreeResult result = getBTApi().receiveDirectPayment(getTimeoutSeconds() * 1000, 
        			payment.getUserId(), formatDollarAmount(payment.getAmount()), convertCreditCard(payment));

            PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(result);
            storeBrainTreeResult(result, payment, paymentAuthorization, false);
            
            return new Result(paymentAuthorization, false);

        } catch (Exception e) {
            logger.error("Couldn't handle payment request due to error", e);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        }
    }

    private Result doCapture(PaymentDTOEx payment, PaymentAuthorizationDTO auth) throws PluggableTaskException {
        try {
        	
        	BrainTreeResult result = getBTApi().capture(getTimeoutSeconds() * 1000, formatDollarAmount(payment.getAmount()), auth.getTransactionId());        			

            PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(result);
            storeBrainTreeResult(result, payment, paymentAuthorization, false);

            return new Result(paymentAuthorization, false);

        } catch (Exception e) {
            logger.error("Couldn't handle payment request due to error", e);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        }
    }

    private boolean doProcess(PaymentDTOEx payment, PaymentAction paymentAction, boolean updateKey)
            throws PluggableTaskException {

    	if(isRefund(payment)) {
            return doRefund(payment).shouldCallOtherProcessors();
        }

        String authorizationId = getAuthorizationIdForCapturePayment(payment);
        if(StringUtils.isNotBlank(authorizationId)) {
            PaymentAuthorizationDTO auth = new PaymentAuthorizationDTO();
            auth.setTransactionId(authorizationId);
            return doCapture(payment, auth).shouldCallOtherProcessors();
        }

        if(isCreditCardStored(payment)) {
            return doPaymentWithStoredCreditCard(payment, paymentAction)
                    .shouldCallOtherProcessors();
        }

        return doPaymentWithoutStoredCreditCard(payment, paymentAction)
                .shouldCallOtherProcessors();
        
    }

    private String getAuthorizationIdForCapturePayment(PaymentDTOEx payment) {
        Optional<MetaFieldValue> authMetaField = payment.getMetaFields().stream()
                .filter(mfv -> mfv.getField().getName().equalsIgnoreCase(AUTHORIZATION_ID_FOR_CAPTURE_PAYMENT))
                .findAny();
        return authMetaField.isPresent() ? (String) authMetaField.get().getValue() : null;
    }

    /*
     * doRefund do void authorized payment or actually refund payment based on settlement status. In case of authorized fund it will void payment, in case of direct payment it will refund payment.
     */
    private BrainTreeResult doVoid(PaymentDTOEx payment) throws PluggableTaskException {
        try {
        	        	        	
        	BrainTreeResult result = getBTApi().voidPayment(getTimeoutSeconds() * 1000, 
            		payment.getAuthorization().getTransactionId());
        	return result;
        } catch (Exception e) {
            logger.error("Couldn't void payment authorization due to error", e);
            throw new PluggableTaskException(e);            
        }
    }

    public boolean process(PaymentDTOEx payment) throws PluggableTaskException {
        logger.debug("Payment processing for {} gateway", getProcessorName());

        if (payment.getPayoutId() != null) {
            return true;
        }

        if(!isApplicable(payment)) {
            return NOT_APPLICABLE.shouldCallOtherProcessors();
        }

        prepareExternalPayment(payment);
        return doProcess(payment, PaymentAction.SALE, true /* updateKey */);
    }

    public void failure(Integer userId, Integer retry) {
        // do nothing
    }

    // As full creative is not using prauth for paymnent, returning false as default value
    public boolean preAuth(PaymentDTOEx payment) throws PluggableTaskException {
        return false;
    }

    public boolean confirmPreAuth(PaymentAuthorizationDTO auth, PaymentDTOEx payment)
            throws PluggableTaskException {

        logger.debug("Confirming pre-authorization for {} gateway", getProcessorName());
        if (!getProcessorName().equals(auth.getProcessor())) {
            /*  let the processor be called and fail, so the caller can do something
                about it: probably re-call this payment task as a new "process()" run */
            logger.warn("The processor of the pre-auth is not {}, is {}", getProcessorName(), auth.getProcessor());
        }

        PaymentInformationDTO card = payment.getInstrument();
        if (card == null) {
            throw new PluggableTaskException("Credit card is required, capturing payment: " + payment.getId());
        }

        if (!isApplicable(payment)) {
            logger.error("This payment can not be captured: {}", payment);
            return NOT_APPLICABLE.shouldCallOtherProcessors();
        }

        // process
        prepareExternalPayment(payment);
        return doCapture(payment, auth).shouldCallOtherProcessors();
    }

    public String storeCreditCard(ContactDTO contact, PaymentInformationDTO paymentInstrument) {
    	
        logger.debug("Storing creadit card info within {} gateway", getProcessorName());
        UserDTO user;
        if (contact != null) {
            UserBL bl = new UserBL(contact.getUserId());
            user = bl.getEntity();
        } else if (paymentInstrument != null && paymentInstrument.getUser() != null) {
            user = paymentInstrument.getUser();
        } else {
            logger.error("Could not determine user id for external credit card storage");
            return null;
        }

        // new contact that has not had a credit card created yet
        if (paymentInstrument == null) {
            logger.warn("No credit card to store externally.");
            return null;
        }

        /*  Note, don't use PaymentBL.findPaymentInstrument() as the given creditCard is still being
            processed at the time that this event is being handled, and will not be found.

            PaymentBL()#create() will cause a stack overflow as it will attempt to update the credit card,
            emitting another NewCreditCardEvent which is then handled by this method and repeated.
         */
        PaymentDTO payment = new PaymentDTO();
        payment.setBaseUser(user);
        payment.setCurrency(user.getCurrency());
        payment.setAmount(CommonConstants.BIGDECIMAL_ONE_CENT);
        
        payment.setPaymentMethod(new PaymentMethodDAS().find(Util.getPaymentMethod(piBl.getCharMetaFieldByType(paymentInstrument, MetaFieldType.PAYMENT_CARD_NUMBER))));
        
        paymentInstrument.setPaymentMethod(payment.getPaymentMethod());
        payment.setIsRefund(0);
        payment.setIsPreauth(1);
        payment.setDeleted(1);
        payment.setAttempt(1);
        payment.setPaymentDate(new Date());
        payment.setCreateDatetime(new Date());

        PaymentDTOEx paymentEx = new PaymentDTOEx(new PaymentDAS().save(payment));
        paymentEx.setInstrument(paymentInstrument);
        try {

            if (!isApplicable(paymentEx)) {
                logger.error("This payment Gateway Key: {}", piBl.getCharMetaFieldByType(paymentEx.getInstrument(), MetaFieldType.GATEWAY_KEY));
                return (null != piBl.getCharMetaFieldByType(paymentEx.getInstrument(), MetaFieldType.GATEWAY_KEY) ? new String(piBl.getCharMetaFieldByType(paymentEx.getInstrument(), MetaFieldType.GATEWAY_KEY)) : null);
            }

        	if(isCreditCardStored(paymentEx)) {
                logger.debug("Customer Key : {}",  piBl.getCharMetaFieldByType(paymentEx.getInstrument(), MetaFieldType.GATEWAY_KEY));
        		return (null != piBl.getCharMetaFieldByType(paymentEx.getInstrument(), MetaFieldType.GATEWAY_KEY) ? new String(piBl.getCharMetaFieldByType(paymentEx.getInstrument(), MetaFieldType.GATEWAY_KEY)) : null);
            }

            BrainTreeApi brainTreeAPI = getBTApi();
            String customerKey = brainTreeAPI.createBTCustomer(convertCreditCard(paymentEx), convertPayer(paymentEx), getTimeoutSeconds()  * 1000, user.getId());
            updatePaymentResult(paymentEx, Constants.PAYMENT_RESULT_SUCCESSFUL);
            return customerKey;
        } catch (PluggableTaskException e) {
            logger.error("Could not process external storage payment", e);
            updatePaymentResult(paymentEx, Constants.PAYMENT_RESULT_FAILED);
            return null;
        }
    }

    private static String getMetaFieldValue(PaymentInformationDTO instrument, String metaFieldName){
    	String brainTreeCustomerId = "";

    	for (MetaFieldValue metaFieldValue : instrument.getMetaFields()) {
            if(metaFieldValue.getField().getName().equalsIgnoreCase(metaFieldName) 
            		&& metaFieldValue.getValue() != null && !metaFieldValue.isEmpty()){
            	brainTreeCustomerId = (String) metaFieldValue.getValue();
            }

        }
        return brainTreeCustomerId;
    }

    /**
     *
     */
    public char[] deleteCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
        //noop
        return null;
    }
}


package com.sapienter.jbilling.server.payment.tasks.paypal;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import paypal.payflow.ACHTender;
import paypal.payflow.BankAcct;
import paypal.payflow.BillTo;
import paypal.payflow.CreditTransaction;
import paypal.payflow.Currency;
import paypal.payflow.Invoice;
import paypal.payflow.PayflowConnectionData;
import paypal.payflow.PayflowConstants;
import paypal.payflow.PayflowUtility;
import paypal.payflow.Response;
import paypal.payflow.SDKProperties;
import paypal.payflow.SaleTransaction;
import paypal.payflow.TransactionResponse;
import paypal.payflow.UserInfo;
import paypal.payflow.VoidTransaction;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValueDAS;
import com.sapienter.jbilling.server.payment.IExternalACHStorage;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.BankAccount;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.PaymentAction;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

public class PayflowExternalACHTask extends PaymentTaskWithTimeout implements
		IExternalACHStorage {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String PRODUCTION_ENV = "Live";
	private static final String PROD_ADDRESS = "payflowpro.paypal.com";
	private static final String SANDBOX_ADDRESS = "pilot-payflowpro.paypal.com";
	private static final String AUTH_TYPE = "PPD";

	private static final PaymentInformationBL piBl = new PaymentInformationBL();

	/* Plugin parameters */
	public static final ParameterDescription PARAMETER_PAYFLOW_USER_ID = new ParameterDescription(
			"PayflowUserId", true, ParameterDescription.Type.STR);
	public static final ParameterDescription PARAMETER_PAYFLOW_PASSWORD = new ParameterDescription(
			"PayflowPassword", true, ParameterDescription.Type.STR);
	public static final ParameterDescription PARAMETER_PAYFLOW_VENDOR = new ParameterDescription(
			"PayflowVendor", true, ParameterDescription.Type.STR);
	public static final ParameterDescription PARAMETER_PAYFLOW_ENVIRONMENT = new ParameterDescription(
			"PayflowEnvironment", false, ParameterDescription.Type.STR);
	public static final ParameterDescription PARAMETER_PAYFLOW_PARTNER = new ParameterDescription(
			"PayflowPartner", false, ParameterDescription.Type.STR);
	public String getUserId() throws PluggableTaskException {
		return ensureGetParameter(PARAMETER_PAYFLOW_USER_ID.getName());
	}

	public String getPassword() throws PluggableTaskException {
		return ensureGetParameter(PARAMETER_PAYFLOW_PASSWORD.getName());
	}

	public String getVendor() throws PluggableTaskException {
		return ensureGetParameter(PARAMETER_PAYFLOW_VENDOR.getName());
	}

	public String getEnvironment() {
		return getOptionalParameter(PARAMETER_PAYFLOW_ENVIRONMENT.getName(),
				PRODUCTION_ENV);
	}

	public String getPartner() {
		return getOptionalParameter(PARAMETER_PAYFLOW_PARTNER.getName(),
				"PayPal");
	}

	// initializer for pluggable params
	{
		descriptions.add(PARAMETER_PAYFLOW_USER_ID);
		descriptions.add(PARAMETER_PAYFLOW_PASSWORD);
		descriptions.add(PARAMETER_PAYFLOW_VENDOR);
		descriptions.add(PARAMETER_PAYFLOW_ENVIRONMENT);
		descriptions.add(PARAMETER_PAYFLOW_PARTNER);
		
        SDKProperties.setHostPort(443);
        SDKProperties.setTimeOut(45);
        SDKProperties.setLogFileName("logs/payflow_java.log");
        SDKProperties.setLoggingLevel(PayflowConstants.SEVERITY_DEBUG);
        SDKProperties.setMaxLogFileSize(100000);
	}

	private UserInfo getUserInfo() throws PluggableTaskException {
		return new UserInfo(getUserId(), getVendor(), getPartner(),
				getPassword());
	}

	private PayflowConnectionData getConnection() {
		if (PRODUCTION_ENV.equals(getEnvironment())) {
			SDKProperties.setHostAddress(PROD_ADDRESS);
			return new PayflowConnectionData(PROD_ADDRESS, 443, 45);
		}
		SDKProperties.setHostAddress(SANDBOX_ADDRESS);
		return new PayflowConnectionData(SANDBOX_ADDRESS, 443, 45);
	}

	public void prepareExternalPayment(PaymentDTOEx payment) {
		if (piBl.useGatewayKey(payment.getInstrument())) {
			logger.debug("bank account number is obscured, retrieving from database to use external store.");
			if (payment.getInstrument().getId() != null) {
				// load only if its saved in database. Otherwise do not
				payment.setInstrument(new PaymentInformationDAS().find(payment
						.getInstrument().getId()));
			}
		} else {
			logger.debug("new bank account or previously un-obscured, using as is.");
		}
	}

	public void updateGatewayKey(PaymentDTOEx payment) {
		PaymentAuthorizationDTO auth = payment.getAuthorization();
		// update the gateway key with the returned PayPal TRANSACTIONID
		PaymentInformationDTO ach = payment.getInstrument();

		piBl.updateCharMetaField(ach, auth.getTransactionId().toCharArray(),
				MetaFieldType.GATEWAY_KEY);

		// obscure new bank account numbers
		if (!com.sapienter.jbilling.common.Constants.PAYMENT_METHOD_GATEWAY_KEY
				.equals(ach.getPaymentMethod().getId()))
			piBl.obscureBankAccountNumber(ach);
	}

	private static boolean isApplicable(PaymentDTOEx payment) {
        if (piBl.isBTPayment(payment.getInstrument())) {
            logger.info("processing payment using unified BT payment since 'BT ID' & 'Type' is provided");
            return false;
        }
		if (piBl.isACH(payment.getInstrument())) {
			return true;
		}
        logger.warn("Can't process without ach");
		return false;
	}

	@Override
	public boolean process(PaymentDTOEx paymentInfo)
			throws PluggableTaskException {
		logger.debug("Payment processing for PayPal payflow gateway");

		if (paymentInfo.getPayoutId() != null) {
			return true;
		}

		if (!isApplicable(paymentInfo)) {
			return NOT_APPLICABLE.shouldCallOtherProcessors();
		}

		prepareExternalPayment(paymentInfo);
		return doProcess(paymentInfo, PaymentAction.SALE, true);
	}

	private boolean doProcess(PaymentDTOEx payment,
			PaymentAction paymentAction, boolean updateKey)
			throws PluggableTaskException {

		if (isRefund(payment)) {
			return doRefund(payment).shouldCallOtherProcessors();
		}
		if (piBl.useGatewayKey(payment.getInstrument())) {
			return doPaymentWithACHStored(payment, paymentAction)
					.shouldCallOtherProcessors();
		}

		return doPaymentWithoutStoredACH(payment, paymentAction, updateKey)
				.shouldCallOtherProcessors();
	}

	private static boolean isRefund(PaymentDTOEx payment) {
		return BigDecimal.ZERO.compareTo(payment.getAmount()) > 0
				|| payment.getIsRefund() != 0;
	}

	private Result doRefund(PaymentDTOEx payment) throws PluggableTaskException {
		String gateWayKey = new String(piBl.getCharMetaFieldByType(
				payment.getInstrument(), MetaFieldType.GATEWAY_KEY));
		if (gateWayKey == null || gateWayKey.trim().length() == 0) {
			return NOT_APPLICABLE;
		}
		Invoice invoice = new Invoice();
		Currency currency = new Currency(payment.getAmount().doubleValue());
		
		invoice.setAmt(currency);
		CreditTransaction creditTransaction = new CreditTransaction(gateWayKey,
				getUserInfo(), getConnection(), invoice, PayflowUtility.getRequestId());
		Response response = creditTransaction.submitTransaction();
		if (response == null) {
			return NOT_APPLICABLE;
		}
		TransactionResponse txResponse = response.getTransactionResponse();
		if (txResponse == null) {
			return NOT_APPLICABLE;
		}
		PaymentAuthorizationDTO paymentAuthorizationDTO = buildPaymentAuthorization(txResponse);
		storePaypalResult(txResponse, payment, paymentAuthorizationDTO, false);
		
		return new Result(paymentAuthorizationDTO, false);
	}

	private Result doPaymentWithACHStored(PaymentDTOEx payment,
			PaymentAction paymentAction) throws PluggableTaskException {
		try {
			Invoice invoice = addBillingInformation(payment);
			if(invoice == null){
				invoice = new Invoice();
			}
            Currency currency = new Currency(payment.getAmount().doubleValue());
			
			invoice.setAmt(currency);
            BankAccount bankAccount = converBankAccount(payment.getInstrument());
            BankAcct bankAcct = new BankAcct(bankAccount.getAccountNumber(), bankAccount.getRoutingNumber());
            bankAcct.setAcctType(bankAccount.getAccountType());
            bankAcct.setName(bankAccount.getCustomerName());
            ACHTender ach = new ACHTender(bankAcct);
            SaleTransaction saleTransaction = new SaleTransaction(getUserInfo(), getConnection(), invoice, ach, PayflowUtility.getRequestId());
			 Response response = saleTransaction.submitTransaction();
			 if (response == null) {
					return NOT_APPLICABLE;
				}
			 TransactionResponse txResponse = response.getTransactionResponse();
				if (txResponse == null) {
					return NOT_APPLICABLE;
				}
			 PaymentAuthorizationDTO paymentAuthorizationDTO = buildPaymentAuthorization(txResponse);
				storePaypalResult(txResponse, payment, paymentAuthorizationDTO, true);
				
				return new Result(paymentAuthorizationDTO, false);
		} catch (Exception e) {
			logger.error("Couldn't handle payment request due to error", e);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
		}
	}

	private Result doPaymentWithoutStoredACH(PaymentDTOEx payment,
			PaymentAction paymentAction, boolean updateKey)
			throws PluggableTaskException {
		try {
			Invoice invoice = addBillingInformation(payment);
			if(invoice == null){
				invoice = new Invoice();
			}
			Currency currency = new Currency(payment.getAmount().doubleValue());
			invoice.setAmt(currency);
			BankAccount bankAccount = converBankAccount(payment.getInstrument());
			BankAcct bankAcct = new BankAcct(bankAccount.getAccountNumber(), bankAccount.getRoutingNumber());
			bankAcct.setAcctType(bankAccount.getAccountType());
			bankAcct.setName(bankAccount.getCustomerName());
			ACHTender ach = new ACHTender(bankAcct);
	        ach.setAuthType(AUTH_TYPE);
	        SaleTransaction saleTransaction = new SaleTransaction(getUserInfo(), getConnection(), invoice, ach, PayflowUtility.getRequestId());
	        Response response = saleTransaction.submitTransaction();
	        if (response == null) {
				return NOT_APPLICABLE;
			}
	        
	        TransactionResponse txResponse = response.getTransactionResponse();
			if (txResponse == null || txResponse.getResult() != 0) {
				return NOT_APPLICABLE;
			}
			logger.debug("Transaction response recieved {}",txResponse.getRespMsg());
			
			PaymentAuthorizationDTO paymentAuthorizationDTO = buildPaymentAuthorization(txResponse);
			storePaypalResult(txResponse, payment, paymentAuthorizationDTO, updateKey);
			
			return new Result(paymentAuthorizationDTO, false);
		} catch (Exception e) {
			logger.error("Couldn't handle payment request due to error", e);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
		}
	}

	private PaymentAuthorizationDTO buildPaymentAuthorization(
			TransactionResponse txResponse) {
		logger.debug("Payment authorization result of PayPal gateway parsing....");

		PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
		paymentAuthDTO.setProcessor("PayPal");

		String txID = txResponse.getPnref();
		if (txID != null) {
			paymentAuthDTO.setTransactionId(txID);
			paymentAuthDTO.setCode1(txID);
			logger.debug("transactionId/code1 [{}]", txID);
		}
		if (0 != txResponse.getResult()) {
			logger.debug("Transaction failed {}", txResponse.getRespMsg());
		}
		paymentAuthDTO.setResponseMessage(txResponse.getRespMsg());

		return paymentAuthDTO;
	}

	private void storePaypalResult(TransactionResponse txResponse, PaymentDTOEx payment,
			PaymentAuthorizationDTO paymentAuthorization, boolean updateKey) {
		PaymentAuthorizationBL authorizationBL = new PaymentAuthorizationBL();
        if (!piBl.isCreditCardObscurred(payment.getInstrument())) {
            piBl.updatePaymentMethodInPaymentInformation(payment.getInstrument());
        }
		if (txResponse.getResult() == 0) {
			payment.setPaymentResult(new PaymentResultDAS()
					.find(Constants.RESULT_OK));
			authorizationBL.create(paymentAuthorization,
					payment.getId());
			payment.setAuthorization(paymentAuthorization);
			if (updateKey) {
				updateGatewayKey(payment);
			}
		} else {
			payment.setPaymentResult(new PaymentResultDAS()
					.find(Constants.RESULT_FAIL));
			authorizationBL.create(paymentAuthorization, payment.getId());
			payment.setAuthorization(paymentAuthorization);
		}
	}

	private static BankAccount converBankAccount(PaymentInformationDTO instrument){
		BankAccount bankAccount = new BankAccount();
		MetaFieldValue<String> metaAccountEncrypted = null;
		for(MetaFieldValue metaFieldValue: instrument.getMetaFields()){

            if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.BANK_ROUTING_NUMBER)){
                bankAccount.setRoutingNumber(new String((char[]) metaFieldValue.getValue()));
            } else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.BANK_ACCOUNT_TYPE)){
                bankAccount.setAccountType(((String) metaFieldValue.getValue()).substring(0, 1));
            }else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED)){
                metaAccountEncrypted = metaFieldValue;
            } else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.BANK_ACCOUNT_NUMBER)){
                bankAccount.setAccountNumber(new String((char[]) metaFieldValue.getValue()));
            }else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.TITLE)){
                bankAccount.setCustomerName((String) metaFieldValue.getValue());
            }

		}
		if(metaAccountEncrypted!=null && metaAccountEncrypted.getValue()!=null){
			bankAccount.setAccountNumber(PaymentInformationBL.decryptString((String) metaAccountEncrypted.getValue()));
		}
		return bankAccount;
	}
	@Override
	public void failure(Integer userId, Integer retry) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean preAuth(PaymentDTOEx paymentInfo)
			throws PluggableTaskException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean confirmPreAuth(PaymentAuthorizationDTO auth,
			PaymentDTOEx paymentInfo) throws PluggableTaskException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String storeACH(ContactDTO contact, PaymentInformationDTO paymentInstrument, boolean updateKey) {
		logger.debug("Storing ach info within PayPal gateway");
        UserDTO user;
        if (contact != null) {
            UserBL bl = new UserBL(contact.getUserId());
            user = bl.getEntity();
            // can not get ach from db as there may be many
        } else if (paymentInstrument != null && paymentInstrument.getUser() != null) {
            user = paymentInstrument.getUser();
        } else {
            logger.error("Could not determine user id for external ach storage");
            return null;
        }

        // new contact that has not had an ach created yet
        if (paymentInstrument == null) {
            logger.warn("No ach to store externally.");
            return null;
        }

        /*  Note, don't use PaymentBL.findPaymentInstrument() as the given ach is still being
            processed at the time that this event is being handled, and will not be found.

            PaymentBL()#create() will cause a stack overflow as it will attempt to update the ach,
            emitting another UpdateACHEvent which is then handled by this method and repeated.
         */
        PaymentDTO payment = new PaymentDTO();
        payment.setBaseUser(user);
        payment.setCurrency(user.getCurrency());
        payment.setAmount(CommonConstants.BIGDECIMAL_ONE_CENT);
        if("ACH".equals(paymentInstrument.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName())){
        	payment.setPaymentMethod(new PaymentMethodDAS().find(Constants.PAYMENT_METHOD_ACH));
        }else{
            logger.warn("No ach to store externally.");
            return null;
        }
        paymentInstrument.setPaymentMethod(payment.getPaymentMethod());
        payment.setIsRefund(0);
        payment.setIsPreauth(1);
        payment.setDeleted(1);
        payment.setAttempt(1);
        payment.setPaymentDate(companyCurrentDate());
        payment.setCreateDatetime(TimezoneHelper.serverCurrentDate());

        PaymentDTOEx paymentEx = new PaymentDTOEx(new PaymentDAS().save(payment));
        paymentEx.setInstrument(paymentInstrument);
        try {
            doProcess(paymentEx, PaymentAction.AUTHORIZATION, updateKey);
            PaymentAuthorizationDTO auth = paymentEx.getAuthorization();
            if(auth == null){
            	return null;
            }
            doVoid(paymentEx);
            updatePaymentResult(paymentEx, Constants.PAYMENT_RESULT_SUCCESSFUL);
            return auth.getTransactionId();
        } catch (PluggableTaskException e) {
            logger.error("Could not process external storage payment", e);
            updatePaymentResult(paymentEx, Constants.PAYMENT_RESULT_FAILED);
            return null;
        }
	}
	
	private void doVoid(PaymentDTOEx payment) throws PluggableTaskException {
        try {
        	
        	VoidTransaction voidTransaction = new VoidTransaction(payment.getAuthorization().getTransactionId(),
    				getUserInfo(), getConnection(), PayflowUtility.getRequestId());
    		voidTransaction.submitTransaction();
        	
        } catch (Exception e) {
            logger.error("Couldn't void payment authorization due to error", e);
            throw new PluggableTaskException(e);
        }
    }
	@Override
	public String deleteACH(ContactDTO contact, PaymentInformationDTO instrument) {
		// TODO Auto-generated method stub
		return null;
	}


    private static Invoice addBillingInformation(PaymentDTOEx payment){
        Invoice invoice = new Invoice();
        BillTo billTo = new BillTo();

        UserWS userWS=new UserBL(payment.getUserId()).getUserWS();

        Integer entityId= userWS.getEntityId();
        String billingGroupNameValue = new MetaFieldDAS().getComapanyLevelMetaFieldValue(
                MetaFieldName.PAYPAL_BILLING_GROUP_NAME.getMetaFieldName(), entityId);

        AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
        MetaFieldGroup metaFieldGroup = accountInformationTypeDAS.getAccountInformationTypeByName(entityId
                , userWS.getAccountTypeId(), billingGroupNameValue);

        if( null == metaFieldGroup || Integer.valueOf(metaFieldGroup.getId()) == null){
        	return null;
        }
        int metaFieldGroupId = metaFieldGroup.getId();
        for (MetaFieldValueWS metaFieldValueWS:userWS.getMetaFields()){
            MetaFieldValue metaFieldValue=new MetaFieldValueDAS().find(metaFieldValueWS.getId());

            if (metaFieldValueWS.getGroupId() != null && metaFieldGroupId == metaFieldValueWS.getGroupId().intValue()){
                if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.ORGANIZATION)){
                    billTo.setFirstName(metaFieldValueWS.getStringValue());
                }else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.LAST_NAME)){
                    billTo.setLastName(metaFieldValueWS.getStringValue());
                }
                else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.ADDRESS1)){
                    billTo.setStreet(metaFieldValueWS.getStringValue());
                }else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.CITY)){
                    billTo.setCity(metaFieldValueWS.getStringValue());
                }else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.STATE_PROVINCE)){
                    billTo.setState(metaFieldValueWS.getStringValue());
                }else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.COUNTRY_CODE)){
                    billTo.setBillToCountry(metaFieldValueWS.getStringValue());
                }else if(metaFieldValue.getField().getFieldUsage() == (MetaFieldType.POSTAL_CODE)){
                    billTo.setZip(metaFieldValueWS.getStringValue());
                }
            }

            if (metaFieldValue.getField().getFieldUsage() == (MetaFieldType.BILLING_EMAIL)){
                billTo.setEmail(metaFieldValueWS.getStringValue());
            }

        }
        invoice.setBillTo(billTo);
        return invoice;
    }
}

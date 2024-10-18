/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

/**
 * Created by Mahesh Shivarkar on 02/04/2015.
 */

package com.sapienter.jbilling.server.payment.tasks.paypal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import paypal.payflow.Currency;
import paypal.payflow.ECDoRequest;
import paypal.payflow.Invoice;
import paypal.payflow.PayPalTender;
import paypal.payflow.PayflowConnectionData;
import paypal.payflow.PayflowUtility;
import paypal.payflow.Response;
import paypal.payflow.SaleTransaction;
import paypal.payflow.TransactionResponse;
import paypal.payflow.UserInfo;

import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.PaymentAction;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Constants;

public class PaymentPaypalExpressCheckoutTask extends PaymentTaskWithTimeout {

	private static final Logger logger = LoggerFactory.getLogger(PaymentPaypalExpressCheckoutTask.class);

	/* Plugin parameters */
	/**
	 * PayPal UserId 
	 */
	public static final ParameterDescription PARAMETER_PAYFLOW_USER_ID = new ParameterDescription(
			"PayflowUserId", true, ParameterDescription.Type.STR);
	/**
	 * PayPal Password 
	 */
	public static final ParameterDescription PARAMETER_PAYFLOW_PASSWORD = new ParameterDescription(
			"PayflowPassword", true, ParameterDescription.Type.STR);
	/**
	 * Vendor 
	 */
	public static final ParameterDescription PARAMETER_PAYFLOW_VENDOR = new ParameterDescription(
			"PayflowVendor", true, ParameterDescription.Type.STR);
	/**
	 * Environment Live/Test 
	 */
	public static final ParameterDescription PARAMETER_PAYFLOW_ENVIRONMENT = new ParameterDescription(
			"PayflowEnvironment", false, ParameterDescription.Type.STR);
	/**
	 * Partner 
	 */
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
				Constants.PAYPAL_PAYFLOW_ENVIRONMENT);
	}

	public String getPartner() {
		return getOptionalParameter(PARAMETER_PAYFLOW_PARTNER.getName(),
				Constants.PAYPAL_PAYFLOW_PARTNER);
	}

	// initializer for pluggable params
	{
		descriptions.add(PARAMETER_PAYFLOW_USER_ID);
		descriptions.add(PARAMETER_PAYFLOW_PASSWORD);
		descriptions.add(PARAMETER_PAYFLOW_VENDOR);
		descriptions.add(PARAMETER_PAYFLOW_ENVIRONMENT);
		descriptions.add(PARAMETER_PAYFLOW_PARTNER);
	}

	// Create the Data Objects.
    // Create the User data object with the required user details.
	private UserInfo getUserInfo() throws PluggableTaskException {
		return new UserInfo(getUserId(), getVendor(), getPartner(),
				getPassword());
	}

	// Create the Payflow Connection data object with the required connection details.
	private PayflowConnectionData getConnection() {
		if (!getEnvironment().isEmpty() && Constants.PAYPAL_PAYFLOW_ENVIRONMENT.equals(getEnvironment())) {
			return new PayflowConnectionData("payflowpro.paypal.com", 443, 45);
		}
		return new PayflowConnectionData("pilot-payflowpro.paypal.com", 443, 45);
	}

	@Override
	public boolean process(PaymentDTOEx paymentInfo)
			throws PluggableTaskException {
		logger.debug("Payment processing for PayPal payflow gateway");

		if (!isApplicable(paymentInfo)) {
			return NOT_APPLICABLE.shouldCallOtherProcessors();
		}

		return doProcess(paymentInfo, PaymentAction.SALE);
	}
	
	private boolean doProcess(PaymentDTOEx payment,
			PaymentAction paymentAction)
			throws PluggableTaskException {
	
		return doPaymentWithPaypalAccount(payment, paymentAction)
				.shouldCallOtherProcessors();
	}
		
	private Result doPaymentWithPaypalAccount(PaymentDTOEx payment,
			PaymentAction paymentAction)
			throws PluggableTaskException {
		try {	
		
	    // For Express Checkout Payment with Paypal account use ECDoRequest.
		logger.debug("Paypal Express checkout TOKEN : {}", getTokenValue(payment));
		logger.debug("Paypal Express checkout PAYER_ID {}: ", getPayerIdValue(payment));
	    
		ECDoRequest doRequest = new ECDoRequest(
	    		getTokenValue(payment), 
	    		getPayerIdValue(payment));
	    
	    // Create a new Invoice data object with the Amount, Billing Address etc. details.
	    Invoice invoice = new Invoice();
	    
	    // Set Amount.
	    Currency currency = new Currency(payment.getAmount().doubleValue());
	    invoice.setAmt(currency);
	    logger.debug(" Payment amount : {}", currency);
	    // Create the Tender object.
	    PayPalTender paypalTender = new PayPalTender(doRequest);
	
	    // Create the transaction object.
	    SaleTransaction transaction = new SaleTransaction(
	    		getUserInfo(), getConnection(), invoice, paypalTender, PayflowUtility.getRequestId());
	    
	    // Submit the transaction.
	    Response response = transaction.submitTransaction();
	    logger.debug(" Response : {}", response);
	    
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
			logger.debug(" Error in authenticating Paypal account: {}",e);
			throw new PluggableTaskException("Error in authenticating Paypal account.", e);
		}
	}	
		
	private static boolean isApplicable(PaymentDTOEx payment) {
		PaymentInformationBL piBl = new PaymentInformationBL();
        if (piBl.isBTPayment(payment.getInstrument())) {
            logger.info("processing payment using unified BT payment since 'BT ID' & 'Type' is provided");
            return false;
        }
		if (piBl.isExpressCheckoutPayment(payment)) {
            return true;
		}
        logger.warn("Can't process without paypal account ");
        return false;
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
		
		if (txResponse.getResult() == 0) {
			payment.setPaymentResult(new PaymentResultDAS()
					.find(Constants.RESULT_OK));
			new PaymentAuthorizationBL().create(paymentAuthorization,
					payment.getId());
			payment.setAuthorization(paymentAuthorization);
			
		} else {
			payment.setPaymentResult(new PaymentResultDAS()
					.find(Constants.RESULT_FAIL));
		}
	}
	
	// Returns Token value received from Paypal in response 
	private static String getTokenValue(PaymentDTOEx paymentInfo){
		
		if (null == paymentInfo){
			return null;
		}
		
		for(PaymentInformationDTO dto: paymentInfo.getPaymentInstruments()){
			for (MetaFieldValue metaFieldValue : dto.getMetaFields()) {
				if(metaFieldValue.getField().getName().equals(Constants.TOKEN)){
	        		return (String) metaFieldValue.getValue();
	            }
			}
		}
		return null;
	}
	
	// Returns unique Payer Id value received from Paypal in response
	private static String getPayerIdValue(PaymentDTOEx paymentInfo){
		
		if (null == paymentInfo){
			return null;
		}
		
		for(PaymentInformationDTO dto: paymentInfo.getPaymentInstruments()){
			for (MetaFieldValue metaFieldValue : dto.getMetaFields()) {
				if(metaFieldValue.getField().getName().equals(Constants.PAYER_ID)){
	        		return (String) metaFieldValue.getValue();
	            }
			}
		}
		return null;
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
}

package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.ep2.*;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Constants;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Krunal Bhavsar
 * 
 */
public class PaymentEP2Task extends PaymentTaskWithTimeout {

	
	  /* Plugin parameters */
	public static final ParameterDescription PARAMETER_EP2_MERCHANT_ID =
	    	new ParameterDescription("MerchantId", true, ParameterDescription.Type.STR, false);
	
    public static final ParameterDescription PARAMETER_EP2_USERNAME =
    	new ParameterDescription("UserName", true, ParameterDescription.Type.STR, false);
    public static final ParameterDescription PARAMETER_EP2_PASSWORD =
    	new ParameterDescription("Password", true, ParameterDescription.Type.STR, true);
    
    public static final ParameterDescription PARAMETER_EP2_URL =
        	new ParameterDescription("url", true, ParameterDescription.Type.STR, false);
    
    //initializer for pluggable params
    {
    	descriptions.add(PARAMETER_EP2_MERCHANT_ID);
    	descriptions.add(PARAMETER_EP2_USERNAME);
    	descriptions.add(PARAMETER_EP2_PASSWORD);
    	descriptions.add(PARAMETER_EP2_URL);
    }
    

    public  String getUserName() {
    	return  parameters.get(PARAMETER_EP2_USERNAME.getName());
    }
    
    public  String getPassword() {
    	return  parameters.get(PARAMETER_EP2_PASSWORD.getName());
    }
    
    public  String getMerchantId() {
    	return  parameters.get(PARAMETER_EP2_MERCHANT_ID.getName());
    }
    
    public  String getUrl() {
    	return  parameters.get(PARAMETER_EP2_URL.getName());
    }
    
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PaymentEP2Task.class));

    /**
     * Creation of a payment
     *
     * @param paymentInfo This can be an extension of PaymentDTO, with the
     *                    additional information for this implementation. For example, a task
     *                    fro credit card processing would expect an extension of PaymentDTO with
     *                    the credit card information.
     * @return If the next pluggable task has to be called or not. True would
     * be returned usually when the gatway is not available.
     */
	@Override
	public boolean process(PaymentDTOEx paymentInfo) throws PluggableTaskException {
    	return processPaymentByTokenId(paymentInfo).shouldCallOtherProcessors();
    }

	/**
	 * creates Ep2Result from gateWay Response.
	 * @param response
	 * @return 
	 * @throws Exception
	 */
    private Ep2Result buildResultFromResponse(String response) throws Exception {
    	String status = EP2ContentManager.getValueByElementTag(response, ResponseParameters.TRANSACTION_STATE.toString());
    	
    	String transactionId = EP2ContentManager.getValueByElementTag(response, ResponseParameters.TRANSACTION_ID.toString());
    	
    	String tokenId = EP2ContentManager.getValueByElementTag(response, ResponseParameters.TOKEN_ID.toString());
    	
    	String description = EP2ContentManager.getValueByElementTagAndAttributeName(response, "status", 
    			ResponseParameters.STATUS_DESCRIPTION.toString());
    	
    	String responseCode = EP2ContentManager.getValueByElementTagAndAttributeName(response, "status", 
    			ResponseParameters.STATUS_CODE.toString());
    	
    	String approvalCode = status.equalsIgnoreCase("success") ? 
    			EP2ContentManager.getValueByElementTag(response, ResponseParameters.AUTHORIZATION_CODE.toString()) : null ;
    	
    	Ep2Result result = new Ep2Result()
    					.addSucceeded(status.equalsIgnoreCase("success"))
    					.addDescription(description.split(":").length == 2 ? description.split(":")[1] : description)
    					.addResponseCode(responseCode)
    					.addTransactionId(transactionId)
    					.addTokenId(tokenId)
    					.addApprovalCode(approvalCode);
    	return result;
    }
    
    /**
     * sends payment request to the Ep2GateWay 
     * @param payment
     * @return
     */
    private Result processPaymentByTokenId(PaymentDTOEx payment) {
    	try {
    	    LOG.debug("Sending Payment request for User: " + payment.getUserId() + " on Ep2 Gateway");
    	    
    	    if(null!=payment.getInvoiceIds() && !payment.getInvoiceIds().isEmpty()) {
    	        LOG.debug("Processing Payment for User: " + payment.getUserId() + ", Paying Invoices:" + payment.getInvoiceIds());
    	    }
    		Ep2Payer payer = convertPayer(payment);
    		String xmlRequestContent = EP2ContentManager.createXMLContent(EP2ContentManager.createPropertyDetailsFromEp2Payer(payer));
    		String xmlResponseBody = sendPaymentRequestToGateWay(xmlRequestContent);
    		Ep2Result result = buildResultFromResponse(xmlResponseBody);
    		PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(result);
    		storeEp2Result(result, payment, paymentAuthorization, false);
    		return new Result(paymentAuthorization, false);
    	} catch(Exception ex) {
    		 LOG.error("Couldn't handle payment request due to error", ex);
             payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
             return NOT_APPLICABLE;
    	}
    }

    /**
     * stores Ep2GateWay Result in  DataBase
     * @param result
     * @param payment
     * @param paymentAuthorization
     * @param updateKey
     */
    private void storeEp2Result(Ep2Result result, PaymentDTOEx payment,
    		PaymentAuthorizationDTO paymentAuthorization, boolean updateKey) {
    	if(result.isSucceeded()) {
    		payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_OK));
    		LOG.debug("Payment Passed on Ep2 Gateway for User: " + payment.getUserId() + ", Payment Amount was: " + payment.getAmount());
    		if(updateKey) {
    			updateGatewayKey(payment, result.getTokenId());
    		}
    	} else {
    	    LOG.debug("Payment Failed on Ep2 Gateway for User: " + payment.getUserId() + ", Payment Amount was: " + payment.getAmount() + ", Faliure Reason: "+result.getDescription());
    		payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_FAIL));
    	}
    	new PaymentAuthorizationBL().create(paymentAuthorization, payment.getId());
		payment.setAuthorization(paymentAuthorization);
    }
    
    /**
     * Updates the gateway key of the credit card associated with this payment. PayPal
     * returns a TRANSACTIONID which can be used to start new transaction without specifying
     * payer info.
     *
     * @param payment successful payment containing the credit card to update.
     *  */
    public void updateGatewayKey(PaymentDTOEx payment, String tokenId) {
        PaymentInformationBL piBl = new PaymentInformationBL();
        // update the gateway key with the returned Ep2 TRANSACTIONID
        PaymentInformationDTO card = payment.getInstrument();
        
        piBl.updateCharMetaField(card, tokenId.toCharArray(), MetaFieldType.GATEWAY_KEY);

        // obscure new credit card numbers
        if (!com.sapienter.jbilling.common.Constants.PAYMENT_METHOD_GATEWAY_KEY.equals(card.getPaymentMethod().getId()))
            piBl.obscureCreditCardNumber(card);
    }
    
    /**
     * Sends Payment Request to Ep2 GateWay and returns xml response 
     * in form of String to the caller.
     * @param xmlRequestContent
     * @return
     * @throws Exception
     */
	private String sendPaymentRequestToGateWay(String xmlRequestContent) throws Exception {
    	
    	HttpClient client = new HttpClient();
    	client.getHttpConnectionManager().getParams().setConnectionTimeout(1000 * getTimeoutSeconds());
		Credentials defaultCredentials = new UsernamePasswordCredentials(getUserName(), getPassword());
		client.getState().setCredentials(AuthScope.ANY, defaultCredentials);

		PostMethod post = new PostMethod(getUrl());

		StringRequestEntity requestEntity = new StringRequestEntity(xmlRequestContent, "text/xml", "UTF-8");
		post.setRequestEntity(requestEntity);

		client.executeMethod(post);
		return post.getResponseBodyAsString();
    }
    
    @Override
    public void failure(Integer userId, Integer retry) {

    }

    /**
     * Does the authorization, but not capture, of a payment. This means that
     * the amount is approved, but if this charge is not confirmed within X
     * number of days, the charge will be dropped and the credit card not charged.
     * The way to confirm the charge is by calling ConfirmPreAuth
     *
     * @param paymentInfo This object needs to have
     *                    - currency
     *                    - amount
     *                    - credit card
     *                    - the id of the existing payment row
     * @return If the next pluggable task has to be called or not. True would
     * be returned usually when the gatway is not available.
     * @throws PluggableTaskException
     */
    @Override
    public boolean preAuth(PaymentDTOEx paymentInfo) throws PluggableTaskException {
        return false;
    }

    /**
     * This will confirm a previously authorized charge, so it is 'captured'.
     * If this method is not called in a pre-auth, the charge will be dropped.
     * By calling this method, the end customer will see the charge in her
     * credit card.
     *
     * @param auth
     * @param paymentInfo
     * @return If the next pluggable task has to be called or not. True would
     * be returned usually when the gatway is not available.
     * @throws PluggableTaskException
     */
    @Override
    public boolean confirmPreAuth(PaymentAuthorizationDTO auth, PaymentDTOEx paymentInfo) throws PluggableTaskException {
        return false;
    }

 
    /**
     * Returns the name of this payment processor.
     * @return payment processor name
     */
    private String getProcessorName() {
        return "Ep2";
    }
    
    private PaymentAuthorizationDTO buildPaymentAuthorization(Ep2Result result) {
        LOG.debug("Payment authorization result of " + getProcessorName() + " gateway parsing....");

        PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
        paymentAuthDTO.setProcessor(getProcessorName());

        String txID = result.getTransactionId();
        if (txID != null) {
            paymentAuthDTO.setTransactionId(txID);
            LOG.debug("transactionId/code1 [" + txID + "]");
        }

        String responseCode = result.getResponseCode();
        String responseMessage= result.getDescription();
        if (responseMessage != null) {
            paymentAuthDTO.setResponseMessage(responseMessage);
            LOG.debug("Response Code  [" + responseCode + "]");
            LOG.debug("Response Message [" + responseMessage + "]");
        }
        paymentAuthDTO.setCode1(responseCode);
        paymentAuthDTO.setApprovalCode(result.getApprovalCode());

        return paymentAuthDTO;
    }
    
    private String getTransactionType(PaymentDTOEx payment) {
    	return isRefund(payment) ? "refund-purchase" : "purchase";
    }
    
    private static boolean isRefund(PaymentDTOEx payment) {
        return BigDecimal.ZERO.compareTo(payment.getAmount()) > 0 || payment.getIsRefund() != 0;
    }
    
    private static String getParentTransactionId(PaymentDTOEx payment) {
        return isRefund(payment) ? new PaymentDAS().getTrasactionIdByPayment(payment.getPayment().getId()) : null;
    }
    
    /**
     * returns payer. payer contains all basic information about payment
     * with token id , merchant id and type of the payment(Refund, Payment)
     * if payment is refund then parent-transaction id will be added in payer object
     * @param paymentInfo
     * @return
     */
    private Ep2Payer convertPayer(PaymentDTOEx paymentInfo) {
    	PaymentInformationBL piBl = new PaymentInformationBL();
    	PaymentInformationDTO card = paymentInfo.getInstrument();
    	String expiryDateFieldValue = new String(piBl.getCharMetaFieldByType(card, MetaFieldType.DATE));
    	String expiryYear = null;
    	String expiryMonth = null;
    	if(null!= expiryDateFieldValue && !expiryDateFieldValue.isEmpty() && expiryDateFieldValue.split("/").length ==2) {
    		expiryYear = expiryDateFieldValue.split("/")[1];
    		expiryMonth = expiryDateFieldValue.split("/")[0];
    	}
    	
    	Ep2Payer payer = new Ep2Payer()
    						 .addPaymentMethodName(Ep2PaymentMethod.CREDIT_CARD.getPaymentMethodName())
    						 .addMerchantId(getMerchantId())
    						 .addRquestedAmount(paymentInfo.getAmount().toString())
    						 .addTokenId(new String(piBl.getCharMetaFieldByType(card, MetaFieldType.GATEWAY_KEY)))
    						 .addExpiryMonth(expiryMonth)
    						 .addExpiryYear(expiryYear)
    						 .addRequestedId(UUID.randomUUID().toString())
    						 .addCurrency("AUD")
    						 .addTransactionType(getTransactionType(paymentInfo))
    						 .addParentTransactionId(getParentTransactionId(paymentInfo))
    						 .addOrderNumber("Order-"+paymentInfo.getId());

    	return payer;

    }
    
    /**
     * returns payment information for specified 
     * transaction id
     * @param transactionId
     * @return
     * @throws Exception
     */
    public String getpaymentInfoByTranctionId(String transactionId) throws Exception {
    	
    	String pasredUrlWithTransactionId = new StringBuilder(getUrl().substring(0, getUrl().lastIndexOf("payments")))
												.append("merchants/")
												.append(getMerchantId())
												.append("/payments/")
												.append(transactionId).toString();
    	
    	HttpClient client = new HttpClient();
    	client.getHttpConnectionManager().getParams().setConnectionTimeout(1000 * getTimeoutSeconds());
    	Credentials defaultCredentials = new UsernamePasswordCredentials(getUserName(), getPassword());
    	client.getState().setCredentials(AuthScope.ANY, defaultCredentials);
    	GetMethod get = new GetMethod(pasredUrlWithTransactionId);
    	client.executeMethod(get);
    	return get.getResponseBodyAsString();
    }
    
}

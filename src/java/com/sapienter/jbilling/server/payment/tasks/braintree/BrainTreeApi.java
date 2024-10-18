package com.sapienter.jbilling.server.payment.tasks.braintree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.tasks.braintree.dto.BrainTreeResult;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCard;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.Payer;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

public class BrainTreeApi {
	
	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BrainTreeApi.class));
	
	private String businessId;
	
	private String REMOTE_API;

    private static final String API_BRAINTREE_CAPTURE = "/api/braintree/captureTransaction/%s/%s/%s";

	public BrainTreeApi(String businessId, String remoteApi){
		this.businessId = businessId;
		this.REMOTE_API = remoteApi;
	}
	
	public BrainTreeResult receivePayment(Integer timeout, String customerId, String amount) throws PluggableTaskException {
		
		BrainTreeResult result = null;
		
		try{
			String url = REMOTE_API + "/api/braintree/createTransactionWithCustomerKey/{businessId}/{customerKey}/{amount}/{isAuthorization}";
			
			url = url.replace("{businessId}", businessId).replace("{customerKey}", customerId).replace("{amount}", amount).replace("{isAuthorization}","false");

			LOG.debug("Make Payment URL : " + url);
			
			result = makeApiCall(url, "", timeout);
			result.setPaymentType("payment");
			
		}catch(Exception e){
			LOG.error("Could not handle payment request due to error ", e);
			throw new PluggableTaskException(e);
		}
		
		return result;
		
	}
	
	public BrainTreeResult receiveDirectPayment(Integer timeout, Integer userId, String amount, CreditCard creditCard) throws PluggableTaskException {
		
		BrainTreeResult result = null;
		
		try{
			String url = REMOTE_API + "/api/braintree/createTransactionWithCardDetails/{businessId}/{productKey}/{amount}/{isAuthorization}";
			
			url = url.replace("{businessId}", businessId).replace("{productKey}", String.valueOf(userId)).replace("{amount}", amount).replace("{isAuthorization}","false");
			
			HashMap<String, Object> requestBodyMap = new HashMap<String, Object>();
			
			Map<String, String> cardInfo = new HashMap<String, String>();
			
			cardInfo.put("cardNumber", new String(creditCard.getAccount()));
			cardInfo.put("expirationDate", new String(creditCard.getExpirationDate()));
			cardInfo.put("cvv", new String(creditCard.getCvv2()));
			
			requestBodyMap.put("cardInfo", cardInfo);			

			LOG.debug("Make Payment URL : " + url);
			
			String requestBody = new ObjectMapper().writeValueAsString(requestBodyMap);
			
			result = makeApiCall(url, requestBody, timeout);
			
			result.setPaymentType("payment");
			
		}catch(Exception e){
			LOG.error("Could not handle payment request due to error ", e);
			throw new PluggableTaskException(e);
		}
		
		return result;
		
	}
	
	public String createBTCustomer(CreditCard creditCard, Payer payer, Integer timeout, Integer userId) throws PluggableTaskException {
		
		String customerKey = null;
		
		try{
			
			HashMap<String, Object> requestBodyMap = new HashMap<String, Object>();
			
			HashMap<String, String> customerInfo = new HashMap<String, String>();
			
			customerInfo.put("firstName", payer.getFirstName());
			customerInfo.put("lastName", payer.getLastName());
			customerInfo.put("country", payer.getCountryCode());
			customerInfo.put("postalCode", payer.getZip());
			
			requestBodyMap.put("customerInfo", customerInfo);
			
			HashMap<String, String> cardInfo = new HashMap<String, String>();
			
			cardInfo.put("cardNumber", new String(creditCard.getAccount()));
			cardInfo.put("expirationDate", new String(creditCard.getExpirationDate()));
			cardInfo.put("cvv", new String(creditCard.getCvv2()));
			
			requestBodyMap.put("cardInfo", cardInfo);
			requestBodyMap.put("productKey", userId);
			
			String requestBody = new ObjectMapper().writeValueAsString(requestBodyMap);
			
			String url = REMOTE_API + "/api/braintree/createVaultWithCardDetails/{businessId}/{customerKey}";
			
			customerKey = UUID.randomUUID().toString();
			
			url = url.replace("{businessId}", businessId).replace("{customerKey}", customerKey);
			
			LOG.debug("URL " + url);
			
			BrainTreeResult result = makeApiCall(url, requestBody, timeout);
			
			if(!result.isSucceseded()){
				customerKey = null;
				throw new PluggableTaskException("Unable to create BT Customer Id.");
			}
			
		}catch(Exception e){
			LOG.error("Could not handle payment request due to error ", e);
			throw new PluggableTaskException(e);
		}
		
		
		return customerKey;
		
	}
	
	public String createBTMigrationCustomer(CreditCard creditCard, Payer payer, Integer timeout, Integer userId, String customerKey) throws PluggableTaskException {
		
		try{
			
			HashMap<String, Object> requestBodyMap = new HashMap<String, Object>();
			
			HashMap<String, String> customerInfo = new HashMap<String, String>();
			
			customerInfo.put("firstName", payer.getFirstName());
			customerInfo.put("lastName", payer.getLastName());
			customerInfo.put("country", payer.getCountryCode());
			customerInfo.put("postalCode", payer.getZip());
			
			requestBodyMap.put("customerInfo", customerInfo);
			
			HashMap<String, String> cardInfo = new HashMap<String, String>();
			
			cardInfo.put("cardNumber", new String(creditCard.getAccount()));
			cardInfo.put("expirationDate", new String(creditCard.getExpirationDate()));
			cardInfo.put("cvv", new String(creditCard.getCvv2()));
			
			requestBodyMap.put("cardInfo", cardInfo);
			requestBodyMap.put("productKey", userId);
			
			String requestBody = new ObjectMapper().writeValueAsString(requestBodyMap);
			
			String vaultId = customerKey.split("\\|")[1];
			
			customerKey = customerKey.split("\\|")[0];
			
			String url = REMOTE_API + "/api/braintree/addEntries/{businessId}/{customerKey}/{vaultId}/{productKey}";
			
			url = url.replace("{businessId}", businessId).replace("{customerKey}", customerKey).replace("{vaultId}", vaultId).replace("{productKey}", String.valueOf(userId));
			
			LOG.debug("URL " + url);
			
			BrainTreeResult result = makeApiCall(url, requestBody, timeout);
			
			if(!result.isSucceseded()){
				customerKey = null;
				throw new PluggableTaskException("Unable to create BT Customer Id.");
			}
			
		}catch(Exception e){
			LOG.error("Could not handle payment request due to error ", e);
			customerKey = null;
			throw new PluggableTaskException(e);
		}
		
		
		return customerKey;
		
	}
	
	public BrainTreeResult refund(Integer timeout, String amount, String transactionId) throws PluggableTaskException {
		
		BrainTreeResult result = null;
		
		try{
			
			String url = REMOTE_API + "/api/braintree/refundTransaction/{businessId}/{transactionId}/{amount}";
			
			url = url.replace("{businessId}", businessId).replace("{transactionId}", transactionId).replace("{amount}", amount);
			
			LOG.debug("URL " + url);
			
			result = makeApiCall(url, "", timeout);
			
			result.setPaymentType("refund");
			
			LOG.debug("result.getErrorCode() " + result.getErrorCode());
			
		}catch(Exception e){
			LOG.error("Could not handle refund request due to error ", e);
			throw new PluggableTaskException(e);
		}
		
		return result;
	}
	
	public BrainTreeResult voidPayment(Integer timeout, String transactionId) throws PluggableTaskException {
		
		BrainTreeResult result = null;
		
		try{
			
			String url = REMOTE_API + "/api/braintree/voidTransaction/{businessId}/{transactionId}";
			
			url = url.replace("{businessId}", businessId).replace("{transactionId}", transactionId);
			
			LOG.debug("URL " + url);
			
			result = makeApiCall(url, "", timeout);
			
		}catch(Exception e){
			LOG.error("Could not handle refund request due to error ", e);
			throw new PluggableTaskException(e);
		}
		
		return result;
	}

    public BrainTreeResult capture(Integer timeout, String amount, String transactionId) throws PluggableTaskException {
        try {
            String url = String.format(REMOTE_API + API_BRAINTREE_CAPTURE, businessId, transactionId, amount);
            BrainTreeResult result = makeApiCall(url, "", timeout);
            result.setPaymentType("capture");
            return result;
        } catch (Exception e) {
            throw new PluggableTaskException("Could not handle capture payment request due to error " , e);
        }
    }

	public BrainTreeResult updateCreditCardDetails(String transactionId, String amount, Integer timeout) throws PluggableTaskException {
		BrainTreeResult result = null;
		
		try{
			
			String url = REMOTE_API + "/api/braintree/capture/{businessId}/{transctionId}/{amount}";
			
			url = url.replace("{businessId}", businessId).replace("transactionId", transactionId).replace("{amount}", amount);
			
			result = makeApiCall(url, "", timeout);
			
		}catch(Exception e){
			LOG.error("Could not handle refund request due to error ", e);
			throw new PluggableTaskException(e);
		}
		
		return result;
	}
	
	private BrainTreeResult makeApiCall(String url, String requestBody, Integer timeout) throws IOException {
				
		BrainTreeResult brainTreeResult = new BrainTreeResult();
		
		HttpsURLConnection httpConnection = (HttpsURLConnection) new URL(url).openConnection();			
		httpConnection.setRequestMethod(RequestMethod.POST.toString());
		httpConnection.setDoOutput(true);
		httpConnection.setConnectTimeout(timeout);
		httpConnection.setInstanceFollowRedirects(false);
		httpConnection.setRequestProperty("Content-Type", "application/json");
		
		OutputStreamWriter writer = new OutputStreamWriter(httpConnection.getOutputStream());
		writer.write(requestBody);
		writer.close();
		
		if(httpConnection.getResponseCode() == HttpsURLConnection.HTTP_OK){
			StringBuffer response = new StringBuffer();
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
			String line;
			while((line = reader.readLine()) != null){
				response.append(line);
			}
			
			LOG.debug("Response => " + response.toString());
			
			Map<String, Object> responseMap = new ObjectMapper().readValue(response.toString(), new TypeReference<Map<String,Object>>() {});
			
			brainTreeResult.setSucceseded((Boolean)responseMap.get("status"));
			
			if(responseMap.get("cardInfo") != null){
				Map<String, Object> cardInfoMap = (Map<String, Object>) responseMap.get("cardInfo");
				
				brainTreeResult.setCardNumber((String)cardInfoMap.get("last4"));
				brainTreeResult.setCardType((String)cardInfoMap.get("type"));
				brainTreeResult.setExpiryDate((String)cardInfoMap.get("expirationDate"));
				
			}
									
			brainTreeResult.setAvs((String)responseMap.get("avsPostalCode"));
			
			if(responseMap.get("error") != null){
				Map<String, Object> errorMap = (Map<String, Object>) responseMap.get("error"); 
				brainTreeResult.setErrorCode((String)errorMap.get("validation_error_code"));
				brainTreeResult.setErrorMessage((String)errorMap.get("validation_error_message"));
			}
						
			
			if(responseMap.get("transactionJdo") != null){				
				brainTreeResult.setTransactionId((String)((Map<String, Object>) responseMap.get("transactionJdo")).get("transactionId"));				
			}
			
		}else{
			brainTreeResult.setSucceseded(false);
			brainTreeResult.setErrorMessage("Remote API Error : " + httpConnection.getResponseCode());
		} 
		
		LOG.debug("httpConnection.getResponseCode() " + httpConnection.getResponseCode());
		
		return brainTreeResult;
	}

}

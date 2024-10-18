package com.sapienter.jbilling.server.payment.tasks.unified.braintree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

public class UnifiedBrainTreeApi {

    private static final String MESSAGE = "message";

    private static final String API_BRAINTREE_PROCESS_PAYMENT = "/api/braintree/processPayment/{type}/{customerKey}/{businessId}/{amount}";

    private static final String API_BRAINTREE_REFUND_TRANSACTION = "/api/braintree/refundTransaction/{businessId}/{transactionId}/{amount}/";

    private static final Logger logger = LoggerFactory.getLogger(UnifiedBrainTreeApi.class);

    private String businessId;
    private String remotePaymentAPI;
    private String remoteRefundAPI;

    public UnifiedBrainTreeApi(String businessId, String remotePaymentApi, String remoteRefundApi) {
        this.businessId = businessId;
        this.remotePaymentAPI = remotePaymentApi;
        this.remoteRefundAPI = remoteRefundApi;
    }

    public UnifiedBrainTreeResult receivePayment(Integer timeout, String customerId, String amount, String type)
            throws PluggableTaskException {
        try {
            UnifiedBrainTreeResult result = null;
            String url = remotePaymentAPI + API_BRAINTREE_PROCESS_PAYMENT;

            url = url.replace("{type}", type).replace("{customerKey}", customerId).replace("{businessId}", businessId)
                    .replace("{amount}", amount);

            logger.debug("Make Payment URL : {}", url);

            result = makeApiCall(url, "", timeout);
            result.setPaymentType("payment");

            return result;
        } catch (Exception e) {
            logger.error("Could not handle payment request due to error ", e);
            throw new PluggableTaskException(e);
        }
    }

    public UnifiedBrainTreeResult refund(Integer timeout, String customerId, String amount)
            throws PluggableTaskException {
        try {
            UnifiedBrainTreeResult result = null;
            String url = remoteRefundAPI + API_BRAINTREE_REFUND_TRANSACTION;

            url = url.replace("{businessId}", businessId).replace("{transactionId}", customerId)
                    .replace("{amount}", amount);

            logger.debug("Make Refund URL {}", url);

            result = makeApiCall(url, "", timeout);
            result.setPaymentType("refund");

            logger.debug("result.getErrorCode() {}", result.getErrorCode());

            return result;
        } catch (Exception e) {
            logger.error("Could not handle refund request due to error ", e);
            throw new PluggableTaskException(e);
        }
    }

    private UnifiedBrainTreeResult makeApiCall(String url, String requestBody, Integer timeout) throws IOException {

        UnifiedBrainTreeResult brainTreeResult = new UnifiedBrainTreeResult();

        HttpsURLConnection httpConnection = (HttpsURLConnection) new URL(url).openConnection();
        httpConnection.setRequestMethod(RequestMethod.POST.toString());
        httpConnection.setDoOutput(true);
        httpConnection.setConnectTimeout(timeout);
        httpConnection.setInstanceFollowRedirects(false);
        httpConnection.setRequestProperty("Content-Type", "application/json");

        try (OutputStreamWriter writer = new OutputStreamWriter(httpConnection.getOutputStream())) {
            writer.write(requestBody);
        } catch (Exception e) {
            logger.error("error occurred in stream writer", e);
        }

        if (httpConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            logger.debug("Response => {}", response.toString());

            createResponse(brainTreeResult, response);

        } else {
            brainTreeResult.setSucceseded(false);
            brainTreeResult.setErrorMessage("Remote API Error : " + httpConnection.getResponseCode());
        }

        logger.debug("httpConnection.getResponseCode() {}", httpConnection.getResponseCode());

        return brainTreeResult;
    }

    @SuppressWarnings("unchecked")
    private void createResponse(UnifiedBrainTreeResult brainTreeResult, StringBuilder response) throws IOException,
            JsonParseException, JsonMappingException {
        Map<String, Object> responseMap = new ObjectMapper().readValue(response.toString(),
                new TypeReference<Map<String, Object>>() {
                });

        brainTreeResult.setSucceseded((Boolean) responseMap.get("status"));

        if (responseMap.get("cardInfo") != null) {
            Map<String, Object> cardInfoMap = (Map<String, Object>) responseMap.get("cardInfo");
            brainTreeResult.setCardNumber((String) cardInfoMap.get("last4"));
            brainTreeResult.setCardType((String) cardInfoMap.get("type"));
            brainTreeResult.setExpiryDate((String) cardInfoMap.get("expirationDate"));
        }

        brainTreeResult.setAvs((String) responseMap.get("avsPostalCode"));

        if (responseMap.get("error") != null) {
            Map<String, Object> errorMap = (Map<String, Object>) responseMap.get("error");
            brainTreeResult.setErrorCode((String) errorMap.get("validation_error_code"));
            brainTreeResult.setErrorMessage((String) errorMap.get("validation_error_message"));
            brainTreeResult.setStatusDesc((String) errorMap.get("status_desc"));
        }

        if (responseMap.get("transactionJdo") != null) {
            Map<String, Object> transactionMap = (Map<String, Object>) responseMap.get("transactionJdo");
            brainTreeResult.setTransactionId((String) transactionMap.get("transactionId"));
            brainTreeResult.setAmount((String) transactionMap.get("amount"));
        }

        if (null != responseMap.get(MESSAGE)) {
            brainTreeResult.setErrorMessage(null != brainTreeResult.getErrorMessage() ? brainTreeResult
                    .getErrorMessage().concat(", message : "+(String) responseMap.get(MESSAGE)) : (String) responseMap
                    .get(MESSAGE));
        }
    }

}

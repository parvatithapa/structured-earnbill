package com.sapienter.jbilling.server.payment;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.paymentUrl.domain.response.PaymentResponse;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.PaypalResult;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type;

public class PhonePeWebHookHandler extends PluggableTask implements IPaymentWebHookHandler {

	private static final String PAYMENT_INTENT_FAILED = "payment_intent.payment_failed";
	private static final String AUTHENTICATION_REQUIRED = "authentication_required";
	private static final List<String> SETUP_INTENT_SUPPORTED_EVENTS =
		Arrays.asList("setup_intent.succeeded", "setup_intent.setup_failed");
	private static final String CHECKOUT_SESSION_COMPLETED_EVENT = "checkout.session.completed";
	private static final List<String> PAYMENT_INTENT_SUPPORTED_EVENTS =
			Arrays.asList("payment_intent.succeeded", PAYMENT_INTENT_FAILED);
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final ParameterDescription PARAM_WEBHOOK_SECRET = new ParameterDescription("WebHookSecret", true, Type.STR, true);
	private static final ParameterDescription PARAM_ACCOUNT_TYPE_ID = new ParameterDescription("accountTypeId", true, Type.INT);
	private static final String PHONEPE_WEBHOOK_HANDLER_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.PhonePeWebHookHandler";
	private static final int DEFAULT_TIME_OUT = 10000;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<HashMap<String, Object>> TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};
	public PhonePeWebHookHandler() {
		descriptions.add(PARAM_WEBHOOK_SECRET);
		descriptions.add(PARAM_ACCOUNT_TYPE_ID);
	}

	@Override
    public Response handleWebhookEvent(Map<String, Object> requestMap, Integer entityId) {
		try {
            String eventBody = (String) requestMap.get("requestBody");
            Map<String, String> headers = (Map<String, String>) requestMap.get("headers");
            String sigHeader = headers.get("x-verify");
			Map<String, String> responseMap = OBJECT_MAPPER.readValue(eventBody, Map.class);
			PaymentResponse response = getPaymentResponseFromWebhook(responseMap);
			if (null == response) {
				logger.error("Payment response form webhook is null {}", PhonePeWebHookHandler.class);
				throw new SessionInternalError("Payment response from webhook is null");
			}

			Integer merchantTransactionId = Integer.valueOf(response.getMerchantTransactionId());
            new WebHookHandlerUtil(gatewayName(), getAccountTypeId()).create(response, eventBody, entityId);


		} catch (Exception exception) {
			// roll back transaction as exception occurred.
			logger.error("Exception while creating order ", exception);
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			Map<String, String> errorBody = Collections.singletonMap("error", ExceptionUtils.getStackTrace(exception));
			return Response.status(500)
					.entity(errorBody.toString())
					.build();
		}
		return Response.status(200)
				.entity("")
				.build();
	}


	private PaymentResponse getPaymentResponseFromWebhook(Map<String, String> responseMap) {
		try {
			if (CollectionUtils.isNotEmpty(responseMap.values())) {
				String encodedResponse = responseMap.get("response");
				String decodedValue = decodeBase64(encodedResponse);
				logger.debug("PhonePePaymentProvider.handleWebhook {}", decodedValue);
				Map<String, Object> map = OBJECT_MAPPER.readValue(decodedValue, TYPE_REF);
				if (CollectionUtils.isNotEmpty(map.values())) {
					PaymentResponse paymentResponse = new PaymentResponse();
					paymentResponse.setResponseCode((String) map.get("code"));
					paymentResponse.setResponseStatus((Boolean) map.get("success"));
					paymentResponse.setResponseMessage((String) map.get("message"));
					Map<String, Object> data = (Map<String, Object>) map.get("data");
					if (null != data) {
						String merchantTransactionId = String.valueOf(data.get("merchantTransactionId"));
						paymentResponse.setMerchantTransactionId(merchantTransactionId.contains("-R") ? merchantTransactionId.replaceAll("-R", "") : merchantTransactionId);
						paymentResponse.setTransactionId(String.valueOf(data.get("transactionId")));
						Map<String, String> metaData = new HashMap<>();
						metaData.put("amount", String.valueOf(data.get("amount")));
						metaData.put("responseType", "UPI");
						metaData.put("accountType", "null");
						metaData.put("ifsc", "AABF0009009");
						metaData.put("payerVpa", null);
						metaData.put("isRefund", merchantTransactionId.contains("-R") ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
						Map<String, String> paymentInstrument = (Map<String, String>) data.get("paymentInstrument");
						if (null != paymentInstrument) {
							metaData.putAll(paymentInstrument);
						}
						paymentResponse.setMetaData(metaData);
						return paymentResponse;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String decodeBase64(String encodedString) {
		return org.apache.commons.codec.binary.StringUtils.newStringUtf8(Base64.decodeBase64(encodedString));
	}

	@Override
	public String gatewayName() {
		return "PHONEPE";
	}

	@Override
	public Integer getAccountTypeId() {
			return Integer.valueOf(parameters.get(PARAM_ACCOUNT_TYPE_ID.getName()));
	}

	private static PaymentResponse getCheckStatusResponse(String merchantTransactionId, String amount) {
		logger.debug("NoPaymentProvider.getCheckStatusResponse invoiceId:: {} amount::{}", merchantTransactionId, amount);
		PaymentResponse paymentResponse = new PaymentResponse();
		paymentResponse.setResponseCode("PAYMENT_SUCCESS");
		paymentResponse.setResponseStatus(true);
		paymentResponse.setResponseMessage("Your payment is successful.");
		paymentResponse.setMerchantTransactionId(merchantTransactionId);
		paymentResponse.setTransactionId("T2312141610343119708467");
		Map<String, String> metaData = new HashMap<>();
		metaData.put("amount", amount);
		metaData.put("responseType", "UPI");
		metaData.put("utr", "206850679072");
		metaData.put("accountType", "null");
		metaData.put("ifsc", "AABF0009009");
		metaData.put("payerVpa", null);
		paymentResponse.setMetaData(metaData);
		return paymentResponse;
	}

	private PaymentAuthorizationDTO buildPaymentAuthorization(PaypalResult paypalResult) {
		logger.debug("Payment authorization result of {} gateway parsing....", gatewayName());

		PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
		paymentAuthDTO.setProcessor(gatewayName());

		paymentAuthDTO.setCode1(StringUtils.EMPTY);
		String txID = paypalResult.getTransactionId();
		if (txID != null) {
			paymentAuthDTO.setTransactionId(txID);
			paymentAuthDTO.setCode1(txID);
			logger.debug("transactionId/code1 [{}]", txID);
		}

		String errorCode = paypalResult.getErrorCode();
		String errorShortMsg = paypalResult.getErrMsg();
		paymentAuthDTO.setResponseMessage(errorShortMsg);
		logger.debug("errorMessage [{}]", errorCode);
		logger.debug("errorShortMessage [{}]", errorShortMsg);

		String avs = paypalResult.getAvs();
		if (avs != null) {
			paymentAuthDTO.setAvs(avs);
			logger.debug("avs [{}]", avs);
		}

		return paymentAuthDTO;
	}
}

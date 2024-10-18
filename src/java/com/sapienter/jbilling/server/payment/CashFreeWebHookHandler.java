package com.sapienter.jbilling.server.payment;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.cashfree.JSON;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.cashfree.model.*;
import com.google.gson.Gson;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.paymentUrl.domain.response.PaymentResponse;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

public class CashFreeWebHookHandler extends PluggableTask implements IPaymentWebHookHandler {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ParameterDescription PARAM_WEBHOOK_SECRET = new ParameterDescription("WebHookSecret", true, ParameterDescription.Type.STR, true);
    private static final ParameterDescription PARAM_ACCOUNT_TYPE_ID = new ParameterDescription("accountTypeId", true, ParameterDescription.Type.INT);
    private static Gson gson = new Gson();

    public CashFreeWebHookHandler() {
        descriptions.add(PARAM_WEBHOOK_SECRET);
        descriptions.add(PARAM_ACCOUNT_TYPE_ID);
        new JSON(); // initialize the JSON class with the adapters
        gson = JSON.getGson();
    }

    @Override
    public Response handleWebhookEvent(Map<String, Object> requestMap, Integer entityId) {
        try {
            String eventBody = (String) requestMap.get("requestBody");
            Map<String, String> headers = (Map<String, String>) requestMap.get("headers");
            PaymentWebhook paymentWebhook = gson.fromJson(eventBody, PaymentWebhook.class);
            PaymentResponse response = getPaymentResponseFromWebhook(paymentWebhook);
            if (null == response) {
                logger.error("Payment response form webhook is null {}", CashFreeWebHookHandler.class);
                throw new SessionInternalError("Payment response from webhook is null");
            }
            logger.debug("Payment response from webhook is = {}", response);
            Map<String, String> metaData = response.getMetaData();
            String webhookType = metaData.get("WEBHOOK_TYPE");
            logger.debug("webhook received - {}", webhookType);

            if (!new PaymentDAS().findPaymentProcessed(response.getTransactionId())) {
                switch (webhookType) {
                    case "PAYMENT_SUCCESS_WEBHOOK":
                    case "PAYMENT_FAILED_WEBHOOK":
                        // call success or payment failed method;
                        new WebHookHandlerUtil(gatewayName(), getAccountTypeId()).create(response, eventBody, entityId);
                        break;
                    case "PAYMENT_USER_DROPPED_WEBHOOK":
                        // call user dropped method;
                        logger.debug("do nothing in - {}", webhookType);
                        break;
                    default:
                        logger.debug("unable to handle the webhook type");
                }
            }
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

    @Override
    public String gatewayName() {
        return "CASHFREE";
    }

    @Override
    public Integer getAccountTypeId() {
        return Integer.valueOf(parameters.get(PARAM_ACCOUNT_TYPE_ID.getName()));
    }

    private PaymentResponse getPaymentResponseFromWebhook(PaymentWebhook paymentWebhook) {
        try {
            if (paymentWebhook != null) {
                PaymentWebhookDataEntity data = paymentWebhook.getData();
                if (data != null){
                    PaymentResponse paymentResponse = new PaymentResponse();
                    if (data.getOrder() != null && data.getOrder().getOrderId() != null) {
                        paymentResponse.setMerchantTransactionId(data.getOrder().getOrderId());
                    }
                    PaymentEntity payment = data.getPayment();
                    PaymentWebhookCustomerEntity customerDetails = data.getCustomerDetails();
                    if (payment != null){
                        paymentResponse.setTransactionId(payment.getCfPaymentId());
                        paymentResponse.setResponseMessage(payment.getPaymentMessage());
                        paymentResponse.setResponseCode(payment.getPaymentStatus().getValue());
                        Map<String, String> metaData = getMetaData(payment, customerDetails);
                        metaData.put("WEBHOOK_TYPE", paymentWebhook.getType());
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

    @NotNull
    private static Map<String, String> getMetaData(PaymentEntity payment, PaymentWebhookCustomerEntity customerDetails) {
        Map<String, String> metaData = new HashMap<>();
        if (null != payment) {
            PaymentEntityPaymentMethod paymentMethod = payment.getPaymentMethod();
            String paymentGroup = payment.getPaymentGroup().toUpperCase();
            if (paymentGroup.equalsIgnoreCase("UPI")) {
                PaymentMethodUPIInPaymentsEntity upiInPaymentsEntity = paymentMethod.getPaymentMethodUPIInPaymentsEntity();
                if (null != upiInPaymentsEntity) {
                    PaymentMethodUPIInPaymentsEntityUpi upi = upiInPaymentsEntity.getUpi();
                    if (null != upi) {
                        metaData.put("payerVpa", upi.getUpiId());
                    }
                }
            } else if (paymentGroup.equalsIgnoreCase("credit_card")) {
                PaymentMethodCardInPaymentsEntity cardInPaymentsEntity = paymentMethod.getPaymentMethodCardInPaymentsEntity();
                if (null != cardInPaymentsEntity) {
                    PaymentMethodCardInPaymentsEntityCard card = cardInPaymentsEntity.getCard();
                    if (null != card) {
                        metaData.put("cardNumber", card.getCardNumber());
                        metaData.put("cardNetwork", card.getCardNetwork());
                        metaData.put("cardType", card.getCardType());
                        metaData.put("bankName", card.getCardBankName());
                        metaData.put("cardCountry", card.getCardCountry());
                        metaData.put("cardNetworkReferenceId", card.getCardNetworkReferenceId());
                    }
                }
            } else if (paymentGroup.equalsIgnoreCase("net_banking")) {
                PaymentMethodNetBankingInPaymentsEntity netBankingInPaymentsEntity = paymentMethod.getPaymentMethodNetBankingInPaymentsEntity();
                if (null != netBankingInPaymentsEntity) {
                    PaymentMethodNetBankingInPaymentsEntityNetbanking netbanking = netBankingInPaymentsEntity.getNetbanking();
                    if (null != netbanking) {
                        metaData.put("bankCode", String.valueOf(netbanking.getNetbankingBankCode()));
                        metaData.put("bankName", netbanking.getNetbankingBankName());
                    }
                }
            } else if (paymentGroup.equalsIgnoreCase("wallet")) {
                PaymentMethodAppInPaymentsEntity appInPaymentsEntity = paymentMethod.getPaymentMethodAppInPaymentsEntity();
                if (null != appInPaymentsEntity) {
                    PaymentMethodAppInPaymentsEntityApp app = appInPaymentsEntity.getApp();
                    if (null != app) {
                        metaData.put("channel", app.getChannel());
                        metaData.put("phone", app.getPhone());
                        metaData.put("provider", app.getProvider());
                    }
                }
            }
            metaData.put("amount", String.valueOf(payment.getPaymentAmount()));
            metaData.put("responseType", paymentGroup);
            metaData.put("isRefund", Boolean.FALSE.toString());
            metaData.put("paymentMessage", payment.getPaymentMessage());
            metaData.put("currency", payment.getPaymentCurrency());
            metaData.put("utr", payment.getBankReference());
            metaData.put("paymentTime", payment.getPaymentTime());
        }
        if (null != customerDetails) {
            metaData.put("customerName", customerDetails.getCustomerName());
            metaData.put("customerPhone", customerDetails.getCustomerPhone());
            metaData.put("customerEmail", customerDetails.getCustomerEmail());
        }
        return metaData;
    }

}

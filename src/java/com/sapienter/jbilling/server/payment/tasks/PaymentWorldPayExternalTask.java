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
package com.sapienter.jbilling.server.payment.tasks;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldExternalHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.BooleanMetaFieldValue;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.worldpay.WorldPayPayerInfo;
import com.sapienter.jbilling.server.payment.tasks.worldpay.WorldpayResult;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.worldpay.gateway.clearwater.client.core.dto.ApiError;
import com.worldpay.gateway.clearwater.client.core.dto.CurrencyCode;
import com.worldpay.gateway.clearwater.client.core.dto.common.CommonToken;
import com.worldpay.gateway.clearwater.client.core.dto.request.CardRequest;
import com.worldpay.gateway.clearwater.client.core.dto.request.OrderRequest;
import com.worldpay.gateway.clearwater.client.core.dto.response.OrderResponse;
import com.worldpay.gateway.clearwater.client.core.exception.WorldpayException;
import com.worldpay.gateway.clearwater.client.ui.dto.order.Transaction;
import com.worldpay.gateway.clearwater.client.ui.dto.order.TransactionHistory;
import com.worldpay.sdk.WorldpayRestClient;

/**
 * @author Brian Cowdery
 * @since 20-10-2009
 */
public class PaymentWorldPayExternalTask extends PaymentWorldPayBaseTask implements IExternalCreditCardStorage {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String REFUND_TRANSACTION = "R";
    protected static final OrderRequest NOT_APPLICABLE_ORDER_REQUEST = new OrderRequest();

    @Override
    String getProcessorName() {
        return "WorldPay";
    }

    @Override
    public boolean process(PaymentDTOEx payment) throws PluggableTaskException {
        logger.debug("Payment processing for {} gateway", getProcessorName());

        if (payment.getPayoutId() != null) {
            return true;
        }

        if (isRefund(payment)) {
            return processRefund(payment).shouldCallOtherProcessors();

        } else if (isCreditCardStored(payment)) {
            try (PaymentInformationBL piBl = new PaymentInformationBL()) {
                prepareExternalPayment(payment);
                SvcType transaction = (BigDecimal.ZERO.compareTo(payment
                        .getAmount()) > 0 || payment.getIsRefund() != 0 ? SvcType.REFUND_CREDIT
                                : (piBl.useGatewayKey(payment.getInstrument()) ? SvcType.RE_AUTHORIZE
                                        : SvcType.SALE));
                logger.debug("creating {}  payment transaction", transaction);
                Result result = doProcess(payment, transaction, null);
                // update the stored external gateway key
                if (Constants.RESULT_OK.equals(payment.getResultId())) {
                    updateGatewayKey(payment);
                }
                return result.shouldCallOtherProcessors();
            } catch (Exception e) {
                logger.error("Credit Card payment not processed {}", e);
            }
        }
        return processOneTimePayment(payment).shouldCallOtherProcessors();
    }

    @Override
    public void failure(Integer userId, Integer retry) {
        // not supported
    }

    @Override
    public boolean preAuth(PaymentDTOEx payment) throws PluggableTaskException {
        logger.debug("Pre-authorization processing for {} gateway", getProcessorName());
        prepareExternalPayment(payment);
        return doProcess(payment, SvcType.AUTHORIZE, null).shouldCallOtherProcessors();
    }

    @Override
    public boolean confirmPreAuth(PaymentAuthorizationDTO auth, PaymentDTOEx payment)
            throws PluggableTaskException {

        logger.debug("Confirming pre-authorization for {} gateway", getProcessorName());

        if (!getProcessorName().equals(auth.getProcessor())) {
            /*  let the processor be called and fail, so the caller can do something
                about it: probably re-call this payment task as a new "process()" run */
            logger.warn("The processor of the pre-auth is not {}, is {}", getProcessorName(),auth.getProcessor());
        }

        PaymentInformationDTO card = payment.getInstrument();
        if (card == null) {
            throw new PluggableTaskException("Credit card is required, capturing payment: " + payment.getId());
        }

        if (!isApplicable(payment)) {
            logger.error("This payment can not be captured {}", payment);
            return true;
        }

        // process
        prepareExternalPayment(payment);
        Result result = doProcess(payment, SvcType.SETTLE, auth);

        // update the stored external gateway key
        if (Constants.RESULT_OK.equals(payment.getResultId())) {
            updateGatewayKey(payment);
        }

        return result.shouldCallOtherProcessors();
    }

    private boolean isAutoPaymentAuthEnabled(PaymentInformationDTO creditCard) {
        return creditCard.getMetaFields().stream()
                .anyMatch(mf -> MetaFieldType.AUTO_PAYMENT_AUTHORIZATION.equals(mf.getField().getFieldUsage()) &&
                        ((BooleanMetaFieldValue) mf).getValue()!=null ? ((BooleanMetaFieldValue) mf).getValue() : Boolean.FALSE);
    }

    private Optional<PaymentInformationDTO> checkAndReturnCreditCardOnUser(Integer userId, PaymentInformationDTO creditCard) {
        Map<String, String> paymentMetaFieldMap = getPaymentInstrumentMetaFields(creditCard);
        String expiryDateFieldValue = paymentMetaFieldMap.get(MetaFieldType.DATE.name());
        String creditCardNumber = paymentMetaFieldMap.get(MetaFieldType.PAYMENT_CARD_NUMBER.name());
        if(StringUtils.isEmpty(expiryDateFieldValue) ||
                StringUtils.isEmpty(creditCardNumber)) {
            logger.debug("credit card number {} or expiry date {} is null or empty", creditCardNumber, expiryDateFieldValue);
            return Optional.empty();
        }
        if(creditCardNumber.length() < 4) {
            logger.debug("credit card number {} too short", creditCardNumber);
            return Optional.empty();
        }
        creditCardNumber = creditCardNumber.substring(creditCardNumber.length() - 4);
        List<PaymentInformationDTO> userCards = new UserDAS().findNow(userId).getPaymentInstruments();
        if(CollectionUtils.isEmpty(userCards)) {
            logger.debug("no payment instruments found on user {}", userId);
            return Optional.empty();
        }
        for(PaymentInformationDTO userCreditCard : new UserDAS().findNow(userId).getPaymentInstruments()) {
            Map<String, String> userPaymentcardMetaFieldMap = getPaymentInstrumentMetaFields(userCreditCard);
            String userCardExpiryDateFieldValue = userPaymentcardMetaFieldMap.get(MetaFieldType.DATE.name());
            String userCreditCardNumber = userPaymentcardMetaFieldMap.get(MetaFieldType.PAYMENT_CARD_NUMBER.name());
            String gateWaykey = userPaymentcardMetaFieldMap.get(MetaFieldType.GATEWAY_KEY.name());
            if(StringUtils.isEmpty(userCardExpiryDateFieldValue) || StringUtils.isEmpty(userCreditCardNumber)
                    || userCreditCardNumber.length() < 4 || StringUtils.isEmpty(gateWaykey)) {
                continue;
            }
            userCreditCardNumber = userCreditCardNumber.substring(userCreditCardNumber.length() - 4);
            if(userCardExpiryDateFieldValue.equals(expiryDateFieldValue)
                    && creditCardNumber.equals(userCreditCardNumber)) {
                return Optional.of(userCreditCard);
            }
        }
        return Optional.empty();
    }

    private Result processOneTimePayment(PaymentDTOEx payment) {
        try {
            PaymentInformationDTO paymentInstrument = payment.getInstrument();
            Optional<PaymentInformationDTO> userPaymentInstrument = checkAndReturnCreditCardOnUser(payment.getUserId(), paymentInstrument);
            if(userPaymentInstrument.isPresent()) {
                logger.debug("payment insturment found on user {}", payment.getUserId());
                paymentInstrument = userPaymentInstrument.get();
                payment.setInstrument(paymentInstrument);
            }
            WorldPayPayerInfo payer = createPayerFromPaymentInformation(paymentInstrument, payment.getUserId());
            boolean createRecurringToken = isAutoPaymentAuthEnabled(paymentInstrument);
            Object token;
            if(!userPaymentInstrument.isPresent()) {
                if(createRecurringToken) {
                    logger.debug("creating recurring token for user {} since AUTO_PAYMENT_AUTHORIZATION "
                            + "is enabled on payment instrument", payment.getUserId());
                }
                token = createTokenRequest(payer, createRecurringToken);
            } else {
                try (PaymentInformationBL piBl = new PaymentInformationBL()) {
                    token = new String(piBl.getCharMetaFieldByType(userPaymentInstrument.get(), MetaFieldType.GATEWAY_KEY));
                }
            }

            BigDecimal paymentAmount = payment.getAmount()
                    .setScale(Constants.BIGDECIMAL_SCALE_STR)
                    .multiply(ONE_HUNDRED);
            OrderRequest orderRequest ;
            if(token instanceof CommonToken) {
                orderRequest = createOrderRequest((CommonToken)token, paymentAmount.intValueExact(),
                        CurrencyCode.fromValue(payment.getCurrency().getCode()), payer,
                        payment.getUserId().toString(), false);
            } else {
                orderRequest = createOrderRequest((String) token, paymentAmount.intValueExact(),
                        CurrencyCode.fromValue(payment.getCurrency().getCode()), payer,
                        payment.getUserId().toString(), false);
            }

            WorldpayResult result = postPaymentRequestToGateWay(orderRequest);
            if(createRecurringToken && !userPaymentInstrument.isPresent()) {
                UserDTO user = new UserDAS().findNow(payment.getUserId());
                logger.debug("Adding instrument on user {}", user.getId());
                PaymentInformationDTO creditCard = payment.getInstrument().getSaveableDTO();
                creditCard.setUser(user);
                int paymentMethodTypeId = creditCard.getPaymentMethodType().getId();
                MetaField gateWayKeyMf = MetaFieldExternalHelper.findPaymentMethodMetaFieldByFieldUsage(MetaFieldType.GATEWAY_KEY, paymentMethodTypeId);
                String gateWayKey = result.getToken();
                creditCard.setMetaField(gateWayKeyMf, gateWayKeyMf.getDataType().equals(DataType.CHAR) ? gateWayKey.toCharArray() : gateWayKey);
                creditCard.setProcessingOrder(1);
                try (PaymentInformationBL piBl = new PaymentInformationBL()) {
                    creditCard = piBl.create(creditCard);
                }
                obscureCreditCardNumber(creditCard); // obscureCreditCardNumber on new credit card.
                user.getPaymentInstruments().add(creditCard); // set one time payment instrument on user.
            }
            WorldPayAuthorization wrapper = new WorldPayAuthorization(result);
            storeWorldPayResult(result, payment, wrapper.getDTO());
            obscureCreditCardNumber(payment);
            return new Result(wrapper.getDTO(), false);
        } catch (Exception e) {
            logger.error("Couldn't handle payment request due to error", e);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        }
    }

    private void obscureCreditCardNumber(PaymentInformationDTO creditCard) {
        try (PaymentInformationBL piBL = new PaymentInformationBL()) {
            // obscure new credit card numbers
            piBL.obscureCreditCardNumber(creditCard);
        } catch (Exception exception) {
            logger.debug("exception: {}", exception);
        }
    }
    private static boolean isRefund(PaymentDTOEx payment) {
        return BigDecimal.ZERO.compareTo(payment.getAmount()) > 0 || payment.getIsRefund() != 0;
    }


    private WorldpayResult sendRefundRequestToGateWay(String transactionId, BigDecimal paymentAmount)
            throws PluggableTaskException {
        WorldpayResult result = new WorldpayResult.WorldpayResultBuilder().build();
        WorldpayRestClient restClient = getRestClient();

        try {
            restClient.getOrderService().refund(transactionId,
                    paymentAmount.intValueExact());
            logger.debug("Payment refunded for Transaction {}", transactionId);

            Transaction transaction = restClient.getOrderService().findOrder(
                    transactionId);
            OrderResponse orderResponse = transaction.getOrderResponse();
            List<TransactionHistory> history = transaction.getHistory();
            String refundTrans = REFUND_TRANSACTION + (history.size() - 1);
            String orderCode=refundTrans + orderResponse.getOrderCode();
            String customerOrderCode= orderResponse.getCustomerOrderCode();
            String status=orderResponse.getPaymentStatus();
            result = new WorldpayResult.WorldpayResultBuilder().setOrderCode(orderCode).
                    setCustomerOrderCode(customerOrderCode).setSucceeded(true).setStatus(status).build();
            logger.debug("Order response : {}", orderResponse);
            return result;
        } catch (WorldpayException exception) {

            result = new WorldpayResult.WorldpayResultBuilder().setSucceeded(false).build();
            logger.error("Error code: {}", exception.getApiError()
                    .getCustomCode());
            logger.error("Error description: {}", exception.getApiError()
                    .getDescription());
            logger.error("Error message: {}", exception.getApiError()
                    .getMessage());
            logger.error("exception.getCause() {}: ", exception.getCause());
            logger.error("exception.getMessage() : {}", exception.getMessage());
            logger.error("exception.getLocalizedMessage() : {}",
                    exception.getLocalizedMessage());

            ApiError apiError = exception.getApiError();
            if (apiError != null) {
                String errorCode= apiError.getCustomCode();
                String errorMessage=apiError.getMessage();
                String errorDescription=apiError.getDescription();
                Integer httpStatusCode=apiError.getHttpStatusCode();
                result = new WorldpayResult.WorldpayResultBuilder().setSucceeded(false).
                        setErrorCode(errorCode).setErrorMessage(errorMessage).setErrorDescription(errorDescription).
                        setHttpStatusCode(httpStatusCode).setStatus("EXCEPTION").build();
                logger.debug(apiError.getCustomCode(), exception);
            }
            return result;
        }
    }

    private Result processRefund(PaymentDTOEx payment) throws PluggableTaskException {
        try {
            PaymentAuthorizationDTO parentPaymentAuthorization = getParentPaymentAuthorization(payment);
            if(null == parentPaymentAuthorization) {
                throw new PluggableTaskException("no parent transaction id found for refund");
            }
            String transactionId = parentPaymentAuthorization
                    .getTransactionId();
            BigDecimal paymentAmount = payment.getAmount()
                    .setScale(Constants.BIGDECIMAL_SCALE_STR)
                    .multiply(ONE_HUNDRED);
            WorldpayResult result = sendRefundRequestToGateWay(transactionId,
                    paymentAmount);

            WorldPayAuthorization wrapper = new WorldPayAuthorization(result);
            storeWorldPayResult(result, payment, wrapper.getDTO());
            return new Result(wrapper.getDTO(), false);
        } catch(PluggableTaskException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Couldn't handle refund payment request due to error", ex);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        }
    }

    private static PaymentAuthorizationDTO getParentPaymentAuthorization(PaymentDTOEx payment) {
        return isRefund(payment) ? new PaymentDAS().findNow(payment.getPayment().getId())
                .getPaymentAuthorizations()
                .stream()
                .findFirst().orElse(null) : null;
    }

    private void obscureCreditCardNumber(PaymentDTOEx payment) {
        try (PaymentInformationBL piBL = new PaymentInformationBL()) {
            // obscure new credit card numbers
            PaymentInformationDTO card = payment.getInstrument();
            piBL.obscureCreditCardNumber(card);
        } catch (Exception exception) {
            logger.debug("exception: {}", exception);
        }
    }

    private void storeWorldPayResult(WorldpayResult result,
            PaymentDTOEx payment, PaymentAuthorizationDTO paymentAuthorization) {
        if (result.isSucceeded()) {
            payment.setPaymentResult(new PaymentResultDAS()
            .find(Constants.RESULT_OK));
        } else {
            payment.setPaymentResult(new PaymentResultDAS()
            .find(Constants.RESULT_FAIL));
        }
        new PaymentAuthorizationBL().create(paymentAuthorization,
                payment.getId());
        payment.setAuthorization(paymentAuthorization);
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

        try(PaymentInformationBL piBl = new PaymentInformationBL())
        {
            if (piBl.useGatewayKey(payment.getInstrument())) {
                logger.debug("credit card is obscured, retrieving from database to use external store.");
                payment.setInstrument(payment.getInstrument());
            } else {
                logger.debug("new credit card or previously un-obscured, using as is.");
            }
        }
        catch (Exception e) {
            logger.error("Prepration of external payment failed {}",e);
        }
    }

    /**
     * Updates the gateway key of the credit card associated with this payment. RBS WorldPay
     * implements the gateway key a per-transaction ORDER_ID that is returned as part of the
     * payment response.
     *
     * @param payment successful payment containing the credit card to update.
     *  */
    public void updateGatewayKey(PaymentDTOEx payment) {
        try (PaymentInformationBL piBl = new PaymentInformationBL()) {
            // update the gateway key with the returned RBS WorldPay ORDER_ID
            PaymentInformationDTO card = payment.getInstrument();

            if (!Constants.PAYMENT_METHOD_GATEWAY_KEY.equals(card
                    .getPaymentMethod().getId())) {
                piBl.obscureCreditCardNumber(card);
            }
        } catch (Exception e) {
            logger.error("Gateway key update didnt happen successfully {}", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Creates a payment of zero dollars and returns the Token Id as the gateway
     * key to be stored for future transactions.
     */
    @Override
    public String storeCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
        // new contact that has not had a credit card created yet
        if (instrument == null) {
            logger.warn("No credit card to store externally.");
            return null;
        }
        try (PaymentInformationBL paymentInfoBl = new PaymentInformationBL()) {
            char[] creditCardNumber = paymentInfoBl.getCharMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
            if (ArrayUtils.isNotEmpty(creditCardNumber) && PaymentInformationBL.paymentCardObscured(creditCardNumber)) {
                logger.warn("Could not create customer token for obscured credit card no.");
                return null;
            }
            char[] gateWaykey = paymentInfoBl.getCharMetaFieldByType(instrument, MetaFieldType.GATEWAY_KEY);
            if(ArrayUtils.isNotEmpty(gateWaykey)) {
                logger.debug("Customer profile alreday created on gateway");
                return null;
            }
        } catch (Exception e) {
            throw new SessionInternalError("Error in storeCreditCard", e);
        }

        WorldPayPayerInfo payerInfo ;
        try {
            payerInfo = createPayerFromPaymentInformation(instrument, null);
        } catch(Exception ex) {
            throw new SessionInternalError("payer creation failed!", ex);
        }

        boolean makeZeroCentPayment = getBooleanParameter(PARAMETER_MAKE_ZERO_CENT_PAYMENT.getName());
        if(makeZeroCentPayment) {
            logger.debug("making zero cent payment for user {}, as plugin in param {} is enabled",
                    instrument.getUser().getId(), PARAMETER_MAKE_ZERO_CENT_PAYMENT.getName());
            /*
             * Note, don't use PaymentBL.findPaymentInstrument() as the given
             * creditCard is still being processed at the time that this event is
             * being handled, and will not be found.
             *
             * PaymentBL()#create() will cause a stack overflow as it will attempt
             * to update the credit card, emitting another NewCreditCardEvent which
             * is then handled by this method and repeated.
             */
            try(PaymentInformationBL piBl = new PaymentInformationBL();
                    PaymentDTO paymentInfo = new PaymentDTO()) {
                UserDTO user = instrument.getUser();
                paymentInfo.setBaseUser(user);
                paymentInfo.setCurrency(user.getCurrency());
                paymentInfo.setAmount(BigDecimal.ZERO);
                paymentInfo.setPaymentMethod(new PaymentMethodDAS().find(Util
                        .getPaymentMethod(piBl.getCharMetaFieldByType(instrument,
                                MetaFieldType.PAYMENT_CARD_NUMBER))));
                paymentInfo.setIsRefund(0);
                paymentInfo.setIsPreauth(1);
                paymentInfo.setDeleted(1);
                paymentInfo.setAttempt(1);
                paymentInfo.setPaymentDate(companyCurrentDate());
                paymentInfo.setCreateDatetime(TimezoneHelper.serverCurrentDate());

                PaymentDTOEx payment = new PaymentDTOEx(new PaymentDAS().save(paymentInfo));
                CommonToken token = createTokenRequest(payerInfo, true);
                OrderRequest orderRequest = createOrderRequest(token, paymentInfo
                        .getAmount().intValueExact(),
                        CurrencyCode.fromValue(payment.getCurrency().getCode()),
                        payerInfo, user.getUserName(), true);
                WorldpayResult result = postPaymentRequestToGateWay(orderRequest);
                logger.debug("Payment sucessful : {}", payment);
                logger.debug("token {} created for user {}", result.getToken(), instrument.getUser().getId());
                // return credit card token key
                return result.getToken();
            } catch (Exception e) {
                throw new SessionInternalError("Could not process external storage payment", e);
            }
        } else {
            try {
                CardRequest cardRequest = createCardRequest(payerInfo.getCreditCardNumber(),
                        payerInfo.getCardHolderName(), Integer.parseInt(payerInfo.getExpiryMonth()),
                        Integer.parseInt(payerInfo.getExpiryYear()), payerInfo.getCvv());
                String token = createToken(cardRequest, true);
                logger.debug("token {} created for user {}", token, instrument.getUser().getId());
                return token;
            } catch(PluggableTaskException ex) {
                throw new SessionInternalError("error creating token", ex);
            } catch (Exception ex) {
                throw new SessionInternalError("Error in storeCreditCard", ex);
            }
        }

    }

    /**
     * Creates {@link OrderRequest} for given {@link WorldPayPayerInfo}.
     * @param token
     * @param amount
     * @param currencyCode
     * @param payerInfo
     * @param orderDescription
     * @param authorized
     * @return
     */
    private OrderRequest createOrderRequest(CommonToken token, int amount, CurrencyCode currencyCode,
            WorldPayPayerInfo payerInfo, String orderDescription, boolean authorized) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setCommonToken(token);
        orderRequest.setAmount(amount);
        orderRequest.setCurrencyCode(currencyCode);
        orderRequest.setName(payerInfo.getCardHolderName());
        orderRequest.setOrderDescription(orderDescription);
        orderRequest.setBillingAddress(getBillingAddress(payerInfo));
        orderRequest.setAuthorizeOnly(authorized);
        return orderRequest;
    }

    /**
     * Creates {@link CommonToken} from {@link WorldPayPayerInfo}
     * @param payerInfo
     * @param reusable
     * @return
     */
    private CommonToken createTokenRequest(WorldPayPayerInfo payerInfo, boolean reusable) {
        CardRequest cardRequest = createCardRequest(payerInfo.getCreditCardNumber(),
                payerInfo.getCardHolderName(), Integer.parseInt(payerInfo.getExpiryMonth()),
                Integer.parseInt(payerInfo.getExpiryYear()), payerInfo.getCvv());
        return new CommonToken(cardRequest, reusable);
    }

    /**
     *
     */
    @Override
    public char[] deleteCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
        //noop
        return new char[0];
    }

    @Override
    public OrderRequest buildOrderRequest(PaymentDTOEx payment, SvcType transaction) throws PluggableTaskException {

        BigDecimal paymentAmount = payment.getAmount()
                .setScale(Constants.BIGDECIMAL_SCALE_STR).multiply(ONE_HUNDRED);
        CurrencyCode currencyCode = CurrencyCode.fromValue(payment
                .getCurrency().getCode());

        PaymentInformationDTO instrument = payment.getInstrument();
        /*
         * Sale transactions do not support the use of the ORDER_ID gateway key.
         * After an initial sale transaction RBS WorldPay will have a record of
         * our transactions for reference - so all other transaction types are
         * safe for use with the stored gateway key.
         */
        try (PaymentInformationBL piBl = new PaymentInformationBL()) {
            if (SvcType.SALE.equals(transaction)
                    && Constants.PAYMENT_METHOD_GATEWAY_KEY.equals(Util
                            .getPaymentMethod(piBl.getCharMetaFieldByType(
                                    instrument,
                                    MetaFieldType.PAYMENT_CARD_NUMBER)))) {
                throw new PluggableTaskException(
                        "Cannot process a SALE transaction with an obscured credit card!");
            }

            WorldPayPayerInfo payerInfo = createPayerFromPaymentInformation(
                    instrument, payment.getUserId());
            OrderRequest orderRequest;
            // Need to check is it existing token, If then get token & do the
            String token = new String(piBl.getCharMetaFieldByType(instrument, MetaFieldType.GATEWAY_KEY));
            // payment
            if (StringUtils.isNotEmpty(token)) {
                orderRequest = createOrderRequest(token, paymentAmount.intValueExact(),
                        currencyCode, payerInfo, payment.getUserId().toString(),
                        false);
            } else {
                CommonToken cardToken = createTokenRequest(payerInfo, true);
                orderRequest = createOrderRequest(cardToken, paymentAmount.intValueExact(),
                        currencyCode, payerInfo, payment.getUserId().toString(),
                        false);
            }

            return orderRequest;
        } catch (Exception e) {
            logger.error("Could not build order request. Error is {}", e);
            return NOT_APPLICABLE_ORDER_REQUEST;
        }
    }

    private static boolean isCreditCardStored(PaymentDTOEx payment) {
        try(PaymentInformationBL piBL = new PaymentInformationBL()) {
            PaymentInformationDTO instrument = payment.getInstrument();
            char [] token = piBL.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.GATEWAY_KEY);
            return piBL.useGatewayKey(instrument) && (token!=null && token.length > 0);
        } catch(Exception e) {
            logger.error("Credit card not stored due to error {}",e);
            return false;
        }
    }

    private WorldPayPayerInfo createPayerFromPaymentInformation(PaymentInformationDTO creditCard, Integer userId) {
        WorldPayPayerInfo payer = new WorldPayPayerInfo.WorldPayPayerInfoBuilder().build();

        UserDTO user = creditCard.getUser();

        if(user == null) {
            user = new UserDAS().findNow(userId);
        }

        Integer entityId= user.getCompany().getId();
        CustomerDTO customer = user.getCustomer();
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        String billingGroupNameValue = metaFieldDAS.getComapanyLevelMetaFieldValue(MetaFieldName.CUSTOMER_BILLING_GROUP_NAME.getMetaFieldName(), entityId);

        AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
        MetaFieldGroup metaFieldGroup = accountInformationTypeDAS.getGroupByNameAndEntityId(entityId
                , EntityType.ACCOUNT_TYPE, billingGroupNameValue, user.getCustomer().getAccountType().getId());

        if (null != metaFieldGroup) {
            Map<String, String> billingAddressMetaFieldMapByMetaFieldType = new HashMap<>();
            billingAddressMetaFieldMapByMetaFieldType.putAll(getCustomerAITMetaFields(customer, metaFieldGroup.getId(), TimezoneHelper.companyCurrentDate(entityId)));

            String city= billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.CITY.toString());
            String billingEmail=billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.BILLING_EMAIL.toString());
            String email=billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.EMAIL.toString());
            String countryCode=billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.COUNTRY_CODE.toString());
            String zip= billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.POSTAL_CODE.toString());
            String  state=billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.STATE_PROVINCE.toString());
            String street =billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.ADDRESS1.toString());
            payer = new WorldPayPayerInfo.WorldPayPayerInfoBuilder()
            .setCity(city)
            .setEmail(billingEmail)
            .setCountryCode(countryCode)
            .setZip(zip)
            .setState(state)
            .setStreet(street)
            .build();

            if(StringUtils.isEmpty(payer.getEmail())) {
                payer = new WorldPayPayerInfo.WorldPayPayerInfoBuilder()
                .setCity(city)
                .setEmail(email)
                .setCountryCode(countryCode)
                .setZip(zip)
                .setState(state)
                .setStreet(street)
                .build();
            }
        }

        Map<String, String> paymentMetaFieldMap = getPaymentInstrumentMetaFields(creditCard);
        if (MapUtils.isNotEmpty(paymentMetaFieldMap)) {
            String expiryDateFieldValue = paymentMetaFieldMap.get(MetaFieldType.DATE.name());
            String creditCardNumber = paymentMetaFieldMap.get(MetaFieldType.PAYMENT_CARD_NUMBER.name());
            String cardHolderName = paymentMetaFieldMap.get(MetaFieldType.TITLE.name());
            String expiryYear = null;
            String expiryMonth = null;

            if(StringUtils.isNotEmpty(expiryDateFieldValue) && !expiryDateFieldValue.isEmpty() && expiryDateFieldValue.split("/").length ==2) {
                expiryYear = expiryDateFieldValue.split("/")[1];
                expiryMonth = expiryDateFieldValue.split("/")[0];
            }
            String cvv = creditCard.getCvv();
            payer = new WorldPayPayerInfo.WorldPayPayerInfoBuilder()
            .setCreditCardNumber(creditCardNumber)
            .setExpiryMonth(expiryMonth)
            .setCardHolderName(cardHolderName)
            .setExpiryYear(expiryYear)
            .setCvv(cvv.toCharArray())
            .build();
        }
        return payer;
    }


    @SuppressWarnings("rawtypes")
    private Map<String,String> getCustomerAITMetaFields(CustomerDTO customer, Integer groupId, Date effectiveDate) {
        Map<Date, List<MetaFieldValue>> aitMetaFields = customer.getAitTimelineMetaFieldsMap().getOrDefault(groupId, Collections.emptyMap());
        Map<String, String> aitFieldNameValueMap = new HashMap<>();
        for(Entry<Date, List<MetaFieldValue>> aitEntry : aitMetaFields.entrySet()) {
            if(aitEntry.getKey().compareTo(effectiveDate)<=0) {
                for(MetaFieldValue aitFieldValue: aitEntry.getValue()) {
                    Object value = aitFieldValue.getValue();
                    MetaFieldType usage = aitFieldValue.getField().getFieldUsage();
                    if(null!= usage) {
                        aitFieldNameValueMap.put(usage.name(), value!=null ? value.toString() : "");
                    }
                }
                return aitFieldNameValueMap;
            }
        }
        return Collections.emptyMap();
    }

    private Map<String, String> getPaymentInstrumentMetaFields(PaymentInformationDTO creditCard) {
        Map<String, String> creditCardFieldMap = new HashMap<>();
        creditCard.getMetaFields().forEach(metaFieldValue -> {
            MetaFieldType type = metaFieldValue.getField().getFieldUsage();
            Object value = metaFieldValue.getValue();
            if(null!=type && null!=value) {
                if(metaFieldValue.getField().getDataType().equals(DataType.CHAR)) {
                    creditCardFieldMap.put(type.name(), new String((char[])value));
                } else {
                    creditCardFieldMap.put(type.name(), value.toString());
                }
            }
        });
        return creditCardFieldMap;
    }

}

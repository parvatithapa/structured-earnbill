package com.sapienter.jbilling.server.payment.tasks.unified.braintree;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.util.Constants;

public class UnifiedBrainTreePaymentExternalTask extends PaymentTaskWithTimeout implements IExternalCreditCardStorage {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final PaymentInformationBL piBl = new PaymentInformationBL();

    private static final String BRAIN_TREE = "BrainTree";

    private static final String[] VALID_BT_PAYMENT_TYPE = { "btcc", "ach", "ec", "gc" };

    private static final String AMERICAN_EXPRESS = "American Express";

    /* Plugin parameters */
    public static final ParameterDescription BUSINESS_ID = new ParameterDescription("Bussiness Id", true,
            ParameterDescription.Type.STR);

    public static final ParameterDescription FC_WEB_SERVICE_PAYMENT_URL = new ParameterDescription(
            "FC Web Service Payment URL", false, ParameterDescription.Type.STR);

    public static final ParameterDescription FC_WEB_SERVICE_REFUND_URL = new ParameterDescription(
            "FC Web Service Refund URL", false, ParameterDescription.Type.STR);

    public String getBusinessId() throws PluggableTaskException {
        return ensureGetParameter(BUSINESS_ID.getName());
    }

    public String getPaymentRemoteAPI() throws PluggableTaskException {
        return getOptionalParameter(FC_WEB_SERVICE_PAYMENT_URL.getName(),
                "https://commonpaymentapi-dot-staging-adaptive-payments.appspot.com");
    }

    public String getRefundRemoteAPI() throws PluggableTaskException {
        return getOptionalParameter(FC_WEB_SERVICE_REFUND_URL.getName(),
                "https://staging-adaptive-payments.appspot.com");
    }

    // initializer for pluggable params
    {
        descriptions.add(BUSINESS_ID);
        descriptions.add(FC_WEB_SERVICE_PAYMENT_URL);
        descriptions.add(FC_WEB_SERVICE_REFUND_URL);
    }

    private UnifiedBrainTreeApi getBTApi() throws PluggableTaskException {
        return new UnifiedBrainTreeApi(getBusinessId(), getPaymentRemoteAPI(), getRefundRemoteAPI());
    }

    /**
     * Updates the gateway key of the credit card associated with this payment. PayPal returns a TRANSACTIONID which can
     * be used to start new transaction without specifying payer info.
     *
     * @param payment
     *            successful payment containing the credit card to update.
     * */
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
     * Utility method to format the given dollar float value to a two digit number in compliance with the PayPal gateway
     * API.
     *
     * @param amount
     *            dollar float value to format
     * @return formatted amount as a string
     */
    private static String formatDollarAmount(BigDecimal amount) {
        amount = amount.abs().setScale(2, RoundingMode.HALF_EVEN); // gateway format, do not change!
        return amount.toPlainString();
    }

    /**
     * Utility method to check if a given {@link PaymentDTOEx} payment can be processed by this task.
     *
     * @param payment
     *            payment to check
     * @return true if payment can be processed with this task, false if not
     */
    private boolean isApplicable(PaymentDTOEx payment) throws PluggableTaskException {
        logger.debug("Is this payment applicable ", payment);
        if (piBl.isBTPayment(payment.getInstrument())) {
            return true;
        }
        logger.warn("Can't process payment if 'BT ID' is not provided");
        return false;
    }

    /**
     * Returns the name of this payment processor.
     * 
     * @return payment processor name
     */
    private String getProcessorName() {
        return BRAIN_TREE;
    }

    private static boolean isRefund(PaymentDTOEx payment) {
        return BigDecimal.ZERO.compareTo(payment.getAmount()) > 0 || payment.getIsRefund() != 0;
    }

    private PaymentAuthorizationDTO buildPaymentAuthorization(UnifiedBrainTreeResult brainTreeResult) {
        logger.debug("Payment authorization result of {} gateway parsing....", getProcessorName());

        PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
        paymentAuthDTO.setProcessor(getProcessorName());

        paymentAuthDTO.setCode1(StringUtils.EMPTY);
        String txID = brainTreeResult.getTransactionId();
        if (txID != null) {
            paymentAuthDTO.setTransactionId(txID);
            paymentAuthDTO.setCode1(txID);
            logger.debug("transactionId/code1 [{}]", txID);
        }

        paymentAuthDTO.setCardCode(brainTreeResult.getCardNumber());
        paymentAuthDTO.setCode2(brainTreeResult.getCardType());

        String errorCode = brainTreeResult.getErrorCode();
        String errorShortMsg = brainTreeResult.getErrorMessage();
        String statusDesc = brainTreeResult.getStatusDesc();
        String errorResponse = brainTreeResult.getErrorResponse();
        paymentAuthDTO.setResponseMessage(errorResponse.length() > 200 ? errorResponse.substring(0, 199)
                : errorResponse);
        logger.debug("errorMessage [{}]", errorCode);
        logger.debug("errorShortMessage [{}]", errorShortMsg);
        logger.debug("status description [{}]", statusDesc);

        String avs = brainTreeResult.getAvs();
        if (avs != null) {
            paymentAuthDTO.setAvs(avs);
            logger.debug("avs [{}]", avs);
        }

        return paymentAuthDTO;
    }

    private void storeBrainTreeResult(UnifiedBrainTreeResult result, PaymentDTOEx payment,
            PaymentAuthorizationDTO paymentAuthorization, boolean updateKey) {
        new PaymentAuthorizationBL().create(paymentAuthorization, payment.getId());
        payment.setAuthorization(paymentAuthorization);
        PaymentInformationDTO informationDTO = payment.getInstrument();
        if(null != informationDTO){
            informationDTO.setPaymentMethod(new PaymentMethodDAS().find(getPaymentMethod(
                    piBl.getPaymentMethodType(informationDTO), result.getCardType())));
            payment.setInstrument(informationDTO);
        }
        if (result.isSucceseded()) {
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_OK));
            if (updateKey) {
                updateGatewayKey(payment);
            }
        } else {
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_FAIL));
        }
    }

    private Result doRefund(PaymentDTOEx payment) throws PluggableTaskException {
        try {

            UnifiedBrainTreeApi api = getBTApi();

            PaymentBL bl = new PaymentBL(payment.getPayment().getId());
            payment.setPayment(bl.getDTOEx(new EntityBL(getEntityId()).getEntity().getLanguageId()));

            String transactionId = payment.getPayment().getAuthorization().getTransactionId();
            UnifiedBrainTreeResult result = api.refund(getTimeoutSeconds() * 1000, transactionId,
                    formatDollarAmount(payment.getAmount()));

            if (!payment.getAmount().equals(result.getAmount())) {
                logger.warn("Payment Amount :{} is not equal to payment amount {} done on gateway",
                        payment.getAmount(), result.getAmount());
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

    private Result doPaymentWithStoredBTID(PaymentDTOEx payment) throws PluggableTaskException {
        try {
            String gatewayKey = null != piBl.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.GATEWAY_KEY) ? new String(
                    piBl.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.GATEWAY_KEY)) : null;
            String type = piBl.getPaymentMethodType(payment.getInstrument());

            if (Arrays.stream(VALID_BT_PAYMENT_TYPE).noneMatch(x -> x.equalsIgnoreCase(type))) {
                logger.error("Couldn't handle payment request as BT payment type is not defined : {}", type);
                payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
                return NOT_APPLICABLE;
            }
            UnifiedBrainTreeResult result = getBTApi().receivePayment(getTimeoutSeconds() * 1000, gatewayKey,
                    formatDollarAmount(payment.getAmount()), type);

            PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(result);
            storeBrainTreeResult(result, payment, paymentAuthorization, false);

            return new Result(paymentAuthorization, false);

        } catch (Exception e) {
            logger.error("Couldn't handle payment request due to error", e);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        }
    }

    private boolean doProcess(PaymentDTOEx payment) throws PluggableTaskException {

        if (isRefund(payment)) {
            return doRefund(payment).shouldCallOtherProcessors();
        }

        return doPaymentWithStoredBTID(payment).shouldCallOtherProcessors();

    }

    public boolean process(PaymentDTOEx payment) throws PluggableTaskException {
        logger.debug("Payment processing for {} gateway", getProcessorName());

        if (payment.getPayoutId() != null) {
            return true;
        }

        if (!isApplicable(payment)) {
            return NOT_APPLICABLE.shouldCallOtherProcessors();
        }

        return doProcess(payment);
    }

    public void failure(Integer userId, Integer retry) {
        // do nothing
    }

    /**
     * As full creative is not storing the creditCard, delete is not possible, returning null as default value
     */
    public char[] deleteCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
        return new char[0];
    }

    /**
     * As full creative is not storing the creditCard, returning null as default value
     */
    public String storeCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
        return null;
    }

    /**
     * As full creative is not using pre-auth for payment, returning false as default value
     */
    public boolean preAuth(PaymentDTOEx payment) throws PluggableTaskException {
        return false;
    }

    /**
     * As full creative is not using pre-auth for payment, returning false as default value
     */
    public boolean confirmPreAuth(PaymentAuthorizationDTO auth, PaymentDTOEx paymentInfo) throws PluggableTaskException {
        return false;
    }

    /**
     * Helper method to identify the payment method
     *
     * @param paymentMethodType
     *            : Type defined in the Payment Method
     * @param cardType
     *            : Card Type which is received from the payment gateway as response
     * @return Integer paymentMethod
     */
    private Integer getPaymentMethod(String paymentMethodType, String cardType) {
        if (paymentMethodType.equalsIgnoreCase("btcc")) {
            if (CreditCardType.AMEX.toString().equalsIgnoreCase(cardType) || AMERICAN_EXPRESS.equalsIgnoreCase(cardType)) {
                return Constants.PAYMENT_METHOD_AMEX;
            } else if (CreditCardType.DINERS.toString().equalsIgnoreCase(cardType)) {
                return Constants.PAYMENT_METHOD_DINERS;
            } else if (CreditCardType.DISCOVER.toString().equalsIgnoreCase(cardType)) {
                return Constants.PAYMENT_METHOD_DISCOVER;
            } else if (CreditCardType.MAESTRO.toString().equalsIgnoreCase(cardType)) {
                return Constants.PAYMENT_METHOD_MAESTRO;
            } else if (CreditCardType.MASTER_CARD.toString().equalsIgnoreCase(cardType)) {
                return Constants.PAYMENT_METHOD_MASTERCARD;
            } else if (CreditCardType.VISA.toString().equalsIgnoreCase(cardType)) {
                return Constants.PAYMENT_METHOD_VISA;
            } else if (CreditCardType.VISA_ELECTRON.toString().equalsIgnoreCase(cardType)) {
                return Constants.PAYMENT_METHOD_VISA_ELECTRON;
            } else if (CreditCardType.JCB.toString().equalsIgnoreCase(cardType)) {
                return Constants.PAYMENT_METHOD_JCB;
            }
        } else if (paymentMethodType.equalsIgnoreCase("ach")) {
            return Constants.PAYMENT_METHOD_ACH;
        } else if (paymentMethodType.equalsIgnoreCase("ec")) {
            return Constants.PAYMENT_METHOD_PAYPAL_ECO;
        }
        return Constants.PAYMENT_METHOD_CUSTOM;
    }

}

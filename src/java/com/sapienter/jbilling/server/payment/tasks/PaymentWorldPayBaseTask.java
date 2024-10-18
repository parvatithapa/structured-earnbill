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

import java.io.DataOutputStream;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.util.List;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.util.ParameterParser;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.PaymentWorldPayBaseTask.SvcType;
import com.sapienter.jbilling.server.payment.tasks.worldpay.WorldPayPayerInfo;
import com.sapienter.jbilling.server.payment.tasks.worldpay.WorldpayResult;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Constants;
import com.worldpay.gateway.clearwater.client.core.dto.ApiError;
import com.worldpay.gateway.clearwater.client.core.dto.CountryCode;
import com.worldpay.gateway.clearwater.client.core.dto.CurrencyCode;
import com.worldpay.gateway.clearwater.client.core.dto.common.Address;
import com.worldpay.gateway.clearwater.client.core.dto.request.CardRequest;
import com.worldpay.gateway.clearwater.client.core.dto.request.OrderRequest;
import com.worldpay.gateway.clearwater.client.core.dto.request.TokenRequest;
import com.worldpay.gateway.clearwater.client.core.dto.response.CardResponse;
import com.worldpay.gateway.clearwater.client.core.dto.response.OrderResponse;
import com.worldpay.gateway.clearwater.client.core.dto.response.TokenResponse;
import com.worldpay.gateway.clearwater.client.core.exception.WorldpayException;
import com.worldpay.sdk.WorldpayRestClient;
import com.worldpay.sdk.util.HttpUrlConnection;
import com.worldpay.sdk.util.JsonParser;

/**
 * Abstract base class that contains all the common functionality needed to make a payment
 * to an RBS World Pay payment gateway.
 *
 * @author Brian Cowdery
 * @since 20-10-2009
 */
public abstract class PaymentWorldPayBaseTask extends PaymentTaskWithTimeout {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Parameters for RBS WorldPay payment gateway requests
     */
    public interface WorldPayParams {
        interface CreditCard {
            public static final String CARD_NUMBER      = "CardNumber";
            public static final String EXPIRATION_DATE  = "ExpirationDate"; // mm/yy or mm/yyyy
            public static final String CVV2             = "CVV2";           // optional CVV or CVC value
        }

        interface ReAuthorize {
            public static final String ORDER_ID = "OrderID";                // order id returned from a previously
        }                                                                   // successful transaction

        interface ForceParams {
            public static final String APPROVAL_CODE = "ApprovalCode";
        }

        interface SettleParams {
            public static final String ORDER_ID = "OrderID";                // order number of the transaction
        }

        /**
         * common parameters for ACH and credit card payment
         */
        interface General {
            public static final String SVC_TYPE       = "SvcType";          // @see SvcType
            public static final String FIRST_NAME     = "FirstName";
            public static final String LAST_NAME      = "LastName";
            public static final String STREET_ADDRESS = "StreetAddress";
            public static final String CITY           = "City";
            public static final String STATE          = "State";
            public static final String ZIP            = "Zip";
            public static final String COUNTRY        = "Country";
            public static final String AMOUNT         = "Amount";
        }
    }

    /**
     * RBS WorldPay gateway response parameters
     */
    public interface WorldPayResponse {
        public static final String TRANSACTION_STATUS = "TransactionStatus"; // @see TransactionStatus

        /*  transaction order number, which may be stored and used for subsequent payments
            through a re-authorization transaction. */
        public static final String ORDER_ID = "OrderId";

        /* approval codes returned by the Issuer if the authorization was approved */
        public static final String APPROVAL_CODE = "ApprovalCode";
        public static final String AVS_RESPONSE = "AVSResponse";            // Address Verification Service
        public static final String CVV2_RESPONSE = "CVV2Response";          // returned if CVV2 value was set

        public static final String ERROR_MSG = "ErrorMsg";
        public static final String ERROR_CODE = "ErrorCode";

    }

    /**
     * Represents the transaction type supported by the RBS WorldPay gateway.
     *
     * Please see <em>Appendix H: SVCTYPE</em> of the <em>API Specification - RBS WorldPay
     * Internet Processing Message Format</em> document.
     */
    public enum SvcType {
        AUTHORIZE       ("Authorize"),
        RE_AUTHORIZE    ("ReAuthorize"),
        SALE            ("Sale"),
        SETTLE          ("Settle"),
        FORCE           ("ForceSettle"),
        PARTIAL_SETTLE  ("PartialSettle"),
        REFUND_ORDER    ("CreditOrder"),
        REFUND_CREDIT   ("Credit");

        private String code;

        SvcType(String code) { this.code = code; }
        public String getCode() { return code; }
    }

    /**
     * Represents transaction status codes returned by the RBS WorldPay gateway.
     *
     * Please see <em>Appendix K: Transaction Status</em> of the <em>API Specification - RBS WorldPay
     * Internet Processing Message Format</em> document.
     */
    public enum TransactionStatus {
        APPROVED        ("0"),
        NOT_APPROVED    ("1"),
        EXCEPTION       ("2");

        private String code;

        TransactionStatus(String code) { this.code = code; }
        public String getCode() { return code; }
    }
    /**
     * Represents Worldpay specific metafields
     * */
    public enum MetaFieldName {
        CUSTOMER_BILLING_GROUP_NAME("Billing Contact Info Group Name");

        private String metaFieldName;

        private MetaFieldName(String metaFieldName) {
            this.metaFieldName = metaFieldName;
        }

        public String getMetaFieldName() {
            return metaFieldName;
        }
    }

    /**
     * Class for encapsulating authorization responses
     */
    public class WorldPayAuthorization {
        private final PaymentAuthorizationDTO paymentAuthDTO;

        public WorldPayAuthorization(String gatewayResponse) {
            logger.debug("Payment authorization result of {}  gateway parsing....",getProcessorName());

            WorldPayResponseParser responseParser = new WorldPayResponseParser(gatewayResponse);
            paymentAuthDTO = new PaymentAuthorizationDTO();
            paymentAuthDTO.setProcessor(getProcessorName());

            String approvalCode = responseParser.getValue(WorldPayResponse.APPROVAL_CODE); // Http response code
            if (approvalCode != null) {
                paymentAuthDTO.setApprovalCode(approvalCode);
                logger.debug("approvalCode [{}]",paymentAuthDTO.getApprovalCode());
            }

            String transactionStatus = responseParser.getValue(WorldPayResponse.TRANSACTION_STATUS); // paymentStatusOrderCode
            if (transactionStatus != null) {
                paymentAuthDTO.setCode2(transactionStatus);
                logger.debug("transactionStatus [{}]",paymentAuthDTO.getCode2());
            }

            String orderID = responseParser.getValue(WorldPayResponse.ORDER_ID); // OrderCode, customerOrderCode
            if (orderID != null) {
                paymentAuthDTO.setTransactionId(orderID);
                paymentAuthDTO.setCode1(orderID);
                logger.debug("transactionID/OrderID [{}]",paymentAuthDTO.getTransactionId());
            }

            String errorMsg = responseParser.getValue(WorldPayResponse.ERROR_MSG);
            if (errorMsg != null) {
                paymentAuthDTO.setResponseMessage(errorMsg);
                logger.debug("errorMessage [{}]", paymentAuthDTO.getResponseMessage());
            }
        }

        public WorldPayAuthorization(WorldpayResult result) {

            logger.debug("Payment authorization result of {} gateway parsing....",getProcessorName());

            paymentAuthDTO = new PaymentAuthorizationDTO();
            paymentAuthDTO.setProcessor(getProcessorName());

            String txID = result.getOrderCode(); // Transaction Id
            if (txID != null) {
                paymentAuthDTO.setTransactionId(txID);
                logger.debug("transactionId/code1 [{}]", txID);
            }

            if (txID != null) {
                paymentAuthDTO.setCode1(txID);
                logger.debug("transactionId [{}]", paymentAuthDTO.getCode1());
            }

            /*String merchantRefNumber = result.getMerchantRefNumber(); // MerchantRefNumber
            if(merchantRefNumber!=null) {
            	paymentAuthDTO.setCode2(merchantRefNumber);
            	LOG.debug("transactionStatus [" + paymentAuthDTO.getCode2() + "]");
            }*/

            if (result.getStatus() != null && !"EXCEPTION".equals(result.getStatus())) {
                logger.debug("Status : {}",result.getStatus());
                if (result.getStatus().equals("SUCCESS") ||
                        result.getStatus().equals("PARTIALLY_REFUNDED") ||
                        result.getStatus().equals("REFUNDED")) {
                    paymentAuthDTO.setCode2(TransactionStatus.APPROVED.getCode());
                } else {
                    paymentAuthDTO.setCode2(TransactionStatus.NOT_APPROVED.getCode());
                }
                logger.debug("transactionStatus [{}]",paymentAuthDTO.getCode2());
            }

            if(result.getHttpStatusCode() !=null) {
                paymentAuthDTO.setApprovalCode(result.getHttpStatusCode().toString());
                logger.debug("approval Code [{}]", paymentAuthDTO.getApprovalCode());
            }

            if(result.getErrorMessage() != null) {
                paymentAuthDTO.setResponseMessage(result.getErrorMessage());
                logger.debug("errorMessage [{}]",paymentAuthDTO.getResponseMessage());
            }

            if (result.getErrorCode() != null) {
                paymentAuthDTO.setCode3(result.getErrorCode());
                logger.debug("errorCode [{}]", paymentAuthDTO.getCode3());
            }

            String avs = result.getAvs();
            if(avs != null) {
                paymentAuthDTO.setAvs(avs);
                logger.debug("avs [{}]", avs);
            }

        }

        public PaymentAuthorizationDTO getDTO() {
            return paymentAuthDTO;
        }

        public Integer getJBResultId() {
            Integer resultId = Constants.RESULT_UNAVAILABLE;

            if (TransactionStatus.APPROVED.getCode().equals(paymentAuthDTO.getCode2())) {
                resultId = Constants.RESULT_OK;
            }

            if (TransactionStatus.NOT_APPROVED.getCode().equals(paymentAuthDTO.getCode2())) {
                resultId = Constants.RESULT_FAIL;
            }

            if (TransactionStatus.EXCEPTION.getCode().equals(paymentAuthDTO.getCode2())) {
                resultId = Constants.RESULT_UNAVAILABLE;
            }

            return resultId;
        }

        public boolean isCommunicationProblem() {
            return TransactionStatus.EXCEPTION.getCode().equals(paymentAuthDTO.getCode2());
        }
    }

    /**
     * Class for gateway response parsing
     */
    private class WorldPayResponseParser {
        private final String gatewayResponse;
        private List<NameValuePair> responseEntries;

        WorldPayResponseParser(String gatewayResponse) {
            this.gatewayResponse = gatewayResponse;
            parseResponse();
        }

        public String getGatewayResponse() {
            return gatewayResponse;
        }

        public List<NameValuePair> getResponseEntries() {
            return responseEntries;
        }

        public String getValue(String responseParamName) {
            String val = null;
            for (NameValuePair pair : responseEntries) {
                if (pair.getName().equals(responseParamName)) {
                    val = pair.getValue();
                    break;
                }
            }
            return val;
        }

        @SuppressWarnings("unchecked")
        private void parseResponse() {
            ParameterParser parser = new ParameterParser();
            responseEntries = parser.parse(gatewayResponse, '&');

        }
    }

    public static final DateTimeFormatter EXPIRATION_DATE_FORMAT = DateTimeFormat.forPattern("MM/yyyy");

    /* optional */
    public static final String PARAMETER_WORLD_PAY_URL = "URL";
    public static final String DEFAULT_WORLD_PAY_URL = "https://tpdev.lynksystems.com/servlet/LynkePmtServlet";


    public static final ParameterDescription PARAMETER_SERVICE_KEY =
            new ParameterDescription("ServiceKey", true, ParameterDescription.Type.STR);

    public static final ParameterDescription PARAMETER_CLIENT_KEY =
            new ParameterDescription("ClientKey", true, ParameterDescription.Type.STR);

    protected static final ParameterDescription PARAMETER_MAKE_ZERO_CENT_PAYMENT =
            new ParameterDescription("make zero cent payment for pre authorisation", false, ParameterDescription.Type.BOOLEAN);

    public PaymentWorldPayBaseTask() {
        descriptions.add(PARAMETER_SERVICE_KEY);
        descriptions.add(PARAMETER_CLIENT_KEY);
        descriptions.add(PARAMETER_MAKE_ZERO_CENT_PAYMENT);
    }


    // START: latest worldpay plugin parameters
    private static final String PARAMETER_TOKEN_URI = "TOKENURI";
    private static final String DEFAULT_TOKEN_URI = "https://api.worldpay.com/v1/tokens";
    // END



    private String url;

    // START:
    private String serviceKey;
    private String clientKey;
    private String tokenURI;
    // END

    public String getGatewayUrl() {
        if (url == null) {
            url = getOptionalParameter(PARAMETER_WORLD_PAY_URL, DEFAULT_WORLD_PAY_URL);
        }
        return url;
    }

    // START: Latest changes for worldpay integration
    public String getServiceKey() throws PluggableTaskException {
        serviceKey = ensureGetParameter(PARAMETER_SERVICE_KEY.getName());
        return serviceKey;
    }

    public String getClientKey() throws PluggableTaskException {
        clientKey = ensureGetParameter(PARAMETER_CLIENT_KEY.getName());
        return clientKey;
    }

    public String gettokenURI() {
        if (tokenURI == null) {
            tokenURI = DEFAULT_TOKEN_URI;
        }
        return tokenURI;
    }
    // END

    /**
     * Utility method to format the given dollar float value to a two
     * digit number in compliance with the RBS WorldPay gateway API.
     *
     * @param amount dollar float value to format
     * @return formatted amount as a string
     */
    public static String formatDollarAmount(BigDecimal amount) {
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
    public static boolean isApplicable(PaymentDTOEx payment) {
        PaymentInformationBL piBl = new PaymentInformationBL();
        if (!piBl.isCreditCard(payment.getInstrument()) && !piBl.isACH(payment.getInstrument())) {
            logger.warn("Can't process without a credit card or ach");
            return false;
        }
        return true;
    }

    /**
     * Returns the name of this payment processor.
     * @return payment processor name
     */
    abstract String getProcessorName();

    /**
     * Constructs a request of NameValuePairs for submission to the configured RBS WorldPay gateway, and
     * returns the NVPList object.
     *
     * @param payment payment to build the request for
     * @param transaction transaction type
     * @return request parameter name value pair list
     * @throws PluggableTaskException if an unrecoverable exception occurs
     */
    abstract OrderRequest buildOrderRequest(PaymentDTOEx payment, SvcType transaction) throws PluggableTaskException;

    /**
     * Process a payment as per the given transaction SvcType. This method relies on the abstract
     * {@link #buildRequest(PaymentDTOEx, SvcType)} method to build the appropriate set of HTTP request
     * parameters for the required transaction/process.
     *
     * @param payment payment to process
     * @param transaction transaction type
     * @param auth payment pre-authorization, may be null.
     * @return payment result
     * @throws PluggableTaskException thrown if payment instrument is not a credit card, or if a refund is attempted with no authorization
     */
    protected Result doProcess(PaymentDTOEx payment, SvcType transaction, PaymentAuthorizationDTO auth)
            throws PluggableTaskException {
        PaymentInformationBL piBl = new PaymentInformationBL();
        if (!isApplicable(payment)) {
            return NOT_APPLICABLE;
        }

        if (!piBl.isCreditCard(payment.getInstrument())) {
            logger.error("Can't process without a credit card");
            throw new PluggableTaskException("Credit card not present in payment");
        }

        if (piBl.isACH(payment.getInstrument())) {
            logger.error("Can't process with a cheque");
            throw new PluggableTaskException("Can't process ACH charge");
        }

        OrderRequest orderRequest = buildOrderRequest(payment, transaction);

        /* Earlier payment gateway integration code, need to check and remove if not required.
         * if (auth != null && !SvcType.RE_AUTHORIZE.equals(transaction)) {
            // add approvalCode & orderID parameters for this settlement transaction
            request.add(WorldPayParams.ForceParams.APPROVAL_CODE, auth.getApprovalCode());
            request.add(WorldPayParams.SettleParams.ORDER_ID, auth.getTransactionId());
        }*/

        if (payment.getIsRefund() == 1
                && (payment.getPayment() == null || payment.getPayment()
                .getAuthorization() == null)) {
            logger.error("Can't process refund without a payment with an authorization record");
            throw new PluggableTaskException("Refund without previous authorization");
        }

        try {
            logger.debug("Processing {}  for credit card" ,transaction);

            WorldpayResult result = postPaymentRequestToGateWay(orderRequest);
            WorldPayAuthorization wrapper = new WorldPayAuthorization(result);

            payment.setPaymentResult(new PaymentResultDAS().find(wrapper.getJBResultId()));

            // if transaction successful store it
            if (wrapper.getJBResultId().equals(Constants.RESULT_OK)) {
                storeProcessedAuthorization(payment, wrapper.getDTO());
            }
            return new Result(wrapper.getDTO(), wrapper.isCommunicationProblem());

        } catch (Exception e) {
            logger.error("Couldn't handle payment request due to error", e);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        }
    }

    protected CardRequest createCardRequest(String creditCardNumber,
            String cardHolderName, int expiryMonth, int expiryYear, char[] cvv) {
        CardRequest cardRequest = new CardRequest();
        cardRequest.setCardNumber(creditCardNumber);
        if (null != cvv) {
            cardRequest.setCvc(cvv.toString()); // Required in case of card security code is switch on in your risk setting
        }
        cardRequest.setName(cardHolderName);
        cardRequest.setExpiryMonth(expiryMonth);
        cardRequest.setExpiryYear(expiryYear);

        return cardRequest;
    }

    protected String createToken(CardRequest cardRequest, Boolean reusable) throws PluggableTaskException {
        TokenRequest tokenRequest = new TokenRequest(cardRequest, reusable);
        tokenRequest.setClientKey(getClientKey());
        return getToken(tokenRequest);
    }

    private String getToken(TokenRequest tokenRequest) throws PluggableTaskException {
        final String json = JsonParser.toJson(tokenRequest);


        try {
            HttpURLConnection httpURLConnection = HttpUrlConnection
                    .getConnection(gettokenURI());
            httpURLConnection.setRequestMethod(RequestMethod.POST.toString());
            DataOutputStream dataOutputStream = new DataOutputStream(
                    httpURLConnection.getOutputStream());
            dataOutputStream.writeBytes(json);

            TokenResponse tokenResponse = JsonParser.toObject(
                    httpURLConnection.getInputStream(), TokenResponse.class);
            return tokenResponse.getToken();
        } catch (Exception e) {
            throw new PluggableTaskException(e);
        }
    }

    protected WorldpayResult postPaymentRequestToGateWay(OrderRequest orderRequest)
            throws PluggableTaskException {
        try {
            WorldpayRestClient restClient = getRestClient();
            OrderResponse orderResponse = restClient.getOrderService().create(orderRequest);
            String customerorderCode= orderResponse.getCustomerOrderCode();
            String orderCode=orderResponse.getOrderCode();
            String paymentStatus= orderResponse.getPaymentStatus();
            WorldpayResult result = new WorldpayResult.WorldpayResultBuilder()
            .setOrderCode(orderCode)
            .setStatus(paymentStatus)
            .setCustomerOrderCode(customerorderCode)
            .setSucceeded(true)
            .token(orderResponse.getToken())
            .build();
            logger.debug("Order response : {}", orderResponse);
            return result;
        } catch (WorldpayException exception) {
            WorldpayResult result = new WorldpayResult.WorldpayResultBuilder().setSucceeded(false).build();
            logger.error("Error code: {}",exception.getApiError().getCustomCode());
            logger.error("Error description: {}", exception.getApiError().getDescription());
            logger.error("Error message: {}", exception.getApiError().getMessage());
            logger.error("exception.getCause() {}: ", exception.getCause());
            logger.error("exception.getMessage() : {}", exception.getMessage());
            logger.error("exception.getLocalizedMessage() : {}",exception.getLocalizedMessage());

            ApiError apiError = exception.getApiError();
            if (apiError != null) {
                String errorCode = apiError.getCustomCode();
                String errorMessage = apiError.getMessage();
                String errorDescription = apiError.getDescription();
                Integer httpStatusCode = apiError.getHttpStatusCode();
                result = new WorldpayResult.WorldpayResultBuilder()
                .setSucceeded(false)
                .setErrorCode(errorCode)
                .setErrorMessage(errorMessage)
                .setErrorDescription(errorDescription)
                .setHttpStatusCode(httpStatusCode)
                .setStatus("EXCEPTION")
                .build();
                logger.error(apiError.getCustomCode(), exception);
            }
            return result;
        }
    }

    protected OrderRequest createOrderRequest(String token, int amount,
            CurrencyCode currencyCode, WorldPayPayerInfo payerInfo,
            String orderDescription, boolean authorized) {

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setToken(token);
        orderRequest.setAmount(amount);
        orderRequest.setCurrencyCode(currencyCode);
        orderRequest.setName(payerInfo.getCardHolderName());
        orderRequest.setOrderDescription(orderDescription);
        orderRequest.setBillingAddress(getBillingAddress(payerInfo));
        if (authorized) {
            orderRequest.setAuthorizeOnly(true);
        }
        return orderRequest;
    }

    public Address getBillingAddress(WorldPayPayerInfo payerInfo) {

        Address address = new Address();
        address.setAddress1(payerInfo.getStreet());
        address.setCity(payerInfo.getCity());
        address.setCountryCode(CountryCode.fromValue(payerInfo.getCountryCode()));
        address.setPostalCode(payerInfo.getZip());

        return address;
    }

    protected WorldpayRestClient getRestClient() throws PluggableTaskException {

        return new WorldpayRestClient(getServiceKey());
    }

    protected boolean validateToken(String token) throws PluggableTaskException {

        try {
            WorldpayRestClient restClient = getRestClient();
            TokenResponse tokenResponse = restClient.getTokenService().get(token);
            CardResponse cardResponse = (CardResponse) tokenResponse.getCommonToken().getPaymentMethod();

            logger.debug("Name: {}", tokenResponse.getCommonToken().getPaymentMethod().getName());
            logger.debug("Expiry Month: {}", cardResponse.getExpiryMonth());
            logger.debug("Expiry Year: {}" , cardResponse.getExpiryYear());
            logger.debug("Card Type: {}" , cardResponse.getCardType());
            logger.debug("Masked Card Number: {}" , cardResponse.getMaskedCardNumber());
            return true;
        } catch (WorldpayException e) {
            ApiError apiError = e.getApiError();
            logger.error("Error code: {}" , apiError.getCustomCode());
            logger.error("Error description: {}" , apiError.getDescription());
            logger.error("Error message: {}" , apiError.getMessage());
            logger.error("Invalid token");
            throw new PluggableTaskException(apiError.getCustomCode() + apiError.getMessage(), e);
        }
    }

}

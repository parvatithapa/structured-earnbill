package com.sapienter.jbilling.paymentUrl.client;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import com.cashfree.model.UpiAdvanceResponseSchema;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlType;
import com.sapienter.jbilling.paymentUrl.domain.PaymentConfiguration;
import com.sapienter.jbilling.paymentUrl.domain.response.PaymentResponse;
import com.sapienter.jbilling.paymentUrl.util.PaymentUrlConstants;

public class PaymentGatewayClient {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CANCEL_PATH = "payment/link/cancel";
    private static final String UPI_QR_PATH = "payment/upiqr";
    private static final String UPI_INTENT_PATH = "payment/upiintent";
    private static final String CARD_PATH = "payment/card";
    private static final String CHECK_STATUS_PATH = "payment/status";
    private static final String VERIFICATION_UPI_PATH = "verification/upi";

    private static String X_ENTITY_ID = "x-entityid";

    private Integer entityId;

    private String baseUrl;

    private String apiKey;
    private int timeout = 10_000;

    public PaymentGatewayClient(String url, String apiKey, Integer entityId) {
        Assert.hasLength(url, "url is required parameter");
        Assert.hasLength(String.valueOf(entityId), "entityId is required parameter");
        Assert.hasLength(apiKey, "apiKey is required parameter");
        this.baseUrl = url.endsWith("/") ? url : (url + "/");
        this.apiKey = apiKey;
        this.entityId = entityId;
    }

    public PaymentGatewayClient(String url, String apiKey, Integer entityId, int timeout) {
        this(url, apiKey, entityId);
        this.timeout = timeout;
    }

    /**
     * Calls Payment Gateway Client to generate upi link api.
     *
     * @param paymentConfiguration
     * @param paymentUrlType
     * @return {@link PaymentResponse}
     */
    public PaymentResponse generatePaymentLink(PaymentConfiguration paymentConfiguration, PaymentUrlType paymentUrlType) {
        RestTemplate restTemplate = createRestTemplate();
        String url = baseUrl + UPI_INTENT_PATH;
        if (paymentUrlType.equals(PaymentUrlType.LINK)) {
            url = baseUrl + CARD_PATH;
        }
        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(url,
            paymentConfiguration,PaymentResponse.class);
        if (null != response) {
            HttpStatus statusCode = response.getStatusCode();
            if(statusCode.is2xxSuccessful()){
                PaymentResponse responseBody = response.getBody();
                if(responseBody.getResponseStatus()) {
                    logger.debug("paymentGatewayRestTemplateClient.generateUpiLink == body == {}",responseBody);
                }
            }
            if(statusCode.is5xxServerError()){
                logger.debug("paymentGatewayRestTemplateClient.generateUpiLink==== failed======");
            }
        }
        return response.getBody();
    }

    /**
     * Calls Payment Gateway Client to check the payment status.
     *
     * @param paymentConfiguration
     * @return {@link PaymentResponse}
     */
    public PaymentResponse checkPaymentStatus(PaymentConfiguration paymentConfiguration) {
        RestTemplate restTemplate = createRestTemplate();

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(baseUrl + CHECK_STATUS_PATH,
            paymentConfiguration,PaymentResponse.class);
        if (null != response) {
            HttpStatus statusCode = response.getStatusCode();
            if(statusCode.is2xxSuccessful()){
                PaymentResponse responseBody = response.getBody();
                logger.debug("paymentGatewayRestTemplateClient.generateUpiLink == body == {}",responseBody);
            }
            if(statusCode.is5xxServerError()){
                logger.debug("paymentGatewayRestTemplateClient.generateUpiLink==== failed======");
            }
        }
        return response.getBody();
    }

    /**
     * Calls Payment Gateway Client to fetch the verificationData using UPI ID.
     *
     * @param paymentConfiguration
     * @return {@link UpiAdvanceResponseSchema}
     */
    public UpiAdvanceResponseSchema getVerificationData(PaymentConfiguration paymentConfiguration) {
        RestTemplate restTemplate = createRestTemplate();

        ResponseEntity<UpiAdvanceResponseSchema> response = restTemplate.postForEntity(baseUrl + VERIFICATION_UPI_PATH,
            paymentConfiguration, UpiAdvanceResponseSchema.class);
        if (null != response) {
            HttpStatus statusCode = response.getStatusCode();
            if (statusCode.is2xxSuccessful()) {
                UpiAdvanceResponseSchema responseBody = response.getBody();
                logger.debug("paymentGatewayRestTemplateClient.getVerificationData == body == {}", responseBody);
            }
            if (statusCode.is5xxServerError()) {
                logger.debug("paymentGatewayRestTemplateClient.getVerificationData==== failed======");
            }
        }
        return response.getBody();
    }

    private SimpleClientHttpRequestFactory httpRequestFactory() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        // Connect timeout
        clientHttpRequestFactory.setConnectTimeout(timeout);
        // Read timeout
        clientHttpRequestFactory.setReadTimeout(timeout);
        return clientHttpRequestFactory;
    }

    private RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory());
        restTemplate.setErrorHandler(new PaymentGatewayExceptionHandler());
        List<ClientHttpRequestInterceptor> interceptors = CollectionUtils
            .isEmpty(restTemplate.getInterceptors()) ? new ArrayList<>()
            : restTemplate.getInterceptors();
        interceptors.add(new PaymentGatewayClient.AddAuthHeader());
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    private class AddAuthHeader implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set(HttpHeaders.ACCEPT,
                MediaType.APPLICATION_JSON_VALUE);
            request.getHeaders().set(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE);
            request.getHeaders().set(X_ENTITY_ID,
                String.valueOf(entityId));
            request.getHeaders().set(PaymentUrlConstants.HEADER_X_API_KEY,apiKey);
            // adding auth token.
//            request.getHeaders().set(AUTH_HEADER_NAME, authToken);
            return execution.execute(request, body);
        }
    }

    public PaymentResponse cancelPaymentUrl(PaymentConfiguration paymentConfiguration) {
        RestTemplate restTemplate = createRestTemplate();

        ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(baseUrl + CANCEL_PATH,
                paymentConfiguration, PaymentResponse.class);
        if( null != response ) {
            HttpStatus statusCode = response.getStatusCode();
            if( statusCode.is2xxSuccessful() ) {
                PaymentResponse responseBody = response.getBody();
                logger.debug("paymentGatewayRestTemplateClient.cancelPaymentUrlLink == body == {}", responseBody);
            }
            if( statusCode.is5xxServerError() ) {
                logger.debug("paymentGatewayRestTemplateClient.cancelPaymentUrlLink==== failed======");
            }
        }
        return response.getBody();
    }
}

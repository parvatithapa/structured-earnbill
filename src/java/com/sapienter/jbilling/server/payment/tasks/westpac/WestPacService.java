package com.sapienter.jbilling.server.payment.tasks.westpac;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class WestPacService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ObjectMapper ERROR_UNMARSHALLER;
    private static final int DEFAULT_TIME_OUT = 10000;
    private static final RestOperations DEFAULT_REST_TEMPLATE;
    private static final String TOEKN_URL = "single-use-tokens";
    private static final String CUSTOMER_URL = "customers/";
    private static final String PAYMENT_URL = "transactions";
    private static final String INVALID_CREDENTIAL_MESSAGE = "Invalid credential Provided";
    private static final String AUTH_FAILED_MESSAGE = "Authentication Failed";
    private static final String GATE_WAY_SERVER_ERROR_MESSAGE = "Failed Beacuse of Gateway server error";
    private String publishableKey;
    private String secretKey;
    private RestOperations restOperations;
    private String url;

    static {
        ERROR_UNMARSHALLER = new ObjectMapper();
        ERROR_UNMARSHALLER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        DEFAULT_REST_TEMPLATE = new RestTemplate(getClientHttpRequestFactory(DEFAULT_TIME_OUT));
    }

    public WestPacService(String url, String publishableKey, String secretKey, Integer timeout) {
        this.publishableKey = publishableKey;
        this.secretKey = secretKey;
        this.url = url.endsWith("/") ? url : (url + "/");
        this.restOperations = timeout.intValue() == DEFAULT_TIME_OUT ? DEFAULT_REST_TEMPLATE :
            new RestTemplate(getClientHttpRequestFactory(timeout));
    }

    private static ClientHttpRequestFactory getClientHttpRequestFactory(int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        return new HttpComponentsClientHttpRequestFactory( HttpClientBuilder.create().setDefaultRequestConfig(config).build() );
    }

    /**
     * Sends Card info to westPac gateway for singleUseTokenId
     * @param cardInfo
     * @return
     */
    public TokenResponse generateOneTimeToken(MultiValueMap<String, String> cardInfo) throws PluggableTaskException {
        try {
            String tokenUrl = this.url + TOEKN_URL;
            logger.debug("Sending token request to url {} with parameters {}", tokenUrl, cardInfo);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(cardInfo, constructAuthHeaderFromCredential(publishableKey));
            return restOperations.exchange(tokenUrl, HttpMethod.POST, request, TokenResponse.class).getBody();
        } catch(HttpClientErrorException ex) {
            if(ex.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
                logger.error(INVALID_CREDENTIAL_MESSAGE, ex);
                throw new PluggableTaskException(AUTH_FAILED_MESSAGE, ex);
            } else if(ex.getRawStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                PayWayError error = logError(ex);
                if(null == error) {
                    throw new PluggableTaskException("Customer Token creation failed on WestPac", ex);
                }
                throw new PluggableTaskException("Customer Token creation failed, "+ error.getErrorMessage() + " passed in request", ex);
            } else {
                throw new PluggableTaskException(ex);
            }
        } catch(HttpServerErrorException ex) {
            logger.error(GATE_WAY_SERVER_ERROR_MESSAGE, ex);
            throw new PluggableTaskException(GATE_WAY_SERVER_ERROR_MESSAGE, ex);
        } catch(Exception ex) {
            logger.error("Error in generateOneTimeToken", ex);
            throw new PluggableTaskException(ex);
        }
    }

    public String createCustomer(String appGeneratedGatewayKey, MultiValueMap<String, String> params) throws PluggableTaskException {
        try {
            String customerUrl = url + CUSTOMER_URL;
            logger.debug("Sending customer request to url {} with parameters {}", customerUrl, params);
            HttpEntity<MultiValueMap<String, String>> customerRequest = new HttpEntity<>(params, constructAuthHeaderFromCredential(secretKey));
            restOperations.exchange( customerUrl + appGeneratedGatewayKey, HttpMethod.PUT, customerRequest, String.class);
            logger.debug("Customer created on west pac with key {}", appGeneratedGatewayKey);
            return appGeneratedGatewayKey;
        } catch(HttpClientErrorException ex) {
            if(ex.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
                logger.error(INVALID_CREDENTIAL_MESSAGE, ex);
                throw new PluggableTaskException(AUTH_FAILED_MESSAGE, ex);
            } else if(ex.getRawStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                PayWayError error = logError(ex);
                if(error == null) {
                    throw new PluggableTaskException("Customer creation failed on WestPac", ex);
                }
                throw new PluggableTaskException("Customer creation failed, "+ error.getErrorMessage() + " passed in request", ex);
            } else {
                throw new PluggableTaskException(ex);
            }
        } catch(HttpServerErrorException ex) {
            logger.error(GATE_WAY_SERVER_ERROR_MESSAGE, ex);
            throw new PluggableTaskException(GATE_WAY_SERVER_ERROR_MESSAGE, ex);
        } catch(Exception ex) {
            logger.error("Error in createCustomer", ex);
            throw new PluggableTaskException(ex);
        }
    }

    /**
     * Sends Payment Request to gateway and construct {@link PaymentResponse}
     * @param params
     * @return
     * @throws PluggableTaskException
     */
    public PaymentResponse createPayment(MultiValueMap<String, String> params) throws PluggableTaskException {
        try {
            String paymentUrl = url + PAYMENT_URL;
            logger.debug("Sending Payment request to url {} with parameters {}", paymentUrl, params);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, constructAuthHeaderFromCredential(secretKey));
            PaymentResponse response = restOperations.exchange(paymentUrl, HttpMethod.POST, request, PaymentResponse.class).getBody();
            logger.debug("payment done {}", response);
            return response;
        } catch(HttpClientErrorException ex) {
            if(ex.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
                logger.error(INVALID_CREDENTIAL_MESSAGE, ex);
                throw new PluggableTaskException(AUTH_FAILED_MESSAGE, ex);
            } else if(ex.getRawStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                PaymentResponse response = new PaymentResponse();
                response.setStatus("failed");
                response.setResponseCode(ex.getRawStatusCode()+ "");
                try {
                    PayWayError error = ERROR_UNMARSHALLER.readValue(ex.getResponseBodyAsString(), PayWayError.class);
                    response.setError(error);
                    response.setResponseText(error.getErrorMessage());
                } catch (IOException ioEx) {
                    logger.error("Json to java converion failed!", ioEx);
                }
                return response;
            } else {
                throw new PluggableTaskException(ex);
            }
        } catch(HttpServerErrorException ex) {
            logger.error(GATE_WAY_SERVER_ERROR_MESSAGE, ex);
            throw new PluggableTaskException(GATE_WAY_SERVER_ERROR_MESSAGE, ex);
        } catch(Exception ex) {
            logger.error("Error in createPayment", ex);
            throw new PluggableTaskException(ex);
        }
    }

    /**
     * Convert Error Response into {@link PayWayError} for HttpStatus.UNPROCESSABLE_ENTITY
     * @param ex
     */
    private PayWayError logError(HttpClientErrorException ex) {
        if(ex.getRawStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            try {
                if(StringUtils.isEmpty(ex.getResponseBodyAsString())) {
                    return null;
                }
                PayWayError error = ERROR_UNMARSHALLER.readValue(ex.getResponseBodyAsString(), PayWayError.class);
                if(null!= error) {
                    logger.error("Request Failed {}", error);
                }
                return error;
            } catch(IOException ioEx) {
                logger.error("Failed Error Object creation", ioEx);
                return null;
            }
        }
        return null;
    }

    private HttpHeaders constructAuthHeaderFromCredential(String credential) {
        byte[] key = getEncodedKey(credential);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", "Basic " + new String(key));
        return HttpHeaders.readOnlyHttpHeaders(headers);
    }

    private byte[] getEncodedKey(String key) {
        return Base64.encodeBase64(key.getBytes(Charset.forName("US-ASCII")));
    }
}

package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.resources.OrderInfo;
import com.sapienter.jbilling.server.order.OrderWS;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vojislav Stanojevikj
 * @since 12-Oct-2016.
 */
final class JBillingRestTemplate {

    private static final int DEFAULT_TIMEOUT = 5000;
    private final RestOperations restOperations;

    private static final Map<Integer, JBillingRestTemplate> CACHED_INSTANCES = new HashMap<>();

    static {
        CACHED_INSTANCES.put(DEFAULT_TIMEOUT, new JBillingRestTemplate(DEFAULT_TIMEOUT));
    }

    private JBillingRestTemplate(int timeout) {
        this.restOperations = new RestTemplate(getClientHttpRequestFactory(timeout));
    }

    public static JBillingRestTemplate getInstance(){
        return CACHED_INSTANCES.get(DEFAULT_TIMEOUT);
    }

    public static JBillingRestTemplate getInstance(int timeout){
        if (timeout != DEFAULT_TIMEOUT){
            CACHED_INSTANCES.put(timeout, new JBillingRestTemplate(timeout));
        }
        return CACHED_INSTANCES.get(timeout);
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory(int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        return new HttpComponentsClientHttpRequestFactory(
                HttpClientBuilder.create().setDefaultRequestConfig(config).build());
    }

    public RestOperations getRestOperations() {
        return restOperations;
    }

    public <T> ResponseEntity<T> sendRequest(String url, HttpMethod method, HttpHeaders headers, T requestBody){
        return restOperations.exchange(url, method,
                new HttpEntity<>(requestBody, headers), new ParameterizedTypeReference<T>(){});
    }

    public <T> ResponseEntity<T> sendRequest(String url, HttpMethod method, HttpHeaders headers,
                                             Object requestBody, Class<T> responseClass){
        return restOperations.exchange(url, method,
                new HttpEntity<>(requestBody, headers), responseClass);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JBillingRestTemplate that = (JBillingRestTemplate) o;

        return !(restOperations != null ? !restOperations.equals(that.restOperations) : that.restOperations != null);

    }

    @Override
    public int hashCode() {
        return restOperations != null ? restOperations.hashCode() : 0;
    }
}

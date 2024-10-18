package com.sapienter.jbilling.appdirect.subscription.oauth;

import com.sapienter.jbilling.appdirect.subscription.http.ResponseErrorHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth.common.signature.SharedConsumerSecretImpl;
import org.springframework.security.oauth.consumer.BaseProtectedResourceDetails;
import org.springframework.security.oauth.consumer.ProtectedResourceDetails;
import org.springframework.security.oauth.consumer.client.OAuthRestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by vaibhavr on 10/07/16.
 */
@Service
public class OAuthRestTemplateService {

	private AtomicReference<RestTemplate>  restTemplateRef = new AtomicReference<>(null);
	private ClientHttpRequestFactory requestFactory;

	@PostConstruct
	public void init() {
		this.requestFactory = new RequestFactory();
	}

	private Integer connectTimeout;
	private Integer connectionRequestTimeout;

	public RestTemplate instance(String consumerKey, String consumerSecret) {
		RestTemplate restTemplate = restTemplateRef.get();
		if (restTemplate == null) {
			restTemplate = new OAuthRestTemplate(requestFactory, authDetails(consumerKey, consumerSecret));
			restTemplate.setErrorHandler(new ResponseErrorHandler());

			if (!restTemplateRef.compareAndSet(null, restTemplate)) {
				restTemplate = restTemplateRef.get();
			}
		}

		return restTemplate;
	}

	public RestTemplate refresh(String consumerKey, String consumerSecret) {

		RestTemplate oldRestTemplate = restTemplateRef.get();
		RestTemplate restTemplate = new OAuthRestTemplate(requestFactory, authDetails(consumerKey, consumerSecret));
		restTemplate.setErrorHandler(new ResponseErrorHandler());

		if (!restTemplateRef.compareAndSet(oldRestTemplate, restTemplate)) {
			restTemplate = restTemplateRef.get();
		}

		return restTemplate;
	}

	private ProtectedResourceDetails authDetails(String consumerKey, String consumerSecret) {
		BaseProtectedResourceDetails authDetails = new BaseProtectedResourceDetails();
		authDetails.setConsumerKey(consumerKey);
		authDetails.setSharedSecret(new SharedConsumerSecretImpl(consumerSecret));
		return authDetails;
	}

	public void setConnectTimeout(Integer connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setConnectionRequestTimeout(Integer connectionRequestTimeout) {
		this.connectionRequestTimeout = connectionRequestTimeout;
	}


	private class RequestFactory extends HttpComponentsClientHttpRequestFactory {

		RequestFactory() {
			setHttpClient(httpClient());
		}

		private CloseableHttpClient httpClient() {
			return HttpClientBuilder
					.create()
					.setConnectionManager(connectionManager())
					.setDefaultRequestConfig(requestConfig())
					.build();
		}

		private PoolingHttpClientConnectionManager connectionManager() {
			PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();
			connMgr.setDefaultMaxPerRoute(4);
			return connMgr;
		}

		private RequestConfig requestConfig() {
			RequestConfig result = RequestConfig.custom()
					.setConnectionRequestTimeout(connectionRequestTimeout)
					.setConnectTimeout(connectTimeout)
					.build();
			return result;
		}
	}
}

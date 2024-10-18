package com.sapienter.jbilling.einvoice.providers.gstzen.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import com.sapienter.jbilling.einvoice.domain.EInvoiceRequest;
import com.sapienter.jbilling.einvoice.domain.EInvoiceResponse;

public class GstZenClient {

	private static final String AUTH_HEADER_NAME = "Token";
	private static final String CANCEL_PATH = "cancel/";

	private String authToken;
	private String url;
	private int timeout = 10_000;

	public GstZenClient(String url, String authToken) {
		Assert.hasLength(url, "url is required parameter");
		Assert.hasLength(authToken, "authToken is required parameter");
		this.url = url.endsWith("/") ? url : (url + "/");
		this.authToken = authToken;
	}

	public GstZenClient(String url, String authToken, int timeout) {
		this(url, authToken);
		this.timeout = timeout;
	}

	/**
	 * Calls GstZen create e invoice api.
	 *
	 * @param eInvoiceRequest
	 * @return {@link EInvoiceResponse}
	 */
	public EInvoiceResponse createEInvoice(EInvoiceRequest eInvoiceRequest) {
		RestTemplate restTemplate = createRestTemplate();
		EInvoiceResponse eInvoiceResponse = restTemplate.postForEntity(url,
				new HttpEntity<>(eInvoiceRequest), EInvoiceResponse.class)
				.getBody();
		if (eInvoiceResponse.getResponseStatus() == 0) {
			throw new GstZenClientException(200, eInvoiceResponse.getUuid(),
					eInvoiceResponse.getMessage());
		}
		return eInvoiceResponse;
	}

	public EInvoiceResponse cancelEInvoice(EInvoiceRequest eInvoiceRequest) {
		RestTemplate restTemplate = createRestTemplate();
		EInvoiceResponse eInvoiceResponse = restTemplate.postForEntity(url + CANCEL_PATH,
				new HttpEntity<>(eInvoiceRequest), EInvoiceResponse.class)
				.getBody();
		if (eInvoiceResponse.getResponseStatus() == 0) {
			throw new GstZenClientException(200, eInvoiceResponse.getUuid(),
					eInvoiceResponse.getMessage());
		}
		return eInvoiceResponse;
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
		restTemplate.setErrorHandler(new GstZenExceptionHandler());
		List<ClientHttpRequestInterceptor> interceptors = CollectionUtils
				.isEmpty(restTemplate.getInterceptors()) ? new ArrayList<>()
						: restTemplate.getInterceptors();
				interceptors.add(new AddAuthHeader());
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
			// adding auth token.
			request.getHeaders().set(AUTH_HEADER_NAME, authToken);
			return execution.execute(request, body);
		}
	}
}

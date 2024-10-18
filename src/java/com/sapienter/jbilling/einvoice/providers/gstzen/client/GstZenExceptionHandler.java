package com.sapienter.jbilling.einvoice.providers.gstzen.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GstZenExceptionHandler extends DefaultResponseErrorHandler {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<HashMap<String, Object>> TYPE_REF = new TypeReference<HashMap<String, Object>>() {
	};

	@Override
	public void handleError(ClientHttpResponse httpResponse) throws IOException {
		try {
			// call parent class default error handler method.
			super.handleError(httpResponse);
		} catch (RestClientResponseException restClientResponseException) {
			Map<String, Object> errorResponse = OBJECT_MAPPER.readValue(
					restClientResponseException.getResponseBodyAsString(),
					TYPE_REF);
			String message = (String) errorResponse.getOrDefault("message",
					restClientResponseException.getLocalizedMessage());
			throw new GstZenClientException(
					restClientResponseException.getRawStatusCode(), message);
		} catch (Exception e) {
			throw new GstZenClientException(500, e.getLocalizedMessage());
		}
	}
}

/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech.
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.client.crm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CRMExceptionHandler extends DefaultResponseErrorHandler {

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
			String message = (String) errorResponse.getOrDefault("Details",
					restClientResponseException.getLocalizedMessage());
			throw new CRMException(restClientResponseException.getRawStatusCode(), UUID.randomUUID().toString(), message);
		} catch (Exception e) {
			throw new CRMException(500, UUID.randomUUID().toString(), e.getLocalizedMessage());
		}
	}
}

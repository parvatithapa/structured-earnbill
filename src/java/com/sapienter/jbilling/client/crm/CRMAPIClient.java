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

import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;

import java.io.IOException;
import java.util.Collections;

public class CRMAPIClient {

    private final TokenService tokenService;
    private final RestTemplate restTemplate;
    private Integer entityId;

    public CRMAPIClient(TokenService tokenService, RestTemplate restTemplate) {
        this.tokenService = tokenService;
        this.restTemplate = restTemplate;
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR;
            }
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    tokenService.refreshToken(entityId); // Refresh token on unauthorized error
                }
            }
        });
        this.restTemplate.setUriTemplateHandler(new DefaultUriTemplateHandler() {
            @Override
            public boolean isStrictEncoding() {
                return true;
            }
        });
    }

    public <T> T exchangeForObject(String url, HttpMethod method, String payload, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getAccessToken(entityId));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = (payload == null) ? new HttpEntity<>(headers) : new HttpEntity<>(payload, headers);
        ResponseEntity<T> response = restTemplate.exchange(url, method, requestEntity, responseType);
        return response.getBody();
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }
}

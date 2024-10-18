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

import static com.sapienter.jbilling.client.crm.CRMConstants.BASE_URL;
import static com.sapienter.jbilling.client.crm.CRMConstants.CRM_CLIENT_ID;
import static com.sapienter.jbilling.client.crm.CRMConstants.CRM_CLIENT_SECRET;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Map;

import com.sapienter.jbilling.server.crm.model.CRMResponse;

public class TokenService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TOKEN_ENDPOINT = "/auth/token";

    private final RestTemplate restTemplate;
    private final CRMHelperService crmHelperService;
    private String accessToken;

    public TokenService(RestTemplate restTemplate, CRMHelperService crmHelperService) {
        this.restTemplate = restTemplate;
        this.crmHelperService = crmHelperService;
        //refreshToken(entityId); // Initialize with a token
    }

    public String getAccessToken(Integer entityId) {
        if (accessToken == null || isTokenExpired()) {
            refreshToken(entityId);
        }
        return accessToken;
    }

    public void refreshToken(Integer entityId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            Map<String, String> crmPluginParams = crmHelperService.getAllPluginParams(entityId);
            if (crmPluginParams.containsKey(CRM_CLIENT_ID) && crmPluginParams.containsKey(CRM_CLIENT_SECRET) && crmPluginParams.containsKey(BASE_URL)) {
                String auth = crmPluginParams.get(CRM_CLIENT_ID) + ":" + crmPluginParams.get(CRM_CLIENT_SECRET);
                byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
                String authHeader = "Basic " + new String(encodedAuth);
                headers.set(HttpHeaders.AUTHORIZATION, authHeader);

                HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(null, headers);

                String baseUrl = crmPluginParams.get(BASE_URL);
                ResponseEntity<CRMResponse> responseEntity = restTemplate.postForEntity(baseUrl + TOKEN_ENDPOINT, requestEntity, CRMResponse.class);
                Map<String, Object> data = responseEntity.getBody().getData();
                accessToken = (String) data.get("access_token");
            }
        } catch (Exception e) {
            logger.error("Error while fetching access token {}", e.getMessage(), e);
        }
    }

    private boolean isTokenExpired() {
        // TODO Implement token expiration logic by storing the token and expiry time in local db
        return false;
    }
}

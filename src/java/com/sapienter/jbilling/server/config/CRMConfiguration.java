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
package com.sapienter.jbilling.server.config;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import com.sapienter.jbilling.client.crm.CRMAPIClient;
import com.sapienter.jbilling.client.crm.CRMHelperService;
import com.sapienter.jbilling.client.crm.TokenService;

@Configuration
public class CRMConfiguration {

    @Bean(name = "crmRestTemplate")
    public RestTemplate crmRestTemplate() {
        return createRestTemplate();
    }

    private RestTemplate createRestTemplate() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new InsecureTrustManager()};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
            CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setConnectionRequestTimeout(10_000);
            requestFactory.setConnectTimeout(10_000);
            requestFactory.setReadTimeout(10_000);
            requestFactory.setHttpClient(httpClient);

            return new RestTemplate(requestFactory);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to create RestTemplate", e);
        }
    }

    private static class InsecureTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    @Bean(name = "tokenService")
    public TokenService tokenService(RestTemplate crmRestTemplate, CRMHelperService crmHelperService) {
        return new TokenService(crmRestTemplate, crmHelperService);
    }

    @Bean(name = "crmApiClient")
    public CRMAPIClient crmApiClient(TokenService tokenService, RestTemplate crmRestTemplate) {
        return new CRMAPIClient(tokenService, crmRestTemplate);
    }

    @Bean(name = "crmHelperService")
    CRMHelperService crmHelperService() {
        return new CRMHelperService();
    }
}
